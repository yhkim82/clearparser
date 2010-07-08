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

/**
 * Node as in Penn Treebank.
 * @author Jinho D. Choi
 * <b>Last update:</b> 02/15/2010
 */
public class TBNode
{
	/** Part-of-speech tag of the node */
	public String pos;
	/** Word-form of the node */
	public String word;
	/**
	 * If the node is a leaf, returns the terminal index (counting traces), starting from 0.
	 * If the node is not a leaf, returns -1.
	 */
	public int terminalIndex;
	/**
	 * If the node is a leaf, returns the token index (not counting traces), starting from 0.
	 * If the node is not a leaf, returns -1.
	 */
	public int tokenIndex;
	/** Parent node */
	protected TBNode nd_parent;
	/** List of children nodes */
	protected ArrayList<TBNode> ls_children;
	
	/**
	 * Initializes the node with its parent and pos-tag.
	 * @param parent parent node
	 * @param pos pos-tag
	 */
	public TBNode(TBNode parent, String pos)
	{
		this.pos      = pos;
		word          = null;
		terminalIndex = -1;
		tokenIndex    = -1;
		nd_parent     = parent;
		ls_children   = null;
	}
	
	/** Returns true if the pos-tag of the node is <code>pos</code>. */
	public boolean isPos(String pos)
	{
		return this.pos.equals(pos);
	}
	
	/** Returns true if the node is a trace. */
	public boolean isTrace()
	{
		return pos.equals(TBLib.POS_TRACE);
	}
	
	/**
	 * Returns the parent node.
	 * If there is none, returns null.
	 */
	public TBNode getParent()
	{
		return nd_parent;
	}
	
	/** Sets the parent node to <code>parent</code>. */
	public void setParent(TBNode parent)
	{
		nd_parent = parent;
	}
	
	/** Returns the list of children nodes. */
	public ArrayList<TBNode> getChildren()
	{
		return ls_children;
	}
	
	/** Adds a child node. */
	public void addChild(TBNode child)
	{
		if (ls_children == null)
			ls_children = new ArrayList<TBNode>();
		
		ls_children.add(child);
	}
	
	public boolean isPhrase()
	{
		return ls_children != null;
	}
	
	public boolean isBasePhrase()
	{
		if (!isPhrase())	return false;
		
		for (int i=0; i<ls_children.size(); i++) 
		{
			TBNode child = ls_children.get(i);
			if (child.isPhrase())	return false;
		}
		
		return true;
	}
	
	public String getPhraseRule()
	{
		StringBuilder builder = new StringBuilder();
		
		for (int i=0; i<ls_children.size(); i++)
		{
			TBNode child = ls_children.get(i);
			builder.append(child.pos);
			builder.append(" ");
		}
		
		return pos + "\t" + builder.toString().trim();
	}
}
