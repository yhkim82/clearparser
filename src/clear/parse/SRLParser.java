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

import clear.decode.AbstractMultiDecoder;
import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLArg;
import clear.ftr.map.SRLFtrMap;
import clear.ftr.xml.SRLFtrXml;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class SRLParser extends AbstractSRLParser
{
	/** Label of Shift transition */
	static public final String LB_SHIFT  = "SH";
	/** Label of NoArc transition */
	static public final String LB_NO_ARC = "NA";
	
	/** For {@link SRLParser#FLAG_TRAIN_CONDITIONAL} only. */
	protected DepTree d_copy = null;
	
	/** Initializes this parser for {@link SRLParser#FLAG_TRAIN_LEXICON}. */
	public SRLParser(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link SRLParser#FLAG_TRAIN_INSTANCE}. */
	public SRLParser(byte flag, SRLFtrXml xml, String[] lexiconFile, String[] instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link SRLParser#FLAG_PREDICT}. */
	public SRLParser(byte flag, SRLFtrXml xml, SRLFtrMap[] map, AbstractMultiDecoder[] decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes this parser for {@link SRLParser#FLAG_TRAIN_CONDITIONAL}. */
	public SRLParser(byte flag, SRLFtrXml xml, SRLFtrMap[] map, AbstractMultiDecoder[] decoder, String[] instanceFile)
	{
		super(flag, xml, map, decoder, instanceFile);
	}
	
	/** Initializes member variables. */
	private void init(DepTree tree)
	{
		tree.setSubcat();
		
		d_tree   = tree;
		i_beta   = nextPredicateId(0);
		i_lambda = i_beta - 1;
		i_dir    = DIR_LEFT;
		ls_args  = new ArrayList<SRLArg>();
		
		if (i_flag == FLAG_TRAIN_CONDITIONAL)	d_copy = tree.clone();
	}
	
	protected int prevPredicateId(int currId)
	{
		for (int id = currId-1; id > 0; id--)
		{
			if (d_tree.get(id).isPredicate())
				return id;
		}
		
		return -1;
	}
	
	protected int nextPredicateId(int currId)
	{
		for (int id = currId+1; id < d_tree.size(); id++)
		{
			if (d_tree.get(id).isPredicate())
				return id;
		}
		
		return d_tree.size();
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta < tree.size())
		{
			if (i_lambda <= 0 || i_lambda >= tree.size())
				shift(true);
			else if (i_flag == FLAG_PREDICT)
				predict();
			else if (i_flag == FLAG_TRAIN_CONDITIONAL)
				trainConditional();
			else
				train();
		}
	}
	
	/** Trains the dependency tree ({@link SRLParser#d_tree}). */
	private void train()
	{
		DepNode lambda = d_tree.get(i_lambda);
		String  label;
		
		if ((label = lambda.getSRLLabel(i_beta)) != null)
			yesArc(lambda, label, 1d);
	//	else if (isShift(d_tree))
	//		shift(false);
		else
			noArc();
	}
	
	/**
	 * This method is called from {@link SRLParser#train()}.
	 * @return true if non-deterministic shift needs to be performed 
	 */
	private boolean isShift(DepTree tree)
	{
		for (int i=i_lambda+i_dir; 0<i && i<tree.size(); i+=i_dir)
		{
			if (tree.get(i).isSRLHead(i_beta))
				return false;
		}

		return true;
	}
	
	/** Predicts dependencies. */
	private void predict()
	{
		predictAux(getBinaryFeatureArray());
	}
	
	private String predictAux(IntArrayList ftr)
	{
		SRLFtrMap       map = getIdxFtrMap();
		OneVsAllDecoder dec = (OneVsAllDecoder)getIdxDecoder();
		JIntDoubleTuple res;
		
		res = dec.predict(ftr);
	//	res = dec.predict(getValueFeatureArray());
		
		String  label  = (res.i < 0) ? LB_NO_ARC : map.indexToLabel(res.i);
		DepNode lambda = d_tree.get(i_lambda);
		
		if      (label.equals(LB_NO_ARC))
			noArc();
		else if (label.equals(LB_SHIFT))
			shift(false);
		else
			yesArc(lambda, label, res.d);
		
		return label;
	}
	
	private void trainConditional()
	{
		String    gLabel = getGoldLabel(d_copy);
		IntArrayList ftr = getBinaryFeatureArray();
		
		printInstance(gLabel, ftr);
		predictAux(ftr);
	}
	
	private String getGoldLabel(DepTree tree)
	{
		DepNode lambda = tree.get(i_lambda);
		String  label;
		
		if ((label = lambda.getSRLLabel(i_beta)) != null)
			return label;
		else if (isShift(d_tree))
			return LB_SHIFT;
		else
			return LB_NO_ARC;
	}
	
	/**
	 * Performs a shift transition.
	 * @param isDeterministic true if this is called for a deterministic-shift.
	 */
	private void shift(boolean isDeterministic)
	{
		if (!isDeterministic)	trainInstance(LB_SHIFT);
		
		if (i_dir == DIR_RIGHT)
		{
			i_beta = nextPredicateId(i_beta);
			ls_args.clear();
		}
		
		i_dir *= -1;
		i_lambda = i_beta + i_dir;
	}
	
	/** Performs a no-arc transition. */
	private void noArc()
	{
		trainInstance(LB_NO_ARC);
	
		i_lambda += i_dir;
	}
	
	private void yesArc(DepNode lambda, String label, double score)
	{
		trainInstance(label);

	    if (i_flag == FLAG_PREDICT)	lambda.addSRLHead(i_beta, label);
		ls_args.add(new SRLArg(i_lambda, label, score));
		
		i_lambda += i_dir;
	}
	
	private void trainInstance(String label)
	{
		if      (i_flag == FLAG_TRAIN_LEXICON)
			addTags(label);
		else if (i_flag == FLAG_TRAIN_INSTANCE)
			printInstance(label, getBinaryFeatureArray());
	}
		
	// ---------------------------- getFtr*() ----------------------------
	
	protected void addLexica(SRLFtrMap map)
	{
		addNgramLexica(map);
	}
	
	protected IntArrayList getBinaryFeatureArray()
	{
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		
		addNgramFeatures(arr, idx);
		addPathFeatures (arr, idx);
		
		return arr;
	}
	
	protected void addPathFeatures(IntArrayList arr, int[] idx)
	{
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (lambda.headId == beta.id)	arr.add(idx[0]);	// +0.05
		else if (beta.headId == lambda.id)	arr.add(idx[0]+1);	// +0.00
		
		idx[0] += 2;
	}
	
	protected ArrayList<JIntDoubleTuple> getValueFeatureArray()
	{
		return null;
	}
}