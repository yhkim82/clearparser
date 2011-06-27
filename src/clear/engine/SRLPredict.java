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
package clear.engine;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.SRLFtrMap;
import clear.ftr.xml.SRLFtrXml;
import clear.parse.AbstractDepParser;
import clear.parse.AbstractSRLParser;
import clear.parse.SRLParser;
import clear.reader.AbstractReader;
import clear.reader.CoNLLXReader;
import clear.reader.DepReader;
import clear.reader.PosReader;
import clear.reader.RawReader;
import clear.reader.SRLReader;
import clear.util.IOUtil;

/**
 * Predicts dependency trees.
 * @author Jinho D. Choi
 * <b>Last update:</b> 6/29/2010
 */
public class SRLPredict extends AbstractCommon
{
	protected final String TAG_PREDICT = "predict";

	@Option(name="-i", usage="input file", required=true, metaVar="REQUIRED")
	private String s_inputFile  = null;
	@Option(name="-o", usage="output file", required=true, metaVar="REQUIRED")
	private String s_outputFile = null;
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile  = null;
	/** Tokenizing modelFile */
	private String s_tokModel   = null;
	/** Part-of-speech tagging modelFile */
	private String s_posModel   = null;
	/** Dependency parsing tagging modelFile */
	private String s_depModel   = null;
	/** Morphological dictionary directory */
	private String s_morphDict  = null;

	public SRLPredict(String[] args)
	{
		CmdLineParser cmd = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
			init();
			predict();
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public SRLPredict(String configFile, String inputFile, String outputFile, String modelFile, String morphDict)
	{
		s_configFile = configFile;
		s_inputFile  = inputFile;
		s_outputFile = outputFile;
		s_modelFile  = modelFile;
		
		try
		{
			init();
			predict();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void predict() throws Exception
	{
		ZipInputStream zin = new ZipInputStream(new FileInputStream(s_modelFile));
		ZipEntry zEntry;	String entry;
		
		SRLFtrXml         xml     = null;
		SRLFtrMap[]       map     = new SRLFtrMap[2];
		OneVsAllDecoder[] decoder = new OneVsAllDecoder[2];
		
		printConfig();
		System.out.println("\n* Predict");
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			if (zEntry.getName().equals(ENTRY_FEATURE))
			{
				System.out.println("- loading feature template");
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
				StringBuilder  build  = new StringBuilder();
				String string;

				while ((string = reader.readLine()) != null)
				{
					build.append(string);
					build.append("\n");
				}
				
				xml = new SRLFtrXml(new ByteArrayInputStream(build.toString().getBytes()));
			}
			else if ((entry = zEntry.getName()).startsWith(ENTRY_LEXICA))
			{
				int i = Integer.parseInt(entry.substring(entry.lastIndexOf(".")+1));
				System.out.println("- loading lexica");
				map[i] = new SRLFtrMap(new BufferedReader(new InputStreamReader(zin)));
			}
			else if (zEntry.getName().startsWith(ENTRY_MODEL))
			{
				int i = Integer.parseInt(entry.substring(entry.lastIndexOf(".")+1));
				System.out.println("- loading model");
				decoder[i] = new OneVsAllDecoder(new BufferedReader(new InputStreamReader(zin)));
			}
		}
		
		AbstractReader<DepNode, DepTree> reader = null;
		
		if      (s_format.equals(AbstractReader.FORMAT_RAW))	reader = new RawReader   (s_inputFile, s_language, s_tokModel, s_posModel, s_morphDict);
		else if (s_format.equals(AbstractReader.FORMAT_POS))	reader = new PosReader   (s_inputFile, s_language, s_morphDict);
		else if (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader   (s_inputFile, true);
		else if (s_format.equals(AbstractReader.FORMAT_CONLLX))	reader = new CoNLLXReader(s_inputFile, true);
		else if (s_format.equals(AbstractReader.FORMAT_SRL))	reader = new SRLReader   (s_inputFile, false);
		
		AbstractDepParser parser = null;
		
		if (s_format.equals(AbstractReader.FORMAT_RAW) || s_format.equals(AbstractReader.FORMAT_POS))
			parser = DepPredict.getDepParser(s_depModel);
		
		AbstractSRLParser labeler = new SRLParser(AbstractSRLParser.FLAG_PREDICT, xml, map, decoder);
		
		reader.setLanguage(s_language);
		labeler.setLanguage(s_language);
		
		PrintStream fout = IOUtil.createPrintFileStream(s_outputFile);
		DepTree     tree;
		
		int n = 0;

		while (true)
		{
			tree = reader.nextTree();
			if (tree == null)	break;
			
			if (s_format.equals(AbstractReader.FORMAT_RAW) || s_format.equals(AbstractReader.FORMAT_POS))
				parser.parse(tree);
			
			if (!s_format.equals(AbstractReader.FORMAT_SRL))
				tree.setPredicates(s_language);
			
			labeler.parse(tree);	n++;
			fout.println(tree+"\n");
			if (n%100 == 0)	System.out.print("\r- labeling: "+n);
		}	System.out.println("\r- labeling: "+n);
		
		fout.flush(); 	fout.close();
	}
	
	protected void initElements()
	{
		if (!s_format.equals(AbstractReader.FORMAT_SRL))
		{
			Element ePredict = getElement(e_config, TAG_PREDICT);
			Element element;
			
			if ((element = getElement(ePredict, TAG_PREDICT_TOK_MODEL)) != null)
				s_tokModel = element.getTextContent().trim();
			
			if ((element = getElement(ePredict, TAG_PREDICT_POS_MODEL)) != null)
				s_posModel = element.getTextContent().trim();
			
			if ((element = getElement(ePredict, TAG_PREDICT_DEP_MODEL)) != null)
				s_depModel = element.getTextContent().trim();
			
			if ((element = getElement(ePredict, TAG_PREDICT_MORPH_DICT)) != null)
				s_morphDict = element.getTextContent().trim();
		}
	}
	
	protected void printConfig()
	{
		System.out.println("* Configurations");
		System.out.println("- language   : "+s_language);
		System.out.println("- format     : "+s_format);
		System.out.println("- model_file : "+s_modelFile);
		System.out.println("- input_file : "+s_inputFile);
		System.out.println("- output_file: "+s_outputFile);
	}
	
	static public void main(String[] args)
	{
		new SRLPredict(args);
	}
}
