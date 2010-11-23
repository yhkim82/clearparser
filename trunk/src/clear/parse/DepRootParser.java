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

import clear.decode.BinaryDecoder;
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
public class DepRootParser extends AbstractDepParser
{
	/** Initializes this parser for {@link DepRootParser#FLAG_PRINT_LEXICON} or {@link DepRootParser#FLAG_PRINT_TRANSITION}. */
	public DepRootParser(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link DepRootParser#FLAG_PRINT_INSTANCE}. */
	public DepRootParser(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link DepRootParser#FLAG_PREDICT}. */
	public DepRootParser(byte flag, DepFtrXml xml, DepFtrMap map, BinaryDecoder decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes lambda_1 and beta using <code>tree</code>. */
	private void init(DepTree tree)
	{
		d_tree      = tree;
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		for (i_beta=tree.size()-1; i_beta>0; i_beta--)
		{
			if (i_flag == FLAG_PREDICT)		predict();
			else							train();
		}
	}
	
	/** Trains the dependency tree ({@link DepRootParser#d_tree}). */
	private void train()
	{
		DepNode beta  = d_tree.get(i_beta);
		String  label = (beta.headId == DepLib.ROOT_ID) ? "1" : "-1";
		
		if (i_flag == FLAG_PRINT_LEXICON)
			addTags(label);
		else if (i_flag == FLAG_PRINT_INSTANCE)
			printInstance(label, getFeatureArray());
	}
	
	private void predict()
	{
		JIntDoubleTuple res   = c_dec.predict(getFeatureArray());
		String          label = t_map.indexToLabel(res.i);
		
		if (label.equals("1"))
			d_tree.setHead(i_beta, DepLib.ROOT_ID, DepLib.ROOT_TAG, res.d);
	}

	// ---------------------------- getFtr*() ----------------------------
	
	private IntArrayList getFeatureArray()
	{
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		
		addNgramFeatures      (arr, idx);
		addPunctuationFeatures(arr, idx);
		addPrevFeatures("VB.*|MD", arr, idx);
		addNextFeatures("VB.*|MD", arr, idx);
		addPrevFeatures("CC"     , arr, idx);
	//	addNextFeatures("CC"     , arr, idx);
		
		return arr;
	}
	
	protected void addLexica()
	{
		addNgramLexica();
		
		DepNode b0 = d_tree.get(i_beta);
		if (b0.isDeprel(DepLib.DEPREL_P))	t_map.addPunctuation(b0.form);
	}
	
	/**
	 * Adds punctuation features.
	 * This method is called from {@link DepRootParser#getFeatureArray()}.
	 */
	private void addPunctuationFeatures(IntArrayList arr, int[] beginIndex)
	{
		int index, n = t_map.n_punctuation;

		index = d_tree.getRightNearestPunctuation(i_beta, d_tree.size()-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
		index = d_tree.getLeftNearestPunctuation(i_beta, 1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
	}
	
	private void addPrevFeatures(String pos, IntArrayList arr, int[] beginIndex)
	{
		int i;
		
		for (i=i_beta-1; i>0; i--)
		{
			if (d_tree.get(i).isPosx(pos))
			{
				arr.add(beginIndex[0]);
				break;
			}
		}
		
		beginIndex[0]++;
	}
	
	private void addNextFeatures(String pos, IntArrayList arr, int[] beginIndex)
	{
		int i;
		
		for (i=i_beta+1; i<d_tree.size(); i++)
		{
			if (d_tree.get(i).isPosx(pos))
			{
				arr.add(beginIndex[0]);
				break;
			}
		}		
		
		beginIndex[0]++;
	}
}