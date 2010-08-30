/**
* Copyright (c) 2007, Regents of the University of Colorado
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
import java.util.HashMap;
import java.util.HashSet;

import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.morph.MorphEnAnalyzer;

/**
 * Tree as in Penn Treebank.
 * @author Jinho D. Choi
 * <b>Last update:</b> 8/24/2010
 */
public class TBTree
{
	/** Pointer to the root node */
	private TBNode nd_root;
	/** Pointer to the current node */
	private TBNode nd_curr;
	/** Pointer to terminal nodes */
	private ArrayList<TBNode> ls_terminal;
	
	/** Initializes the tree. */
	public TBTree()
	{
		ls_terminal = new ArrayList<TBNode>();
	}
	
	/** Sets the current pointer to <code>root</code>. */
	public void setRoot(TBNode root)
	{
		nd_root = nd_curr = root;
	}
	
	/** Adds a terminal node. */
	public void addTerminal(TBNode node)
	{
		ls_terminal.add(node);
	}
	
	/** Returns the current node. */
	public TBNode getCurrNode()
	{
		return nd_curr;
	}
	
	public TBNode getAntecedent(int coIndex)
	{
		return getAntecedentAux(nd_root, coIndex);
	}
	
	public TBNode getAntecedentAux(TBNode curr, int coIndex)
	{
		if (curr.coIndex == coIndex)	return curr;
		if (!curr.isPhrase())			return null;
		
		for (TBNode child : curr.getChildren())
		{
			TBNode node = getAntecedentAux(child, coIndex);
			if (node != null)	return node;
		}
		
		return null;
	}
	
	public ArrayList<TBNode> getTerminalNodes()
	{
		return ls_terminal;
	}
	
	/** Returns the bitset of terminal indices of the subtree of the current node. */
	public BitSet getSubTerminalIndices()
	{
		return nd_curr.getSubTerminalBitSet();
	}
	
	/**
	 * Returns the bitset of token indices of the subtree of the current node.
	 * Each index gets added by <code>offset</code> (e.g., if <code>offset</code> is 1, [0,1,2] becomes [1,2,3]).
	 */
	public BitSet getSubTokenIndiced(int offset)
	{
		return nd_curr.getSubTokenBitSet(offset);
	}
	
	/**
	 * Moves the current pointer to the <code>height</code>'s ancestor of the <code>terminalIndex</code>'th node.
	 * Returns false if either the terminal-index or the height is out of range; otherwise, returns true.
	 * @see TBTree#moveToTerminal(int)
	 * @see TBTree#moveToAncestor(int)
	 * @param terminalIndex index of the terminal node (starting from 0)
	 * @param height height of the ancestor (0 indicates the current node)
	 */
	public boolean moveTo(int terminalIndex, int height)
	{
		if (moveToTerminal(terminalIndex))
			return moveToAncestor(height);
		
		return false;
	}
	
	/**
	 * Moves the current pointer to <code>terminalIndex</code>'th node.
	 * Returns false if the terminal-index does not exist; otherwise, returns true.
	 * @param terminalIndex index of the terminal node (starting from 0) 
	 */
	public boolean moveToTerminal(int terminalIndex)
	{
		for (TBNode node : ls_terminal)
		{
			if (node.terminalId == terminalIndex)
			{
				nd_curr = node;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Moves the current pointer to its <code>height</code>'th ancestor.
	 * If <code>height <= 0</code>, do nothing.
	 * Returns false if the height is out of range; otherwise, returns true.
	 * @param height height of the ancestor (0 indicates the self node)
	 */
	public boolean moveToAncestor(int height)
	{
		TBNode tmp = nd_curr;
		
		for (; height>0; height--)
		{
			if (tmp.getParent() == null)	return false;
			tmp = tmp.getParent();
		}
			
		nd_curr = tmp;
		return true;
	}
	
	/** @return a dependency tree converted from the phrase structure tree. */
	public DepTree toDepTree(TBHeadRules headrules, MorphEnAnalyzer morph)
	{
		DepTree tree = new DepTree();
		
		initDepTree(morph, tree, nd_root);
		setDepHeads(tree, nd_root, headrules);
		setDepRoot (tree);
		setEmptyCategory(tree);
		DepTree copy = removeEmptyCategories(tree);
		copy.checkTree();
		
		return copy;
	}
	
	/** Initializes <code>tree</code> using the subtree of <code>curr</code>. */
	private void initDepTree(MorphEnAnalyzer morph, DepTree tree, TBNode curr)
	{
		if (curr.isPhrase())
		{
			for (TBNode child : curr.getChildren())
				initDepTree(morph, tree, child);
		}
		else
		{
			DepNode node = new DepNode();

			node.id    = curr.terminalId + 1;
			node.form  = curr.form;
			node.pos   = curr.pos;
			node.lemma = "";
	//		node.lemma = morph.getLemma(curr.form, curr.pos);
			
			tree.add(node);
		}
	}

	/** Assigns the root of the dependency tree. */
	private void setDepRoot(DepTree tree)
	{
		for (int i=1; i<tree.size(); i++)
		{
			DepNode node = tree.get(i);
			
			if (node.headId == DepLib.NULL_HEAD_ID)
				node.setHead(DepLib.ROOT_ID, DepLib.DEPREL_ROOT, 0);
		}
	}
	
	private void setDepHeads(DepTree tree, TBNode curr, TBHeadRules headrules)
	{
		if (!curr.isPhrase())			return;
		
		// traverse all subtrees
		for (TBNode child : curr.getChildren())
			setDepHeads(tree, child, headrules);

		// top-level constituent
		if (curr.isPos(TBLib.POS_TOP))	return;
		
		// find heads of all subtrees
		if (!findGapHeads(curr, headrules))	findHeads(curr, headrules);
		if (isCoordination(curr))			setCoordination(tree, curr);
		setGap(tree, curr);
	//	setLeftAttachedPunctuation(tree, curr);
		
		reconfigureHead(tree, curr);
		setDepHeadsAux (tree, curr);
	}
	
	private boolean findGapHeads(TBNode curr, TBHeadRules headrules)
	{
		if (!curr.isPos(TBLib.POS_VP+"|"+TBLib.POS_S) || !curr.containsGap())	return false;
			
		TBHeadRule        headrule = headrules.getHeadRule(DepLib.DEPREL_GAP);
		ArrayList<TBNode> children = curr.getChildren();
		
		for (String rule : headrule.rules)
		{
			for (int i=0; i<children.size(); i++)
				if (findHeadsAux(curr, children.get(i), rule))
					return true;
		}
		
		return false;
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
		
	/*	if (curr.isPos(TBLib.POS_SBAR))
		{
			TBNode child = children.get(0);
			if (child.isEmptyCategory() && child.isForm("0"))
			{
				curr.headId = child.headId;
				return;
			}
		}*/
		
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
		
		if (curr.headId < 0)
		{
			if (headrule.dir == -1)
				curr.headId = children.get(0).headId;
			else
				curr.headId = children.get(children.size()-1).headId;
		}
	}
	
	/** This method is called by {@link TBTree#findHeads(TBNode, TBHeadRules)}. */
	private boolean findHeadsAux(TBNode curr, TBNode child, String rule)
	{
		if (child.isRule(rule) && !TBLib.isPunctuation(child.pos) && !child.isEmptyCategoryRec())
		{
			curr.headId = child.headId;
			return true;
		}
		
		return false;
	}
	
	/** @return true if <code>curr</code> consists of coordination structure. */
	private boolean isCoordination(TBNode curr)
	{
		return curr.isPos(TBLib.POS_UCP) || curr.containsPos(TBLib.POS_CC) || curr.containsPos(TBLib.POS_CONJP) || curr.containsTag(TBLib.TAG_ETC);
	}
	
	/** Reconstructs heads for coordinations. */
	private void setCoordination(DepTree tree, TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		int lastHeadId = -1;
		
		for (int i=children.size()-2; i>=0; i--)
		{
			TBNode conj = children.get(i);
			if (!TBLib.isConjunction(conj.pos))	continue;
			
			TBNode prev = getConjunct(children, i, false, -1);
			TBNode next = getConjunct(children, i, false,  1);
			
			if (prev == null)
			{
				if (lastHeadId != -1 && TBLib.isCorrelativeConjunction(conj.toWords()))
					setDependency(tree, lastHeadId, conj.headId, DepLib.DEPREL_CONJ);
				break;
			}
			if (next == null)	continue;
			
			if (!setCoordinationAux(tree, curr, conj, prev, next))
			{
				prev = getConjunct(children, i, true, -1);
				next = getConjunct(children, i, true,  1);
					
				if (prev == null)
				{
					if (lastHeadId != -1 && TBLib.isCorrelativeConjunction(conj.toWords()))
						setDependency(tree, lastHeadId, conj.headId, DepLib.DEPREL_CONJ);
					break;
				}
				if (next == null)	continue;
				
				if (setCoordinationAux(tree, curr, conj, prev, next))
					lastHeadId = prev.headId;
			}
			else
				lastHeadId = prev.headId;
			
			i = prev.childId;
		}
	}
	
	/** Set dependencies for coordination structure. */
	private boolean setCoordinationAux(DepTree tree, TBNode curr, TBNode conj, TBNode prev, TBNode next)
	{
		ArrayList<TBNode> children = curr.getChildren();

		if (curr.isPos(TBLib.POS_UCP) ||
			prev.isPos(next.pos) ||
			next.isTag(TBLib.TAG_ETC) ||
			(TBLib.isWordConjunction(conj.pos) && next.childId == children.size()-1) || 
			(TBLib.isNounLike(prev.pos)      && TBLib.isNounLike(next.pos)) ||
			(TBLib.isAdjectiveLike(prev.pos) && TBLib.isAdjectiveLike(next.pos)) ||
			(curr.isPos(TBLib.POS_WHADVP)    && TBLib.isWhAdjectiveLike(prev.pos) && TBLib.isWhAdjectiveLike(next.pos)))
		{
			for (int i=prev.childId+1; i<=conj.childId; i++)
			{
				TBNode node = children.get(i);
				setDependency(tree, node.headId, prev.headId);
				if (TBLib.isWordConjunction(node.pos))	prev = node;
			}
			
			for (int i=conj.childId+1; i<=next.childId-1; i++)
			{
				TBNode node = children.get(i);
				setDependency(tree, node.headId, next.headId);
			}
			
			DepNode dNode = tree.get(next.headId+1);
			
			if (dNode.isDeprel(DepLib.DEPREL_GAP))
			{
				if (TBLib.isWordConjunction(prev.pos))
				{
					setDependency(tree, prev.headId, dNode.headId-1);
					setDependency(tree, next.headId, prev.headId, DepLib.DEPREL_GAP);
				}
			}
			else
				setDependency(tree, next.headId, prev.headId, DepLib.DEPREL_CONJ);
			
			return true;
		}
		
		return false;
	}
	
	private TBNode getConjunct(ArrayList<TBNode> children, int id, boolean more, int dir)
	{
		String skip1 = "PRN|INTJ|EDITED|META|CODE";
		String skip2 = "ADVP|SBAR";
		
		for (int i=id+dir; 0<=i && i<children.size(); i+=dir)
		{
			TBNode node = children.get(i);
			
			if (!TBLib.isConjunction(node.pos) && !TBLib.isPunctuation(node.pos) && !node.isEmptyCategoryRec() && !node.isPos(skip1) && !(more && node.isPos(skip2)))
				return node;
		}
		
		return null;
	}
	
	private void setGap(DepTree tree, TBNode curr)
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
					DepNode dNode = tree.get(child.headId+1);
					
					if (dNode.isDeprel(DepLib.DEPREL_CONJ))
						dNode.deprel = DepLib.DEPREL_GAP;
					else
						setDependency(tree, child.headId, head.headId, DepLib.DEPREL_GAP);
					
					continue outer;
				}
			}
			
			ArrayList<TBNode> siblings = curr.getParent().getChildren();
			
			for (int j=curr.childId-1; j>=0; j--)
			{
				TBNode head;
				
				if ((head = siblings.get(j).getGapNode(child.gapIndex)) != null)
				{
					setDependency(tree, curr.headId, head.headId, DepLib.DEPREL_GAP);
					return;
				}
			}
		}
	}
	
	private void setLeftAttachedPunctuation(DepTree tree, TBNode curr)
	{
		if (curr.isPos(TBLib.POS_PRN))	return;
		ArrayList<TBNode> children = curr.getChildren();
		
		for (TBNode child : children)
		{
			if (!TBLib.isLeftAttachedPunctuation(child.pos))	continue;
			if (hasHead(tree, child.terminalId))				continue;

			for (int i=child.childId-1; i>=0; i--)
			{
				TBNode node = children.get(i);
				
				if (!node.isEmptyCategory() && !TBLib.isPunctuation(node.pos))
				{
					setDependency(tree, child.terminalId, node.headId);
					break;
				}
			}
		}
	}
	
	private void reconfigureHead(DepTree tree, TBNode curr)
	{
		BitSet  set = curr.getSubTerminalBitSet();
		DepNode tmp = tree.get(curr.headId+1);
		
		while (tmp.hasHead && set.get(tmp.headId-1))
			tmp = tree.get(tmp.headId);
			
		curr.headId = tmp.id - 1;
	}

	private void setDepHeadsAux(DepTree tree, TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		
		for (int i=0; i<children.size(); i++)
		{
			TBNode child = children.get(i);
			
			if (child.headId == curr.headId)	continue;
			if (hasHead(tree, child.headId))	continue;
		
			TBNode tCurr  = ls_terminal.get(curr .headId);
			TBNode tChild = ls_terminal.get(child.headId);
			
			String deprel = "DEPREL";
			
			if (child.isRule(TBLib.POS_EDITED) || child.isRule(TBLib.POS_META))
				deprel = DepLib.DEPREL_IGNORE;
			if (child.isRule(TBLib.POS_COMMA) && i-1 >= 0 && children.get(i-1).isRule(TBLib.POS_EDITED))
				deprel = DepLib.DEPREL_IGNORE;
			else if (TBLib.isSubject(child.pos))
				deprel = DepLib.DEPREL_SBJ;
			else if (tCurr.isRule(TBLib.POS_TO) && tChild.isRule(TBLib.POS_VB))
				deprel = DepLib.DEPREL_IM;
			
			setDependency(tree, child.headId, curr.headId, deprel);
		}
	}
	
	
	private void setEmptyCategory(DepTree tree)
	{
		HashSet<String> sRNR = new HashSet<String>();
		
		for (int i=tree.size()-1; i>=0; i--)
		{
			DepNode ec = tree.get(i);
			
			if (!ec.form.startsWith(TBLib.EC_EXP) &&
				!ec.form.startsWith(TBLib.EC_ICH) &&
				!ec.form.startsWith(TBLib.EC_PPA) &&
				!ec.form.startsWith(TBLib.EC_RNR) && 
				!ec.form.startsWith(TBLib.EC_TRACE))	continue;
			
			String[] tmp = ec.form.split("-");
			if (tmp.length <= 1 || !tmp[1].matches("\\d*"))	continue;
			
			int coIndex = Integer.parseInt(tmp[1]);
			TBNode antecedent = getAntecedent(coIndex);
			if (antecedent == null)	return;
			
			DepNode ante = tree.get(antecedent.headId+1);
			if (ante.isPos(TBLib.POS_NONE))	return;
			
			if (ante.id == ec.headId)	return;
			
			if (ec.form.startsWith(TBLib.EC_EXP))
			{
				ante.deprel = DepLib.DEPREL_EXTR;
				continue;
			}
			else if (ec.form.startsWith(TBLib.EC_RNR))
			{
				if (sRNR.contains(ec.form))	continue;
				sRNR.add(ec.form);
			}
			
			if (tree.isAncestor(ante.id, ec.headId))
			{
				for (DepNode node : tree.getDependents(ante.id))
				{
					if (node.id == ec.headId || tree.isAncestor(node.id, ec.headId))
					{
						node.setHead(ante.headId, ante.deprel, 1);
						break;
					}
				}
			}
			
			ante.setHead(ec.headId, ec.deprel, 1);
		}
	}
	
	private DepTree removeEmptyCategories(DepTree tree)
	{
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		for (int i=0,j=0; i<tree.size(); i++)
		{
			DepNode node = tree.get(i);
			map.put(i, j);
			if (!node.isPos(TBLib.POS_NONE))	j++;
		}
		
		DepTree copy = new DepTree();
		
		for (int i=1; i<tree.size(); i++)
		{
			DepNode node = tree.get(i);
			if (!node.isPos(TBLib.POS_NONE))
			{
				node.id = map.get(node.id);
				node.headId = map.get(node.headId);
				copy.add(node);
			}
		}
		
		return copy;
	}
	
	/** Assigns the dependency head of the current node. */
	private void setDependency(DepTree tree, int currId, int headId)
	{
		String deprel = "DEP";
		TBNode node   = ls_terminal.get(currId);
		
		if      (TBLib.isPunctuation(node.pos))	deprel = DepLib.DEPREL_P;
		else if (TBLib.isConjunction(node.pos))	deprel = DepLib.DEPREL_COORD;	
		
		tree.setHead(currId+1, headId+1, deprel, 1);
	}
	
	/** Assigns the dependency head of the current node. */
	private void setDependency(DepTree tree, int currId, int headId, String deprel)
	{
		tree.setHead(currId+1, headId+1, deprel, 1);
	}
	
	/** @return true if the current node already has its dependency head. */
	private boolean hasHead(DepTree tree, int currId)
	{
		return tree.get(currId+1).hasHead;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
/*	public void checkPhrases(Hashtable<String,String> hash)
	{
		checkPhrases(nd_root, hash);
	}
	
	private void checkPhrases(TBNode curr, Hashtable<String,String> hash)
	{
		if (!curr.isPhrase())	return;
		ArrayList<TBNode> children = curr.getChildren();
		TBNode first = children.get(0);
		TBNode last  = children.get(children.size()-1);
		
		if (curr.isRule("NP") && curr.countsPos(",") == 1)
		{
	//		if (!curr.containsPos("VB.*") && !curr.containsPos("VP") && !curr.containsPos("NN.*") && !curr.containsPos("NP") && !curr.containsPos("IN") && !curr.containsPos("PP"))// && !curr.containsPos("NN*") && !curr.containsPos("PDT") && !curr.containsPos("DT"))// && !curr.containsPos("CD") && !curr.containsPos("IN"))
	//		if (curr.containsPos("PP"))
				hash.put(curr.toPostags().trim(), curr.toWords().trim());
		}
		
		for (int i=0; i<children.size(); i++)
		{
			TBNode child = children.get(i);
			checkPhrases(child, hash);
		}
		
	/*	TBNode first = children.get(0);
		TBNode last  = children.get(children.size()-1);
		
		if (curr.isPos("VP") && curr.contains("CC") && !first.isPos("CC") && !last.isPos("CC") && !TBLib.isCorrelativeConjunction(curr.getFirst("CC").toWords()))
		{
			String poss  = curr.toPoss().trim();
			
			if (!poss.replaceFirst("[A-Z]+ CC [A-Z]+", "").equals("") && !poss.endsWith("CC ADVP"))
			{
				boolean isCoord = false;
				for (int i=0; i<children.size(); i++)
				{
					StringBuilder build = new StringBuilder();
					
					for (int j=i; j<children.size(); j++)
					{
						TBNode child = children.get(j);
						build.append(child.getStripedPos());
						build.append(" ");
					}
					
					if (TBLib.isCoordination(build.toString().trim()))
					{
						isCoord = true;
						break;
					}
				}
				
				if (!isCoord)
					hash.put(poss,curr.toWords());
			}
		}*/
		
		// add conjunction phrases to <set>
	/* 	if (curr.isPos("CONJP"))	set.add(curr.toWords());
		
		for (int i=0; i<children.size(); i++)
		{
			TBNode child = children.get(i);
			checkPhrases(child, set);
		}*/
		
		// conjunction is the 1st token in a phrase
	/*	for (int i=0; i<children.size(); i++)
		{
			TBNode child = children.get(i);
			
		//	if (child.isPos(TBLib.POS_CC) && (i == 0) && child.terminalId != 0 && !child.isForm("both") && !child.isForm("neither") && !child.isForm("either") && !curr.isPos("QP") && !curr.isPos("ADVP") && !curr.isPos("FRAG") && !curr.isPos("NAC"))
			if (child.isPos(TBLib.POS_CC) && (i == 0) && curr.isPos("QP"))
				System.out.println((child.toWords())+"\n"+nd_root.toWords()+"\n");
			
			checkPhrases(child, set);
		}*/
		
		// conjunction is the last token in a phrase
	/*	for (int i=0; i<children.size(); i++)
		{
			TBNode child = children.get(i);
			
			if (child.isPos(TBLib.POS_CC) && (i == children.size()-1))
				System.out.println((child.toWords())+"\n"+nd_root.toWords()+"\n");
			
			checkPhrases(child, set);
		}*/
		
	/*	for (int i=0; i<children.size(); i++)
		{
			TBNode child = children.get(i);
			
			if (i > 0 && i < children.size()-1)
			{
				TBNode prev  = children.get(i-1);
				TBNode next  = children.get(i+1);
				
				if (child.isPos(TBLib.POS_CC) && !prev.isPos(next.getStripedPos()))
				{
					if (prev.isPos("JJ*") && next.isPos("JJ*"))
				//	if (prev.isPos("NN*") && next.pos.contains("NOM"))
				//	if (prev.pos.contains("NOM") && next.isPos("NN*"))
				//	if (prev.pos.contains("NOM") && next.isPos("NN*"))
						System.out.println(prev.toWords()+" | "+child.toWords()+" | "+next.toWords()+"\n"+nd_root.toWords()+"\n");
				}
			}
			
			checkPhrases(child, set);
		}
	}*/
}
