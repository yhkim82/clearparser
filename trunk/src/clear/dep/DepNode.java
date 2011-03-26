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

import java.util.HashSet;

import clear.dep.feat.AbstractFeat;
import clear.dep.srl.SRLInfo;
import clear.ftr.FtrLib;
import clear.reader.AbstractReader;

/**
 * Dependency node.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
public class DepNode
{
	/** Index of the current node */
	public int     id;
	/** Word form */
	public String  form;
	/** Lemma */
	public String  lemma;
	/** Part-of-speech tag */
	public String  pos;
	/** Feats */
	public AbstractFeat feats;
	/** Index of the head node */
	public int     headId; 
	/** Dependency label */
	public String  deprel; 
	/** Score of the headId'th node being the head of the node */
	public double  score;
	/** True if the node already found its head */
	public boolean hasHead;
	/** Leftmost dependent */
	public DepNode leftMostDep;
	/** Rightmost dependent */
	public DepNode rightMostDep;
	/** Left sibling */
	public DepNode leftSibling;
	/** Right sibling */
	public DepNode rightSibling;
	/** For Czech: conjunction head */
	public DepNode coordHead;
	/** Skip this node if it is true */
	public boolean isSkip;
	/** SRL information */
	public SRLInfo srlInfo;
	/** 1 if the node is non-projective (experimental) */
	public byte    nonProj = 0;
	
	public HashSet<String> s_topics; 
	public String v_cluster = null;
	
	/** Initializes the node as a null node. */
	public DepNode()
	{
		init(DepLib.NULL_ID, FtrLib.TAG_NULL, FtrLib.TAG_NULL, FtrLib.TAG_NULL, null, DepLib.NULL_HEAD_ID, FtrLib.TAG_NULL, 0, false, null, null, null, null, null, false, null);
		s_topics = new HashSet<String>();
	}
	
	public void toRoot()
	{
		id     = DepLib.ROOT_ID;
		form   = DepLib.ROOT_TAG;
		lemma  = DepLib.ROOT_TAG;
		pos    = DepLib.ROOT_TAG;
		deprel = DepLib.ROOT_TAG;
	}
		
	private void init(int id, String form, String lemma, String pos, AbstractFeat feats, int headId, String deprel, double score, boolean hasHead, DepNode leftMostDep, DepNode rightMostDep, DepNode leftSibling, DepNode rightSibling, DepNode coordHead, boolean isSkip, SRLInfo srlInfo)
	{
		this.id     = id;
		this.form   = form;
		this.lemma  = lemma;
		this.pos    = pos;
		this.feats  = feats;
		this.headId = headId;
		this.deprel = deprel;
		
		this.score        = score;
		this.hasHead      = hasHead;
		this.leftMostDep  = leftMostDep;
		this.rightMostDep = rightMostDep;
		this.leftSibling  = leftSibling;
		this.rightSibling = rightSibling;
		this.coordHead    = coordHead;
		this.isSkip       = isSkip;
		
		this.srlInfo = (srlInfo != null) ? srlInfo.clone() : null;
	}
	
	/** @return ({@link DepNode#hasHead}) ? {@link DepNode#headId} : {@link DepLib#NULL_HEAD_ID}. */
	public int getHeadId()
	{
		return hasHead ? headId : DepLib.NULL_HEAD_ID;
	}
		
	/** @return ({@link DepNode#hasHead}) ? {@link DepNode#deprel} : {@link FtrLib#TAG_NULL}. */
	public String getDeprel()
	{
		return hasHead ? deprel : null;
	}
	
	/**
	 * Sets the <code>headId</code>'th node as the head of the node.
	 * @param headId index of the head node
	 * @param deprel dependency label between the current and the head nodes
	 * @param score  score of the headId'th node being the head of the node
	 */
	public void setHead(int headId, String deprel, double score)
	{
		this.headId  = headId;
		this.deprel  = deprel;
		this.score   = score;
		this.hasHead = true;
	}
	
	public void addSRLHead(int headId, String label)
	{
		srlInfo.addHead(headId, label);
	}
	
	/** @return true if the node is a null node. */
	public boolean isNull()
	{
		return id == DepLib.NULL_ID;
	}
	
	/** @return true if the node is the root. */
	public boolean isRoot()
	{
		return id == DepLib.ROOT_ID;
	}
	
	public boolean isPredicate()
	{
		return srlInfo != null && srlInfo.isPredicate();
	}
	
	/** @return true if the form of the node is <code>form</code>. */
	public boolean isForm(String form)
	{
		return this.form.equals(form);
	}
	
	/** @return true if the lemma of the node is <code>lemma</code>. */
	public boolean isLemma(String lemma)
	{
		return this.lemma.equals(lemma);
	}
	
	/** @return true if the part-of-speech tag of the node is <code>pos</code>. */
	public boolean isPos(String pos)
	{
		return this.pos.equals(pos);
	}
	
	/** @return true if the part-of-speech tag of the node starts with <code>posx</code>. */
	public boolean isPosx(String regex)
	{
		return this.pos.matches(regex);
	}
	
	/** @return true if the dependency label of the node is <code>deprel</code>. */
	public boolean isDeprel(String deprel)
	{
		return this.deprel.equals(deprel);
	}
	
	public boolean isSRLHead(int headId)
	{
		return srlInfo.isHead(headId);
	}
	
	public boolean isSRLHeadMatch(String regex)
	{
		return srlInfo.isHeadMatch(regex);
	}
	
	public String getSRLLabel(int headId)
	{
		return srlInfo.getLabel(headId);
	}
	
	public String getFeat(int index)
	{
		if (feats != null)	return feats.get(index);
		else				return null;
	}
	
	public void copy(DepNode node)
	{
		init(node.id, node.form, node.lemma, node.pos, node.feats, node.headId, node.deprel, node.score, node.hasHead, node.leftMostDep, node.rightMostDep, node.leftSibling, node.rightSibling, node.coordHead, node.isSkip, node.srlInfo);
	}
	
	public DepNode clone()
	{
		DepNode node = new DepNode();
		
		node.copy(this);
		return node;
	}
	
	public void clearSRLHeads()
	{
		srlInfo.heads.clear();
	}
	
	public boolean isPassive()
	{
		String feat = getFeat(0);
		return feat != null && feat.equals("1"); 
	}
	
	/**
	 * @return the string representation of the node.
	 * Each field is separated by {@link DepNode#FIELD_DELIM}.
	 */
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(id);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(form);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(lemma);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(pos);		buff.append(AbstractReader.FIELD_DELIM);
	//	buff.append(pos);		buff.append(AbstractReader.FIELD_DELIM);
		if (feats == null)		buff.append(DepLib.FIELD_BLANK);
		else					buff.append(feats.toString());
		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(headId);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(deprel);
		
		if (srlInfo != null)
		{
			buff.append(AbstractReader.FIELD_DELIM);
			buff.append(srlInfo.toString());
		}
		
		return buff.toString();
	}
	
	public String toStringNonProj()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(id);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(form);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(lemma);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(pos);		buff.append(AbstractReader.FIELD_DELIM);
		buff.append(headId);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(deprel);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(nonProj);
		
		return buff.toString();
	}
}
