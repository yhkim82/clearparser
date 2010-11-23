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

import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.kohsuke.args4j.Option;

import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.model.OneVsAllModel;
import clear.parse.ShiftEagerParser;
import clear.reader.AbstractReader;
import clear.reader.DepReader;
import clear.reader.RichReader;
import clear.util.IOUtil;

/**
 * Trains conditional dependency parser.
 * <b>Last update:</b> 11/19/2010
 * @author Jinho D. Choi
 */
public class TestDepSECon extends AbstractTrain
{
	private final int MAX_ITER = 10;
	
	@Option(name="-d", usage="development file", required=true, metaVar="REQUIRED")
	private String s_devFile = null; 
	
	private StringBuilder s_build = null;
	private DepFtrXml     t_xml   = null;
	private DepFtrMap     t_map   = null;
	private OneVsAllModel m_model = null;
	
	public TestDepSECon(String[] args)
	{
		super(args);
	}
	
	protected void train() throws Exception
	{
		printConfig();
		
		int    i = 0;
		String instanceFile = "instaces.ftr";
		String log          = "\n== Iteration: "+i+" ==\n";
		
		s_build = new StringBuilder();
		s_build.append(log);
		System.out.print(log);
		
		trainDepParser(ShiftEagerParser.FLAG_PRINT_LEXICON , null,         null);
		trainDepParser(ShiftEagerParser.FLAG_PRINT_INSTANCE, instanceFile, null);
		m_model = (OneVsAllModel)trainModel(instanceFile, null);
		
		double prevAcc = 0, currAcc;
		
		do
		{
			currAcc = trainDepParser(ShiftEagerParser.FLAG_PREDICT, s_devFile+".parse."+i, null);
			if (currAcc <= prevAcc)	break;

			prevAcc = currAcc;
			trainDepParser(ShiftEagerParser.FLAG_TRAIN_CONDITIONAL, instanceFile, null);
			
			log = "\n== Iteration: "+(++i)+" ==\n";
			s_build.append(log);
			System.out.print(log);

			m_model = null;
			m_model = (OneVsAllModel)trainModel(instanceFile, null);
		}
		while (i < MAX_ITER);
		
		new File(ENTRY_LEXICA).delete();
		new File(instanceFile).delete();
		System.out.println(s_build.toString());
	}
	
	/** Trains the dependency parser. */
	private double trainDepParser(byte flag, String outputFile, JarArchiveOutputStream zout) throws Exception
	{
		ShiftEagerParser parser  = null;
		OneVsAllDecoder  decoder = null;
		PrintStream      fout    = null;
		
		if (flag == ShiftEagerParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("\n* Save lexica");
			parser = new ShiftEagerParser(flag, s_featureXml);
		}
		else if (flag == ShiftEagerParser.FLAG_PRINT_INSTANCE)
		{
			System.out.println("\n* Print training instances");
			System.out.println("- loading lexica");
			parser = new ShiftEagerParser(flag, t_xml, ENTRY_LEXICA, outputFile);
		}
		else if (flag == ShiftEagerParser.FLAG_PREDICT)
		{
			System.out.println("\n* Predict");
			decoder = new OneVsAllDecoder(m_model);
			parser  = new ShiftEagerParser(flag, t_xml, t_map, decoder); 
			fout    = IOUtil.createPrintFileStream(outputFile);
		}
		else if (flag == ShiftEagerParser.FLAG_TRAIN_CONDITIONAL)
		{
			System.out.println("\n* Train conditional");
			decoder = new OneVsAllDecoder(m_model);
			parser  = new ShiftEagerParser(flag, t_xml, t_map, decoder, outputFile);
		}
		
		AbstractReader<DepNode, DepTree> reader = null;
		DepTree tree;	int n;
		
		String  inputFile;
		boolean isTrain;
		
		if (flag == ShiftEagerParser.FLAG_PREDICT)
		{
			inputFile = s_devFile;
			isTrain   = false;
		}
		else
		{
			inputFile = s_trainFile;
			isTrain   = true;
		}
		
		if      (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader (inputFile, isTrain);
		else if (s_format.equals(AbstractReader.FORMAT_RICH))	reader = new RichReader(inputFile, isTrain);
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			
			if (flag == ShiftEagerParser.FLAG_PREDICT)
				fout.println(tree+"\n");
			if (n % 1000 == 0)
				System.out.printf("\r- parsing: %dK", n/1000);
		}
		
		System.out.println("\r- parsing: "+n);
		
		if (flag == ShiftEagerParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("- saving");
			parser.saveTags(ENTRY_LEXICA);
			t_xml = parser.getDepFtrXml();
		}
		else if (flag == ShiftEagerParser.FLAG_PRINT_INSTANCE)
		{
			parser.closeOutputStream();
			t_map = parser.getDepFtrMap();
		}
		else if (flag == ShiftEagerParser.FLAG_PREDICT)
		{
			fout.close();
			
			String[] args = {"-g", "/data/choijd/opt/clearparser/wsj/conll-tst.auto", "-s", outputFile};
			String   log  = "\n* Development accuracy\n";
			
			System.out.print(log);
			DepEvaluate eval = new DepEvaluate(args);
			
			s_build.append(log);
			s_build.append("- LAS: "+eval.getLas()+"\n");
			s_build.append("- UAS: "+eval.getUas()+"\n");
			s_build.append("- LS : "+eval.getLs()+"\n");
			
			return eval.getLas();
		}
		else if (flag == ShiftEagerParser.FLAG_TRAIN_CONDITIONAL)
		{
			parser.closeOutputStream();
		}
		
		return 0;
	}
	
	protected void printConfig()
	{
		super.printConfig();
		System.out.println("- dev_file   : "+s_devFile);
		System.out.println("- feature_xml: "+s_featureXml);
	}
	
	static public void main(String[] args)
	{
		new TestDepSECon(args);
	}
}
