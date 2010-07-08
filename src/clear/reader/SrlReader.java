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
package clear.reader;

import java.io.IOException;

import clear.propbank.PBArg;
import clear.srl.SrlNode;
import clear.srl.SrlTree;

/**
 * Semantic-role labeling reader.
 * @author Jinho D. Choi
 * <b>Last update:</b> 02/17/2010
 */
public class SrlReader extends AbstractReader<SrlNode,SrlTree>
{
	boolean b_train;
	
	/**
	 * Initializes the semantic-role labeling reader for <code>filename</code>.
	 * @param filename name of the file containing dependency trees and semantic-role labeling information
	 */
	public SrlReader(String filename)
	{
		super(filename);
	}
	
	/** 
	 * Returns the next semantic-role labeling tree.
	 * If there is no more tree, returns null.
	 */
	public SrlTree nextTree()
	{
		SrlTree tree = new SrlTree();
		boolean isNext = false;
		
		try
		{
			isNext = appendNextTree(tree);
		}
		catch (IOException e) {e.printStackTrace();}

		return isNext ? tree : null;
	}
	
	/**
	 * Return the semantic-role labeling node containing values from <code>line</code>.
	 * @param line <id>\t<form>\t<lemma>\t<pos>\t<headId>\t<deprel>[\t<rolesetId>\t<arg>*]
	 */
	protected SrlNode toNode(String line, int id)
	{
		String[] str  = line.split(FIELD_DELIM);
		SrlNode  node = new SrlNode();

		node.form   = str[1];
		node.lemma  = str[2];
		node.pos    = str[4];
		node.headId = Integer.parseInt(str[6]);
		node.deprel = str[7];
		
		if (b_train)
		{
			node.rolesetId = str[8];
			
			if (str.length > 9)
			{
				String[] args = str[9].split(SrlNode.ARG_DELIM);
				for (String arg : args)	node.addArg(new PBArg(arg));
			}
		}
		
		return node;
	}
}
