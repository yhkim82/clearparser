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
package clear.reader;

import java.io.IOException;

import clear.srl.merge.MergeNode;
import clear.srl.merge.MergeTree;

/**
 * Dependency reader.
 * @author Jinho D. Choi
 * <b>Last update:</b> 6/26/2010
 */
public class MergeReader extends AbstractReader<MergeNode,MergeTree>
{
	/**
	 * Initializes the merge reader for <code>filename</code>.
	 * @param filename name of the file containing dependency trees
	 */
	public MergeReader(String filename)
	{
		super(filename);
	}
	
	/** 
	 * Returns the next dependency tree.
	 * If there is no more tree, returns null.
	 */
	public MergeTree nextTree()
	{
		MergeTree tree = new MergeTree();
		boolean isNext = false;
		
		try
		{
			isNext = appendNextTree(tree);
		}
		catch (IOException e) {e.printStackTrace();}

		return isNext ? tree : null;
	}
	
	protected MergeNode toNode(String line, int id)
	{
		MergeNode node = new MergeNode();
		String[]  str  = line.split(FIELD_DELIM);
		
		node.id      = (float)id;
		node.form    = str[1];
		node.lemma   = str[2];
		node.pos     = str[3];
		node.headId  = Float.parseFloat(str[4]);
		node.deprel  = str[5];
		node.roleset = str[6];
		node.args    = str[7];
		
		return node;
	}
}
