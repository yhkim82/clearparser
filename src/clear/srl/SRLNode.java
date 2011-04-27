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
import java.util.Collections;

import clear.ftr.FtrLib;
import clear.propbank.PBLoc;
import clear.reader.AbstractReader;
import clear.treebank.TBEnLib;
import clear.util.JArrays;
import clear.util.JString;

import com.carrotsearch.hppc.FloatArrayList;
import com.carrotsearch.hppc.FloatFloatOpenHashMap;

/**
 * Semantic role labeling node.
 * @author Jinho D. Choi
 * <b>Last update:</b> 1/26/2011
 */
public class SRLNode implements Comparable<SRLNode>
{
	public float   id;
	public String  form;
	public String  lemma;
	public String  pos;
	public SRLHead dHead;				// dependency head
	public String  roleset;				// PropBank roleset ID
	public float   anteId;				// antecedent ID
	public ArrayList<SRLHead> sHeads;	// SRL heads
	public ArrayList<SRLArg>  sArgs;
	public SRLNode prevNode;			// previous token
	public SRLNode nextNode;			// next token
	public PBLoc   pbLoc = null;
	
	public SRLNode()
	{
		id       = SRLLib.NULL_ID;
		form     = FtrLib.TAG_NULL;
		lemma    = FtrLib.TAG_NULL;
		pos      = FtrLib.TAG_NULL;
		dHead    = new SRLHead(SRLLib.NULL_HEAD_ID, FtrLib.TAG_NULL);
		anteId   = SRLLib.NULL_HEAD_ID;
		roleset  = SRLLib.FIELD_BLANK;
		sHeads   = new ArrayList<SRLHead>();
		sArgs    = new ArrayList<SRLArg>();
		prevNode = null;
		nextNode = null;
	}
	
	/** Makes this node an artificial root node. */
	public void toRoot()
	{
		id    = SRLLib.ROOT_ID;
		form  = SRLLib.ROOT_TAG;
		lemma = SRLLib.ROOT_TAG;
		pos   = SRLLib.ROOT_TAG;
		dHead.set(SRLLib.NULL_HEAD_ID, SRLLib.ROOT_TAG);
	}
	
	public void setId(float id)
	{
		this.id = id;
	}
	
	public void setAntecedentId(float anteId)
	{
		this.anteId = anteId;
	}
	
	public float getDepHeadId()
	{
		return dHead.headId;
	}
	
	public String getDeprel()
	{
		return dHead.label;
	}
	
	public void setDepHeadId(float headId)
	{
		dHead.setHeadId(headId);
	}
	
	public void setDeprel(String deprel)
	{
		dHead.setLabel(deprel);
	}
	
	public void setDepHead(float headId, String deprel)
	{
		dHead.set(headId, deprel);
	}
	
	public void setDepHead(float headId, String deprel, double score)
	{
		dHead.set(headId, deprel, score, true);
	}

	public void addSRLHead(float headId, String label)
	{
		sHeads.add(new SRLHead(headId, label));
	}
	
	public void addSRLHead(float headId, String label, double score)
	{
		sHeads.add(new SRLHead(headId, label, score, true));
	}
	
	public void addSRLHeads(ArrayList<SRLHead> sHeads)
	{
		this.sHeads.addAll(sHeads);
	}
	
	public void removeSRLHeads(ArrayList<SRLHead> sHeads)
	{
		ArrayList<SRLHead> delList = new ArrayList<SRLHead>();
		
		for (SRLHead tHead : this.sHeads)
		{
			for (SRLHead pHead : sHeads)
			{
				if (tHead.equals(pHead))
				{
					delList.add(tHead);
					break;
				}
			}
		}
		
		this.sHeads.removeAll(delList);
	}
	
	public void addSRLArgs(ArrayList<SRLArg> sArgs)
	{
		this.sArgs.addAll(sArgs);
	}
	
	public boolean isForm(String regex)
	{
		return form.matches(regex);
	}
	
	public boolean isPos(String regex)
	{
		return pos.matches(regex);
	}
	
	public boolean isDeprel(String regex)
	{
		return dHead.isLabel(regex);
	}
	
	public boolean isRoot()
	{
		return id == SRLLib.ROOT_ID;
	}
	
	public boolean isPredicate()
	{
		return !roleset.equals(SRLLib.FIELD_BLANK);
	}
	
	public boolean isEmptyCategory()
	{
		return pos.equals(TBEnLib.POS_NONE);
	}
	
	public boolean hasDepHead()
	{
		return dHead.hasHead;
	}
	
	public boolean hasSRLHead()
	{
		return !sHeads.isEmpty();
	}
	
	public boolean hasAntecedent()
	{
		return anteId > 0;
	}
	
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(JString.getNormalizedForm(id));
		buff.append(AbstractReader.FIELD_DELIM);
		
		buff.append(form);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(lemma);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(pos);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append("_");	buff.append(AbstractReader.FIELD_DELIM);	// feat
		buff.append(JString.getNormalizedForm(dHead.headId));
		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(dHead.label);
		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(roleset);		buff.append(AbstractReader.FIELD_DELIM);
		
	/*	if (anteId > 0)			buff.append(JString.getNormalizedForm(anteId));
		else					buff.append(SRLLib.FIELD_BLANK);
		buff.append(AbstractReader.FIELD_DELIM);*/
		
		if (sHeads.isEmpty())
			buff.append(SRLLib.FIELD_BLANK);
		else
		{
			Collections.sort(sHeads);
			
			for (int i=0; i<sHeads.size(); i++)
			{
				if (i > 0)	buff.append(SRLLib.HEAD_DELIM);
				buff.append(sHeads.get(i));
			}
		}
		buff.append(AbstractReader.FIELD_DELIM);
		
		SRLArg arg;
		
		if (sArgs.isEmpty())
			buff.append(SRLLib.FIELD_BLANK);
		else
		{
			Collections.sort(sArgs);
			
			for (int i=0; i<sArgs.size(); i++)
			{
				arg = sArgs.get(i);
				
				if (arg.ids.length > 0)
				{
					buff.append(sArgs.get(i));
					buff.append(SRLLib.HEAD_DELIM);
				}
			}
		}
		
		buff.append(AbstractReader.FIELD_DELIM);
		if (pbLoc == null)	buff.append(SRLLib.FIELD_BLANK);	
		else				buff.append(pbLoc.toString());
		
		return buff.toString();
	}
	
	public String toCoNLL09(FloatArrayList predIDs)
	{
		StringBuilder buff = new StringBuilder();
		String headId = JString.getNormalizedForm(dHead.headId);
		
		buff.append(JString.getNormalizedForm(id));
		buff.append(AbstractReader.FIELD_DELIM);
		
		buff.append(form);					buff.append(AbstractReader.FIELD_DELIM);
		buff.append(lemma);					buff.append(AbstractReader.FIELD_DELIM);
		buff.append(lemma);					buff.append(AbstractReader.FIELD_DELIM);
		buff.append(pos);					buff.append(AbstractReader.FIELD_DELIM);
		buff.append(pos);					buff.append(AbstractReader.FIELD_DELIM);
		buff.append(SRLLib.FIELD_BLANK);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(SRLLib.FIELD_BLANK);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(headId);				buff.append(AbstractReader.FIELD_DELIM);
		buff.append(headId);				buff.append(AbstractReader.FIELD_DELIM);
		buff.append(dHead.label);			buff.append(AbstractReader.FIELD_DELIM);
		buff.append(dHead.label);			buff.append(AbstractReader.FIELD_DELIM);
		
		if (isPredicate())	buff.append("Y");
		else				buff.append(SRLLib.FIELD_BLANK);
		buff.append(AbstractReader.FIELD_DELIM);
		
		buff.append(roleset);
		buff.append(AbstractReader.FIELD_DELIM);
		
		String[] arr = new String[predIDs.size()];
		for (int i=0; i<arr.length; i++)	arr[i] = SRLLib.FIELD_BLANK;
		
		for (SRLHead head : sHeads)
		{
			int idx = predIDs.indexOf(head.headId);
			arr[idx] = head.label;
		}
		
		buff.append(JArrays.join(arr, AbstractReader.FIELD_DELIM));
		
		return buff.toString();
	}
	
	@Override
	public int compareTo(SRLNode node)
	{
		float diff = id - node.id;
		
		if      (diff < 0)	return -1;
		else if (diff > 0)	return  1;
		
		return  0;
	}
	
	public void remapIDs(FloatFloatOpenHashMap map)
	{
		id = map.get(id);
		dHead.setHeadId(map.get(dHead.headId));
		anteId = map.get(anteId);
		
		for (SRLHead head : sHeads)
			head.setHeadId(map.get(head.headId));
		
		for (SRLArg arg : sArgs)
		{
			FloatArrayList list = new FloatArrayList();
			
			for (float f : arg.ids)
			{
				if ((f = map.get(f)) > 0)
					list.add(f);
			}
			
			arg.ids = list.toArray();
		}
	}
}
