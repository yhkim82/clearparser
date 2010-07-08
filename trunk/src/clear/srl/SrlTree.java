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

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import clear.dep.AbstractTree;
import clear.dep.DepLib;
import clear.dep.DepTree;
import clear.propbank.PBArg;
import clear.util.DSUtil;
import clear.util.JSet;
import clear.util.tuple.JIntIntTuple;

/**
 * Semantic-role labeling tree.
 * @see SrlNode
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/14/2010
 */
@SuppressWarnings("serial")
public class SrlTree extends AbstractTree<SrlNode>
{
	/**
	 * Initializes the semantic-role labeling tree.
	 * The root node is already inserted.
	 */
	public SrlTree()
	{
		add(new SrlNode(true));
	}
	
	/**
	 * Initializes the semantic-role labeling tree copied from the dependency tree.
	 * @param dTree dependency tree
	 */
	public SrlTree(DepTree dTree)
	{
		for (int i=0; i<dTree.size(); i++)
			add(new SrlNode(dTree.get(i)));
	}
	
	/** Sets all children IDs. */
	public void setChildrenIDs()
	{
		for (int currId=1; currId<size(); currId++)
		{
			SrlNode curr = get(currId);
			SrlNode head = get(curr.headId);
			
			curr.hasHead = true;
			head.addChildID(currId);
		}
	}

	/** Sets all sub-node IDs (including the top-head node). */
	public void setSubIDs()
	{
		SrlNode       root        = get(DepLib.ROOT_ID);
		TIntArrayList childrenIDs = root.getChildrenIdList();
		
		for (int i=0; i<childrenIDs.size(); i++)
			setSubIDsAux(childrenIDs.get(i));
	}
	
	/** This method is called from {@link DepTree#setSubIDs()}. */
	private void setSubIDsAux(int headId)
	{
		SrlNode head = get(headId);
		head.addSubID(headId);
		
		TIntArrayList childrenIDs = head.getChildrenIdList();
		
		for (int i=0; i<childrenIDs.size(); i++)
		{
			SrlNode child = get(childrenIDs.get(i));
			setSubIDsAux(child.id);
			head.addSubIDs(child.getSubIdSet());
		}
	}
	
	/**
	 * Returns the list of head IDs that are the roots of all sub-node IDs.
	 * POST: <code>subIDs</code> does not include IDs in the returned list. 
	 * @param subIDs list of sub-node IDs
	 */
	public TIntArrayList getHeadIDs(TIntArrayList subIDs)
	{
		TIntArrayList list = new TIntArrayList();
		
		while (subIDs.size() > 0)
		{
			JIntIntTuple max = new JIntIntTuple(-1, -1);
			
			for (int i=0; i<subIDs.size(); i++)
			{
				SrlNode     curr  = get(subIDs.get(i));
				TIntHashSet inter = JSet.intersection(curr.getSubIdSet(), subIDs);
				
				if (inter.size() == subIDs.size())
				{
					max.set(curr.id, inter.size());
					break;
				}
				
				if (inter.size() > max.int2)	max.set(curr.id, inter.size());
			}
			
			if (max.int1 >= 0)
			{
				list.add(max.int1);
				subIDs.removeAll(get(max.int1).getSubIdSet());
			}
			else	return null;
		}
		
		list.sort();
		return list;
	}
	
	public String injectArg(PBArg arg)
	{
	//	if (!arg.label.equals("ARG0"))	return null;
		
		TIntArrayList ids = arg.ids;
		if (ids.size() > 2)	System.out.println(ids+"\n"+this+"\n");
		
		return null;
	}
	
	public TIntArrayList getSubIDs(String label, int predId, TIntArrayList headIDs)
	{
		// Model 1
		TIntHashSet set = new TIntHashSet();
		
		for (int i=0; i<headIDs.size(); i++)
		{
			int headId = headIDs.get(i);
			set.addAll(get(headId).getSubIdSet());
		}
		
		// Model 2
		if (label.equals("ARGM-MOD"))
		{
			SrlNode head = get(headIDs.get(0));
			SrlNode next = get(head.id+1);
			
			if (head.isPos("MD") && next.headId == head.id && next.isPos("CC"))
			{
				set.removeAll(get(predId).getSubIdSet());
				TIntArrayList list = DSUtil.toSortedTIntArrayList(set);

				for (int i=0; i<list.size(); i++)
				{
					int id = list.get(i);
					if (id < head.id)	set.remove(id);
					if (id > predId )	set.remove(id);
				}
			}
			
			if (set.contains(predId))	return headIDs;
			else						return DSUtil.toSortedTIntArrayList(set);
		}
		
		if (label.equals("ARGM-NEG"))
		{
			TIntArrayList list = new TIntArrayList(set);
			
			for (int i=0; i<list.size(); i++)
			{
				SrlNode node = get(list.get(i));
				
				if (headIDs.contains(node.id))							continue;
				if (node.isPosx("RB") && headIDs.contains(node.headId))	continue;
				
				set.remove(node.id);
			}
			
			return DSUtil.toSortedTIntArrayList(set);
		}
		
		// Model 4
		SrlNode pred = get(predId);
		
		if (set.contains(predId))
		{
			TIntArrayList rList;
			
			if (pred.isDeprel("VC") || pred.isDeprel("IM") || pred.isDeprel("PRD") || pred.isDeprel("CONJ"))
			{
				SrlNode head = get(predId);
				
				while (true)
				{
					if (head.isDeprel("VC"))	// #2.1
					{
						head = get(head.headId);
						while (head.isDeprel("VC"))	head = get(head.headId);
					}
					else if (head.isDeprel("IM"))
					{
						head = get(head.headId);
						if (head.isDeprel("OPRD"))	head = get(head.headId);
					}
					else if (head.isDeprel("PRD"))
					{
						head = get(head.headId);
					}
					else if (head.isDeprel("CONJ"))
					{
						head = get(head.headId);
						if (head.isDeprel("COORD"))	head = get(head.headId);
					}
					else
					{
						rList = DSUtil.toSortedTIntArrayList(head.getSubIdList());
						break;
					}
				}
			}
			else	rList = DSUtil.toSortedTIntArrayList(pred.getSubIdList());
			
			if (predId+1 < size())
			{
				SrlNode node = get(predId+1);
			
				if (node.isDeprel("OPRD") && node.headId == predId)
					rList.removeAll(node.getSubIdSet());
			}
			
			if (rList.size() > 0)
			{
				SrlNode node = get(rList.get(0));
			
				if (isRelativizer(node))	rList.removeAt(0);
			}
			
			if (rList.size() > 1)
			{
				SrlNode node = get(rList.get(0));
				SrlNode next = get(rList.get(1));
			
				if (isRelativizer(next))
				{
					rList.removeAt(1);
					if (node.isPos("IN") && next.headId == node.id)	rList.removeAt(0);
				}
			}
			
			set.removeAll(rList);
			
			// Model 5
			rList = DSUtil.toSortedTIntArrayList(set);
			
			for (int i=rList.size()-1; i>=0; i--)
			{
				SrlNode node = get(rList.get(i));
				
				if (node.isDeprel("P"))	set.remove(node.id);
				else					break;
			}
			
			if ((pred.isPos("VBG") || pred.isPos("VBN")) && !pred.isDeprel("VC"))
			{
				rList = DSUtil.toSortedTIntArrayList(set);
				
				if (rList.size() > 1)
				{
					SrlNode prev0 = get(rList.get(0));
					SrlNode prev1 = get(rList.get(1));
					
					if (prev0.id < predId && prev1.id > predId)
					{
						if (prev0.isLemma("a") || prev0.isLemma("the") || prev0.isPos("CC"))
							set.remove(prev0.id);
					}
				}

				rList    = DSUtil.toSortedTIntArrayList(set);
				int size = rList.size();
				
				if (!pred.isDeprel("SUB") && size > 0 && rList.get(size-1) > predId)
				{
					boolean isComma = false, isNextHead = false;
					
					for (int i=0; i<size; i++)
					{
						int id = rList.get(i);
						if (id < predId)	continue;
						
						if (id == pred.headId)
						{
							isNextHead = true;
							break;
						}
					}
					
					if (!isNextHead)
					{
						for (int i=0; i<size; i++)
						{
							SrlNode node = get(rList.get(i));
							
							if (node.id > predId)
							{
								if (i != 0 && (node.isForm(",") || node.isForm(":") || node.isForm(";") || node.isForm("--")))
								{
									isComma = true;
									for (int j=i; j<size; j++)	set.remove(rList.get(j));
								}
								
								break;
							}
						}
					}
					
					if (!isComma)
					{
						DepTree tree = new DepTree(this);
						int headId = -1;
						
						for (int i=0; i<size; i++)
						{
							int id = rList.get(i);
							if (id > predId)	break;
							
							if (tree.isAncestor(id, predId))
							{
								headId = id;	break;
							}
						}
						
						for (int i=0; i<size; i++)
						{
							int id = rList.get(i);
							if (id > predId)	break;
							
							if (id != headId && !tree.isAncestor(headId, id))
								set.remove(id);
						}
					}
				}
			}
		}

		return DSUtil.toSortedTIntArrayList(set);
	}
	
	private boolean isRelativizer(SrlNode node)
	{
		return (node.isPos("WDT") || node.isPos("WP") || node.isPos("WRB") || node.isForm("that") ||
				node.isForm("what") || node.isForm("who") || node.isForm("whom") || node.isForm("when") || node.isForm("where") || node.isForm("why") || node.isForm("which") || node.isForm("how") ||
				node.isForm("whatever") || node.isForm("whoever") || node.isForm("whenever") || node.isForm("wherever") || node.isForm("whichever") || node.isForm("however"));
	}
}
