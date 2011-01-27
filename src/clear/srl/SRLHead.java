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

import clear.ftr.FtrLib;
import clear.util.JString;

/**
 * Semantic role labeling head.
 * @author Jinho D. Choi
 * <b>Last update:</b> 1/26/2011
 */
public class SRLHead implements Comparable<SRLHead>
{
	static final public String DELIM = ":";
	
	public float   headId;
	public String  label;
	public double  score;
	public boolean hasHead;
	
	public SRLHead()
	{
		set(SRLLib.NULL_HEAD_ID, FtrLib.TAG_NULL, 0, false);
	}
	
	public SRLHead(float headId, String label)
	{
		set(headId, label, 0, false);
	}

	public SRLHead(float headId, String label, double score, boolean hasHead)
	{
		set(headId, label, score, hasHead);
	}
	
	public void set(float headId, String label)
	{
		this.headId = headId;
		this.label  = label;
	}
	
	public void set(float headId, String label, double score, boolean hasHead)
	{
		this.headId  = headId;
		this.label   = label;
		this.score   = score;
		this.hasHead = hasHead;
	}
	
	public void setHeadId(float headId)
	{
		this.headId = headId;
	}
	
	public void setLabel(String label)
	{
		this.label = label;
	}
	
	public boolean isHeadId(float headId)
	{
		return this.headId == headId;
	}
	
	public boolean isLabel(String regex)
	{
		return label.matches(regex);
	}
	
	public boolean equals(SRLHead head)
	{
		return headId == head.headId && label.equals(head.label);
	}
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(JString.getNormalizedForm(headId));
		build.append(DELIM);
		build.append(label);
		
		return build.toString();
	}

	@Override
	public int compareTo(SRLHead head)
	{
		float diff = headId - head.headId;
		
		if      (diff < 0)	return -1;
		else if (diff > 0)	return  1;
		
		return  0;
	}
}
