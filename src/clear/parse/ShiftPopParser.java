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
import clear.decode.OneVsAllDecoder;
import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.util.tuple.JIntDoubleTuple;
import clear.util.tuple.JObjectDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class ShiftPopParser extends AbstractDepParser
{
	/** Label of Shift transition */
	static public final String LB_SHIFT     = "SH";
	/** Label of No-Arc transition */
	static public final String LB_NO_ARC    = "NA";
	/** Label of Left-Pop transition */
	static public final String LB_LEFT_POP  = "LP";
	/** Label of Left-Arc transition */
	static public final String LB_LEFT_ARC  = "LA";
	/** Label of Right-Arc transition */
	static public final String LB_RIGHT_ARC = "RA";
	/** Delimiter between transition and dependency label */
	static public final String LB_DELIM     = "-";
	
	/** For {@link AbstractDepParser#FLAG_TRAIN_CONDITIONAL} only. */
	protected DepTree d_copy = null;
	
	/** Initializes this parser for {@link ShiftPopParser#FLAG_PRINT_LEXICON} or {@link ShiftPopParser#FLAG_PRINT_TRANSITION}. */
	public ShiftPopParser(byte flag, String filename)
	{
		super(flag, filename);
	}

	/** Initializes this parser for {@link ShiftPopParser#FLAG_PRINT_INSTANCE}. */
	public ShiftPopParser(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		super(flag, xml, lexiconFile, instanceFile);
	}
	
	/** Initializes this parser for {@link ShiftPopParser#FLAG_PREDICT}. */
	public ShiftPopParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractMultiDecoder decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes this parser for {@link ShiftPopParser#FLAG_TRAIN_CONDITIONAL}. */
	public ShiftPopParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractMultiDecoder decoder, String instanceFile)
	{
		super(flag, xml, map, decoder, instanceFile);
	}
	
	/** Initializes lambda_1 and beta using <code>tree</code>. */
	private void init(DepTree tree)
	{
		findRightDep(tree);
		d_tree   = tree;
		i_lambda = 0;
		i_beta   = 1;
		
		if      (i_flag == FLAG_PRINT_TRANSITION)	printTransition("", "");
		else if (i_flag == FLAG_TRAIN_CONDITIONAL)	d_copy = tree.clone();
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta < tree.size())	// beta is not empty
		{
			if (i_lambda == -1)			// lambda_1 is empty: deterministic shift
			{	shift(true); continue;	}
			else if (tree.get(i_lambda).isSkip)
			{	i_lambda--;	continue;	}
			else if (i_flag == FLAG_PREDICT)
				predict();
			else if (i_flag == FLAG_TRAIN_CONDITIONAL)
				trainConditional();
			else
				train();
			
			d_tree.n_trans++;
		}
		
		if      (i_flag == FLAG_PRINT_TRANSITION)	f_out.println();
		else if (i_flag == FLAG_PREDICT)			postProcess();
		else if (i_flag == FLAG_TRAIN_CONDITIONAL)	postProcessConditional();
	}
	
	/** Trains the dependency tree ({@link ShiftPopParser#d_tree}). */
	private void train()
	{
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if (lambda.headId == beta.id)
		{
			if (isPop(d_tree))	leftPop(lambda, beta, lambda.deprel, 1d);
			else				leftArc(lambda, beta, lambda.deprel, 1d);
		}
		else if (lambda.id == beta.headId)	rightArc(lambda, beta, beta.deprel, 1d);
		else if (isShift(d_tree))			shift(false);
		else								noArc();
	}
	
	/**
	 * This method is called from {@link ShiftPopParser#train()}.
	 * @return true if non-deterministic shift needs to be performed 
	 */
	private boolean isShift(DepTree tree)
	{
		DepNode beta = tree.get(i_beta);
		DepNode node;
		
		for (int i=i_lambda; i>=0; i--)
		{
			node = tree.get(i);
			
			if (node.headId == beta.id || node.id == beta.headId)
				return false;
		}

		return true;
	}
	
	private boolean isPop(DepTree tree)
	{
		int i, size = tree.size();
		
		for (i=i_beta+1; i<size; i++)
		{
			if (tree.get(i).headId == i_lambda)
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
		int     index  = label.indexOf(LB_DELIM);
		String  trans  = (index > 0) ? label.substring(0,index) : label;
		String  deprel = (index > 0) ? label.substring(index+1) : "";
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);

		if      (trans.equals( LB_LEFT_POP) && !d_tree.isAncestor(lambda, beta) && lambda.id != DepLib.ROOT_ID)
			leftPop (lambda, beta, deprel, res.d);
		else if (trans.equals( LB_LEFT_ARC) && !d_tree.isAncestor(lambda, beta) && lambda.id != DepLib.ROOT_ID)
			leftArc (lambda, beta, deprel, res.d);
		else if (trans.equals(LB_RIGHT_ARC) && !d_tree.isAncestor(beta, lambda))
			rightArc(lambda, beta, deprel, res.d);
		else if (trans.equals(LB_SHIFT))
			shift(false);
		else
			noArc();
		
		return label;
	}
	
	private String getGoldLabel(DepTree tree)
	{
		DepNode lambda = tree.get(i_lambda);
		DepNode beta   = tree.get(i_beta);
		
		if (lambda.headId == beta.id)
		{
			if (isPop(tree))	return LB_LEFT_POP + LB_DELIM + lambda.deprel;
			else				return LB_LEFT_ARC + LB_DELIM + lambda.deprel;
		}
		else if (lambda.id == beta.headId)	return LB_RIGHT_ARC + LB_DELIM + beta  .deprel;
		else if (isShift(tree))				return LB_SHIFT;
		else								return LB_NO_ARC;
	}
	
	/** Predicts dependencies for tokens that have not found their heads during parsing. */
	private void postProcess()
	{
		int currId, maxId, i, n = d_tree.size();
		JObjectDoubleTuple<String> max;
		DepNode curr, node;
		
		for (currId=1; currId<n; currId++)
		{
			curr = d_tree.get(currId);
			if (curr.hasHead)	continue;
			
			max   = new JObjectDoubleTuple<String>(null, -1000);
			maxId = -1;
			
			for (i=currId-1; i>=0; i--)
			{
				node = d_tree.get(i);
				if (d_tree.isAncestor(curr, node))	continue;
				maxId = getMaxHeadId(curr, node, maxId, max, LB_RIGHT_ARC);
			}
			
			for (i=currId+1; i<d_tree.size(); i++)
			{
				node = d_tree.get(i);
				if (d_tree.isAncestor(curr, node))	continue;
				maxId = getMaxHeadId(curr, node, maxId, max, LB_LEFT_ARC+"|"+LB_LEFT_POP);
			}
		
			if (maxId != -1)	curr.setHead(maxId, max.object, max.value);
		}
	}
	
	/** This method is called from {@link ShiftPopParser#postProcess()}. */
	private int getMaxHeadId(DepNode curr, DepNode head, int maxId, JObjectDoubleTuple<String> max, String sTrans)
	{
		if (curr.id < head.id)
		{
			i_lambda = curr.id;
			i_beta   = head.id;
		}
		else
		{
			i_lambda = head.id;
			i_beta   = curr.id;
		}
		
		JIntDoubleTuple[] aRes = ((OneVsAllDecoder)c_dec).predictAll(getFeatureArray());
		JIntDoubleTuple   res;
		String label, trans;
		int    index;
		
		if (curr.id < head.id && t_map.indexToLabel(aRes[0].i).equals(LB_SHIFT))
			return maxId;
		
		for (int i=0; i<aRes.length; i++)
		{
			res = aRes[i];
			
			label = t_map.indexToLabel(res.i);
			index = label.indexOf(LB_DELIM);
			if (index == -1)	continue;
			trans = label.substring(0, index);
			
			if (trans.matches(sTrans))
			{
				if (max.value < res.d)
				{
					String deprel = label.substring(index+1);
					max.set(deprel, res.d);
					maxId = head.id;
				}	break;
			}
		}
		
		return maxId;
	}
	
	private void postProcessConditional()
	{
		int currId, n = d_tree.size();
		DepNode curr;
		
		for (currId=1; currId<n; currId++)
		{
			if (d_tree.get(currId).hasHead)	continue;
			curr = d_copy.get(currId);

			i_lambda = currId - 1;
			i_beta   = currId;
			
			if (isShift(d_copy))
				printInstance(LB_SHIFT, getFeatureArray());
		
			if (currId < curr.headId)
			{
				i_lambda = currId;
				i_beta   = curr.headId;
			}
			else
			{
				i_lambda = curr.headId;
				i_beta   = currId;
			}
			
			printInstance(getGoldLabel(d_copy), getFeatureArray());
		}
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
			
		i_lambda = i_beta++;
		
		if (i_flag == FLAG_PRINT_TRANSITION)
		{
			if (isDeterministic)	printTransition("DT-SHIFT", "");
			else					printTransition("NT-SHIFT", "");
		}
	}
	
	/** Performs a no-arc transition. */
	private void noArc()
	{
		if      (i_flag == FLAG_PRINT_LEXICON )	addTags      (LB_NO_ARC);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(LB_NO_ARC, getFeatureArray());
		
		i_lambda--;
		
		if (i_flag == FLAG_PRINT_TRANSITION)	printTransition("NO-ARC", "");
	}
	
	private void leftPop(DepNode lambda, DepNode beta, String deprel, double score)
	{
		String  label = LB_LEFT_POP + LB_DELIM + deprel;
		
	    if      (i_flag == FLAG_PRINT_LEXICON)  addTags      (label);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label, getFeatureArray());

		lambda.setHead(beta.id, deprel, score);
		lambda.isSkip = true;
		if (lambda.id < beta.leftDepId)	beta.leftDepId = lambda.id;
		i_lambda--;
		
		if (i_flag == FLAG_PRINT_TRANSITION)
			printTransition("LEFT-POP", lambda.id+" <-"+deprel+"- "+beta.id);
	}
	
	/**
	 * Performs a left-arc transition.
	 * @param lambda lambda_1[0]
	 * @param beta   beta[0]
	 * @param deprel dependency label between <code>lambda</code> and <code>beta</code>
	 * @param score  dependency score between <code>lambda</code> and <code>beta</code>
	 */
	private void leftArc(DepNode lambda, DepNode beta, String deprel, double score)
	{
		String  label = LB_LEFT_ARC + LB_DELIM + deprel;
		
	    if      (i_flag == FLAG_PRINT_LEXICON)  addTags      (label);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label, getFeatureArray());

		lambda.setHead(beta.id, deprel, score);
		if (lambda.id < beta.leftDepId)	beta.leftDepId = lambda.id;
		i_lambda--;
		
		if (i_flag == FLAG_PRINT_TRANSITION)
			printTransition("LEFT-ARC", lambda.id+" <-"+deprel+"- "+beta.id);
	}
	
	/**
	 * Performs a right-arc transition.
	 * @param lambda lambda_1[0]
	 * @param beta   beta[0]
	 * @param deprel dependency label between lambda_1[0] and beta[0]
	 * @param score  dependency score between lambda_1[0] and beta[0]
	 */
	private void rightArc(DepNode lambda, DepNode beta, String deprel, double score)
	{
		String label = LB_RIGHT_ARC + LB_DELIM + deprel;
		
		if      (i_flag == FLAG_PRINT_LEXICON)	addTags      (label);
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label, getFeatureArray());

		beta.setHead(lambda.id, deprel, score);
		if (lambda.rightDepId < beta.id)	lambda.rightDepId = beta.id;
		i_lambda--;
		
		if (i_flag == FLAG_PRINT_TRANSITION)
			printTransition("RIGHT-ARC", lambda.id+" -"+deprel+"-> "+beta.id);
	}
	
	/**
	 * Prints the current transition.
	 * @param trans transition
	 * @param arc   lambda_1[0] <- deprel -> beta[0]
	 */
	private void printTransition(String trans, String arc)
	{
		StringBuilder build = new StringBuilder();
		
		// operation
		build.append(trans);
		build.append("\t");
		
		// lambda_1
		build.append("[");
		if (i_lambda >= 0)	build.append(0);
		if (i_lambda >= 1)	build.append(":"+i_lambda);
		build.append("]\t");
		
		// lambda_2
		build.append("[");
		if (getLambda2Count() > 0)	build.append(i_lambda+1);
		if (getLambda2Count() > 1)	build.append(":"+(i_beta-1));
		build.append("]\t");
		
		// beta
		build.append("[");
		if (i_beta < d_tree.size())		build.append(i_beta);
		if (i_beta <= d_tree.size())	build.append(":"+(d_tree.size()-1));
		build.append("]\t");
		
		// transition
		build.append(arc);
		f_out.println(build.toString());
	}
	
	/** @return number of nodes in lambda_2 (list #2) */
	private int getLambda2Count()
	{
		return i_beta - (i_lambda+1);
	}
	
	// ---------------------------- getFtr*() ----------------------------
	
	private IntArrayList getFeatureArray()
	{
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		
		addNgramFeatures      (arr, idx);
		addPunctuationFeatures(arr, idx);
		
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
	 * This method is called from {@link ShiftPopParser#getFeatureArray()}.
	 */
	private void addPunctuationFeatures(IntArrayList arr, int[] beginIndex)
	{
		int index, n = t_map.n_punctuation;
		
		index = d_tree.getRightNearestPunctuation(i_lambda, i_beta-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;		// 86.12 -> 86.30 (+0.18)
		
		index = d_tree.getRightNearestPunctuation(i_beta, d_tree.size()-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;		// 86.30 -> 86.33 (+0.03)
		
		index = d_tree.getLeftNearestPunctuation(i_beta, i_lambda+1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;		// 86.33 -> 86.36 (+0.03)
		
	/*	index = d_tree.getLeftNearestPunctuation(i_lambda, 1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;		// 86.30 -> 86.29 (-0.01) */	
	}
}