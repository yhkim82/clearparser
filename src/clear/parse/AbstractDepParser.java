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
import java.util.ArrayList;
import java.util.regex.Matcher;

import clear.decode.AbstractDecoder;
import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.FtrLib;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.ftr.xml.FtrTemplate;
import clear.ftr.xml.FtrToken;
import clear.reader.DepReader;
import clear.train.kernel.AbstractKernel;
import clear.util.DSUtil;
import clear.util.IOUtil;
import clear.util.tuple.JIntDoubleTuple;
import clear.util.tuple.JObjectDoubleTuple;
import clear.util.tuple.JObjectObjectTuple;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;

/**
 * Shift-eager dependency parser.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
abstract public class AbstractDepParser
{
	/** Flag for shift-eager algortihm */
	static final public String ALG_SHIFT_EAGER = "shift-eager";
	/** Flag for shift-pop algortihm */
	static final public String ALG_SHIFT_POP   = "shift-pop";
	
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
	
	/** {@link ShiftEagerParser#FLAG_*} */
	protected byte            i_flag;
	/** Feature templates */
	protected DepFtrXml       t_xml;
	/** Feature mappings */
	protected DepFtrMap       t_map;
	/** Machine learning decoder */
	protected AbstractDecoder c_dec;
	/** Prints training instances */
	protected PrintStream     f_out;
	
	/** Current dependency tree */
	protected DepTree d_tree;
	/** Index of lambda_1 */
	protected int     i_lambda;
	/** Index of beta */
	protected int     i_beta;
	/** Language */
	protected String  s_language = DepReader.LANG_EN;
//	protected byte    i_kernel = AbstractKernel.KERNEL_BINARY;
	protected ArrayList<String> prev_transitions;
	
/*	final double[] N_coord = {0.19852856404476413, 0.1138994884501673, 0.0, 0.22533005735300438, 0.18045058647042403, 0.0, 0.0, 0.0, 0.14941652963010274, 0.0, 0.0, 0.13237477405153722, 0.0};
	final double[] B_coord = {0.0, 0.0, 0.0, 0.17333699913253287, 0.17204764906925044, 0.0, 0.0, 0.2039590631354908, 0.0, 0.0, 0.21516029181025698, 0.013083110936248277, 0.22241288591622072};
	final double[] p_coord = {0.0, 0.12749176751182362, 0.0, 0.15481143197864297, 0.14115159974523328, 0.0, 0.0, 0.1858712886046102, 0.0, 0.0, 0.1858712886046102, 0.01893133495046956, 0.1858712886046102};
	final double[] A_coord = {0.20192412812944133, 0.18967967639663277, 0.20695309937684486, 0.19700448234393791, 0.20443861375314312, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	final double[] f_coord = {0.0, 0.0, 0.0, 0.791885800150263, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.20811419984973703, 0.0};*/

	/** Initializes this parser for {@link AbstractDepParser#FLAG_PRINT_LEXICON} or {@link AbstractDepParser#FLAG_PRINT_TRANSITION}. */
	public AbstractDepParser(byte flag, String filename)
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
	
	/** Initializes this parser for {@link AbstractDepParser#FLAG_PRINT_INSTANCE}. */
	public AbstractDepParser(byte flag, DepFtrXml xml, String lexiconFile, String instanceFile)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = new DepFtrMap(t_xml, lexiconFile);
		f_out  = IOUtil.createPrintFileStream(instanceFile);
	}
	
	/** Initializes this parser for {@link AbstractDepParser#FLAG_PREDICT}. */
	public AbstractDepParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractDecoder decoder)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = map;
		c_dec  = decoder;
	}
	
	/** Initializes this parser for {@link AbstractDepParser#FLAG_TRAIN_CONDITIONAL}. */
	public AbstractDepParser(byte flag, DepFtrXml xml, DepFtrMap map, AbstractDecoder decoder, String instanceFile)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = map;
		c_dec  = decoder;
		f_out  = IOUtil.createPrintFileStream(instanceFile);
	}
	
	public void setLanguage(String language)
	{
		s_language = language;
	}
	
	protected void preProcess(DepTree tree)
	{
		if (s_language.equals(DepReader.LANG_EN))
			preProcessEn(tree);
		else if (s_language.equals(DepReader.LANG_CZ))
			preProcessCz(tree);
	}
	
	protected void preProcessEn(DepTree tree)
	{
		int i, j, size = tree.size();
		DepNode head;
		
		for (i=1; i<size; i++)
		{
			head = tree.get(i);
			
			if (head.isPosx("IN"))
			{
				for (j=i+1; j<size; j++)
				{
					if (tree.get(j).isPosx("NN.*|CD") && !(j+1 < size && tree.get(j+1).isPosx("NN.*|CD|POS")))
					{
						head.rightMostDep = tree.get(j);
						i = j;	break;
					}
				}
			}
		}
	}
	
	protected void preProcessCz(DepTree tree)
	{
		preProcessCzMorph(tree);
		preProcessCzCoord(tree);
	}
	
	protected void preProcessCzMorph(DepTree tree)
	{
		DepNode node;
		String  feat;
		
		for (int i=1; i<tree.size(); i++)
		{
			node = tree.get(i);
			
			if ((feat = node.getFeat(2)) != null)		// degree of Comparison
			{
				node.pos += feat;
			}
			if ((feat = node.getFeat(8)) != null)		// name
			{
				node.lemma = "$SEM="+feat+"$";
			}
			else if ((feat = node.getFeat(9)) != null)	// number
			{
				if (feat.equals("n"))	node.lemma = "$CRD$";
			}
		}
	}
	
	protected void preProcessCzCoord(DepTree tree)
	{
		int coordId, nextId, prevId, l, size = tree.size(), gap = 10, count, total;
		DepNode coord, prev, next;
		String[] prevFeats, nextFeats;
		String nextPos;
		double score, bestScore;
		JObjectObjectTuple<DepNode,DepNode> bestPair = new JObjectObjectTuple<DepNode,DepNode>(null,null);
		
		for (coordId=1; coordId<size; coordId++)
		{
			coord = tree.get(coordId);
			if (!coord.getFeat(9).equals("^") && !coord.lemma.matches(",|:|&|\\+"))	continue;
			bestScore = 0;
			
			for (nextId=coordId+1; nextId<=coordId+gap && nextId<size; nextId++)
			{
				next      = tree.get(nextId);
				nextFeats = next.feats.feats;
				nextPos   = nextFeats[9];
				total     = 0;
				
				for (String feat : nextFeats)
					if (feat != null)	total++;

				for (prevId=coordId-1; prevId>=coordId-gap && prevId>0; prevId--)
				{
					prev      = tree.get(prevId);
					prevFeats = prev.feats.feats;
					if (!nextPos.equals(prevFeats[9]))	continue;
					
					count = 0;
					
					for (l=0; l<nextFeats.length; l++)
					{
						if (l != 9 && nextFeats[l] != null && prevFeats[l] != null && nextFeats[l].equals(prevFeats[l]))
							count++;
					}
					
					score = (double)count / total;
					
					if (score > bestScore)
					{
						bestScore = score;
						bestPair.set(prev, next);
					}
					
					if (score >= 0.8)	break;
				}
			}
			
			if (bestScore > 0)
			{
				coord.leftMostDep  = bestPair.object1;
				coord.rightMostDep = bestPair.object2;
				bestPair.object1.coordHead = coord;
				bestPair.object2.coordHead = coord;
			}
		}
	}
	
	/** Parses <code>tree</code>. */
	abstract public    void parse(DepTree tree);
	abstract protected void addLexica();
	abstract protected IntArrayList getBinaryFeatureArray();
	abstract protected ArrayList<JIntDoubleTuple> getValueFeatureArray();
	
	public DepFtrXml getDepFtrXml()
	{
		return t_xml;
	}
	
	public DepFtrMap getDepFtrMap()
	{
		return t_map;
	}
	
	/**
	 * Adds tags to {@link ShiftEagerParser#t_map}.
	 * @param label <trainsition>[-<dependency label>]
	 */
	protected void addTags(String label)
	{
		t_map.addLabel(label);
		addLexica();
	}
	
	/** Saves tags from {@link AbstractDepParser#t_map} to <code>lexiconFile</code>. */
	public void saveTags(String lexiconFile)
	{
		t_map.save(t_xml, lexiconFile);
	}
	
	public void closeOutputStream()
	{
		f_out.flush();
		f_out.close();
	}
	
	/**
	 * Prints the current training instance.
	 * @param label <trainsition>[-<dependency label>]
	 */
	protected void printInstance(String label, IntArrayList ftr)
	{
		int index = t_map.labelToIndex(label);
		
		if (index >= 0)
		{
			f_out.println(index + AbstractKernel.COL_DELIM + DSUtil.toString(ftr, AbstractKernel.COL_DELIM));
			
		/*	{
				StringBuilder build = new StringBuilder();
				build.append(index);
				
				for (JIntDoubleTuple tup : getValueFeatureArray())
				{
					build.append(AbstractKernel.COL_DELIM);
					build.append(tup.i);
					build.append(AbstractKernel.FTR_DELIM);
					build.append(tup.d);
				}
				
				f_out.println(build.toString());
			}*/
		}
	}
	
	// ---------------------------- getFtr*() ----------------------------
	
	protected void addNgramLexica()
	{
		FtrTemplate[][] templates = t_xml.a_ngram_templates;
		FtrTemplate[]   template;
		int i, j, n, m = templates.length;
		String ftr;
		
		for (j=0; j<m; j++)
		{
			template = templates[j];
			n        = template.length;
			
			for (i=0; i<n; i++)
			{
				if ((ftr = getBinaryFeatures(template[i])) != null)
					t_map.addNgram(j, ftr);
			}
			
		/*	{
				for (i=0; i<n; i++)
				{
					for (JObjectDoubleTuple<String> oFtr : getValueFeatures(template[i]))
						t_map.addNgram(j, oFtr.object);
				}
			}*/
		}
	}
	
	protected void addNgramFeatures(IntArrayList arr, int[] beginIndex)
	{
		FtrTemplate[][] templates = t_xml.a_ngram_templates;
		FtrTemplate[]   template;
		int i, j, n, m = templates.length, size, value;
		String ftr;
		ObjectIntOpenHashMap<String> map;
		
		for (j=0; j<m; j++)
		{
			map  = t_map.getNgramHashMap(j);
			size = t_map.n_ngram[j];
			
			template = templates[j];
			n        = template.length;
			
			for (i=0; i<n; i++)
			{
				if ((ftr = getBinaryFeatures(template[i])) != null)
				{
					value = map.get(ftr);
					if (value > 0)	arr.add(beginIndex[0]+value-1);
				}
				
				beginIndex[0] += size;
			}
		}
	}
	
	protected void addNgramFeatures(ArrayList<JIntDoubleTuple> arr, int[] beginIndex)
	{
		FtrTemplate[][] templates = t_xml.a_ngram_templates;
		FtrTemplate[]   template;
		int i, j, n, m = templates.length, size, value;
		ObjectIntOpenHashMap<String> map;
		
		for (j=0; j<m; j++)
		{
			map  = t_map.getNgramHashMap(j);
			size = t_map.n_ngram[j];
			
			template = templates[j];
			n        = template.length;
			
			for (i=0; i<n; i++)
			{
				for (JObjectDoubleTuple<String> ftr : getValueFeatures(template[i]))
				{
					value = map.get(ftr.object);
					
					if (value > 0)
						arr.add(new JIntDoubleTuple(beginIndex[0]+value-1, ftr.value));
				}

				beginIndex[0] += size;
			}
		}
	}
		
	/** @return feature retrieved from <code>ftr</code>. */
	protected String getBinaryFeatures(FtrTemplate ftr)
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
	
	protected ArrayList<JObjectDoubleTuple<String>> getValueFeatures(FtrTemplate ftr)
	{
		return null;
	}
	
	protected void addLanguageSpecificLexica()
	{
		if (s_language.equals(DepReader.LANG_EN))
		{
			addEnPunctuationLexica();
		}
		else if (s_language.equals(DepReader.LANG_CZ))
		{
			addCzPunctuationLexica();
		}
	}
	
	protected void addEnPunctuationLexica()
	{
		DepNode b0 = d_tree.get(i_beta);
		
		if (b0.isDeprel(DepLib.DEPREL_P))	t_map.addPunctuation(b0.form);	
	}
	
	protected void addCzPunctuationLexica()
	{
		DepNode b0 = d_tree.get(i_beta);
		
		if (b0.isPos("Z"))	t_map.addPunctuation(b0.form);
	}
	
	protected void addLanguageSpecificFeatures(IntArrayList arr, int[] beginIndex)
	{
		if (s_language.equals(DepReader.LANG_EN))
		{
			addEnPunctuationFeatures(arr, beginIndex);
		}
		else if (s_language.equals(DepReader.LANG_CZ))
		{
			addCzPunctuationFeatures(arr, beginIndex);
			addCzCoordFeatures      (arr, beginIndex);
		//	addCzCaseFeatures       (arr, beginIndex);
		}
	}
	
	/**
	 * Adds punctuation features.
	 * This method is called from {@link ShiftPopParser#getFeatureArray()}.
	 */
	protected void addEnPunctuationFeatures(IntArrayList arr, int[] beginIndex)
	{
		int index, n = t_map.n_punctuation;
		
		index = d_tree.getRightNearestPunctuation(i_lambda, i_beta-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
		index = d_tree.getRightNearestPunctuation(i_beta, d_tree.size()-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
		index = d_tree.getLeftNearestPunctuation(i_beta, i_lambda+1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
	/*	index = d_tree.getLeftNearestPunctuation(i_lambda, 1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n; */
	}
	
	protected void addCzPunctuationFeatures(IntArrayList arr, int[] beginIndex)
	{
		int index, n = t_map.n_punctuation;
		
		index = d_tree.getRightNearestPunctuation(i_lambda, i_beta-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
	/*	index = d_tree.getRightNearestPunctuation(i_beta, d_tree.size()-1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
		index = d_tree.getLeftNearestPunctuation(i_beta, i_lambda+1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n;
		
		index = d_tree.getLeftNearestPunctuation(i_lambda, 1, t_map);
		if (index != -1)	arr.add(beginIndex[0] + index);
		beginIndex[0] += n; */ 	
	}
	
	protected void addCzCoordFeatures(IntArrayList arr, int[] beginIndex)
	{
		DepNode lambda = d_tree.get(i_lambda);
		DepNode beta   = d_tree.get(i_beta);
		
		if      (lambda.coordHead.id == i_beta)	arr.add(beginIndex[0]);
		else if (lambda.coordHead.id > 0)		arr.add(beginIndex[0]+1);
		if      (beta.coordHead.id == i_lambda)	arr.add(beginIndex[0]+2);

		beginIndex[0] += 3;
	}
	
	protected void addCzCaseFeatures(IntArrayList arr, int[] beginIndex)
	{
		if (d_tree.get(i_lambda).isPos("V"))
		{
			if (!d_tree.existsLeftDependent (i_lambda, "Sb"))	arr.add(beginIndex[0]);
			if (!d_tree.existsLeftDependent (i_lambda, "Obj"))	arr.add(beginIndex[0]+1);
			if (!d_tree.existsRightDependent(i_lambda, "Sb"))	arr.add(beginIndex[0]+2);
			if (!d_tree.existsRightDependent(i_lambda, "Obj"))	arr.add(beginIndex[0]+3);
		}
		
		if (d_tree.get(i_beta).isPos("V"))
		{
			if (!d_tree.existsLeftDependent (i_beta  , "Sb"))	arr.add(beginIndex[0]+4);
			if (!d_tree.existsLeftDependent (i_beta  , "Obj"))	arr.add(beginIndex[0]+5);
		}
		
		beginIndex[0] += 6;
	}
		
	/** @return field retrieved from <code>token</code> */
	protected String getField(FtrToken token)
	{
		int index = (token.source == DepFtrXml.LAMBDA) ? i_lambda : i_beta;
		index += token.offset;
		
		if (!d_tree.isRange(index) || (token.source == DepFtrXml.LAMBDA && index == i_beta) || (token.source == DepFtrXml.BETA && index == i_lambda))
			return null;
		
		DepNode node = null;
		
		if      (token.relation == null)			node = d_tree.get(index);
		else if (token.isRelation(DepFtrXml.R_HD))	node = d_tree.getHead(index);
		else if (token.isRelation(DepFtrXml.R_LM))	node = d_tree.getLeftMostDependent(index);
		else if (token.isRelation(DepFtrXml.R_RM))	node = d_tree.getRightMostDependent(index);
		
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(DepFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(DepFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(DepFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(DepFtrXml.F_DEPREL))
		{
			return node.getDeprel();
		}
		else if ((m = DepFtrXml.P_FEAT.matcher(token.field)).find())
		{
			int idx = Integer.parseInt(m.group(1));
			return node.getFeat(idx);
		}
		else if ((m = DepFtrXml.P_TRANS.matcher(token.field)).find())
		{
			int idx = prev_transitions.size() - Integer.parseInt(m.group(1)) - 1;
			return (idx >= 0) ? prev_transitions.get(idx) : null;
		}
		
	//	System.err.println("Error: unspecified feature '"+token.field+"'");
		return null;
	}
}