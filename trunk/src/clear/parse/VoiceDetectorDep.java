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

import java.util.ArrayList;

import clear.decode.AbstractDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.feat.FeatEnglish;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class VoiceDetectorDep extends AbstractDepParser
{
	public int[] correct   = new int[3];
	public int[] precision = new int[3];
	public int[] recall    = new int[3];
	
	/** Initializes this parser for {@link VoiceDetectorDep#FLAG_TRAIN_LEXICON}. */
	public VoiceDetectorDep(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link VoiceDetectorDep#FLAG_TRAIN_INSTANCE}. */
	public VoiceDetectorDep(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link VoiceDetectorDep#FLAG_PREDICT}. */
	public VoiceDetectorDep(byte flag, DepFtrXml xml, DepFtrMap map, AbstractDecoder decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes this parser for {@link VoiceDetectorDep#FLAG_TRAIN_CONDITIONAL}. */
	public VoiceDetectorDep(byte flag, DepFtrXml xml, DepFtrMap map, AbstractDecoder decoder, String instanceFile)
	{
		super(flag, xml, map, decoder, instanceFile);
	}
	
	/** Initializes member variables. */
	private void init(DepTree tree)
	{
		d_tree = tree;
		i_beta = tree.nextPredicateId(0);
		
		if (i_flag == FLAG_PREDICT)
		{
			for (int i=1; i<tree.size(); i++)
				tree.get(i).feats = null;
		}
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta < tree.size())
		{
			if (i_flag == FLAG_PREDICT)
				predict();
			else
				train();
			
			i_beta = tree.nextPredicateId(i_beta);
		}
	}
	
	/** Trains the dependency tree ({@link VoiceDetectorDep#d_tree}). */
	private void train()
	{
		DepNode beta  = d_tree.get(i_beta);
		String  voice = beta.getFeat(0);
		String  label = (voice != null) ? "1" : "0";
		
		if      (i_flag == FLAG_PRINT_LEXICON)
			addTags(label);
		else if (i_flag == FLAG_PRINT_INSTANCE)
			printInstance(label, getBinaryFeatureArray());
	}
	
	/** Predicts dependencies. */
	private void predict()
	{
		JIntDoubleTuple res = c_dec.predict(getBinaryFeatureArray());
		String        label = t_map.indexToLabel(res.i);
		
		DepNode beta = d_tree.get(i_beta);
		String voice = beta.getFeat(0);
		
		if (voice != null)
		{
			if (voice.matches("1|2"))
			{
				recall[1]++;
			
				if (label.equals("1"))
				{
					correct[0]++;
					correct[1]++;
				}
			}
			else
			{
				recall[2]++;
				
				if (label.equals("1"))
				{
					correct[0]++;
					correct[2]++;
				}
			}
			
			recall[0]++;
		}
		
		beta.feats = new FeatEnglish();
		
		if (label.equals("1"))
		{
			precision[0]++;
			precision[1]++;
			precision[2]++;
		}
		
		beta.feats.feats[0] = label;
	}
		
	// ---------------------------- getFtr*() ----------------------------
	
	protected void addLexica()
	{
		addNgramLexica();
	}
	
	protected IntArrayList getBinaryFeatureArray()
	{
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		
		addNgramFeatures (arr, idx);
		addConjFeature(arr, idx);
		return arr;
	}
	
	protected void addConjFeature(IntArrayList arr, int[] idx)
	{
		DepNode beta = d_tree.get(i_beta);
		
		while (beta.deprel.matches("COORD|CONJ"))
			beta = d_tree.get(beta.headId);

		if (beta.id != i_beta && beta.isPredicate())
		{
			String voice = beta.getFeat(0);
			
			if (voice != null && voice.equals("1"))
				arr.add(idx[0]);
		}
		
		idx[0]++;
	}
	
	protected ArrayList<JIntDoubleTuple> getValueFeatureArray()
	{
		return null;
	}
}