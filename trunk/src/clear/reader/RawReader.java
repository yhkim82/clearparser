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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.morph.MorphEnAnalyzer;


/**
 * Part-of-speech dependency reader.
 * @author Jinho D. Choi
 * <b>Last update:</b> 6/26/2010
 */
public class RawReader extends AbstractReader<DepNode,DepTree>
{
	private TokenizerME     tokenizer;
	private POSTaggerME     tagger;
	private MorphEnAnalyzer morph_analyzer = null;
	
	/**
	 * Initializes the dependency reader for <code>filename</code>.
	 * @param filename name of the file containing dependency trees
	 */
	public RawReader(String filename, String language, String tokenModel, String posModel, String morphDict)
	{
		super(filename);
		
		try
		{
			tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
			tagger    = new POSTaggerME(new POSModel(new FileInputStream(posModel)));
		}
		catch (Exception e) {e.printStackTrace();}
		
		if ((language.equals(LANG_EN)))	morph_analyzer = new MorphEnAnalyzer(morphDict);
	}
	
	/** 
	 * Returns the next dependency tree.
	 * If there is no more tree, returns null.
	 */
	public DepTree nextTree()
	{
		DepTree tree = null;
		
		try
		{
			String line = f_in.readLine();
			if (line == null)	return null;
			
			String tokens[] = tokenize(line);
			String tags  [] = tagger.tag(tokens);
			
			tree = new DepTree();
			DepNode node;
			
			for (int i=0; i<tokens.length; i++)
			{
				node = new DepNode();
				node.id    = i+1;
				node.form  = tokens[i];
				node.pos   = tags[i];
				node.lemma = (morph_analyzer != null) ? morph_analyzer.getLemma(node.form, node.pos) : node.form;
				
				tree.add(node);
			}
		}
		catch (IOException e1) {e1.printStackTrace();}

		return tree;
	}
	
	private String[] tokenize(String line)
	{
		ArrayList<String> list = new ArrayList<String>();
		
		for (String token : tokenizer.tokenize(line))
		{
			for (String tok : token.split("-|/"))
				list.add(tok);
		}
		
		String[] tokens = new String[list.size()];
		list.toArray(tokens);
		
		return tokens;
	}
	
	protected DepNode toNode(String line, int id)
	{
		return null;
	}
}
