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
package clear.srl;

import java.util.ArrayList;
import java.util.Arrays;

import clear.dep.DepLib;
import clear.dep.ITree;

import com.carrotsearch.hppc.FloatObjectOpenHashMap;

/**
 * Semantic role labeling tree.
 * @author Jinho D. Choi
 * <b>Last update:</b> 1/26/2011
 */
public class SRLTree extends FloatObjectOpenHashMap<SRLNode> implements ITree<SRLNode>
{
	private SRLNode root_node;
	private SRLNode last_node;
	
	/** Initializes this tree with an artificial root node. */
	public SRLTree()
	{
		root_node = new SRLNode();
		
		root_node.toRoot();
		add(root_node);
	}

	/** Adds a node to this tree. */
	public boolean add(SRLNode node)
	{
		float prevId = -1, nextId = Float.MAX_VALUE;
		
		for (float id : keySet().toArray())
		{
			if (id < node.id && id > prevId)
				prevId = id;
			else if (id > node.id && id < nextId)
				nextId = id;
		}
		
		if (prevId != -1)
		{
			node.prevNode = get(prevId);
			node.prevNode.nextNode = node;
		}
		
		if (nextId != Float.MAX_VALUE)
		{
			node.nextNode = get(nextId);
			node.nextNode.prevNode = node;
		}
		
		if (last_node == null || last_node.id < node.id)
			last_node = node;
		
		put(node.id, node);
		return true;
	}
	
	public SRLNode getRootNode()
	{
		return root_node;
	}
	
	public SRLNode getLastNode()
	{
		return last_node;
	}
	
	public float[] getOrderedIDs()
	{
		float[] ids = keySet().toArray();
		Arrays.sort(ids);
		
		return ids;
	}
	
	/** @return true if the ID exists in this tree. */
	public boolean isRange(float id)
	{
		return keySet().contains(id);
	}

	/** Sets a dependency head. */
	public void setDepHead(float currId, float headId, String deprel, double score)
	{
		get(currId).setDepHead(headId, deprel, score);
	}
	
	/** @return true if the <code>node1Id</code>'th node is the ancestor of the <code>node2Id</code>'th node. */
	public boolean isAncestor(float nodeId1, float nodeId2)
	{
		SRLNode node2 = get(nodeId2);
		
		if (!node2.hasDepHead())				return false;
		if ( node2.getDepHeadId() == nodeId1)	return true;
		
		return isAncestor(nodeId1, node2.getDepHeadId());
	}
	
	/** @return all dependents of <code>currId</code>'th node, sorted by IDs. */
	public ArrayList<SRLNode> getDependents(float currId)
	{
		ArrayList<SRLNode> list = new ArrayList<SRLNode>();
		SRLNode node = root_node;
		
		while (node.nextNode != null)
		{
			node = node.nextNode;
			if (node.getDepHeadId() == currId)	list.add(node);
		}
		
		return list;
	}
	
	/** Makes non-projective dependencies on punctuation projective. */
	public void projectizePunc()
	{
		SRLNode curr = root_node, head, node;
		float sId, eId;
		
		while (curr.nextNode != null)
		{
			curr = curr.nextNode;
			if (curr.isDeprel(SRLLib.DEPREL_P))	continue;
			head = get(curr.getDepHeadId());
			
			if (curr.id < head.id)
			{	sId = curr.id;	eId = head.id;	}
			else
			{	sId = head.id;	eId = curr.id;	}
			
			node = get(sId);
			
			while (node.nextNode.id != eId)
			{
				node = node.nextNode;
				
				if (node.isDeprel(DepLib.DEPREL_P) && (sId > node.getDepHeadId() || node.getDepHeadId() > eId))
				{
					if (curr.getDepHeadId() != DepLib.ROOT_ID)
						node.setDepHeadId(curr.getDepHeadId());
					else
						node.setDepHeadId(curr.id);
				}
			}
		}
	}
	
	/** @return true if this is a well-formed dependency graph. */
	public boolean checkTree()
	{
		int countRoot = 0;
		SRLNode  node = root_node;
		
		while (node.nextNode != null)
		{
			node = node.nextNode;
			
			if (node.getDepHeadId() == DepLib.ROOT_ID)
				countRoot++;
			
			if (!isRange(node.getDepHeadId()))
			{
				System.err.println("Not connected: "+node.id+" <- "+node.getDepHeadId());
				return false;
			}
			
			if (isAncestor(node.id, node.getDepHeadId()))
			{
				System.err.println("Cycle exists: "+node.id+" <-*-> "+node.getDepHeadId());
				return false;
			}
		}
		
		if (countRoot != 1)
		{
			System.err.println("Not single-rooted: "+countRoot);
			System.out.println(toString());
			return false;
		}
		
		return true;
	}
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		SRLNode       node  = root_node;
		
		while (node.nextNode != null)
		{
			node = node.nextNode;
			build.append(node);
			build.append("\n");
		}
		
		return build.toString().trim();
	}
}
