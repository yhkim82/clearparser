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

/**
 * Tree as in Penn Treebank.
 * @author Jinho D. Choi
 * <b>Last update:</b> 9/1/2010
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
	
	/** @return the root node. */
	public TBNode getRootNode()
	{
		return nd_root;
	}
	
	/** @return the current node. */
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
	
	/** @return list of terminal nodes. */
	public ArrayList<TBNode> getTerminalNodes()
	{
		return ls_terminal;
	}
	
	/** @return the bitset of terminal indices of the subtree of the current node. */
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
	
	public HashSet<String> getAllPos()
	{
		HashSet<String> set = new HashSet<String>();
		
		getAllPosAux(nd_root, set);
		return set;
	}
	
	private void getAllPosAux(TBNode curr, HashSet<String> set)
	{
		set.add(curr.pos);
		
		if (curr.isPhrase())
		{
			for (TBNode child : curr.getChildren())
				getAllPosAux(child, set);
		}
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
	
	/**
	 * @param verbPos pos-tag of all verbs (e.g., "VB.*")
	 * @return terminal IDs for all verbs 
	 */
	public ArrayList<Integer> getAllVerbIDs(String verbPos)
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		getAllVerbTerminalIDsAux(ids, nd_root, verbPos);
		
		return ids;
	}
	
	private void getAllVerbTerminalIDsAux(ArrayList<Integer> ids, TBNode curr, String verbPos)
	{
		if (curr.isPos(verbPos))
			ids.add(curr.terminalId);
		
		if (curr.isPhrase())
		{
			for (TBNode child : curr.getChildren())
				getAllVerbTerminalIDsAux(ids, child, verbPos);
		}
	}
	
	public String toTree()
	{
		return toTreeAux(nd_root, "");
	}
	
	private String toTreeAux(TBNode node, String indent)
	{
		String str = indent + "(" + node.getTags();
		if (!node.isPhrase())	return str += " " + node.form + ")";
		
		for (TBNode child : node.getChildren())
			str += "\n" + toTreeAux(child, indent+"  ");
		
		return str+")";	
	}
	
	public String toStanfordPos()
	{
		StringBuilder build = new StringBuilder();
		
		toStanfordPosAux(nd_root, build);
		return build.toString().trim();
	}
	
	private void toStanfordPosAux(TBNode curr, StringBuilder build)
	{
		if (!curr.isPhrase())
		{
			build.append(curr.form);
			build.append("_");
			build.append(curr.pos);
			build.append(" ");
		}
		else
		{
			for (TBNode child : curr.getChildren())
				toStanfordPosAux(child, build);
		}
	}
	
	public void countTags(HashMap<String,Integer> map, int[] tCount, int[] pCount, int[] overlap)
	{
		countTagsAux(nd_root, map, tCount, pCount, overlap);
	}
	
	private void countTagsAux(TBNode curr, HashMap<String,Integer> map, int[] tCount, int[] pCount, int[] overlap)
	{
		if (!curr.isPhrase())	return;
		
		if (curr.tags != null)
		{
			for (String tag : curr.tags)
			{
				Integer idx = map.get(tag);
				if (idx != null)	tCount[idx]++;
			}
		}
		
		if (curr.pb_labels != null)
		{
			for (String tag : curr.pb_labels)
			{
				Integer idx = map.get(tag);
				if (idx != null)
				{
					pCount[idx]++;
					if (curr.tags != null)
					{
						if (curr.tags.contains(tag))	overlap[idx]++;
						else if (tag.equals("DIS") && curr.tags.contains("VOC"))	overlap[idx]++;
					}
					
				}
			}
		}
		
		for (TBNode child : curr.getChildren())
			countTagsAux(child, map, tCount, pCount, overlap);
	}
	
	public TBNode getCoIndexedNode(int coIndex)
	{
		return getCoIndexedNodeAux(getRootNode(), coIndex);
	}
	
	private TBNode getCoIndexedNodeAux(TBNode curr, int coIndex)
	{
		if (!curr.isPhrase())			return null;
		if (curr.coIndex == coIndex)	return curr;
		
		for (TBNode child : curr.getChildren())
		{
			TBNode node = getCoIndexedNodeAux(child, coIndex);
			if (node != null)	return node;
		}
		
		return null;
	}
	
	public void checkNumChildren()
	{
		checkNumChildrenAux(nd_root);
	}
	
	private void checkNumChildrenAux(TBNode curr)
	{
		if (!curr.isPhrase())	return;
		
		if (curr.isPos("CAPTION") && curr.getChildren().size() > 1)
			System.out.println(curr.toWords());
		
		for (TBNode child : curr.getChildren())
			checkNumChildrenAux(child);
	}
	
	public boolean isUnder(int terminalIndex, String phrase)
	{
		for (int i=1; i<100; i++)
		{
			moveTo(terminalIndex, i);
			if (nd_curr == null)		return false;
			if (nd_curr.isPos(phrase))	return true;
		}
					
		return false;
	}
}
