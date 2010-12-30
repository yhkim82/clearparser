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
package clear.srl.merge;

import java.util.ArrayList;

import clear.reader.AbstractReader;
import clear.srl.SrlHead;
import clear.util.tuple.JObjectObjectTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Merge node.
 * @author Jinho D. Choi
 * <b>Last update:</b> 12/22/2010
 */
public class MergeNode
{
	public float  id;
	public String form;
	public String lemma;
	public String pos;
	public float  headId;
	public String deprel;
	public String roleset;
	public String args;
	public ArrayList<SrlHead> sHeads;
	public ArrayList<JObjectObjectTuple<String, IntArrayList>> sArgs;
	
	public MergeNode()
	{
		sHeads = new ArrayList<SrlHead>();
	}
	
	public void setDepHead(float headId, String deprel)
	{
		this.headId = headId;
		this.deprel = deprel;
	}
	
	public void addSrlHead(float headId, String label)
	{
		sHeads.add(new SrlHead(headId, label));
	}
	
	public void addSrlArg(String label, IntArrayList iArgs)
	{
		sArgs.add(new JObjectObjectTuple<String, IntArrayList>(label, iArgs));
	}
	
	public boolean isPos(String regex)
	{
		return pos.matches(regex);
	}
	
	public boolean isDeprel(String regex)
	{
		return deprel.matches(regex);
	}
	
	public boolean isPredicate()
	{
		return !roleset.equals("-");
	}
	
	public boolean isSArg(int argId)
	{
		for (JObjectObjectTuple<String, IntArrayList> arg : sArgs)
		{
			if (arg.value.contains(argId))
				return true;
		}
		
		return false;
	}
	
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(id);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(form);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(lemma);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(pos);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(headId);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(deprel);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(roleset);
		
		for (SrlHead head : sHeads)
		{
			buff.append(AbstractReader.FIELD_DELIM);
			buff.append(head.toString());
		}
		
		return buff.toString();
	}
}
