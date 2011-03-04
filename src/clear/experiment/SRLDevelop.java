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
package clear.experiment;

import java.io.File;
import java.io.PrintStream;

import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.engine.AbstractTrain;
import clear.engine.SRLEvaluate;
import clear.ftr.map.SRLFtrMap;
import clear.ftr.xml.SRLFtrXml;
import clear.model.OneVsAllModel;
import clear.parse.AbstractSRLParser;
import clear.parse.SRLParser;
import clear.reader.AbstractReader;
import clear.reader.SRLReader;
import clear.util.IOUtil;

/**
 * Trains conditional dependency parser.
 * <b>Last update:</b> 11/19/2010
 * @author Jinho D. Choi
 */
public class SRLDevelop extends AbstractTrain
{
	private final int MAX_ITER = 5;
	
	@Option(name="-t", usage="feature template file", required=true, metaVar="REQUIRED")
	private String s_featureXml = null;
	@Option(name="-i", usage="training file", required=true, metaVar="REQUIRED")
	private String s_trainFile  = null;
	@Option(name="-d", usage="development file", required=true, metaVar="REQUIRED")
	private String s_devFile    = null; 
	
	private StringBuilder   s_build = null;
	private SRLFtrXml       t_xml   = null;
	private SRLFtrMap[]     t_map   = null;
	private OneVsAllModel[] m_model = null;
	
	public void initElements() {}
	
	protected void train() throws Exception
	{
		printConfig();
		
		int    i = 0;
		String instanceFile[] = {"instaces0.ftr", "instaces1.ftr"};
		String log          = "\n== Bootstrapping: "+i+" ==\n";
		
		s_build = new StringBuilder();
		t_map   = new SRLFtrMap[instanceFile.length];
		m_model = new OneVsAllModel[instanceFile.length];
		s_build.append(log);
		System.out.print(log);
		
		trainDepParser(SRLParser.FLAG_TRAIN_LEXICON , null, null);
		trainDepParser(SRLParser.FLAG_TRAIN_INSTANCE, instanceFile, null);
		
		for (int j=0; j<instanceFile.length; j++)
			m_model[j] = (OneVsAllModel)trainModel(instanceFile[j], null);
		
		double prevAcc = 100, currAcc;
		
		do
		{
			String[] labelFile = {s_devFile+".label."+i};
			currAcc = trainDepParser(SRLParser.FLAG_PREDICT, labelFile, null);
			if (currAcc >= 84)	System.out.println("== BINGO ==");
			if (currAcc <= prevAcc)	break;
			
			prevAcc = currAcc;
			trainDepParser(SRLParser.FLAG_TRAIN_CONDITIONAL, instanceFile, null);
			
			log = "\n== Bootstrapping: "+(++i)+" ==\n";
			s_build.append(log);
			System.out.print(log);

			m_model = new OneVsAllModel[instanceFile.length];
			for (int j=0; j<instanceFile.length; j++)
				m_model[j] = (OneVsAllModel)trainModel(instanceFile[j], null);
		}
		while (i < MAX_ITER);
		
		new File(ENTRY_LEXICA+".0").delete();
		new File(ENTRY_LEXICA+".1").delete();
		for (String filename : instanceFile)	new File(filename).delete();
		System.out.println(s_build.toString());
	}
	
	/** Trains the dependency parser. */
	private double trainDepParser(byte flag, String[] outputFile, JarArchiveOutputStream zout) throws Exception
	{
		AbstractSRLParser labeler = null;
		OneVsAllDecoder[] decoder = null;
		PrintStream[]     fout    = null;
		String[] lexiconFile = {ENTRY_LEXICA+".0", ENTRY_LEXICA+".1"};
		
		if (flag == SRLParser.FLAG_TRAIN_LEXICON)
		{
			System.out.println("\n* Save lexica");
			labeler = new SRLParser(flag, s_featureXml);
		}
		else if (flag == SRLParser.FLAG_TRAIN_INSTANCE)
		{
			System.out.println("\n* Print training instances");
			System.out.println("- loading lexica");
			
			labeler = new SRLParser(flag, t_xml, lexiconFile, outputFile);
		}
		else if (flag == SRLParser.FLAG_PREDICT)
		{
			System.out.println("\n* Predict");
			
			decoder = new OneVsAllDecoder[m_model.length];
			for (int i=0; i<decoder.length; i++)
				decoder[i] = new OneVsAllDecoder(m_model[i]);
			
			fout = new PrintStream[outputFile.length];
			for (int i=0; i<fout.length; i++)
				fout[i] = IOUtil.createPrintFileStream(outputFile[i]);
			
			labeler = new SRLParser(SRLParser.FLAG_PREDICT, t_xml, t_map, decoder);
		}
		else if (flag == SRLParser.FLAG_TRAIN_CONDITIONAL)
		{
			System.out.println("\n* Train conditional");
			
			decoder = new OneVsAllDecoder[m_model.length];
			for (int i=0; i<decoder.length; i++)
				decoder[i] = new OneVsAllDecoder(m_model[i]);
			
			labeler = new SRLParser(flag, t_xml, t_map, decoder, outputFile);
		}
		
		String  inputFile;
		boolean isTrain;
		
		if (flag == SRLParser.FLAG_PREDICT)
		{
			inputFile = s_devFile;
			isTrain   = false;
		}
		else
		{
			inputFile = s_trainFile;
			isTrain   = true;
		}
		
		AbstractReader<DepNode, DepTree> reader = new SRLReader(inputFile, isTrain);
		DepTree tree;	int n;
		
		labeler.setLanguage(s_language);
		reader.setLanguage(s_language);
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			labeler.parse(tree);
			
			if (flag == SRLParser.FLAG_PREDICT)
				fout[0].println(tree+"\n");
			if (n % 1000 == 0)
				System.out.printf("\r- parsing: %dK", n/1000);
		}
		
		System.out.println("\r- parsing: "+n);
		
		if (flag == SRLParser.FLAG_TRAIN_LEXICON)
		{
			System.out.println("- saving");
			labeler.saveTags(lexiconFile);
			t_xml = labeler.getFtrXml();
		}
		else if (flag == SRLParser.FLAG_TRAIN_INSTANCE)
		{
			labeler.closeOutputStream();
			t_map = labeler.getFtrMap();
		}
		else if (flag == SRLParser.FLAG_PREDICT)
		{
			for (int i=0; i<fout.length; i++)
				fout[i].close();
			
			String[] args = {"-g", s_devFile, "-s", outputFile[0]};
			String   log  = "\n* Development accuracy\n";
			
			System.out.print(log);
			SRLEvaluate eval = new SRLEvaluate(args);
			
			s_build.append(log);
			s_build.append("- F1: "+eval.getF1()+"\n");
			
			return eval.getF1();
		}
		else if (flag == SRLParser.FLAG_TRAIN_CONDITIONAL)
		{
			labeler.closeOutputStream();
		}
		
		return 0;
	}
	
	protected void printConfig()
	{
		System.out.println("* Configurations");
		System.out.println("- language   : "+s_language);
		System.out.println("- format     : "+s_format);
		System.out.println("- parser     : "+s_depParser);
		System.out.println("- feature_xml: "+s_featureXml);
		System.out.println("- train_file : "+s_trainFile);
		System.out.println("- dev_file   : "+s_devFile);
	}
	
	static public void main(String[] args)
	{
		SRLDevelop developer = new SRLDevelop();
		CmdLineParser    cmd = new CmdLineParser(developer);
		
		try
		{
			cmd.parseArgument(args);
			developer.init();
			developer.train();
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
