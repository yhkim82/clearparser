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
			node.lemma = morph.getLemma(curr.form, curr.pos);
			
			tree.add(node);
		}
	}

	private void setDepHeads(DepTree tree, TBNode curr, TBHeadRules headrules)
	{
		if (!curr.isPhrase())			return;
		
		// traverse all subtrees
		for (TBNode child : curr.getChildren())
			setDepHeads(tree, child, headrules);

		// top-level constituent
		if (curr.isRuleMatch(TBLib.POS_TOP))	return;
		
		// find heads of all subtrees
		findHeads(curr, headrules);
	//	setCoord(tree, curr);
	//	setHyphs(tree);
		
		// reconfigure the head ID
		reconfigureHead(tree, curr);
		
		setDepHeads(tree, curr);
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
	
	/** Finds heads of all phrases under <code>curr</code> using <code>headrules</code>. */
	private void findHeads(TBNode curr, TBHeadRules headrules)
	{
		TBHeadRule        headrule = headrules.getHeadRule(curr.getPos());
		ArrayList<TBNode> children = curr.getChildren();
		
		if (curr.isRuleMatch(TBLib.POS_SBAR))
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
	private void setCoord(DepTree tree, TBNode curr)
	{
		ArrayList<TBNode> children = curr.getChildren();
		
		// coordinating conjunctions
		for (int i=children.size()-2; i>0; i--)
		{
			TBNode child = children.get(i);
			String cpos  = child.getPos();
			int    off   = 0;
			
			if (!TBLib.isWordConjunction(cpos) && !TBLib.isPuncConjunction(cpos))
				continue;
			
			TBNode prev = children.get(i-1);
			TBNode next = children.get(i+1);
			
			if (TBLib.isHyphen(prev.pos) && TBLib.isHyphen(next.pos))
			{
				if (i-2 >= 0 && i+2 < children.size())
				{
					prev = children.get(i-2);
					next = children.get(i+2);
					off++;
				}
				else	continue;
			}
			
			String ppos = prev.getPos();
			
			if (TBLib.isPuncConjunction(ppos) && TBLib.isWordConjunction(cpos))		// , and
			{
				setDependency(tree, prev.headId, child.headId, DepLib.DEPREL_CMOD);
				
				if (i-2 >= 0)
				{
					prev = children.get(i-2);
					off++;
				}
				else
					break;
			}
			
			if (!setCoordination(tree, curr, child, prev, next) && i+2 < children.size())
			{
				TBNode next2 = children.get(i+2);
				
				if (next.isRuleMatch(TBLib.POS_ADVP) && setCoordination(tree, curr, child, prev, next2))
				{
					String pos    = ls_terminal.get(next2.headId).pos;
					String deprel = DepLib.DEPREL_DEP;
					
					if      (TBLib.isVerb(pos))			deprel = DepLib.DEPREL_ADV;
					else if (TBLib.isNoun(pos))			deprel = DepLib.DEPREL_NMOD;
					else if (TBLib.isAdjective(pos))	deprel = DepLib.DEPREL_AMOD;
					else if (TBLib.isPreposition(pos))	deprel = DepLib.DEPREL_PMOD;
					else if (TBLib.isCardinal(pos))		deprel = DepLib.DEPREL_QMOD;
				
					setDependency(tree, next.headId, next2.headId, deprel);
				}
			}
			
			i -= off;
		}

		// conjunction is the first child
		TBNode child = children.get(0);
		String cpos  = child.getPos();
		
		if (TBLib.isWordConjunction(cpos) && children.size() > 1)
		{
			if (TBLib.isCorrelativeConjunction(child.toAllWords()))
			{
				TBNode next = children.get(1);
				setDependency(tree, next.headId, child.headId, DepLib.DEPREL_CONJ);
			}
			else if (curr.isRuleMatch(TBLib.POS_QP))
			{
				for (int i=1; i<children.size(); i++)
				{
					TBNode next = children.get(i);
					if (next.headId != curr.headId)
						setDependency(tree, next.headId, curr.headId, DepLib.DEPREL_QMOD);
				}
				
				setDependency(tree, curr.headId, child.headId, DepLib.DEPREL_CONJ);
				if (child.terminalId > 0)
					setDependency(tree, child.headId, child.terminalId-1, DepLib.DEPREL_COORD);
				else
					System.err.println("QP: no coord");
			}
		}
	}
	
	private boolean setCoordination(DepTree tree, TBNode curr, TBNode child, TBNode prev, TBNode next)
	{
		String ppos = prev.getPos();
		String npos = next.getPos();
		
		if (curr.isRuleMatch(TBLib.POS_UCP) ||
			prev.isRuleMatch(npos) ||
			(TBLib.isNounLike(ppos) && TBLib.isNounLike(npos)) ||
			(TBLib.isAdjectiveLike(ppos) && TBLib.isAdjectiveLike(npos)) ||
			(curr.isRuleMatch(TBLib.POS_WHADVP) && TBLib.isWhAdjectiveLike(ppos) && TBLib.isWhAdjectiveLike(npos)))
		{
			setDependency(tree, child.headId, prev .headId, DepLib.DEPREL_COORD);
			setDependency(tree, next .headId, child.headId, DepLib.DEPREL_CONJ);
			
			return true;
		}

		return false;
	}
	
	private void setHyphs(DepTree tree)
	{
		for (int hyphId=1; hyphId<ls_terminal.size(); hyphId++)
		{
			TBNode hyph = ls_terminal.get(hyphId);
			if (!TBLib.isHyphen(hyph.pos))	continue;
			
			for (int prevId=hyphId-1; prevId >= 0; prevId--)
			{
				TBNode prev = ls_terminal.get(prevId);
				
				if (!prev.isEmptyCategory())
				{
					setDependency(tree, hyphId, prevId, DepLib.DEPREL_HYPH);
					break;
				}
			}
		}
	}
	
	private void reconfigureHead(DepTree tree, TBNode curr)
	{
		BitSet  set = curr.getSubTerminalBitSet();
		DepNode tmp = tree.get(curr.headId+1);
		
		while (tmp.hasHead && set.get(tmp.headId))
			tmp = tree.get(tmp.headId);
			
		curr.headId = tmp.id - 1;
	}

	private void setDepHeads(DepTree tree, TBNode curr)
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
		
		if (curr.isRuleMatch("FRAG") && children.size() > 1)
		{
		//	if (!curr.containsPos("NN*") && !curr.containsPos("NP") && !curr.containsPos("PP"))// && !curr.containsPos("JJS") && !curr.containsPos("RBS") && !curr.containsPos("CD") && !curr.containsPos("NN*") && !curr.containsPos("PDT") && !curr.containsPos("DT"))// && !curr.containsPos("CD") && !curr.containsPos("IN"))
			if (curr.containsPos("S"))
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
