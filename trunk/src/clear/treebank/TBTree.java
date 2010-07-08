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

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * Tree as in Penn Treebank.
 * @author Jinho D. Choi
 * <b>Last update:</b> 02/15/2010
 */
public class TBTree
{
	/** Pointer to the current node */
	private TBNode nd_curr;
	/** Pointer to the terminal nodes */
	private ArrayList<TBNode> ls_terminal;
	
	/** Initializes the tree. */
	public TBTree()
	{
		ls_terminal = new ArrayList<TBNode>();
	}
	
	/** Sets the current pointer to <code>root</code>. */
	public void setRoot(TBNode root)
	{
		nd_curr = root;
	}
	
	/** Adds a terminal node. */
	public void addTerminalNode(TBNode node)
	{
		ls_terminal.add(node);
	}
	
	/** Returns the current node. */
	public TBNode getCurrNode()
	{
		return nd_curr;
	}
	
	/** Returns the list of terminal indices of the subtree of the current node. */
	public TIntArrayList getSubTerminalIndices()
	{
		TIntArrayList indices = new TIntArrayList();
		getSubTerminalIndicesAux(nd_curr, indices);
		
		indices.trimToSize();	indices.sort();
		return indices;
	}
	
	/** This method is called from {@link TBTree#getSubTerminalIndices()}. */
	private void getSubTerminalIndicesAux(TBNode node, TIntArrayList indices)
	{
		if (node.getChildren() == null)
			indices.add(node.terminalIndex);
		else
		{
			for (TBNode child : node.getChildren())
				getSubTerminalIndicesAux(child, indices);
		}
	}
	
	/**
	 * Returns the list of token indices of the subtree of the current node.
	 * Each index gets added by <code>offset</code> (e.g., if <code>offset</code> is 1, [0,1,2] becomes [1,2,3]).
	 */
	public TIntArrayList getSubTokenIndices(int offset)
	{
		TIntArrayList indices = new TIntArrayList();
		getSubTokenIndicesAux(nd_curr, indices, offset);
		
		indices.trimToSize();	indices.sort();
		return indices;
	}

	/** This method is called from {@link TBTree#getSubTokenIndices(int)}. */
	private void getSubTokenIndicesAux(TBNode node, TIntArrayList indices, int offset)
	{
		if (node.getChildren() == null)
		{
			int tokenIndex = node.tokenIndex + offset;
			if (tokenIndex >= offset)	indices.add(tokenIndex);
		}
		else
		{
			for (TBNode child : node.getChildren())
				getSubTokenIndicesAux(child, indices, offset);
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
			if (node.terminalIndex == terminalIndex)
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
}
