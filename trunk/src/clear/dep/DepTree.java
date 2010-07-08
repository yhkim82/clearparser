/**
* Copyright (c) 2009, Regents of the University of Colorado
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
package clear.dep;


import java.util.ArrayList;

import clear.ftr.FtrMap;
import clear.srl.SrlNode;
import clear.srl.SrlTree;

/**
 * Dependency tree.
 * @see DepNode
 * @author Jinho D. Choi
 * <b>Last update:</b> 5/1/2010
 */
@SuppressWarnings("serial")
public class DepTree extends AbstractTree<DepNode>
{
	/** Number of transitions made during parsing */
	public int    n_trans;
	/** Global score excluding scores from nodes (e.g. SHIFT scores) */
	public double d_score;
	
	/**
	 * Initializes the dependency tree.
	 * The root node is already inserted.
	 */
	public DepTree()
	{
		add(new DepNode(true));
		init(0, 0d);
	}
	
	/**
	 * Initializes the dependency tree with nodes in <code>tree</code>.
	 * @see DepNode#DepNode(SrlNode)
	 */
	public DepTree(SrlTree tree)
	{
		for (int i=0; i<tree.size(); i++)
			add(new DepNode(tree.get(i)));
		
		init(0, 0d);
	}
	
	/**
	 * Initializes member variables.
	 * @param nTrans {@link DepTree#n_trans}
	 * @param score  {@link DepTree#d_score}
	 */
	public void init(int nTrans, double score)
	{
		n_trans = nTrans;
		d_score = score;
	}
	
	/** @return true if the <code>node1Id</code>'th node is the ancestor of the <code>node2Id</code>'th node. */
	public boolean isAncestor(int nodeId1, int nodeId2)
	{
		DepNode node2 = get(nodeId2);
		
		if (!node2.hasHead)				return false;
		if ( node2.headId == nodeId1)	return true;
		
		return isAncestor(nodeId1, node2.headId);
	}
	
	/** @return true if the <code>node1</code> is the ancestor of the <code>node2</code>. */
	public boolean isAncestor(DepNode node1, DepNode node2)
	{
		if (!node2.hasHead)				return false;
		if ( node2.headId == node1.id)	return true;
		
		return isAncestor(node1, get(node2.headId));
	}
	
	/**
	 * Sets the <code>headId</code>'th node as the head of the <code>currId</code>'th node.
	 * @see   DepNode#setHead(int, String, double)
	 * @param currId index of the current node
	 * @param headId index of the head node
	 * @param deprel dependency label between the current and the head nodes
	 * @param score  score of the headId'th node being the head of the node
	 */
	public void setHead(int currId, int headId, String deprel, double score)
	{	
		DepNode curr = get(currId);
		curr.setHead(headId, deprel, score);
	}
	
	/**
	 * Copies contents of <code>tree</code> to this tree.
	 * <code>this.size()</code> is assumed to be equal to <code>tree.size()</code>. 
	 * @see DepNode#copy(DepNode)
	 */
	public void copy(DepTree tree)
	{
		for (int i=1; i<size(); i++)
			get(i).copy(tree.get(i));
		
		init(tree.n_trans, tree.d_score);
	}
	
	/**
	 * @see    DepNode#clone()
	 * @return the clone of this tree.
	 */
	public DepTree clone()
	{
		DepTree tree = new DepTree();
		
		for (int i=1; i<size(); i++)
			tree.add(get(i).clone());
		
		tree.init(n_trans, d_score);
		return tree;
	}
	
	/**
	 * Returns the head of the <code>currId</code>'th node.
	 * If there is no such head, returns a null node.
	 * @param currId index of the node to find the head for
	 */
	public DepNode getHead(int currId)
	{
		DepNode curr = get(currId);
		
		return curr.hasHead ? get(curr.headId) : new DepNode();
	}
	
	/**
	 * Returns the leftmost dependent of the <code>currId</code>'th node.
	 * It searches upto the <code>leftBoundId</code>'th node (inclusive).
	 * If there is no such dependent, returns a null node.
	 * @param currId    index of the node to find the leftmost dependent for
	 * @param leftBound leftmost-index of the node to check (inclusive)
	 */
	public DepNode getLeftMostDependent(int currId, int leftBound)
	{
		DepNode curr = get(currId);
		
		for (int i=leftBound; i<currId; i++)
		{
			DepNode node = get(i);
			if (node.hasHead && node.headId == curr.id)     return node;
		}
		
		return new DepNode();
	}
	
	/** @return the leftmost dependent of the <code>currId</code>'th node. */
	public DepNode getLeftMostDependent(int currId)
	{
		DepNode curr = get(currId);
		
		return isRange(curr.leftDepId) ? get(curr.leftDepId) : new DepNode();
	}
	
	/**
	 * Returns the left-nearest dependent of the <code>currId</code>'th node.
	 * If there is no such dependent, returns an empty node.
	 * @param currId index of the current node
	 */
	public DepNode getLeftNearestDependent(int currId, int leftBound)
	{
		for (int i=currId-1; i>=leftBound; i--)
		{
			DepNode node = get(i);
			if (node.hasHead && node.headId == currId)	return node;
		}
		
		return new DepNode();
	}
	
	/** @return the leftnearest dependent of the <code>currId</code>'th node. */
	public DepNode getLeftNearestDependent(int currId)
	{
		return getLeftNearestDependent(currId, 1);
	}
	
	/**
     * Returns the index of the left-nearest punctuation of the <code>currId</code>'th node.
     * Punctuation is defined in <code>lib</code> and the index can be retrieved from it. 
     * It stops searching when it meets <code>leftBoundId</code>'th node.
     * @param currId      index of the current node
     * @param leftBoundId index of the left bound
     * @param map         feature mapping containing indices of punctuation
     */
	public int getLeftNearestPunctuation(int currId, int leftBoundId, FtrMap map)
	{
		for (int i=currId-1; i>=leftBoundId; i--)
		{
			int puncIndex = map.punctuationToIndex(get(i).form);
			if (puncIndex > 0)    return puncIndex;
		}
		
		return -1;
	}
	
	/**
	 * Returns the rightmost dependent of the <code>currId</code>'th node.
	 * It searches upto the <code>rightBoundId</code>'th node (inclusive).
	 * If there is no such dependent, returns a null node.
	 * @param currId      index of the node to find the rightmost dependent for
	 * @param rightBound the rightmost-index of the node to check (includsive)
	 */
	public DepNode getRightMostDependent(int currId, int rightBound)
	{
		DepNode curr = get(currId);
		
		for (int i=rightBound; i>currId; i--)
		{
			DepNode node = get(i);
			if (node.hasHead && node.headId == curr.id)     return node;
		}
		
		return new DepNode();
	}
	
	/** @return the rightmost dependent of the <code>currId</code>'th node. */
	public DepNode getRightMostDependent(int currId)
	{
		DepNode curr = get(currId);
		
		return isRange(curr.rightDepId) ? get(curr.rightDepId) : new DepNode();
	}
	
	/**
	 * Returns the right-nearest dependent of the <code>currId</code>'th node.
	 * If there is no such dependent, returns an empty node.
	 * @param currId index of the current node
	 */
	public DepNode getRightNearestDependent(int currId, int rightBound)
	{
		for (int i=currId+1; i<=rightBound; i++)
		{
			DepNode node = get(i);
			if (node.hasHead && node.headId == currId)	return node;
		}
		
		return new DepNode();
	}
	
	/** Returns the right-nearest dependent of the <code>currId</code>'th node. */
	public DepNode getRightNearestDependent(int currId)
	{
		return getRightNearestDependent(currId, size()-1);
	}
	
	/**
     * Returns the index of the right-nearest punctuation of the <code>currId</code>'th node.
     * Punctuation is defined in <code>lib</code> and the index can be retrieved from it. 
     * It searches upto the <code>rightBoundId</code>'th node (inclusive).
     * @param currId index of the current node
     * @param rightBoundId index of the right bound
     * @param map feature mapping  containing indices of punctuation
     */
	public int getRightNearestPunctuation(int currId, int rightBoundId, FtrMap map)
	{
		for (int i=currId+1; i<=rightBoundId; i++)
		{
			int puncIndex = map.punctuationToIndex(get(i).form);
			if (puncIndex > 0)    return puncIndex;
		}
		
		return -1;
	}
	
	/** @return the score of the tree. */
	public double getScore()
	{
		double score = d_score;
		for (int i=1; i<size(); i++)	score += get(i).score;

		return score;
	}
	
	/** @return true if <code>currId</code>'th node has children. */
	public boolean hasChild(int currId)
	{
		for (int i=1; i<size(); i++)
		{
			if (i == currId)				continue;
			if (get(i).headId == currId)	return true;
		}
		
		return false;
	}
	
	public ArrayList<DepNode> findRoots()
	{
		ArrayList<DepNode> list = new ArrayList<DepNode>();
		
		for (int currId=1; currId<size(); currId++)
		{
			DepNode curr = get(currId);
			if (curr.headId == DepLib.ROOT_ID)	list.add(curr);
		}
		
		return list;
	}
}
