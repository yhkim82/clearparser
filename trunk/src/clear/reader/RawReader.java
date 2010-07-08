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

import java.io.BufferedReader;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.morph.MorphEnAnalyzer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Raw dependency reader.
 * @author Jinho D. Choi
 * <b>Last update:</b> 6/26/2010
 */
public class RawReader extends AbstractReader<DepNode,DepTree>
{
	private List<ArrayList<? extends HasWord>> ls_sentence;
	private MaxentTagger                       pos_tagger;
	private MorphEnAnalyzer                    morph_analyzer;
	private int                                sen_index;

	@SuppressWarnings("static-access")
	public RawReader(String inputFile, String language, String posModelFile, String morphDictDir)
	{
		super(inputFile);
		
		try
		{
			pos_tagger     = new MaxentTagger(posModelFile);
			ls_sentence    = pos_tagger.tokenizeText(new BufferedReader(new FileReader(inputFile)));
			if ((language.equals(LANG_EN)))	morph_analyzer = new MorphEnAnalyzer(morphDictDir);
			sen_index      = 0;
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public DepTree nextTree()
	{
		if (sen_index >= ls_sentence.size())	return null;
		
		ArrayList<TaggedWord> ls_token = pos_tagger.tagSentence(ls_sentence.get(sen_index++));
		DepTree               tree     = new DepTree();
		
		for (int id=0; id<ls_token.size(); id++)
		{
			TaggedWord token = ls_token.get(id);
			DepNode    node  = new DepNode();
			
			node.id    = id + 1;
			node.form  = token.word();
			node.pos   = token.tag();
			node.lemma = (morph_analyzer != null) ? morph_analyzer.getLemma(node.form, node.pos) : node.form;
			
			tree.add(node);
		}
		
		return tree;
	}

	protected DepNode toNode(String line, int id)
	{
		return null;
	}
}
