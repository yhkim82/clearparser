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

import java.io.PrintStream;

import clear.decode.AbstractMultiDecoder;
import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.FtrLib;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.ftr.xml.FtrTemplate;
import clear.ftr.xml.FtrToken;
import clear.train.kernel.AbstractKernel;
import clear.util.DSUtil;
import clear.util.IOUtil;
import clear.util.tuple.JIntDoubleTuple;
import clear.util.tuple.JObjectDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class ShiftEagerParser
{
	/** Flag to print lexicons */
	static public final byte FLAG_PRINT_LEXICON     = 0;
	/** Flag to print training instances */
	static public final byte FLAG_PRINT_INSTANCE    = 1;
	/** Flag to print transitions */
	static public final byte FLAG_PRINT_TRANSITION  = 2;
	/** Flag to predict dependencies */
	static public final byte FLAG_PREDICT           = 3;
	/** Flag to train automatic dependencies */
	static public final byte FLAG_TRAIN_CONDITIONAL = 4;
	
	/** Label of Shift transition */
	static public final String LB_SHIFT     = "SH";
	/** Label of No-Arc transition */
	static public final String LB_NO_ARC    = "NA";
	/** Label of Left-Arc transition */
	static public final String LB_LEFT_ARC  = "LA";
	/** Label of Right-Arc transition */
	static public final String LB_RIGHT_ARC = "RA";
	/** Delimiter between transition and dependency label */
	static public final String LB_DELIM     = "-";
	
	/** {@link ShiftEagerParser#FLAG_*} */
	private byte                 i_flag;
	/** Feature templates */
	private DepFtrXml            t_xml;
	/** Feature mappings */
	private DepFtrMap            t_map;
	/** Machine learning decoder */
	private AbstractMultiDecoder c_dec;
	/** Prints training instances */
	private PrintStream          f_out;
	
	/** Current dependency tree */
	private DepTree d_tree;
	/** Index of lambda_1 */
	private int     i_lambda;
	/** Index of beta */
	private int     i_beta;
	
	/** For train-conditional only */
	private DepTree d_copy;
	private int i_correct = 0, i_total = 0;
	
	/**
	 * Initializes this parser for {@link ShiftEagerParser#FLAG_PRINT_LEXICON} or {@link ShiftEagerParser#FLAG_PRINT_TRANSITION}.
	 * @param flag        {@link ShiftEagerParser#i_flag}
	 * @param filename    name of a feature-xml/transition-output file
	 */
	public ShiftEagerParser(byte flag, String filename)
	{
		i_flag = flag;
		
		if (flag == FLAG_PRINT_LEXICON)
		{
			t_xml = new DepFtrXml(filename);
			t_map = new DepFtrMap(t_xml);
		}
		else if (flag == FLAG_PRINT_TRANSITION)
		{
			f_out = IOUtil.createPrintFileStream(filename);
		}
	}

	/**
	 * Initializes this parser for {@link ShiftEagerParser#FLAG_PRINT_INSTANCE}.
	 * @param flag        {@link ShiftEagerParser#i_flag}
	 * @param featureXml  name of a feature XML file
	 * @param lexiconFile name of a lexicon file
	 * @param instanceFile  name of a instance/transition output file
	 */
	public ShiftEagerParser(byte flag, String featureXml, String lexiconFile, String instanceFile)
	{
		i_flag = flag;
		t_xml  = new DepFtrXml(featureXml);
		t_map  = new DepFtrMap(t_xml, lexiconFile);
		f_out  = IOUtil.createPrintFileStream(instanceFile);
	}
	
	/**
	 * {@link ShiftEagerParser#FLAG_PREDICT}
	 * @param flag  {@link ShiftEagerParser#i_flag}
	 */
	public ShiftEagerParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractMultiDecoder decoder)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = map;
		c_dec  = decoder;
	}
	
	/** {@link ShiftEagerParser#FLAG_TRAIN_CONDITIONAL} */
	public ShiftEagerParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractMultiDecoder decoder, String instanceFile)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = map;
		c_dec  = decoder;
		f_out  = IOUtil.createPrintFileStream(instanceFile);
	}
	
	/** Initializes lambda1, lambda2, and beta using <code>tree</code>. */
	private void init(DepTree tree)
	{
		d_tree   = tree;
		i_lambda = 0;
		i_beta   = 1;
		
		if (i_flag == FLAG_PRINT_TRANSITION)	printTransition("", "");
		if (i_flag == FLAG_TRAIN_CONDITIONAL)			d_copy = tree.clone();
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta < tree.size())	// beta is not empty
		{
			d_tree.n_trans++;
			
			if (i_lambda == -1)			// lambda_1 is empty: deterministic shift
				shift(true);	
			else if (i_flag == FLAG_PREDICT)
				predict();
			else if (i_flag == FLAG_TRAIN_CONDITIONAL)
				trainConditional();
			else
				train();
		}
		
		if      (i_flag == FLAG_PRINT_TRANSITION)	f_out.println();
		else if (i_flag == FLAG_PREDICT)			postProcess();
	}
	
	/** Trains the dependency tree ({@link ShiftEagerParser#d_tree}). */
	private void train()
	{
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (lambda.headId == beta.id)	leftArc (lambda, beta, lambda.deprel, 1d);
		else if (lambda.id == beta.headId)	rightArc(lambda, beta, beta  .deprel, 1d);
		else if (isShift(d_tree))			shift(false);
		else								noArc();
	}
	
	/**
	 * This method is called from {@link ShiftEagerParser#train()}.
	 * @return true if non-deterministic shift needs to be performed 
	 */
	private boolean isShift(DepTree tree)
	{
		DepNode beta = tree.get(i_beta);
		
		for (int i=i_lambda; i>=0; i--)
		{
			DepNode lambda = tree.get(i);
			
			if (lambda.headId == beta.id || lambda.id == beta.headId)
				return false;
		}

		return true;
	}
	
	/** Predicts dependencies. */
	private void predict()
	{
		JIntDoubleTuple res = c_dec.predict(getFeatureArray());
		
		String  label  = (res.i < 0) ? LB_NO_ARC : t_map.indexToLabel(res.i);
		int     index  = label.indexOf(LB_DELIM);
		String  trans  = (index > 0) ? label.substring(0,index) : label;
		String  deprel = (index > 0) ? label.substring(index+1) : "";
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (trans.equals( LB_LEFT_ARC) && !d_tree.isAncestor(lambda, beta) && lambda.id != DepLib.ROOT_ID)
			leftArc (lambda, beta, deprel, res.d);
		else if (trans.equals(LB_RIGHT_ARC) && !d_tree.isAncestor(beta, lambda))
			rightArc(lambda, beta, deprel, res.d);
		else if (trans.equals(LB_SHIFT))
			shift(false);
		else
			noArc();
	}
	
	private void trainConditional()
	{
		IntArrayList ftr = getFeatureArray();
		
		DepNode lambda = d_copy.get(i_lambda);
		DepNode beta   = d_copy.get(i_beta);
		String  gLabel = null;
		
		if      (lambda.headId == beta.id)	gLabel = LB_LEFT_ARC  + LB_DELIM + lambda.deprel;
		else if (lambda.id == beta.headId)	gLabel = LB_RIGHT_ARC + LB_DELIM + beta  .deprel;
		else if (isShift(d_copy))			gLabel = LB_SHIFT;
		else								gLabel = LB_NO_ARC;
		
		f_out.println(t_map.labelToIndex(gLabel) + AbstractKernel.COL_DELIM + DSUtil.toString(ftr," "));

		JIntDoubleTuple res = c_dec.predict(ftr);
		
		String sLabel = (res.i < 0) ? LB_NO_ARC : t_map.indexToLabel(res.i);
		int    index  = sLabel.indexOf(LB_DELIM);
		String trans  = (index > 0) ? sLabel.substring(0,index) : sLabel;
		String deprel = (index > 0) ? sLabel.substring(index+1) : "";
		lambda = d_tree.get(i_lambda);
		beta   = d_tree.get(i_beta);
		
		if      (trans.equals( LB_LEFT_ARC) && !d_tree.isAncestor(lambda, beta) && lambda.id != DepLib.ROOT_ID)
			leftArc (lambda, beta, deprel, res.d);
		else if (trans.equals(LB_RIGHT_ARC) && !d_tree.isAncestor(beta, lambda))
			rightArc(lambda, beta, deprel, res.d);
		else if (trans.equals(LB_SHIFT))
			shift(false);
		else
			noArc();
		
		if (gLabel.equals(sLabel))	i_correct++;
		i_total++;
	}
	
	public double getAccuracy()
	{
		return (double)i_correct/i_total;
	}
	
	public DepFtrXml getDepFtrXml()
	{
		return t_xml;
	}
	
	public DepFtrMap getDepFtrMap()
	{
		return t_map;
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
				maxId = getMaxHeadId(curr, node, maxId, max, LB_LEFT_ARC);
			}
		
			if (maxId != -1)	curr.setHead(maxId, max.object, max.value);
		}
	}
	
	/** This method is called from {@link ShiftEagerParser#postProcess()}. */
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
		
		JIntDoubleTuple[] aRes = c_dec.predictAll(getFeatureArray());
		JIntDoubleTuple   res;
		String label, trans;
		int    index;
		
		for (int i=0; i<aRes.length; i++)
		{
			res = aRes[i];
			
			label = t_map.indexToLabel(res.i);
			index = label.indexOf(LB_DELIM);
			if (index == -1)	continue;
			trans = label.substring(0, index);
			
			if (trans.equals(sTrans))
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
	
	/**
	 * Performs a shift transition.
	 * @param isDeterministic true if this is called for a deterministic-shift.
	 */
	private void shift(boolean isDeterministic)
	{
		if (!isDeterministic)
		{
			if      (i_flag == FLAG_PRINT_LEXICON )	addTags      (LB_SHIFT);
			else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(LB_SHIFT);
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
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(LB_NO_ARC);
		
		i_lambda--;
		
		if (i_flag == FLAG_PRINT_TRANSITION)	printTransition("NO-ARC", "");
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
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label);

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
		else if (i_flag == FLAG_PRINT_INSTANCE)	printInstance(label);

		beta.setHead(lambda.id, deprel, score);
		if (lambda.rightDepId < beta.id)	lambda.rightDepId = beta.id;
		i_lambda--;
		
		if (i_flag == FLAG_PRINT_TRANSITION)
			printTransition("RIGHT-ARC", lambda.id+" -"+deprel+"-> "+beta.id);
	}
	
	/**
	 * Adds tags to {@link ShiftEagerParser#t_map}.
	 * @param label <trainsition>[-<dependency label>]
	 */
	private void addTags(String label)
	{
		t_map.addLabel(label);
		
		if      (label.startsWith(LB_LEFT_ARC))		addRules( 1);
		else if (label.startsWith(LB_RIGHT_ARC))	addRules(-1);
		
		getFeatureArray();
	}
	
	private void addRules(int dir)
	{
		FtrTemplate[] template = t_xml.a_rule_templates;
		int i, n = template.length;
		String ftr;
		
		for (i=0; i<n; i++)
		{
			ftr = getFeature(template[i]);
			if (ftr == null)	continue;
			
			t_map.addRule(i, ftr, dir);
		}
	}
	
	/** Saves tags from {@link ShiftEagerParser#t_map} to <code>lexiconFile</code>. */
	public void saveTags(String lexiconFile, int ngramCutoff)
	{
		t_map.save(t_xml, lexiconFile, ngramCutoff);
	}
	
	public void closeOutputStream()
	{
		f_out.flush();	f_out.close();
	}
	
	/**
	 * Prints the current training instance.
	 * @param label <trainsition>[-<dependency label>]
	 */
	private void printInstance(String label)
	{
		f_out.println(getInstance(label));
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
	
	/**
	 * Returns an instance consists of feature set represented as an integer array and <code>label</code>.
	 * @param label <trainsition>[-<dependency label>]
	 */
	private String getInstance(String label)
	{
		return t_map.labelToIndex(label) + AbstractKernel.COL_DELIM + DSUtil.toString(getFeatureArray()," ");
	//	return t_map.labelToIndex(label) + AbstractKernel.COL_DELIM + DSUtil.toString(getFeatureArray(),":1 ");
	}
	
	private IntArrayList getFeatureArray()
	{
		if (i_flag == FLAG_PRINT_LEXICON)	// store features for configuration files
		{
			FtrTemplate[][] templates = t_xml.a_ngram_templates;
			int i, j, n, m = templates.length;
			FtrTemplate[] template;
			String ftr;
			
			for (j=0; j<m; j++)
			{
				template = templates[j];
				n        = template.length;
				
				for (i=0; i<n; i++)
				{
					ftr = getFeature(template[i]);
					if (ftr == null)	continue;
					
					t_map.addNgram(j, ftr);
				}
			}
			
			DepNode b0 = d_tree.get(i_beta);
			if (b0.isDeprel(DepLib.DEPREL_P))	t_map.addPunctuation(b0.form);
			
			return null;
		}
		
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		
		addNgramFeatures      (arr, idx);
		addRuleFeatures       (arr, idx);
		addPunctuationFeatures(arr, idx);
		
		return arr;
	}
	
	private void addNgramFeatures(IntArrayList arr, int[] beginIndex)
	{
		FtrTemplate[][] templates = t_xml.a_ngram_templates;
		
		int i, j, n, m = templates.length, size, value;
		ObjectIntOpenHashMap<String> map;
		FtrTemplate[] template;
		String ftr;
		
		for (j=0; j<m; j++)
		{
			map  = t_map.getNgramHashMap(j);
			size = t_map.n_ngram[j];
			
			template = templates[j];
			n        = template.length;
			
			for (i=0; i<n; i++)
			{
				ftr = getFeature(template[i]);
				if (ftr != null)
				{
					value = map.get(ftr);
					if (value > 0)	arr.add(beginIndex[0]+value-1);
				}
				
				beginIndex[0] += size;
			}
		}
	}
	
	private void addRuleFeatures(IntArrayList arr, int[] beginIndex)
	{
		int i, n = t_xml.a_rule_templates.length, value;
		String ftr;
		
		for (i=0; i<n; i++)
		{
			ftr = getFeature(t_xml.a_rule_templates[i]);
			if (ftr != null)
			{
				value = t_map.ruleToIndex(i, ftr);
				if      (value < 0)	arr.add(beginIndex[0]);
				else if (value > 0)	arr.add(beginIndex[0]+1);
			}
		
			beginIndex[0] += 2;
		}
	}
	
	/**
	 * Adds punctuation features.
	 * This method is called from {@link ShiftEagerParser#getFeatureArray()}.
	 */
	private void addPunctuationFeatures(IntArrayList arr, int[] beginIndex)
	{
		int index, n = t_map.n_punctuation;
		
		index = d_tree.getRightNearestPunctuation(i_lambda, i_beta-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
		index = d_tree.getLeftNearestPunctuation(i_beta, i_lambda+1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;

		index = d_tree.getRightNearestPunctuation(i_beta, d_tree.size()-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;	// 88.54 -> 88.62
		
	/*	index = d_tree.getLeftNearestPunctuation(i_lambda, 1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += t_map.n_punctuation;	// 88.62 -> 87.56(-)*/	
	}
	
	/** @return feature retrieved from <code>ftr</code>. */
	private String getFeature(FtrTemplate ftr)
	{
		StringBuilder build = new StringBuilder();
		int i, n = ftr.tokens.length;
		String field;
		
		for (i=0; i<n; i++)
		{
			field = getField(ftr.tokens[i]);
			if (field == null)	return null;
			
			if (i > 0)	build.append(FtrLib.TAG_DELIM);
			build.append(field);
		}
		
		return build.toString();
	}
		
	/** @return field retrieved from <code>token</code> */
	private String getField(FtrToken token)
	{
		int index = (token.source == DepFtrXml.LAMBDA) ? i_lambda : i_beta;
		index    += token.offset;
		
		if (!d_tree.isRange(index) || (token.source == DepFtrXml.LAMBDA && index == i_beta) || (token.source == DepFtrXml.BETA && index == i_lambda))
			return null;
	
		DepNode node = null;
		
		if      (token.relation == null)				node = d_tree.get(index);
		else if (token.relation.equals(DepFtrXml.R_HD))	node = d_tree.getHead(index);
		else if (token.relation.equals(DepFtrXml.R_LM))	node = d_tree.getLeftMostDependent(index);
		else if (token.relation.equals(DepFtrXml.R_RM))	node = d_tree.getRightMostDependent(index);
		
		if (node == null)	return null;
		
		if      (token.field.equals(DepFtrXml.F_FORM))		return node.form;
		else if (token.field.equals(DepFtrXml.F_LEMMA))		return node.lemma;
		else if (token.field.equals(DepFtrXml.F_POS))		return node.pos;
		else if (token.field.equals(DepFtrXml.F_DEPREL))	return node.getDeprel();
		
		System.err.println("Error: unspecified feature '"+token.field+"'");
		return null;
	}
}