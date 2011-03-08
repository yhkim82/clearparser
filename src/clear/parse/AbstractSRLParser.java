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
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLArg;
import clear.ftr.FtrLib;
import clear.ftr.map.SRLFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.ftr.xml.FtrTemplate;
import clear.ftr.xml.FtrToken;
import clear.ftr.xml.SRLFtrXml;
import clear.reader.DepReader;
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
abstract public class AbstractSRLParser
{
	/** Flag to print lexicons */
	static public final byte FLAG_TRAIN_LEXICON     = 0;
	/** Flag to print training instances */
	static public final byte FLAG_TRAIN_INSTANCE    = 1;
	/** Flag to train automatic dependencies */
	static public final byte FLAG_TRAIN_CONDITIONAL = 2;
	/** Flag to predict dependencies */
	static public final byte FLAG_PREDICT           = 3;
	
	protected final byte DIR_LEFT  = -1;
	protected final byte DIR_RIGHT =  1;
	
	protected final byte IDX_LEFT  = 0;
	protected final byte IDX_RIGHT = 1;
	
	/** {@link AbstractDepParser#FLAG_*} */
	protected byte              i_flag;
	/** Feature templates */
	protected SRLFtrXml            t_xml;
	/** Feature mappings */
	protected SRLFtrMap[]       t_map;
	/** Machine learning decoder */
	protected AbstractDecoder[] c_dec;
	/** Prints training instances */
	protected PrintStream[]     f_out;
	
	/** Current dependency tree */
	protected DepTree d_tree;
	/** Index of lambda_1 */
	protected int     i_lambda;
	/** Index of beta */
	protected int     i_beta;
	/** Direction: -1 to the left, +1 to the right */
	protected byte    i_dir;
	/** Language */
	protected String  s_language = DepReader.LANG_EN;
	/** List of arguments for the current predicate */
	protected ArrayList<SRLArg> ls_args;
	
	/** Initializes this parser for {@link AbstractSRLParser#FLAG_TRAIN_LEXICON} or {@link AbstractSRLParser#FLAG_PRINT_TRANSITION}. */
	public AbstractSRLParser(byte flag, String filename)
	{
		i_flag = flag;
		
		if (flag == FLAG_TRAIN_LEXICON)
		{
			t_xml = new SRLFtrXml(filename);
			t_map = new SRLFtrMap[2];
			
			for (int i=0; i<t_map.length; i++)
				t_map[i] = new SRLFtrMap(t_xml);
		}
	}
	
	/** Initializes this parser for {@link AbstractSRLParser#FLAG_TRAIN_INSTANCE}. */
	public AbstractSRLParser(byte flag, SRLFtrXml xml, String[] lexiconFile, String[] instanceFile)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = new SRLFtrMap[lexiconFile.length];
		f_out  = new PrintStream[instanceFile.length];
		
		for (int i=0; i<t_map.length; i++)
			t_map[i] = new SRLFtrMap(t_xml, lexiconFile[i]); 
		
		for (int i=0; i<f_out.length; i++)
			f_out[i] = IOUtil.createPrintFileStream(instanceFile[i]);
	}
	
	/** Initializes this parser for {@link AbstractSRLParser#FLAG_PREDICT}. */
	public AbstractSRLParser(byte flag, SRLFtrXml xml, SRLFtrMap[] map, AbstractDecoder[] decoder)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = map;
		c_dec  = decoder;
	}
	
	/** Initializes this parser for {@link AbstractSRLParser#FLAG_TRAIN_CONDITIONAL}. */
	public AbstractSRLParser(byte flag, SRLFtrXml xml, SRLFtrMap[] map, AbstractDecoder[] decoder, String[] instanceFile)
	{
		i_flag = flag;
		t_xml  = xml;
		t_map  = map;
		c_dec  = decoder;
		f_out  = new PrintStream[instanceFile.length];
		
		for (int i=0; i<f_out.length; i++)
			f_out[i] = IOUtil.createPrintFileStream(instanceFile[i]);
	}
	
	public void setLanguage(String language)
	{
		s_language = language;
	}
		
	/** Parses <code>tree</code>. */
	abstract public    void parse(DepTree tree);
	abstract protected void addLexica(SRLFtrMap map);
	abstract protected IntArrayList getBinaryFeatureArray();
	abstract protected ArrayList<JIntDoubleTuple> getValueFeatureArray();
	
	public SRLFtrXml getFtrXml()
	{
		return t_xml;
	}
	
	public SRLFtrMap[] getFtrMap()
	{
		return t_map;
	}
	
	protected SRLFtrMap getIdxFtrMap()
	{
		return (i_dir == DIR_LEFT) ? t_map[IDX_LEFT] : t_map[IDX_RIGHT];
	}
	
	protected PrintStream getIdxPrintStream()
	{
		return (i_dir == DIR_LEFT) ? f_out[IDX_LEFT] : f_out[IDX_RIGHT];
	}
	
	protected AbstractDecoder getIdxDecoder()
	{
		return (i_dir == DIR_LEFT) ? c_dec[IDX_LEFT] : c_dec[IDX_RIGHT];
	}
	
	/**
	 * Adds tags to {@link ShiftEagerParser#t_map}.
	 * @param label <trainsition>[-<dependency label>]
	 */
	protected void addTags(String label)
	{
		SRLFtrMap map = getIdxFtrMap();
		
		map.addLabel(label);
		addLexica(map);	
	}
	
	/** Saves tags from {@link AbstractSRLParser#t_map} to <code>lexiconFile</code>. */
	public void saveTags(String[] lexiconFile)
	{
		for (int i=0; i<t_map.length; i++)
			t_map[i].save(t_xml, lexiconFile[i]);
	}
	
	public void closeOutputStream()
	{
		for (int i=0; i<f_out.length; i++)
			f_out[i].close();
	}
	
	/**
	 * Prints the current training instance.
	 * @param label <trainsition>[-<dependency label>]
	 */
	protected void printInstance(String label, IntArrayList ftr)
	{
		int index = getIdxFtrMap().labelToIndex(label);
		
		if (index >= 0)
		{
			PrintStream fout = getIdxPrintStream();
			fout.println(index + AbstractKernel.COL_DELIM + DSUtil.toString(ftr, AbstractKernel.COL_DELIM));
			
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
	
	protected void addNgramLexica(SRLFtrMap map)
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
					map.addNgram(j, ftr);
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
		SRLFtrMap tmap = getIdxFtrMap();
		String ftr;
		ObjectIntOpenHashMap<String> map;
		
		for (j=0; j<m; j++)
		{
			map  = tmap.getNgramHashMap(j);
			size = tmap.n_ngram[j];
			
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
	
	protected void addNgramFeatures(SRLFtrMap tmap, ArrayList<JIntDoubleTuple> arr, int[] beginIndex)
	{
		FtrTemplate[][] templates = t_xml.a_ngram_templates;
		FtrTemplate[]   template;
		int i, j, n, m = templates.length, size, value;
		ObjectIntOpenHashMap<String> map;
		
		for (j=0; j<m; j++)
		{
			map  = tmap.getNgramHashMap(j);
			size = tmap.n_ngram[j];
			
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
	
	/** @return field retrieved from <code>token</code> */
	protected String getField(FtrToken token)
	{
		int index = (token.source == SRLFtrXml.LAMBDA) ? i_lambda : i_beta;
		index += token.offset;
		
		if (!d_tree.isRange(index) || (token.source == SRLFtrXml.LAMBDA && index == i_beta) || (token.source == SRLFtrXml.BETA && index == i_lambda))
			return null;
		
		DepNode node = null;
		
		if      (token.relation == null)			node = d_tree.get(index);
		else if (token.isRelation(SRLFtrXml.R_HD))	node = d_tree.getHead(index);
		else if (token.isRelation(SRLFtrXml.R_LM))	node = d_tree.getLeftMostDependent(index);
		else if (token.isRelation(SRLFtrXml.R_RM))	node = d_tree.getRightMostDependent(index);
		else if (token.isRelation(SRLFtrXml.R_LS))	node = d_tree.getLeftSibling(index);
		else if (token.isRelation(SRLFtrXml.R_RS))	node = d_tree.getRightSibling(index);
		
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(SRLFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(SRLFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(SRLFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(SRLFtrXml.F_DEPREL))
		{
			return node.getDeprel();
		}
		else if ((m = SRLFtrXml.P_FEAT.matcher(token.field)).find())
		{
			int idx = Integer.parseInt(m.group(1));
			return node.getFeat(idx);
		}
		else if ((m = SRLFtrXml.P_SUBCAT_D.matcher(token.field)).find())
		{
			byte idx = Byte.parseByte(m.group(1));
			return d_tree.getSubcat(DepFtrXml.F_DEPREL, node.id, idx);
		}
		else if ((m = SRLFtrXml.P_SUBCAT_P.matcher(token.field)).find())
		{
			byte idx = Byte.parseByte(m.group(1));
			return d_tree.getSubcat(DepFtrXml.F_POS, node.id, idx);
		}
		else if ((m = SRLFtrXml.P_PATH_D.matcher(token.field)).find())
		{
			byte idx = Byte.parseByte(m.group(1));
			return d_tree.getPath(DepFtrXml.F_DEPREL, node.id, i_beta, idx);
		}
		else if ((m = SRLFtrXml.P_PATH_P.matcher(token.field)).find())
		{
			byte idx = Byte.parseByte(m.group(1));
			return d_tree.getPath(DepFtrXml.F_POS, node.id, i_beta, idx);
		}
		
	//	System.err.println("Error: unspecified feature '"+token.field+"'");
		return null;
	}
}