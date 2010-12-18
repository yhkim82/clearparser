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
	/** Index of the head node */
	public int     headId; 
	/** Dependency label */
	public String  deprel; 
	/** Score of the headId'th node being the head of the node */
	public double  score;
	/** True if the node already found its head */
	public boolean hasHead;
	/** Index of the left-dependent */
	public int     leftDepId;
	/** Index of the right-dependent */
	public int     rightDepId;
	/** Skip this node if it is true */
	public boolean isSkip;
	/** 1 if the node is non-projective (experimental) */
	public byte    nonProj = 0;
	
	/** Initializes the node as a null node. */
	public DepNode()
	{
		init(DepLib.NULL_ID, FtrLib.TAG_NULL, FtrLib.TAG_NULL, FtrLib.TAG_NULL, DepLib.NULL_HEAD_ID, FtrLib.TAG_NULL);
	}
	
	/**
	 * If (<code>isRoot</code> is true ), initializes the node as the root.
	 * If (<code>isRoot</code> is false), initializes the node as a null node.
	 */
	public DepNode(boolean isRoot)
	{
		if (isRoot)	init(DepLib.ROOT_ID, DepLib.ROOT_TAG, DepLib.ROOT_TAG, DepLib.ROOT_TAG, DepLib.NULL_HEAD_ID, DepLib.ROOT_TAG);
		else		init(DepLib.NULL_ID, FtrLib.TAG_NULL, FtrLib.TAG_NULL, FtrLib.TAG_NULL, DepLib.NULL_HEAD_ID, FtrLib.TAG_NULL);
	}
	
	/** Calls {@link DepNode#init(int, String, String, String, int, String)}. */
	public DepNode(int id, String form, String lemma, String pos, int headId, String deprel)
	{
		init(id, form, lemma, pos, headId, deprel);
	}
	
	/** Initializes the node with parameter values. */
	public void init(int id, String form, String lemma, String pos, int headId, String deprel)
	{
		this.id         = id;
		this.form       = form;
		this.lemma      = lemma;
		this.pos        = pos;
		this.headId     = headId;
		this.deprel     = deprel;
		this.score      = 0;
		this.hasHead    = false;
		this.leftDepId  = Integer.MAX_VALUE;
		this.rightDepId = -1;
		this.nonProj    = 0;
		this.isSkip     = false;
	}
	
	public void init(int id, String form, String lemma, String pos, int headId, String deprel, double score, boolean hasHead, int leftDepId, int rightDepId, byte nonProj)
	{
		this.id         = id;
		this.form       = form;
		this.lemma      = lemma;
		this.pos        = pos;
		this.headId     = headId;
		this.deprel     = deprel;
		this.score      = score;
		this.hasHead    = hasHead;
		this.leftDepId  = leftDepId;
		this.rightDepId = rightDepId;
		this.nonProj    = nonProj;
		this.isSkip     = false;
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
	
	public void unhead()
	{
		headId  = DepLib.NULL_HEAD_ID;
		deprel  = FtrLib.TAG_NULL;
		hasHead = false;
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
	
	public void copy(DepNode node)
	{
		init(node.id, node.form, node.lemma, node.pos, node.headId, node.deprel, node.score, node.hasHead, node.leftDepId, node.rightDepId, node.nonProj);
	}
	
	public DepNode clone()
	{
		DepNode node = new DepNode();
		
		node.copy(this);
		return node;
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
		buff.append(headId);	buff.append(AbstractReader.FIELD_DELIM);
		buff.append(deprel);
		
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
