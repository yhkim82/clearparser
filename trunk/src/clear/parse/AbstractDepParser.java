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

import clear.decode.AbstractDecoder;
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
abstract public class AbstractDepParser
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
		i_flag    = flag;
		t_xml     = xml;
		t_map     = map;
		c_dec     = decoder;
		f_out     = IOUtil.createPrintFileStream(instanceFile);
	}
	
	/** Parses <code>tree</code>. */
	abstract public    void parse(DepTree tree);
	abstract protected void addLexica();
	
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
			f_out.println(index + AbstractKernel.COL_DELIM + DSUtil.toString(ftr, AbstractKernel.COL_DELIM));
	}
	
	protected void printInstance(String label, ArrayList<JIntDoubleTuple> ftr)
	{
		int index = t_map.labelToIndex(label);
		
		if (index >= 0)
		{
			StringBuilder build = new StringBuilder();
			build.append(index);
			
			for (JIntDoubleTuple tup : ftr)
			{
				build.append(AbstractKernel.COL_DELIM);
				build.append(tup.i);
				build.append(AbstractKernel.FTR_DELIM);
				build.append(tup.d);
			}
			
			f_out.println(build.toString());
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
				ftr = getFeature(template[i]);
				if (ftr == null)	continue;
				
				t_map.addNgram(j, ftr);
			}
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
	
	protected void addNgramFeatures(ArrayList<JIntDoubleTuple> arr, int[] beginIndex)
	{
		FtrTemplate[][] templates = t_xml.a_ngram_templates;
		FtrTemplate[]   template;
		int i, j, n, m = templates.length, size, value;
		JObjectDoubleTuple  <String> ftr;
		ObjectIntOpenHashMap<String> map;
		
		for (j=0; j<m; j++)
		{
			map  = t_map.getNgramHashMap(j);
			size = t_map.n_ngram[j];
			
			template = templates[j];
			n        = template.length;
			
			for (i=0; i<n; i++)
			{
				ftr = getFeatureValue(template[i]);
				if (ftr != null)
				{
					value = map.get(ftr.object);
					if (value > 0)	arr.add(new JIntDoubleTuple(beginIndex[0]+value-1, ftr.value));
				}
				
				beginIndex[0] += size;
			}
		}
	}
	
	/** @return feature retrieved from <code>ftr</code>. */
	protected String getFeature(FtrTemplate ftr)
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
	
	/** @return ("feature", probability) retrieved from <code>ftr</code>. */
	protected JObjectDoubleTuple<String> getFeatureValue(FtrTemplate ftr)
	{
		StringBuilder build = new StringBuilder();
		int i, n = ftr.tokens.length;
		JObjectDoubleTuple<String> field;
		double value = 1;
		
		for (i=0; i<n; i++)
		{
			field = getFieldValue(ftr.tokens[i]);
			if (field == null)	return null;
			
			if (i > 0)	build.append(FtrLib.TAG_DELIM);
			build.append(field.object);
			value *= field.value;
		}
		
		return new JObjectDoubleTuple<String>(build.toString(), value);
	}
		
	/** @return field retrieved from <code>token</code> */
	protected String getField(FtrToken token)
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
	
	/** @return ("field", probability) retrieved from <code>token</code> */
	protected JObjectDoubleTuple<String> getFieldValue(FtrToken token)
	{
		int index = (token.source == DepFtrXml.LAMBDA) ? i_lambda : i_beta;
		index    += token.offset;
		
		if (!d_tree.isRange(index) || (token.source == DepFtrXml.LAMBDA && index == i_beta) || (token.source == DepFtrXml.BETA && index == i_lambda))
			return null;
	
		DepNode node  = null;
		double  value = 1f;
		
		if (token.relation == null)
		{
			node = d_tree.get(index);
		}
		else if (token.relation.equals(DepFtrXml.R_HD))
		{
			node = d_tree.getHead(index);
			if (node != null)	value = d_tree.get(index).score;
		}
		else if (token.relation.equals(DepFtrXml.R_LM))
		{
			node = d_tree.getLeftMostDependent(index);
			if (node != null)	value = node.score;
		}
		else if (token.relation.equals(DepFtrXml.R_RM))
		{
			node = d_tree.getRightMostDependent(index);
			if (node != null)	value = node.score;
		}
		
		if (node == null)	return null;
		
		if      (token.field.equals(DepFtrXml.F_FORM))		return new JObjectDoubleTuple<String>(node.form       , value);
		else if (token.field.equals(DepFtrXml.F_LEMMA))		return new JObjectDoubleTuple<String>(node.lemma      , value);
		else if (token.field.equals(DepFtrXml.F_POS))		return new JObjectDoubleTuple<String>(node.pos        , value);
		else if (token.field.equals(DepFtrXml.F_DEPREL))	return new JObjectDoubleTuple<String>(node.getDeprel(), value);
		
		System.err.println("Error: unspecified feature '"+token.field+"'");
		return null;
	}
}