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
package clear.parse;

import java.util.HashSet;

import clear.decode.AbstractDecoder;
import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class DepSemParser extends AbstractDepParser
{
	HashSet<String> s_sem;
	
	/** Initializes this parser for {@link DepSemParser#FLAG_PRINT_LEXICON} or {@link DepSemParser#FLAG_PRINT_TRANSITION}. */
	public DepSemParser(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link DepSemParser#FLAG_PRINT_INSTANCE}. */
	public DepSemParser(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link DepSemParser#FLAG_PREDICT}. */
	public DepSemParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractDecoder decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes lambda_4 and beta using <code>tree</code>. */
	private void init(DepTree tree)
	{
		d_tree = tree;
		
		s_sem = new HashSet<String>();
		s_sem.add(DepLib.DEPREL_LOC);
		s_sem.add(DepLib.DEPREL_TMP);
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		for (i_beta=1; i_beta<tree.size(); i_beta++)
		{
			DepNode beta = d_tree.get(i_beta);
			
			if (!beta.isPosx("IN|TO") || beta.rightDepId == DepLib.NULL_ID)
				continue;
			
			i_lambda = beta.rightDepId;
			
			if (i_flag == FLAG_PREDICT)
				predict(beta);
			else
				train(beta);
		}
	}
	
	/** Trains the dependency tree ({@link DepSemParser#d_tree}). */
	private void train(DepNode beta)
	{
		String label = (s_sem.contains(beta.deprel)) ? beta.deprel : "-1";
		
		if      (i_flag == FLAG_PRINT_LEXICON )	addTags      (label);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label, getFeatureArray());
	}
	
	/** Predicts dependencies. */
	private void predict(DepNode beta)
	{
		JIntDoubleTuple res = c_dec.predict(getFeatureArray());
		String label = t_map.indexToLabel(res.i);
		
		if  (!label.equals("-1"))
		{
			beta.semDeprel = label;
	//		beta.setHead(0, label, 0);
		}
	}
	
	// ---------------------------- getFtr*() ----------------------------
	
	protected IntArrayList getFeatureArray()
	{
		if (i_flag == FLAG_PRINT_LEXICON)	// store features for configuration files
		{
			addNgramLexica();
			return null;
		}
		
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		
		addNgramFeatures(arr, idx);
		return arr;
	}

	protected void addLexica()
	{
		addNgramLexica();
	}
}