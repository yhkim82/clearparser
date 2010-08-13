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
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;

/**
 * Treebank node.
 * @author Jinho D. Choi
 * <b>Last update:</b> 8/12/2010
 */
public class TBNode
{
	/** Word-form of the node */
	public String form;
	/** Part-of-speech tag of the node */
	public String pos;
	/** Parent node */
	protected TBNode nd_parent;
	/** List of children nodes */
	protected ArrayList<TBNode> ls_children;
	/**
	 * If the node is a terminal, returns the terminal index (counting ECs), starting from 0.
	 * If the node is a phrase, returns -1.
	 */
	public int terminalId;
	/**
	 * If the node is a terminal, returns the token index (not counting ECs), starting from 0.
	 * If the node is a phrase, returns -1.
	 */
	public int tokenId;
	/**
	 * If the node is a terminal, returns {@link TBNode#terminalId}.
	 * If the node is a phrase, returns the head index.
	 */
	public int headId;
	
	/** Initializes the node with its parent node and pos-tag. */
	public TBNode(TBNode parent, String pos)
	{
		form        = null;
		this.pos    = pos;
		terminalId  = -1;
		tokenId     = -1;
		headId      = -1;
		nd_parent   = parent;
		ls_children = null;
	}
	
	/**
	 * Returns true if the word-form of this node is <code>form</code>.
	 * If the node is a phrase, returns false.
	 */
	public boolean isForm(String form)
	{
		return this.form != null && this.form.equals(form);
	}
	
	/**
	 * Returns true if the rule applies to this node.
	 * If <code>rule</code> starts with '-', it compares the function tag; otherwise, compares the pos-tag.
	 */
	public boolean isRuleMatch(String rule)
	{
		if (rule.charAt(0) == '-')
			return isTag(rule.substring(1));
		else
			return isPos(rule);
	}
	
	/** Returns true if the pos-tag of this node is <code>pos</code> in regular expression (e.g., NN.*|VB). */
	public boolean isPos(String rule)
	{
		return getPos().matches(rule);
	}
	
	/** Returns true is the function tag of this node is <code>tag</code>. */
	public boolean isTag(String tag)
	{
		String[] tags = getTags();
		if (tags == null)	return false;
		
		for (String t : tags)
			if (t.equals(tag))	return true;
		
		return false;
	}
	
	/** Returns true if the node is an empty category. */
	public boolean isEmptyCategory()
	{
		return pos.equals(TBLib.POS_NONE);
	}
	
	/** Returns true if the node is a phrase. */
	public boolean isPhrase()
	{
		return ls_children != null;
	}
	
	/** Returns true if this node is a phrase and contains <code>pos</code> as pos-tag of its children. */
	public boolean containsPos(String pos)
	{
		if (!isPhrase())	return false;
		
		for (TBNode child : ls_children)
			if (child.isPos(pos))	return true;
		
		return false;
	}
	
	/** Returns true if this node is a phrase and contains <code>tag</code> as function-tag of its children. */
	public boolean containsTag(String tag)
	{
		if (!isPhrase())	return false;
		
		for (TBNode child : ls_children)
			if (child.isTag(tag))	return true;
		
		return false;
	}
	
	/** Return the number of children whose pos-tag is <code>pos</code>. */
	public int countsPos(String pos)
	{
		if (!isPhrase())	return 0;
		int count = 0;
		
		for (TBNode child : ls_children)
			if (child.isRuleMatch(pos))	count++;
		
		return count;
	}
	
	/**
	 * Returns the parent node.
	 * If there is none, returns null.
	 */
	public TBNode getParent()
	{
		return nd_parent;
	}
	
	/** Returns the list of children nodes. */
	public ArrayList<TBNode> getChildren()
	{
		return ls_children;
	}
	
	/** Return the pos-tag without function tags. */
	public String getPos()
	{
		return pos.split("-|=")[0];
	}
	
	/**
	 * Returns an array of function tags.
	 * If there is no function tag, returns null.
	 */
	public String[] getTags()
	{
		String[] org = pos.split("-|=");
		if (org.length == 1)	return null;
		
		return Arrays.copyOfRange(org, 1, org.length);
	}
	
	/** Sets the parent node to <code>parent</code>. */
	public void setParent(TBNode parent)
	{
		nd_parent = parent;
	}
	
	/** Adds a child node. */
	public void addChild(TBNode child)
	{
		if (ls_children == null)
			ls_children = new ArrayList<TBNode>();
		
		ls_children.add(child);
	}
	
	/** Returns word-forms of the node's subtree, recursively. */
	public String toAllWords()
	{
		return toAllWords(this);
	}
	
	/** Auxiliary method of {@link TBNode#toAllWords()}. */
	private String toAllWords(TBNode curr)
	{
		if (curr.isPhrase())
		{
			StringBuilder build = new StringBuilder();
			
			for (TBNode child : curr.getChildren())
			{
				build.append(toAllWords(child));
				build.append(" ");
			}

			return build.toString().trim();
		}
		else
			return curr.form;
	}
	
	/** Returns pos-tags of the node's subtree, recursively. */
	public String toAllPos()
	{
		StringBuilder build = new StringBuilder();
		
		for (TBNode child : ls_children)
		{
			build.append(child.getPos());
			build.append(" ");
		}
		
		return build.toString();
	}
	
	/** Returns the bitset of terminal indices of the subtree of this node. */
	public BitSet getSubTerminalBitSet()
	{
		BitSet set = new BitSet();
		getSubTerminalBitSetAux(this, set);
		
		return set;
	}
	
	/** This method is called from {@link TBTree#getSubTerminalBitSet()}. */
	private void getSubTerminalBitSetAux(TBNode node, BitSet set)
	{
		if (node.getChildren() == null)
			set.set(node.terminalId);
		else
		{
			for (TBNode child : node.getChildren())
				getSubTerminalBitSetAux(child, set);
		}
	}

	/**
	 * Returns the bitset of token indices of the subtree of the current node.
	 * Each index gets added by <code>offset</code> (e.g., if <code>offset</code> is 1, [0,1,2] becomes [1,2,3]).
	 */
	public BitSet getSubTokenBitSet(int offset)
	{
		BitSet set = new BitSet();
		getSubTokenBitSetAux(this, set, offset);
		
		return set;
	}

	/** This method is called from {@link TBNode#getSubTokenBitSet(int)}. */
	private void getSubTokenBitSetAux(TBNode node, BitSet set, int offset)
	{
		if (node.getChildren() == null)
		{
			int tokenIndex = node.tokenId + offset;
			if (tokenIndex >= offset)	set.set(tokenIndex);
		}
		else
		{
			for (TBNode child : node.getChildren())
				getSubTokenBitSetAux(child, set, offset);
		}
	}
}
