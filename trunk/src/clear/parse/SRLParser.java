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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import clear.decode.AbstractDecoder;
import clear.decode.OneVsAllDecoder;
import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLArg;
import clear.dep.srl.SRLHead;
import clear.dep.srl.SRLInfo;
import clear.dep.srl.SRLProb;
import clear.ftr.map.SRLFtrMap;
import clear.ftr.xml.SRLFtrXml;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;

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
	
	/** For {@link SRLParser#FLAG_TRAIN_BOOST} only. */
	protected DepTree d_copy = null;
	
	private boolean b_ArgAdded;
	private String  s_prevArg;
	
	/** {@link AbstractSRLParser#FLAG_TRAIN_PROBABILITY}. */
	public SRLParser(byte flag)
	{
		super(flag);
	}
	
	/** {@link AbstractSRLParser#FLAG_TRAIN_LEXICON}. */
	public SRLParser(byte flag, String xmlFile)
	{
		super(flag, xmlFile);
	}

	/** {@link AbstractSRLParser#FLAG_TRAIN_INSTANCE}. */
	public SRLParser(byte flag, SRLFtrXml xml, String[] lexiconFile)
	{
		super(flag, xml, lexiconFile);
	}
	
	/** {@link AbstractSRLParser#FLAG_PREDICT} or {@link AbstractSRLParser#FLAG_TRAIN_BOOST}. */
	public SRLParser(byte flag, SRLFtrXml xml, SRLFtrMap[] map, AbstractDecoder[] decoder)
	{
		super(flag, xml, map, decoder);
	}
	
	/** Initializes member variables. */
	private void init(DepTree tree)
	{
		tree.setSubcat();
		
		d_tree   = tree;
		i_beta   = tree.nextPredicateId(0);
		i_lambda = i_beta - 1;
		i_dir    = DIR_LEFT;
		
		ls_args = new ArrayList<SRLArg>();
		ls_argn = new ArrayList<String>();
		s_args  = new HashSet<String>();
		
		if (i_flag == FLAG_TRAIN_BOOST)
		{
			d_copy = tree.clone();
			d_tree.clearSRLHeads();
		}
		
		b_ArgAdded = false;
	}
	
	/** Parses <code>tree</code>. */
	public void parse(DepTree tree)
	{
		init(tree);
		
		while (i_beta < tree.size())
		{
			if (i_lambda <= 0 || i_lambda >= tree.size() || isShift())
				shift();
			else if (i_flag == FLAG_PREDICT)
				predict();
			else if (i_flag == FLAG_TRAIN_BOOST)
				trainConditional();
			else
				train();
		}
	}
	
	/** Trains the dependency tree ({@link SRLParser#d_tree}). */
	private void train()
	{
		String label = getGoldLabel(d_tree);
		
		if (label.equals(LB_NO_ARC))
			noArc();
		else
			yesArc(label, 1d);
	}
	
	/** Predicts dependencies. */
	private void predict()
	{
		predictAux(getFeatureArray());
	}
		
	private void predictAux(IntArrayList ftr)
	{
		SRLFtrMap       map = getFtrMap();
		OneVsAllDecoder dec = getDecoder();
		JIntDoubleTuple res = dec.predict(ftr);
		
		String label = (res.i < 0) ? LB_NO_ARC : map.indexToLabel(res.i);
		
		if (label.equals(LB_NO_ARC))
			noArc();
		else
			yesArc(label, res.d);
	}
	
	private void trainConditional()
	{
		String    gLabel = getGoldLabel(d_copy);
		IntArrayList ftr = getFeatureArray();
		
		saveInstance(gLabel, ftr);
		predictAux(ftr);
	}
	
	private String getGoldLabel(DepTree tree)
	{
		DepNode lambda = tree.get(i_lambda);
		String  label;
		
		if ((label = lambda.getSRLLabel(i_beta)) != null)
			return label;
		else
			return LB_NO_ARC;
	}
	
	/**
	 * Performs a shift transition.
	 * @param isDeterministic true if this is called for a deterministic-shift.
	 */
	private void shift()
	{
		if (i_dir == DIR_RIGHT)
		{
			shiftRight();
			i_beta = d_tree.nextPredicateId(i_beta);
		}
		
		i_dir *= -1;
		i_lambda = i_beta + i_dir;
	}
	
	/** Called from {@link SRLParser#shift()} for {@link AbstractSRLParser#DIR_RIGHT}. */
	private void shiftRight()
	{
		if (i_flag == FLAG_PREDICT || i_flag == FLAG_TRAIN_BOOST)
		{
			String label;
			
			for (SRLArg arg : ls_args)
			{
				label = arg.label.substring(1);
				d_tree.get(arg.argId).addSRLHead(i_beta, label);
			}
		}
		else if (i_flag == FLAG_TRAIN_PROBABILITY)
		{
			DepNode beta = d_tree.get(i_beta);
			p_prob.add1dArgs(beta, s_args);
			p_prob.add2dArgs(beta, ls_args);
		}

		ls_args.clear();
		ls_argn.clear();
		s_args .clear();
	}
	
	/** Performs a no-arc transition. */
	private void noArc()
	{
		trainInstance(LB_NO_ARC);
		i_lambda += i_dir;
	}
	
	private void yesArc(String label, double score)
	{
		trainInstance(label);
		
		if (label.matches("A\\d"))	ls_argn.add(label);
		if (i_dir == DIR_LEFT)		label = SRLProb.SYM_PREV + label;
		else						label = SRLProb.SYM_NEXT + label;
		
		SRLArg arg = new SRLArg(i_lambda, label, score);
		
		ls_args.add(arg);
		s_args .add(label);
		
		i_lambda += i_dir;
		b_ArgAdded = true;
	}
	
	private void trainInstance(String label)
	{
		if (i_flag == FLAG_TRAIN_LEXICON)
			addTags(label);
		else if (i_flag == FLAG_TRAIN_INSTANCE)
			saveInstance(label, getFeatureArray());
	}
		
	// ---------------------------- getFtr*() ----------------------------
	
	protected void addLexica(SRLFtrMap map, boolean isAI)
	{
		addNgramLexica(map);
		addSetLexica  (map, 0, d_tree.getDeprelDepSet(i_beta));
		addStrLexica  (map, 1, getPredArg());
	//	addSetLexica  (map, 2, getArgnSet());
	}
	
	protected void addSetLexica(SRLFtrMap map, int ftrId, AbstractCollection<String> ftrs)
	{
		for (String ftr : ftrs)
			map.addFtr(ftrId, ftr);
	}
	
	protected void addStrLexica(SRLFtrMap map, int ftrId, String ftr)
	{
		if (ftr != null)	map.addFtr(ftrId, ftr);
	}
	
	protected String getPredArg()
	{
		if (i_dir == DIR_RIGHT)	return null;
		SRLInfo info = d_tree.get(i_lambda).srlInfo;
		
		if (!info.heads.isEmpty())
		{
			for (int i=info.heads.size()-1; i>=0; i--)
			{
				SRLHead head = info.heads.get(i);
				
				if (head.headId < i_beta)
				{
					DepNode pred = d_tree.get(head.headId);
					return pred.lemma+"_"+head.label;
				}
			}
		}
		
		return null;
	}
	
	protected HashSet<String> getArgnSet()
	{
		HashSet<String> set = new HashSet<String>();
		String sub;
		DepNode beta = d_tree.get(i_beta);
		
		for (String label : s_args)
		{
			if ((sub = label.substring(1)).matches("A\\d"))
				set.add(beta.lemma+"_"+sub);
		}
		
		return set;
	}
	
	protected IntArrayList getFeatureArray()
	{
		// add features
		IntArrayList arr = new IntArrayList();
		int idx[] = {1};
		SRLFtrMap map = getFtrMap();
		
		addNgramFeatures (arr, idx, map);
		addBinaryFeatures(arr, idx);
		addSetFeatures   (arr, idx, map, 0, d_tree.getDeprelDepSet(i_beta));
		addStrFeatures   (arr, idx, map, 1, getPredArg());
	//	addSetFeatures   (arr, idx, map, 2, getArgnSet());
		
		return arr;
	}
	
	protected void addBinaryFeatures(IntArrayList arr, int[] idx)
	{
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (lambda.headId == i_beta)	arr.add(idx[0]);
		else if (beta.headId == i_lambda)	arr.add(idx[0]+1);
		
		while (DepLib.M_VC.matcher(beta.deprel).matches())
		{
			beta = d_tree.get(beta.headId);
			
			if (d_tree.getDeprelDepSet(beta.id).contains(DepLib.DEPREL_SBJ))
			{
				arr.add(idx[0]+2);
				break;
			}
		}

		idx[0] += 3;
	}
	
	protected void addSetFeatures(IntArrayList arr, int[] idx, SRLFtrMap map, int ftrId, AbstractCollection<String> ftrs)
	{
		IntArrayList list = new IntArrayList();
		int i;
		
		for (String ftr : ftrs)
		{
			if ((i = map.ftrToIndex(ftrId, ftr)) >= 0)
				list.add(idx[0]+i);
		}
		
		int[] tmp = list.toArray();
		Arrays.sort(tmp);
		arr.add(tmp, 0, tmp.length);
		idx[0] += map.n_ftr[ftrId];
	}
	
	protected void addStrFeatures(IntArrayList arr, int[] idx, SRLFtrMap map, int ftrId, String ftr)
	{
		if (ftr != null)
		{
			int index = map.ftrToIndex(ftrId, ftr);
			if (index >= 0)	arr.add(idx[0]+index);	
		}
		
		idx[0] += map.n_ftr[ftrId];
	}
	
//	==================================== PROBABILITY ====================================
	
/*	@SuppressWarnings("unchecked")
	protected void setPredictArgs()
	{
		if (i_flag == FLAG_TRAIN_PROBABILITY || !d_tree.isRange(i_beta))	return;
		
		DepNode                         beta = d_tree.get(i_beta);
		ObjectDoubleOpenHashMap<String> pMap = p_prob.getProb1d(beta, i_dir);
		if (pMap == null)	return;
		
		ArrayList<JObjectDoubleTuple<String>> argn = new ArrayList<JObjectDoubleTuple<String>>();
		String label;	double prob;
		
		p_argn.clear();
		
		for (ObjectCursor<String> cur : pMap.keySet())
		{
			label = cur.value;
			prob  = pMap.get(label);	
			
			if (label.matches("A\\d"))
			{
				argn.add(new JObjectDoubleTuple<String>(label, prob));
			}
			else if (m_argm.containsKey(label))
			{
				if (label.matches("AM-MOD|AM-NEG"))
					continue;
				else
					dev += m_argm.get(label);
			}
			else if (label.startsWith("C-"))
			{
				sub = label.substring(2);
				
				if (!s_argn.contains(sub) && !s_argm.contains(sub))
				{
					prob[index] = p_prob.d_smooth;
					continue;
				}
			}
		}
	
		Collections.sort(argn);
		p_argn.clear();
		
		for (JObjectDoubleTuple<String> tup : argn)
			p_argn.add(tup.object);
	}
	
	protected String getPredictSeq()
	{
		if (p_argn.isEmpty())	return null;
		
		StringBuilder build = new StringBuilder();
		DepNode       beta  = d_tree.get(i_beta);

		build.append(beta.lemma);
		build.append("_");
		build.append(p_argn.get(0));
		
		return build.toString();
	}*/
	
//	==================================== SHIFT ====================================
	
	protected boolean isShift()
	{
		DepNode pred = d_tree.get(i_beta);
		
		if (i_flag == FLAG_PREDICT)
		{
			return isShiftProb(pred);
		}
		else
		{
			DepTree tree = (i_flag == FLAG_TRAIN_BOOST) ? d_copy : d_tree;
			
		}
		
		return false;
	}
	
	protected boolean isShiftProb(DepNode pred)
	{
		return false;
	}
	
	public double getScore(ObjectDoubleOpenHashMap<String> prob1d)
	{
		double score = 0;
		
		
		return score;
	}
	
	/**
	 * This method is called from {@link SRLParser#train()}.
	 * @return true if non-deterministic shift needs to be performed 
	 */
	protected boolean isShift(DepTree tree)
	{
		for (int i=i_lambda; 0<i && i<tree.size(); i+=i_dir)
		{
			if (tree.get(i).isSRLHead(i_beta))
				return false;
		}

		return true;
	}
}