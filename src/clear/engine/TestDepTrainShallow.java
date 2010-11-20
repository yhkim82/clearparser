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

import java.io.File;
import java.io.PrintStream;

import org.kohsuke.args4j.Option;

import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.model.OneVsAllModel;
import clear.parse.RLShallowParser;
import clear.reader.AbstractReader;
import clear.reader.CoNLLReader;
import clear.reader.DepReader;
import clear.util.IOUtil;

/**
 * Trains dependency parser.
 * <b>Last update:</b> 11/17/2010
 * @author Jinho D. Choi
 */
public class TestDepTrainShallow extends AbstractTrain
{
	@Option(name="-e", usage="evaluation file", required=true, metaVar="REQUIRED")
	private String s_evalFile = null; 
	@Option(name="-p", usage="parse file", required=true, metaVar="REQUIRED")
	private String s_parseFile = null;
	
	private DepFtrXml     t_xml;
	private DepFtrMap     t_map;
	private OneVsAllModel m_model;
	
	public TestDepTrainShallow(String[] args)
	{
		super(args);
	}
	
	protected void train() throws Exception
	{
		printConfig();
		String instanceFile = "instances.ftr";
		
		trainDepParser(RLShallowParser.FLAG_PRINT_LEXICON , null);
		trainDepParser(RLShallowParser.FLAG_PRINT_INSTANCE, instanceFile);
		
		m_model = trainModel(instanceFile, null);
		new File(instanceFile).delete();
		trainDepParser(RLShallowParser.FLAG_PREDICT, s_parseFile);
		
		String[] eArgs = {"-g", s_evalFile, "-s", s_parseFile, "-b", "1"};
		System.out.println();
		new DepEvaluate(eArgs);
	}
	
	/** Trains the dependency parser. */
	private void trainDepParser(byte flag, String outputFile) throws Exception
	{
		RLShallowParser parser = null;
		PrintStream      fout   = null;
		
		if (flag == RLShallowParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("\n* Save lexica");
			parser = new RLShallowParser(flag, s_featureXml);
		}
		else if (flag == RLShallowParser.FLAG_PRINT_INSTANCE)
		{
			System.out.println("\n* Print training instances");
			System.out.println("- loading lexica");
			parser = new RLShallowParser(flag, t_xml, ENTRY_LEXICA, outputFile);
		}
		else if (flag == RLShallowParser.FLAG_PREDICT)
		{
			System.out.println("\n* Predict");
			parser = new RLShallowParser(flag, t_xml, t_map, new OneVsAllDecoder(m_model)); 
			fout   = IOUtil.createPrintFileStream(outputFile);
		}
		
		AbstractReader<DepNode, DepTree> reader = null;
		DepTree tree;		int n;
		String  filename;	boolean isTrain;
		
		if (flag == RLShallowParser.FLAG_PREDICT)
		{
			filename = s_evalFile;
			isTrain  = false;
		}
		else
		{
			filename = s_trainFile;
			isTrain  = true;
		}
		
		if (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader  (filename, isTrain);
		else 											reader = new CoNLLReader(filename, isTrain);
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			if (flag == RLShallowParser.FLAG_PREDICT)	fout.println(tree+"\n");
			if (n % 1000 == 0)	System.out.printf("\r- parsing: %dK", n/1000);
		}	System.out.println("\r- parsing: "+n);
		
		if (flag == RLShallowParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("- saving");
			parser.saveTags(ENTRY_LEXICA);
			t_xml = parser.getDepFtrXml();
		}
		else if (flag == RLShallowParser.FLAG_PRINT_INSTANCE)
		{
			parser.closeOutputStream();
			t_map = parser.getDepFtrMap();
			new File(ENTRY_LEXICA).delete();
		}
		else if (flag == RLShallowParser.FLAG_PREDICT)
		{
			fout.close();
		}
	}
	
	protected void printConfig()
	{
		super.printConfig();
		System.out.println("- eval_file  : "+s_evalFile);
		System.out.println("- parse_file : "+s_parseFile);
	}
	
	static public void main(String[] args)
	{
		new TestDepTrainShallow(args);
	}
}
