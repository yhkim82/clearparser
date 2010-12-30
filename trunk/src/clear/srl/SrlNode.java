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

import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.ftr.FtrLib;

/**
 * Intermediate node.
 * @author Jinho D. Choi
 * <b>Last update:</b> 12/22/2010
 */
public class SrlNode extends DepNode
{
	public String roleset;
	public ArrayList<SrlHead> sHeads;
	
	/** Initializes the node as a null node. */
	public SrlNode()
	{
		init(DepLib.NULL_ID, FtrLib.TAG_NULL, FtrLib.TAG_NULL, FtrLib.TAG_NULL, DepLib.NULL_HEAD_ID, FtrLib.TAG_NULL, null);
	}
	
	/**
	 * If (<code>isRoot</code> is true ), initializes the node as the root.
	 * If (<code>isRoot</code> is false), initializes the node as a null node.
	 */
	public SrlNode(boolean isRoot)
	{
		if (isRoot)	init(DepLib.ROOT_ID, DepLib.ROOT_TAG, DepLib.ROOT_TAG, DepLib.ROOT_TAG, DepLib.NULL_HEAD_ID, DepLib.ROOT_TAG, null);
		else		init(DepLib.NULL_ID, FtrLib.TAG_NULL, FtrLib.TAG_NULL, FtrLib.TAG_NULL, DepLib.NULL_HEAD_ID, FtrLib.TAG_NULL, null);
	}
	
	/** Calls {@link SrlNode#init(int, String, String, String, int, String, ArrayList)}. */
	public SrlNode(int id, String form, String lemma, String pos, int headId, String deprel, ArrayList<SrlHead> sHeads)
	{
		init(id, form, lemma, pos, headId, deprel, sHeads);
	}
	
	/** Initializes the node with parameter values. */
	public void init(int id, String form, String lemma, String pos, int headId, String deprel, ArrayList<SrlHead> sHeads)
	{
		super.init(id, form, lemma, pos, headId, deprel);
		this.sHeads = sHeads;
	}
	
	public void init(int id, String form, String lemma, String pos, int headId, String deprel, double score, boolean hasHead, DepNode leftMostDep, DepNode rightMostDep, boolean isSkip, ArrayList<SrlHead> sHeads)
	{
		super.init(id, form, lemma, pos, headId, deprel, score, hasHead, leftMostDep, rightMostDep, isSkip);
		this.sHeads = sHeads;
	}
}
