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
package clear.srl.merge;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.ITree;
import clear.srl.SrlHead;
import clear.srl.SrlLib;
import clear.util.tuple.JObjectObjectTuple;

import com.carrotsearch.hppc.FloatObjectOpenHashMap;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;

/**
 * Dependency tree.
 * @see DepNode
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
public class MergeTree extends FloatObjectOpenHashMap<MergeNode> implements ITree<MergeNode>
{
	public MergeTree()
	{
		MergeNode root = new MergeNode();
		root.id = SrlLib.ROOT_ID;
		
		add(root);
	}

	public boolean add(MergeNode node)
	{
		put(node.id, node);
		return true;
	}
	
	public void mapArgs()
	{
		mapHeads();
		processArgs();
	}
	
	public void mapHeads()
	{
		MergeNode node;
		
		for (int predId=1; predId<size(); predId++)
		{
			node = get(predId);
			
			if (node.isPredicate())
				mapHeads(predId, node.args);
		}
	}
	
	/** Called from {@link MergeTree#mapHeads()}. */
	private void mapHeads(int predId, String args)
	{
		String[] aArgs = args.split(";");
		String[] tmp;
		String label;
		int beginId, endId;
		IntArrayList heads;
		
		MergeNode pred = get(predId);
		pred.sArgs = new ArrayList<JObjectObjectTuple<String, IntArrayList>>();
		
		for (String arg : aArgs)
		{
			tmp     = arg.split(":");
			label   = tmp[0];
			beginId = Integer.parseInt(tmp[1]);
			endId   = Integer.parseInt(tmp[2]);
			heads   = getHeads(label, predId, beginId, endId);
			
			if (heads.size() == 0)		continue;
			if (label.equals("C-V"))	label = "V";
			
			pred.addSrlArg(label, heads);
		}
	}
	
	/** Called from {@link MergeTree#mapHeads(int, String)}. */
	private IntArrayList getHeads(String label, int predId, int beginId, int endId)
	{
		IntArrayList list = new IntArrayList();
		int i;
		
		if (label.equals("V") || label.equals("C-V"))
		{
			for (i=beginId; i<=endId; i++)
			{
				if (beginId != predId)
					list.add(i);
			}
		}
		else
		{
			MergeNode node;
			
			for (i=beginId; i<=endId; i++)
			{
				node = get(i);
				
				if (node.headId < beginId || endId < node.headId)
					list.add(i);
			}
		}
		
		list.trimToSize();
		return list;
	}
	
	public void processArgs()
	{
		MergeNode    node;
		String       label, tmp;
		IntArrayList iArgs;
		int          i, predId;
		ArrayList<JObjectObjectTuple<String, IntArrayList>> sArgs;
		JObjectObjectTuple<String, IntArrayList> arg;
		boolean b = false;
		
		for (predId=1; predId<size(); predId++)
		{
			node  = get(predId);
			if (!node.isPredicate())	continue;
			sArgs = node.sArgs;
			
			for (i=0; i<sArgs.size(); i++)
			{
				arg   = sArgs.get(i);
				label = arg.key;
				iArgs = arg.value;
				tmp   = label+" "+predId+" "+iArgs;
				
				if (processCArgs(label, predId, iArgs, sArgs))	continue;
				if (iArgs.size() <= 1)							continue;
				if (processRArgs(label, node, iArgs))			continue;
			//	if (processSubtree(iArgs))						continue;
				if (processNMODs(label, predId, iArgs))			continue;
				
				
			/*	{
					System.out.println(tmp);
					b = true;
					continue;
				}*/
				
				
				
		//		if (mergeNMODs(label, predId, iArgs))	continue;
				
			//	System.out.println(predId+" "+iArgs+"\n"+toString()+"\n");
			}
			
			for (i=0; i<sArgs.size(); i++)
			{
				arg   = sArgs.get(i); 
				label = arg.key;
				iArgs = arg.value;
				
				for (int j=0; j<iArgs.size(); j++)
				{
					node = get(iArgs.get(j));
					node.addSrlHead(predId, label);
				}
				
			//	if (!label.startsWith("C-") && iArgs.size() > 1)
			//		System.out.println(iArgs);
			//		System.out.println(iArgs+"\n"+toString());
			}
		}
		
		if (b)	System.out.println(toString());
	}
	
	private boolean processCArgs(String label, int predId, IntArrayList cArgs, ArrayList<JObjectObjectTuple<String, IntArrayList>> sArgs)
	{
		if (!label.startsWith("C-"))	return false;
		
		label = label.substring(2);
		IntOpenHashSet aArgs = null;
		
		for (JObjectObjectTuple<String, IntArrayList> arg : sArgs)
		{
			if (arg.key.equals(label))
				aArgs = new IntOpenHashSet(arg.value);
		}
		
		IntOpenHashSet set = new IntOpenHashSet();
		MergeNode      node;
		
		for (int i=0; i<cArgs.size(); i++)
		{
			node = get(cArgs.get(i));
			
			if (node.headId < predId && predId < node.id)	continue;
			if (aArgs.contains((int)node.headId))			set.add((int)node.id);
		}

		cArgs.removeAll(set);
		return true;
	}
	
	private boolean processRArgs(String label, MergeNode pred, IntArrayList rArgs)
	{
		if (label.startsWith("R-"))	return false;
		int lastId = rArgs.size() - 1;
		
		MergeNode node = get(rArgs.get(lastId));
		if (!node.isPos("WDT"))		return false;
		
		IntArrayList iArgs = new IntArrayList(1);
		iArgs.add((int)node.id);
		
		pred.addSrlArg("R-"+label, iArgs);
		rArgs.remove(lastId);

		return rArgs.size() <= 1;
	}
	
	private boolean processSubtree(IntArrayList iArgs)
	{
		int i, j, size = iArgs.size(), argId;
		IntOpenHashSet set = new IntOpenHashSet();
		MergeNode curr;
		
		for (i=0; i<size; i++)
		{
			curr = get(iArgs.get(i));
			if (!curr.isPredicate())	continue;
			
			for (j=0; j<size; j++)
			{
				if (i == j)	continue;
				argId = iArgs.get(j);
				
				if (curr.isSArg(argId))
					set.add(argId);
			}
		}
		
		iArgs.removeAll(set);
		return iArgs.size() <= 1;
	}
	
	private boolean processNMODs(String label, int predId, IntArrayList iArgs)
	{
		MergeNode node, head;
		int i, currId, size = iArgs.size();
		
		for (i=0; i<size; i++)
		{
			currId = iArgs.get(i);
			node   = get(currId);
			
			if (i > 0 && iArgs.get(i-1)+1 != currId)	return false;
			if (!node.isDeprel(DepLib.DEPREL_NMOD))		return false;
		}
		
		head = get(iArgs.get(size-1));
		
		for (i=0; i<size-1; i++)
		{
			node = get(iArgs.get(i));
			node.headId = head.id;
		}
		
		iArgs.removeRange(0, size-1);
		return true;
	}
	
	private void mergeADVnP(IntArrayList list)
	{
		MergeNode node, head = null;
		int i, size = list.size();
		IntOpenHashSet set = new IntOpenHashSet();
		
		for (i=0; i<size; i++)
		{
			node = get(list.get(i));
			
			if (!node.isDeprel(DepLib.DEPREL_ADV+"|"+DepLib.DEPREL_DEP+"|"+DepLib.DEPREL_PRN+"|"+DepLib.DEPREL_P) && head == null)
				head = node;
		}

		// all nodes are ADV or P
		if (head == null)
		{
			for (i=0; i<size; i++)
			{
				node = get(list.get(i));
				
				if (node.isDeprel(DepLib.DEPREL_ADV))
				{
					head = node;
					break;
				}
			}
			
			if (head == null)	return;
		}
		
		for (i=0; i<size; i++)
		{
			node = get(list.get(i));
			if (node.id != head.id && node.isDeprel(DepLib.DEPREL_ADV+"|"+DepLib.DEPREL_DEP+"|"+DepLib.DEPREL_PRN+"|"+DepLib.DEPREL_P))
			{
				node.headId = head.id;
				set.add(list.get(i));
			}
		}
		
		list.removeAll(set);
	}
	
	private boolean mergeNMOD(IntArrayList list)
	{
		MergeNode node, head = null;
		int i, size = list.size();
		IntOpenHashSet set = new IntOpenHashSet();
		
		for (i=0; i<size; i++)
		{
			node = get(list.get(i));
			if (node.isDeprel(DepLib.DEPREL_NMOD))
			{
				if (head != null && head.headId == node.headId)
				{
					node.headId = head.id;
					set.add(list.get(i));
				}
				
				head = node;
			}
		}
		
		list.removeAll(set);
		return set.size() > 0;
	}

	public boolean isAdjoint(int fstId, int lstId)
	{
		for (int i=fstId+1; i<lstId; i++)
		{
			if (!isAncestor(fstId, i) && !isAncestor(lstId, i))
				return false;
		}
		
		return true;
	}
	
	public boolean isAncestor(float nodeId1, float nodeId2)
	{
		MergeNode node2 = get(nodeId2);
		
		if (node2.headId == nodeId1)		return true;
		if (node2.headId == DepLib.ROOT_ID)	return false;
		
		return isAncestor(nodeId1, node2.headId);
	}
	
	public String toString()
	{
		float[] keys = keySet().toArray();
		Arrays.sort(keys);
		
		StringBuilder build = new StringBuilder();
		
		for (float index : keys)
		{
			if (index == 0)	continue;
			build.append(get(index));
			build.append("\n");
		}
		
		return build.toString().trim();
	}
}
