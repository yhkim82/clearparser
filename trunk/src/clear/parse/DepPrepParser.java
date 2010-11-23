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
public class DepPrepParser extends AbstractDepParser
{
	/** Label of No-Arc transition */
	static public final String LB_NO_ARC    = "NA";
	/** Label of Right-Arc transition */
	static public final String LB_RIGHT_ARC = "RA";
	
	/** Initializes this parser for {@link DepPrepParser#FLAG_PRINT_LEXICON} or {@link DepPrepParser#FLAG_PRINT_TRANSITION}. */
	public DepPrepParser(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link DepPrepParser#FLAG_PRINT_INSTANCE}. */
	public DepPrepParser(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link DepPrepParser#FLAG_PREDICT}. */
	public DepPrepParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractDecoder decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes lambda_4 and beta using <code>tree</code>. */
	private void init(DepTree tree)
	{
		d_tree   = tree;
		i_lambda = tree.size() - 1;
		i_beta   = tree.size() - 2;
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta > 0)	// beta is not empty
		{
			d_tree.n_trans++;
			
			DepNode beta = d_tree.get(i_beta);
			
			if (!beta.isPosx("IN|TO") || i_lambda == d_tree.size())
				shift();	
			else if (i_flag == FLAG_PREDICT)
				predict(beta);
			else
				train(beta);
		}
	}
	
	/** Trains the dependency tree ({@link DepPrepParser#d_tree}). */
	private void train(DepNode beta)
	{
		DepNode lambda = d_tree.get(i_lambda);
		
		if (lambda.headId == beta.id)	rightArc(lambda, beta, 1d);
		else							noArc();
	}
	
	/** Predicts dependencies. */
	private void predict(DepNode beta)
	{
		JIntDoubleTuple res = c_dec.predict(getFeatureArray());
		
		String  label  = t_map.indexToLabel(res.i);
		DepNode lambda = d_tree.get(i_lambda);
		
		if  (label.equals(LB_RIGHT_ARC))
			rightArc(lambda, beta, res.d);
		else
			noArc();
	}
		
	/**
	 * Performs a shift transition.
	 */
	private void shift()
	{
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

	    int i;    DepNode tmp;
	    
	    for (i=i_beta+1; i<i_lambda; i++)
	    {
	    	tmp = d_tree.get(i);
	    	
	    	if (tmp.rightDepId == i_lambda)
	    	{
	    		tmp.rightDepId = DepLib.NULL_ID;
	    		break;
	    	}
	    }
	    
		beta.rightDepId = i_lambda;
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
		
		addNgramFeatures(arr, idx);
		return arr;
	}

	@Override
	protected void addLexica()
	{
		addNgramLexica();
	}
}