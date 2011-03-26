/**
* Copyright (c) 2010, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package clear.treebank;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

import clear.dep.DepLib;
import clear.morph.MorphEnAnalyzer;
import clear.propbank.PBLib;
import clear.srl.SRLLib;
import clear.srl.SRLNode;
import clear.srl.SRLTree;

import com.carrotsearch.hppc.FloatFloatOpenHashMap;

/**
 * Treebank + Propbank -> SrlTree.
 * @author Jinho D. Choi
 * <b>Last update:</b> 1/19/2011
 */
public class TBPBEnConvert
{
	private TBTree  p_tree;
	private SRLTree s_tree;
	
	boolean b_reverseVC = false;
	boolean b_funcTag   = true;
	boolean b_includeEC = false;
	
	/** @return a semantic role labeling tree converted from <code>pTree</code>. */
	public SRLTree toSrlTree(TBTree pTree, TBHeadRules headrules, MorphEnAnalyzer morph)
	{
		p_tree = pTree;
		s_tree = new SRLTree();
		
		reconfigureAntecedents();
		initSrlTree(morph, pTree.getRootNode());
		setDepHeads(pTree.getRootNode(), headrules);
		setDepRoot();
			
		remapEmptyCategory();
		p_tree.mapSRLTree(s_tree);
		SRLTree copy = removeEmptyCategories();
		copy.projectizePunc();
		copy.checkTree();
		
		return copy;
	}
	
	/** Initializes {@link TBPBEnConvert#s_tree} using the subtree of <code>curr</code>. */
	private void initSrlTree(MorphEnAnalyzer morph, TBNode curr)
	{
		if (curr.isPhrase())
		{
			for (TBNode child : curr.getChildren())
				initSrlTree(morph, child);
		}
		else
		{
			SRLNode node = new SRLNode();

			node.id    = curr.terminalId + 1;
			node.form  = curr.form;
			node.pos   = curr.pos;
			node.lemma = (morph != null) ? morph.getLemma(curr.form, curr.pos) : node.form.toLowerCase();
			
			s_tree.add(node);
		}
	}
	
	/** Finds heads for all phrases. */
	private void setDepHeads(TBNode curr, TBHeadRules headrules)
	{
		if (!curr.isPhrase())	return;
		
		// traverse all subtrees
		for (TBNode child : curr.getChildren())
			setDepHeads(child, headrules);

		// top-level constituent
		if (curr.isPos(TBLib.POS_TOP))	return;
		
		// find heads of all subtrees
		findHeads(curr, headrules);
		
		if (isCoordination(curr))
			setCoordination(curr);
		else if (curr.isPos(TBEnLib.POS_NP+"|"+TBEnLib.POS_NX+"|"+TBEnLib.POS_NML))
			setApposition  (curr);
		
		setGap(curr);		
		reconfigureHead(curr);
		setDepHeadsAux (curr);
	}
	
	/**
	 * Finds heads of all phrases under <code>curr</code> using <code>headrules</code>.
	 * <code>beginId</code> inclusive, <code>endId</code> exclusive.
	 */
	private void findHeads(TBNode curr, TBHeadRules headrules)
	{
		TBHeadRule        headrule = headrules.getHeadRule(curr.pos);
		ArrayList<TBNode> children = curr.getChildren();
		
		if (children.size() == 1)
		{
			curr.headId = children.get(0).headId;
			return;
		}
		
		for (String rule : headrule.rules)
		{
			if (headrule.dir == -1)
			{
				for (int i=0; i<children.size(); i++)
					if (findHeadsAux(curr, children.get(i), rule))
						return;
			}
			else
			{
				for (int i=children.size()-1; i>=0; i--)
					if (findHeadsAux(curr, children.get(i), rule))
						return;
			}
		}
		
		// head not found (because all children are either empty-category or punctuation
		if (curr.headId < 0)
		{
			if (headrule.dir == -1)
				curr.headId = children.get(0).headId;
			else
				curr.headId = children.get(children.size()-1).headId;
		}
	}
	
	/** This method is called by {@link TBPBEnConvert#findHeads(TBNode, TBHeadRules)} and {@link TBPBEnConvert#findGapHeads(TBNode, TBHeadRules)}. */
	private boolean findHeadsAux(TBNode curr, TBNode child, String rule)
	{
		if (curr.isPos(TBEnLib.POS_NP) && (child.isTag(TBEnLib.TAG_BNF) || child.isTag(TBEnLib.TAG_DIR) || child.isTag(TBEnLib.TAG_LOC) || child.isTag(TBEnLib.TAG_MNR) || child.isTag(TBEnLib.TAG_PRP) || child.isTag(TBEnLib.TAG_TMP)))
			return false;
		
		if (child.isRule(rule) && !TBEnLib.isPunctuation(child.pos) && !child.isEmptyCategoryRec() && !child.isPos(TBEnLib.POS_EDITED) && !isAuxMod(curr, child))
		{
			curr.headId = child.headId;
			return true;
		}
		
		return false;
	}
	
	private boolean isAuxMod(TBNode curr, TBNode child)
	{
		if (child.form != null && (TBEnLib.isAux(child.form) || child.isPos(TBEnLib.POS_MD)))
		{
			ArrayList<TBNode> children = curr.getChildren();
			
			for (int i=child.childId+1; i<children.size(); i++)
				if (children.get(i).isPos(TBEnLib.POS_VP))	return true;
		}
		
		return false;
	}
	
	/** @return true if <code>curr</code> consists of coordination structure. */
	private boolean isCoordination(TBNode curr)
	{
		return curr.isPos(TBEnLib.POS_UCP) || curr.containsPos(TBEnLib.POS_CC) || curr.containsPos(TBEnLib.POS_CONJP) || curr.containsTag(TBEnLib.TAG_ETC);
	}
	
	/** Reconstructs heads for coordinations. */
	private void setCoordination(TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		
		for (int i=children.size()-2; i>=0; i--)
		{
			TBNode conj = children.get(i);
			if (!TBEnLib.isConjunction(conj.pos))	continue;
			
			TBNode prev = getConjunct(children, i, false, -1);
			TBNode next = getConjunct(children, i, false,  1);
			
			if (prev == null)	break;
			if (next == null)	continue;
			
			if (!setCoordinationAux(curr, conj, prev, next))
			{
				prev = getConjunct(children, i, true, -1);
				next = getConjunct(children, i, true,  1);
					
				if (prev == null)	break;
				if (next == null)	continue;
				
				setCoordinationAux(curr, conj, prev, next);
			}
			
			i = prev.childId;
		}
	}
	
	private TBNode getConjunct(ArrayList<TBNode> children, int id, boolean more, int dir)
	{
		String skip1 = TBEnLib.POS_PRN+"|"+TBEnLib.POS_INTJ+"|"+TBEnLib.POS_EDITED+"|"+TBEnLib.POS_META+"|"+TBEnLib.POS_CODE;
		String skip2 = TBEnLib.POS_ADVP+"|"+TBEnLib.POS_SBAR;
		
		for (int i=id+dir; 0<=i && i<children.size(); i+=dir)
		{
			TBNode node = children.get(i);
			
			if (!TBEnLib.isConjunction(node.pos) &&
				!TBEnLib.isPunctuation(node.pos) &&
				!node.isEmptyCategoryRec() &&
				!node.isPos(skip1) && !(more && node.isPos(skip2)))
				return node;
		}
		
		return null;
	}
	
	/** Set dependencies for coordination structure. */
	private boolean setCoordinationAux(TBNode curr, TBNode conj, TBNode prev, TBNode next)
	{
		ArrayList<TBNode> children = curr.getChildren();

		if (curr.isPos(TBEnLib.POS_UCP) ||
			prev.isPos(next.pos) ||
			next.isTag(TBEnLib.TAG_ETC) ||
			(TBEnLib.isWordConjunction(conj.pos) && next.childId == children.size()-1) || 
			(TBEnLib.isNounLike(prev.pos)        && TBEnLib.isNounLike(next.pos)) ||
			(TBEnLib.isAdjectiveLike(prev.pos)   && TBEnLib.isAdjectiveLike(next.pos)) ||
			(curr.isPos(TBEnLib.POS_WHADVP)      && TBEnLib.isWhAdverbLike(prev.pos) && TBEnLib.isWhAdverbLike(next.pos)))
		{
			for (int i=prev.childId+1; i<=conj.childId; i++)
			{
				TBNode node = children.get(i);
				setDependency(node.headId, prev.headId, getDeprel(curr, node));
				if (TBEnLib.isWordConjunction(node.pos))	prev = node;
			}
			
			for (int i=conj.childId+1; i<=next.childId-1; i++)
			{
				TBNode node = children.get(i);
				setDependency(node.headId, next.headId, getDeprel(curr, node));
			}
			
			SRLNode dNode = s_tree.get(next.headId+1);
			
			if (dNode.isDeprel(DepLib.DEPREL_GAP+".*"))
			{
				if (TBEnLib.isWordConjunction(prev.pos))
				{
					setDependency(prev.headId, (int)dNode.getDepHeadId()-1, DepLib.DEPREL_COORD);
					setDependency(next.headId, prev.headId                , dNode.getDeprel());
				}
			}
			else
				setDependency(next.headId, prev.headId, DepLib.DEPREL_CONJ);
			
			return true;
		}
		
		return false;
	}
	
	private void setGap(TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		
		outer : for (int i=children.size()-1; i>=0; i--)
		{
			TBNode child = children.get(i);
			if (child.gapIndex == -1)	continue;
			
			for (int j=i-1; j>=0; j--)
			{
				TBNode head = children.get(j);
				
				if (head.coIndex == child.gapIndex || head.gapIndex == child.gapIndex)
				{
					SRLNode dNode = s_tree.get(child.headId+1);
					
					if (dNode.isDeprel(DepLib.DEPREL_CONJ))
						dNode.dHead.setLabel(DepLib.DEPREL_GAP);
					else
						setDependency(child.headId, head.headId, DepLib.DEPREL_GAP);
					
					continue outer;
				}
			}
			
			ArrayList<TBNode> siblings = curr.getParent().getChildren();
			
			for (int j=curr.childId-1; j>=0; j--)
			{
				TBNode head;
				
				if ((head = siblings.get(j).getGapNode(child.gapIndex)) != null)
				{
					String deprel = getTagDeprel(p_tree.getTerminalNodes().get(curr.headId).getParent());
					
					if (deprel == null || !isFuncTag(deprel))
						deprel = "";
					else
						deprel = "-"+deprel;
					
					setDependency(curr.headId, head.headId, DepLib.DEPREL_GAP+deprel);
					return;
				}
			}
		}
	}
	
	private void setApposition(TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		
		for (int i=children.size()-3; i>=0; i--)
		{
			TBNode fst = children.get(i);
			TBNode mid = children.get(i+1);
			TBNode lst = children.get(i+2);
			
			if (fst.isPos(TBEnLib.POS_NP) && mid.isPos(TBEnLib.POS_COMMA) && lst.isPos(TBEnLib.POS_NP))
				setDependency(lst.headId, fst.headId, DepLib.DEPREL_APPO);
		}
	}
	
	/** Finds the head node within the subtree of <code>curr</code>. */
	private void reconfigureHead(TBNode curr)
	{
		BitSet  set = curr.getSubTerminalBitSet();
		SRLNode tmp = s_tree.get(curr.headId+1);
		
		while (tmp.hasDepHead() && set.get((int)tmp.getDepHeadId()-1))
			tmp = s_tree.get(tmp.getDepHeadId());
			
		curr.headId = (int)tmp.id - 1;
	}

	private void setDepHeadsAux(TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		TBNode child, prev;
		int i, j;
		
		outer: for (i=0; i<children.size(); i++)
		{
			child = children.get(i);
			
			if (child.headId == curr.headId)	continue;
			if (hasHead(child.headId))			continue;
		
			if (child.isPos(TBEnLib.POS_HYPH))
			{
				for (j=i-1; j>=0; j--)
				{
					prev = children.get(j);
					if (!prev.isEmptyCategoryRec() && !TBEnLib.isPunctuation(prev.pos))
					{
						setDependency(child.headId, prev.headId, DepLib.DEPREL_P);
						continue outer;
					}
				}
			}
			
			String deprel = getDeprel(curr, child);
			setDependency(child.headId, curr.headId, deprel);
		}
	}
	
	/** Assigns the root of the dependency tree. */
	private void setDepRoot()
	{
		SRLNode node = s_tree.getRootNode();
		
		while (node.nextNode != null)
		{
			node = node.nextNode;
			
			if (node.dHead.headId == DepLib.NULL_HEAD_ID)
				node.setDepHead(DepLib.ROOT_ID, DepLib.DEPREL_ROOT);
		}
	}
	
	private String getDeprel(TBNode parent, TBNode child)
	{
		String deprel;
		
		TBNode p = p_tree.getTerminalNodes().get(parent.headId);
		TBNode c = p_tree.getTerminalNodes().get(child .headId);
		
		if ((deprel = getTagDeprel(child)) != null)
		{
			TBNode tNode = getTagNode(parent, p, TBEnLib.TAG_SBJ);
			
			if (deprel.equals(TBEnLib.TAG_PRD) && tNode != null)
				return DepLib.DEPREL_OPRD;
			
			if (isFuncTag(deprel))	return deprel;
		}
		if ((deprel = getObjectDeprel(parent, child, c)) != null)
			return deprel;
		if (TBEnLib.isWordConjunction(child.pos))
			return DepLib.DEPREL_COORD;
		if (TBEnLib.isPunctuation(child.pos))
			return DepLib.DEPREL_P;
		if (child.isPos(TBEnLib.POS_PRN+"|"+TBEnLib.POS_META))
			return child.pos;
		if ((parent.isPos(TBEnLib.POS_VP) || TBEnLib.isSentence(parent.pos)) && child.isPos(TBEnLib.POS_PP+"|"+TBEnLib.POS_ADVP+"|"+TBEnLib.POS_SBAR+"|"+TBEnLib.POS_RB))
			return DepLib.DEPREL_ADV;
		if (parent.isPos(TBEnLib.POS_VP) && (child.isPos(TBEnLib.POS_PRT) || c.isPos(TBEnLib.POS_RP)))
			return DepLib.DEPREL_PRT;
		if (p.isPos(TBEnLib.POS_TO) && child.isPos(TBEnLib.POS_VP))
			return DepLib.DEPREL_IM;
		if (p.isPos(TBEnLib.POS_VB) && c.isPos(TBEnLib.POS_TO))		// when VC is reversed
			return DepLib.DEPREL_IM;
		if (b_reverseVC && TBEnLib.isAux(c.form) && p.isPos("VB.*"))
			return DepLib.DEPREL_AUX;
		if (b_reverseVC && c.isPos(TBEnLib.POS_MD) && p.isPos("VB.*"))
			return DepLib.DEPREL_MOD;
		if (parent.isPos(TBEnLib.POS_VP+"|"+TBEnLib.POS_SQ+"|"+TBEnLib.POS_SINV) && child.isPos(TBEnLib.POS_VP) && p_tree.getTerminalNode(child.headId).isPos("VB.*"))
			return DepLib.DEPREL_VC;
		if (parent.isPos(TBEnLib.POS_SBAR) && p.isPos(TBEnLib.POS_IN+"|"+TBEnLib.POS_TO+"|"+TBEnLib.POS_DT))
			return DepLib.DEPREL_SUB;
		if (parent.isPos(TBEnLib.POS_NP+"|"+TBEnLib.POS_NX+"|"+TBEnLib.POS_NAC+"|"+TBEnLib.POS_NML+"|"+TBEnLib.POS_WHNP))
			return DepLib.DEPREL_NMOD;
		if (parent.isPos(TBEnLib.POS_ADJP+"|"+TBEnLib.POS_ADVP+"|"+TBEnLib.POS_WHADJP+"|"+TBEnLib.POS_WHADVP))
			return DepLib.DEPREL_AMOD;
		if (parent.isPos(TBEnLib.POS_PP+"|"+TBEnLib.POS_WHPP))
			return DepLib.DEPREL_PMOD;
		if (parent.isPos(TBEnLib.POS_QP))
			return DepLib.DEPREL_QMOD;
		if (child.isPos(TBEnLib.POS_INTJ) || c.isPos(TBEnLib.POS_UH))
			return DepLib.DEPREL_INTJ;
		if (child.isPos(TBEnLib.POS_EDITED))
			return DepLib.DEPREL_EDIT;
		if (child.isPos(TBEnLib.POS_CIT))
			return DepLib.DEPREL_CIT;
		if (child.isPos(TBEnLib.POS_ADVP) || c.isPos(TBEnLib.POS_RB))
			return DepLib.DEPREL_ADV;
		if (TBEnLib.isNounLike(parent.pos))
			return DepLib.DEPREL_NMOD;
		
		return DepLib.DEPREL_DEP;
	}
	
	private boolean isFuncTag(String deprel)
	{
		return b_funcTag || deprel.matches(TBEnLib.TAG_SBJ+"|"+TBEnLib.TAG_LGS);
	}
	
	private String getTagDeprel(TBNode child)
	{
		if (child.isTag(TBEnLib.TAG_SBJ))										return DepLib.DEPREL_SBJ;
		if (child.isPos(TBEnLib.POS_PP) && child.containsTag(TBEnLib.TAG_LGS))	return DepLib.DEPREL_LGS;
		if (child.isTag(TBEnLib.TAG_DTV))										return DepLib.DEPREL_DTV;
	//	if (child.isTag(TBEnLib.TAG_CLF))										return DepLib.DEPREL_CLF;
		if (child.isTag(TBEnLib.TAG_EXT))										return DepLib.DEPREL_EXT;
		if (child.isTag(TBEnLib.TAG_LOC))										return DepLib.DEPREL_LOC;
		if (child.isTag(TBEnLib.TAG_TMP))										return DepLib.DEPREL_TMP;
		if (child.isPos(TBEnLib.POS_PP) && child.isTag(TBEnLib.TAG_BNF))		return DepLib.DEPREL_BNF;
		if (child.isTag(TBEnLib.TAG_DIR))										return DepLib.DEPREL_DIR;
		if (child.isTag(TBEnLib.TAG_MNR))										return DepLib.DEPREL_MNR;
		if (child.isTag(TBEnLib.TAG_PRP))										return DepLib.DEPREL_PRP;
		if (child.isTag(TBEnLib.TAG_SEZ))										return DepLib.DEPREL_SEZ;
		if (child.isTag(TBEnLib.TAG_VOC))										return DepLib.DEPREL_VOC;
		if (child.isTag(TBEnLib.TAG_PRD))										return DepLib.DEPREL_PRD;
		if (child.isTag(TBEnLib.TAG_ADV))										return DepLib.DEPREL_ADV;

		return null;
	}
	
	private String getObjectDeprel(TBNode parent, TBNode child, TBNode c)
	{
		if (!parent.isPos(TBEnLib.POS_VP))	return null;
		
		String deprel = getObjectDeprelAux(child, c);
		if (deprel != null)	return deprel;
		
		if (child.isPos(TBEnLib.POS_UCP))
		{
			deprel = getObjectDeprelAux(child, c);
			if (deprel != null)	return deprel;
		}
		
		return null;
	}
	
	private String getObjectDeprelAux(TBNode child, TBNode c)
	{
		if (child.isPos(TBEnLib.POS_NP) ||
			child.isPos(TBEnLib.POS_SBAR) && !(c.form.toLowerCase().matches("as|because|for|since|with")) ||
			child.isPos(TBEnLib.POS_S+"|"+TBEnLib.POS_SQ+"|"+TBEnLib.POS_SINV+"|"+TBEnLib.POS_SBARQ))
		{
			TBNode tNode = getTagNode(child, c, TBEnLib.TAG_PRD);
			
			if (tNode != null)
				return DepLib.DEPREL_OPRD;
			
			return c.isPos(TBEnLib.POS_TO+"|"+TBEnLib.POS_VBG+"|"+TBEnLib.POS_VBN) ? DepLib.DEPREL_OPRD : DepLib.DEPREL_OBJ;
		}
		
		return null;
	}
	
	private TBNode getTagNode(TBNode root, TBNode c, String tag)
	{
		if (c.isTag(tag))	return c;
		
		TBNode parent = c.getParent();
		
		while (parent != null && !parent.equals(root))
		{
			if (parent.isTag(tag))	return parent;
			parent = parent.getParent();
		}

		return null;
	}
	
	/** Redirects empty categories' antecedents. */
	private void remapEmptyCategory()
	{
		HashSet<String> sRNR = new HashSet<String>();
		float[] ids = s_tree.getOrderedIDs();
		SRLNode ec;		
		
		for (int i=ids.length-1; i>0; i--)
		{
			ec = s_tree.get(ids[i]);
			
			// checks for empty categories
			if (!ec.form.startsWith(TBEnLib.EC_EXP) &&
				!ec.form.startsWith(TBEnLib.EC_ICH) &&
			//	!ec.form.startsWith(TBEnLib.EC_PPA) &&
				!ec.form.startsWith(TBEnLib.EC_RNR) && 
				!ec.form.startsWith(TBEnLib.EC_TRACE))	continue;
			
			// checks if there is co-index
			String[] tmp = ec.form.split("-");
			if (tmp.length <= 1 || !tmp[1].matches("\\d*"))	continue;
			
			// finds its antecedent
			int coIndex = Integer.parseInt(tmp[1]);
			TBNode antecedent = p_tree.getAntecedent(coIndex);
			if (antecedent == null)	continue;
						
			SRLNode ante = s_tree.get(antecedent.headId+1);
			if (ante.isPos(TBLib.POS_NONE))		continue;
			if (ante.id == ec.getDepHeadId())	continue;
			
			if (ec.form.startsWith(TBEnLib.EC_EXP))
			{
				ante.dHead.label = DepLib.DEPREL_EXTR;
				continue;
			}
			
			if (ec.form.startsWith(TBEnLib.EC_RNR))
			{
				if (sRNR.contains(ec.form))	continue;
				sRNR.add(ec.form);
			}
			
			if (s_tree.isAncestor(ante.id, ec.getDepHeadId()))
			{
				if (ec.form.startsWith(TBEnLib.EC_RNR))
				{
					for (SRLNode node : s_tree.getDependents(ante.id))
					{
						if (node.id == ec.getDepHeadId() || s_tree.isAncestor(node.id, ec.getDepHeadId()))
						{
							node.setDepHead(ante.getDepHeadId(), ante.getDeprel(), 1);
							break;
						}
					}
				}
				else
				{
					SRLNode head = s_tree.get(ec.getDepHeadId());
					
					while (head.getDepHeadId() != ante.id)
						head = s_tree.get(head.getDepHeadId());
					
					head.setDepHead(ante.getDepHeadId(), ante.getDeprel(), 1);
				}
			}
			
			String deprel = ec.getDeprel();
			while (ec.hasDepHead() && s_tree.get(ec.getDepHeadId()).isPos(TBEnLib.POS_NONE))
				ec = s_tree.get(ec.getDepHeadId());
			
			ante.setDepHead(ec.getDepHeadId(), deprel, 1);
		}
	}
	
	private void reconfigureAntecedents()
	{
		ArrayList<TBNode> terminalNodes = p_tree.getTerminalNodes();
		TBNode node, ante;
		
		for (int i=0; i<terminalNodes.size(); i++)
		{
			node = terminalNodes.get(i);
			if (!node.hasAntecedent())	continue;
			
			ante = node.antecedent;
			
			while (ante.isEmptyCategoryRec())
			{
				if (ante.isPhrase())
					ante = p_tree.getTerminalNode(ante.pbLoc.terminalId);
				
				if (ante.hasAntecedent())
					ante = ante.antecedent;
				else
					break;
			}
			
			node.antecedent = ante;
		}
	}
	
	/**
	 * Removes all empty categories from <code>tree</code>.
	 * @return dependency tree without empty categories.
	 */
	private SRLTree removeEmptyCategories()
	{
		FloatFloatOpenHashMap map = new FloatFloatOpenHashMap();
		SRLNode sNode = s_tree.getRootNode();
		TBNode  tNode;
		
		float newId = SRLLib.ROOT_ID;
		map.put(SRLLib.ROOT_ID, SRLLib.ROOT_ID);
		
		while (sNode.nextNode != null)
		{
			sNode = sNode.nextNode;
			tNode = p_tree.getTerminalNode((int)sNode.id-1);
			
			if (!sNode.isPos(TBLib.POS_NONE))
			{
				newId = (float)Math.floor(newId + 1);
				map.put(sNode.id, newId);
			}
			else if (isDroppedArgument(tNode))
			{
				newId += 0.1;
				map.put(sNode.id, newId);
			}
		}
		
		SRLTree copy = new SRLTree();
		sNode = s_tree.getRootNode();
		
		while (sNode.nextNode != null)
		{
			sNode = sNode.nextNode;
			tNode = p_tree.getTerminalNode((int)sNode.id-1);
			
			if (!sNode.isPos(TBLib.POS_NONE) || isDroppedArgument(tNode))
			{
				if (!processComplementizer(sNode))
					processEmptyCategory(sNode);
				
				sNode.remapIDs(map);
				copy.add(sNode);
			}
		}
		
		return copy;
	}
	
	private boolean isDroppedArgument(TBNode node)
	{
		if (!b_includeEC)	return false;
		TBNode next = p_tree.getTerminalNode(node.terminalId+1);
		
		if (node.isForm("0") && next != null && !next.isPos(TBEnLib.POS_NONE))
			return node.hasAntecedent();
		
		if (node.isForm("\\*PRO\\*.*|\\*|\\*-\\d") && node.getParent().isFollowedBy("VP"))
			return true;
		
		return false;
	}
	
	private boolean processComplementizer(SRLNode sNode)
	{
		TBNode tNode = p_tree.getTerminalNode((int)sNode.id-1);
		
		if (!tNode.hasAntecedent())
			return false;
		if (tNode.pbLoc.type == null || !tNode.pbLoc.isType(PBLib.PROP_OP_COMP))
			return false;
		
		sNode.anteId = tNode.antecedent.headId + 1;
		return true;
	}
	
	private void processEmptyCategory(SRLNode sNode)
	{
		if (!sNode.isPos(TBEnLib.POS_NONE))	return;
		
		int lastIndex = sNode.form.lastIndexOf("-");
		
		if (lastIndex != -1)
			sNode.form = sNode.form.substring(0, lastIndex);
		
		sNode.lemma = sNode.form;
		
		TBNode tNode = p_tree.getTerminalNode((int)sNode.id-1);
		
		if (tNode.hasAntecedent())
			sNode.anteId = tNode.antecedent.headId + 1;
	}
	
	/** Assigns the dependency head of the current node. */
	private void setDependency(int currId, int headId, String deprel)
	{
		s_tree.setDepHead(currId+1, headId+1, deprel, 1);
	}
	
	/** @return true if the current node already has its dependency head. */
	private boolean hasHead(int currId)
	{
		return s_tree.get(currId+1).hasDepHead();
	}
}
