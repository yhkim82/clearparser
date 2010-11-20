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

import clear.decode.AbstractMultiDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.FtrLib;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class RLShallowParser extends AbstractDepParser
{
	/** Initializes this parser for {@link RLShallowParser#FLAG_PRINT_LEXICON} or {@link RLShallowParser#FLAG_PRINT_TRANSITION}. */
	public RLShallowParser(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link RLShallowParser#FLAG_PRINT_INSTANCE}. */
	public RLShallowParser(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link RLShallowParser#FLAG_PREDICT}. */
	public RLShallowParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractMultiDecoder decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes this parser for {@link RLShallowParser#FLAG_TRAIN_CONDITIONAL}. */
	public RLShallowParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractMultiDecoder decoder, String instanceFile)
	{
		super(flag, xml, map, decoder, instanceFile);
	}
	
	/** Initializes lambda_4 and beta using <code>tree</code>. */
	private void init(DepTree tree)
	{
		d_tree   = tree;
		i_lambda = tree.size() - 1;
		i_beta   = tree.size() - 2;
		
		if (i_flag == FLAG_TRAIN_CONDITIONAL)	d_copy = tree.clone();
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta > 0)	// beta is not empty
		{
			d_tree.n_trans++;
			
			if (i_lambda == d_tree.size())			// lambda_1 is empty: deterministic shift
				shift(true);	
			else if (i_flag == FLAG_PREDICT)
				predict();
			else if (i_flag == FLAG_TRAIN_CONDITIONAL)
				trainConditional();
			else
				train();
		}
	}
	
	/** Trains the dependency tree ({@link RLShallowParser#d_tree}). */
	private void train()
	{
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (lambda.headId == beta.id)	rightArc(lambda, beta, 1d);
		else if (isShift(d_tree))			shift(false);
		else								noArc();
	}
	
	/**
	 * This method is called from {@link RLShallowParser#train()}.
	 * @return true if non-deterministic shift needs to be performed 
	 */
	private boolean isShift(DepTree tree)
	{
		DepNode beta = tree.get(i_beta);
		int i, n = d_tree.size();
		
		for (i=i_lambda; i<n; i++)
		{
			DepNode lambda = tree.get(i);
			
			if (lambda.headId == beta.id)
				return false;
		}

		return true;
	}
	
	/** Predicts dependencies. */
	private void predict()
	{
		predictAux(getFeatureArray());
	}
	
	private void trainConditional()
	{
		IntArrayList ftr = getFeatureArray();
		String gLabel = getGoldLabel(d_copy);
		
		printInstance(gLabel, ftr);
		predictAux(ftr);
	}
	
	private String predictAux(IntArrayList ftr)
	{
		JIntDoubleTuple res = c_dec.predict(ftr);
		
		String  label  = (res.i < 0) ? LB_NO_ARC : t_map.indexToLabel(res.i);
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (label.equals(LB_RIGHT_ARC) && !d_tree.isAncestor(lambda, beta))
			rightArc(lambda, beta, res.d);
		else if (label.equals(LB_SHIFT))
			shift(false);
		else
			noArc();
		
		return label;
	}
	
	private String getGoldLabel(DepTree tree)
	{
		DepNode lambda = tree.get(i_lambda);
		DepNode beta   = tree.get(i_beta);
		
		if      (lambda.headId == beta.id)	return LB_RIGHT_ARC;
		else if (isShift(tree))				return LB_SHIFT;
		else								return LB_NO_ARC;
	}
		
	/**
	 * Performs a shift transition.
	 * @param isDeterministic true if this is called for a deterministic-shift.
	 */
	private void shift(boolean isDeterministic)
	{
		if (!isDeterministic)
		{
			if      (i_flag == FLAG_PRINT_LEXICON )	addTags      (LB_SHIFT);
			else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(LB_SHIFT, getFeatureArray());
		}
			
		i_lambda = i_beta--;
	}
	
	/** Performs a no-arc transition. */
	private void noArc()
	{
		if      (i_flag == FLAG_PRINT_LEXICON )	addTags      (LB_NO_ARC);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(LB_NO_ARC, getFeatureArray());
		
		i_lambda++;
	}
	
	/**
	 * Performs a left-arc transition.
	 * @param lambda lambda_1[0]
	 * @param beta   beta[0]
	 * @param deprel dependency label between <code>lambda</code> and <code>beta</code>
	 * @param score  dependency score between <code>lambda</code> and <code>beta</code>
	 */
	private void rightArc(DepNode lambda, DepNode beta, double score)
	{
		String  label = LB_RIGHT_ARC;
		
	    if      (i_flag == FLAG_PRINT_LEXICON)  addTags      (label);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label, getFeatureArray());

		lambda.setHead(beta.id, FtrLib.TAG_NULL, score);
		if (lambda.id > beta.rightDepId)	beta.rightDepId = lambda.id;
		
		i_lambda = i_beta--;
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
		
		addNgramFeatures      (arr, idx);
		return arr;
	}
}