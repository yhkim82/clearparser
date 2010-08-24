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

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;

import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.morph.MorphEnAnalyzer;

/**
 * Tree as in Penn Treebank.
 * @author Jinho D. Choi
 * <b>Last update:</b> 8/13/2010
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
		
		return tree;
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
		int count = 0;
		
		for (int i=1; i<tree.size(); i++)
		{
			DepNode node = tree.get(i);
			
			if (node.headId == DepLib.NULL_HEAD_ID)
			{
				node.setHead(DepLib.ROOT_ID, DepLib.DEPREL_ROOT, 0);
				count++;
			}
			else
			{
				if (tree.isAncestor(node.id, node.headId))
					System.err.println("Error: cycle exists.");
			}
		}
		
		if (count > 1)	System.err.println("# of roots: "+count);
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
		findHeads(curr, headrules);
		setCoordination(tree, curr);
	//	setLeftAttachedPunctuation(tree, curr);
		
		reconfigureHead(tree, curr);
		setDepHeadsAux (tree, curr);
	}
	
	/**
	 * Finds heads of all phrases under <code>curr</code> using <code>headrules</code>.
	 * <code>beginId</code> inclusive, <code>endId</code> exclusive.
	 */
	private void findHeads(TBNode curr, TBHeadRules headrules)
	{
		TBHeadRule        headrule = headrules.getHeadRule(curr.getPos());
		ArrayList<TBNode> children = curr.getChildren();
		
		if (children.size() == 1)
		{
			curr.headId = children.get(0).headId;
			return;
		}
		
		if (curr.isPos(TBLib.POS_SBAR))
		{
			TBNode child = children.get(0);
			if (child.isEmptyCategory() && child.isForm("0"))
			{
				curr.headId = child.headId;
				return;
			}
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
		if (child.isRuleMatch(rule) && !TBLib.isPunctuation(child.getPos()))
		{
			curr.headId = child.headId;
			return true;
		}
		
		return false;
	}
	
	/** Reconstructs heads for coordinations. */
	private void setCoordination(DepTree tree, TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		
		for (int i=children.size()-2; i>=0; i--)
		{
			TBNode conj = children.get(i);
			if (!TBLib.isConjunction(conj.getPos()))	continue;
			
			TBNode prev = getPrevConjunct(children, i, false);
			TBNode next = getNextConjunct(children, i, false);
			
			if (prev == null)	break;
			if (next == null)	continue;
			
			if (!setCoordinationAux(tree, curr, conj, prev, next))
			{
				prev = getPrevConjunct(children, i, true);
				next = getNextConjunct(children, i, true);
					
				if (prev == null)	break;
				if (next == null)	continue;
				
				setCoordinationAux(tree, curr, conj, prev, next);
			}
			
			i = prev.childId;
		}
	}
	
	private boolean setCoordinationAux(DepTree tree, TBNode curr, TBNode conj, TBNode prev, TBNode next)
	{
		ArrayList<TBNode> children = curr.getChildren();
		String ppos = prev.getPos();
		String npos = next.getPos();

		if (curr.isPos(TBLib.POS_UCP) ||
			(TBLib.isWordConjunction(conj.getPos()) && next.childId == children.size()-1) || 
			prev.isPos(npos) ||
			(TBLib.isNounLike(ppos) && TBLib.isNounLike(npos)) ||
			(TBLib.isAdjectiveLike(ppos) && TBLib.isAdjectiveLike(npos)) ||
			(curr.isPos(TBLib.POS_WHADVP) && TBLib.isWhAdjectiveLike(ppos) && TBLib.isWhAdjectiveLike(npos)))
		{
			for (int i=prev.childId+1; i<=conj.childId; i++)
			{
				TBNode node = children.get(i);
				setDependency(tree, node.headId, prev.headId);
				if (TBLib.isWordConjunction(node.getPos()))	prev = node;
			}
			
			setDependency(tree, next.headId, prev.headId);
			return true;
		}
		
		return false;
	}
	
	private TBNode getPrevConjunct(ArrayList<TBNode> children, int id, boolean more)
	{
		for (int i=id-1; i>=0; i--)
		{
			TBNode prev = children.get(i);
			String ppos = prev.getPos();
			
			if (!TBLib.isConjunction(ppos) && !TBLib.isPunctuation(ppos) && !(more && isSkip(prev)))
				return prev;
		}
		
		return null;
	}
	
	private TBNode getNextConjunct(ArrayList<TBNode> children, int id, boolean more)
	{
		for (int i=id+1; i<children.size(); i++)
		{
			TBNode next = children.get(i);
			String npos = next.getPos();
			
			if (!TBLib.isConjunction(npos) && !TBLib.isPunctuation(npos) && !(more && isSkip(next)))
				return next;
		}
		
		return null;
	}
	
	private boolean isSkip(TBNode node)
	{
		return node.isPos("ADVP|SBAR|PRN|INTJ|EDITED|META|CODE");
	}
	
	private void setLeftAttachedPunctuation(DepTree tree, TBNode curr)
	{
		if (curr.isPos(TBLib.POS_PRN))	return;
		ArrayList<TBNode> children = curr.getChildren();
		
		for (TBNode child : children)
		{
			if (!TBLib.isLeftAttachedPunctuation(child.getPos()))	continue;
			if (hasHead(tree, child.terminalId))					continue;
			
			if (!setLeftDependency(tree, children, child, DepLib.DEPREL_P, child.childId-1))
			{
				ArrayList<TBNode> siblings = curr.getParent().getChildren();
				setLeftDependency(tree, siblings, child, DepLib.DEPREL_P, curr.childId-1);
			}
		}
	}
	
	private boolean setLeftDependency(DepTree tree, ArrayList<TBNode> nodeList, TBNode child, String deprel, int beginId)
	{
		for (int i=beginId; i>=0; i--)
		{
			TBNode prev = nodeList.get(i);
			
			if (!prev.isEmptyCategory())
			{
				setDependency(tree, child.terminalId, prev.headId, deprel);
				return true;
			}
		}
		
		return false;
	}
	
	private void reconfigureHead(DepTree tree, TBNode curr)
	{
		BitSet  set = curr.getSubTerminalBitSet();
		DepNode tmp = tree.get(curr.headId+1);
		
		while (tmp.hasHead && set.get(tmp.headId))
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
			
			if (child.isRuleMatch(TBLib.POS_EDITED) || child.isRuleMatch(TBLib.POS_META))
				deprel = DepLib.DEPREL_IGNORE;
			if (child.isRuleMatch(TBLib.POS_COMMA) && i-1 >= 0 && children.get(i-1).isRuleMatch(TBLib.POS_EDITED))
				deprel = DepLib.DEPREL_IGNORE;
			else if (TBLib.isSubject(child.pos))
				deprel = DepLib.DEPREL_SBJ;
			else if (tCurr.isRuleMatch(TBLib.POS_TO) && tChild.isRuleMatch(TBLib.POS_VB))
				deprel = DepLib.DEPREL_IM;
			
			setDependency(tree, child.headId, curr.headId, deprel);
		}
	}
	
	/** Assigns the dependency head of the current node. */
	private void setDependency(DepTree tree, int currId, int headId)
	{
		tree.setHead(currId+1, headId+1, "DEP", 1);
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void checkPhrases(Hashtable<String,String> hash)
	{
		checkPhrases(nd_root, hash);
	}
	
	private void checkPhrases(TBNode curr, Hashtable<String,String> hash)
	{
		if (!curr.isPhrase())	return;
		ArrayList<TBNode> children = curr.getChildren();
		TBNode first = children.get(0);
		TBNode last  = children.get(children.size()-1);
		
		if (curr.isRuleMatch("NP") && curr.countsPos(",") == 1)
		{
	//		if (!curr.containsPos("VB.*") && !curr.containsPos("VP") && !curr.containsPos("NN.*") && !curr.containsPos("NP") && !curr.containsPos("IN") && !curr.containsPos("PP"))// && !curr.containsPos("NN*") && !curr.containsPos("PDT") && !curr.containsPos("DT"))// && !curr.containsPos("CD") && !curr.containsPos("IN"))
	//		if (curr.containsPos("PP"))
				hash.put(curr.toAllPos().trim(), curr.toAllWords().trim());
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
		}*/
	}
	
	
}
