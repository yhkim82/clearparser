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

import clear.dep.DepNode;
import clear.propbank.PBArg;
import clear.reader.AbstractReader;
import clear.reader.SrlReader;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Semantic-role labeling node.
 * @author Jinho D. Choi
 * <b>Last update:</b> 02/17/2010
 */
public class SrlNode extends DepNode
{
	/** Delimiter between Propbank arguments */
	static public String ARG_DELIM = ";";
	
	/** Propbank roleset (or frameset) ID (e.g., run.01) */
	public  String rolesetId;
	/** List of Propbank arguments */
	private ArrayList<PBArg> a_args = null;
	
	/** List of children IDs */
	protected TIntArrayList a_childrenIDs = null;
	/** Set of sub-node IDs */
	protected TIntHashSet   s_subIDs      = null; 

	
	/** Initializes the node as a null node. */
	public SrlNode()
	{
		init(AbstractReader.EMPTY_FIELD);
	}
	
	/**
	 * If ( <code>isRoot</code>), initializes the node as the root.
	 * If (!<code>isRoot</code>), initializes the node as a null node.
	 */
	public SrlNode(boolean isRoot)
	{
		super(isRoot);
		init(AbstractReader.EMPTY_FIELD);
	}
	
	/** Initializes the node with values from <code>node</code>. */
	public SrlNode(DepNode node)
	{
		super(node.id, node.form, node.lemma, node.pos, node.headId, node.deprel);
		init(AbstractReader.EMPTY_FIELD);
	}
	
	/**
	 * Initializes the node.
	 * @param rolesetId Propbank roleset ID (e.g., run.01)
	 */
	private void init(String rolesetId)
	{
		this.rolesetId = rolesetId;
		a_args         = new ArrayList<PBArg>();
		
		a_childrenIDs = new TIntArrayList();
		s_subIDs      = new TIntHashSet();
	}
	
	/**
	 * Adds a Propbank argument.
	 * @param arg Propbank argument
	 */
	public void addArg(PBArg arg)
	{
		a_args.add(arg);
	}
	
	/** @return list of Propbank arguments */
	public ArrayList<PBArg> getArgList()
	{
		return a_args;
	}
	
	/** @return true if the node is a predicate */
	public boolean isPredicate()
	{
		return !rolesetId.equals(AbstractReader.EMPTY_FIELD);
	}
	
	/**
	 * Returns the string representation of the node.
	 * Each field is separated by {@link SrlNode#FIELD_DELIM}.
	 */
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(super.toString());	buff.append(SrlReader.FIELD_DELIM);
		buff.append(rolesetId);			buff.append(SrlReader.FIELD_DELIM);
	
		for (int i=0; i<a_args.size(); i++)
		{
			PBArg arg = a_args.get(i);
			
			buff.append(arg);
			if (i+1 < a_args.size())	buff.append(ARG_DELIM);
		}
		
		return buff.toString();
	}
	
	
	/** Adds <code>id</code> to the children list.  */
	public void addChildID(int id)
	{
		a_childrenIDs.add(id);
	}
	
	/** Removes <code>id</code> from the children list. */
	public void removeChildID(int id)
	{
		a_childrenIDs.remove(id);
	}
		
	/** @return the unsorted-list of children IDs. */
	public TIntArrayList getChildrenIdList()
	{
		return a_childrenIDs; 
	}
	
	/** Adds <code>id</code> to the sub-node set. */
	public void addSubID(int id)
	{
		s_subIDs.add(id);
	}
	
	/** Adds <code>ids</code> to the sub-node set. */
	public void addSubIDs(TIntHashSet ids)
	{
		s_subIDs.addAll(ids);
	}
	
	/** @return true if <code>id</code> is in the sub-node set. */
	public boolean isSubID(int id)
	{
		return s_subIDs.contains(id);
	}

	/** @return the set of sub-node IDs. */
	public TIntHashSet getSubIdSet()
	{
		return s_subIDs;
	}
	
	/** @return the sorted-list of sub-node IDs. */
	public TIntArrayList getSubIdList()
	{
		TIntArrayList list = new TIntArrayList(s_subIDs);
		list.sort();

		return list;
	}
}
