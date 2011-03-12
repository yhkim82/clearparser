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

import clear.decode.BinaryDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLEval;
import clear.engine.AbstractTrain;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.model.BinaryModel;
import clear.parse.AbstractDepParser;
import clear.parse.VoiceDetectorDep;
import clear.reader.AbstractReader;
import clear.reader.SRLReader;
import clear.train.AbstractTrainer;
import clear.util.IOUtil;

/**
 * Trains conditional dependency parser.
 * <b>Last update:</b> 11/19/2010
 * @author Jinho D. Choi
 */
public class VoiceDevelop extends AbstractTrain
{
	@Option(name="-t", usage="feature template file", required=true, metaVar="REQUIRED")
	private String s_featureXml = null;
	@Option(name="-i", usage="training file", required=true, metaVar="REQUIRED")
	private String s_trainFile  = null;
	@Option(name="-d", usage="development file", required=true, metaVar="REQUIRED")
	private String s_devFile    = null; 
	
	private DepFtrXml   t_xml   = null;
	private DepFtrMap   t_map   = null;
	private BinaryModel m_model = null;
	
	public void initElements() {}
	
	protected void train() throws Exception
	{
		printConfig();
		String instanceFile = "instaces.ftr";
		trainer_type = AbstractTrainer.ST_BINARY;
		
		trainDepParser(VoiceDetectorDep.FLAG_PRINT_LEXICON , null, null);
		trainDepParser(VoiceDetectorDep.FLAG_PRINT_INSTANCE, instanceFile, null);
		m_model = (BinaryModel)trainModel(instanceFile, null);
		trainDepParser(VoiceDetectorDep.FLAG_PREDICT, s_devFile+".voice", null);
		
		new File(ENTRY_LEXICA).delete();
		new File(instanceFile).delete();
	}
	
/*	private void trainMallet(String instanceFile)
	{
		ArrayList<String[]> xs = new ArrayList<String[]>();
		ArrayList<String>   ys = new ArrayList<String>();
		
		BufferedReader fin = IOUtil.createBufferedFileReader(instanceFile);
		String line;
		
		try
		{
			while ((line = fin.readLine()) != null)
			{
				String[] tmp = line.split(" ");
				
				xs.add(Arrays.copyOfRange(tmp, 1, tmp.length));
				ys.add(tmp[0]);
			}
		}
		catch (IOException e) {e.printStackTrace();}
		
		String[][] axs = new String[xs.size()][];
		String[]   ays = new String[ys.size()];
		
		xs.toArray(axs);
		ys.toArray(ays);
	}*/
	
	/** Trains the dependency parser. */
	private void trainDepParser(byte flag, String outputFile, JarArchiveOutputStream zout) throws Exception
	{
		VoiceDetectorDep parser  = null;
		BinaryDecoder    decoder = null;
		PrintStream      fout    = null;
		
		if (flag == VoiceDetectorDep.FLAG_PRINT_LEXICON)
		{
			System.out.println("\n* Save lexica");
			parser = new VoiceDetectorDep(flag, s_featureXml);
		}
		else if (flag == VoiceDetectorDep.FLAG_PRINT_INSTANCE)
		{
			System.out.println("\n* Print training instances");
			System.out.println("- loading lexica");
			parser = new VoiceDetectorDep(flag, t_xml, ENTRY_LEXICA, outputFile);	
		}
		else if (flag == VoiceDetectorDep.FLAG_PREDICT)
		{
			System.out.println("\n* Predict");
			decoder = new BinaryDecoder(m_model);
			fout    = IOUtil.createPrintFileStream(outputFile);
			parser  = new VoiceDetectorDep(AbstractDepParser.FLAG_PREDICT, t_xml, t_map, decoder);
		}
		
		String  inputFile;
		boolean isTrain;
		
		if (flag == VoiceDetectorDep.FLAG_PREDICT)
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
		
		parser.setLanguage(s_language);
		reader.setLanguage(s_language);
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			
			if (flag == VoiceDetectorDep.FLAG_PREDICT)
				fout.println(tree+"\n");
			if (n % 1000 == 0)
				System.out.printf("\r- parsing: %dK", n/1000);
		}
		
		System.out.println("\r- parsing: "+n);
		
		if (flag == VoiceDetectorDep.FLAG_PRINT_LEXICON)
		{
			System.out.println("- saving");
			parser.saveTags(ENTRY_LEXICA);
			t_xml = parser.getDepFtrXml();
		}
		else if (flag == VoiceDetectorDep.FLAG_PRINT_INSTANCE)
		{
			parser.closeOutputStream();
			t_map = parser.getDepFtrMap();
		}
		else if (flag == VoiceDetectorDep.FLAG_PREDICT)
		{
			fout.close();
			
			for (int i=0; i<3; i++)
			{
				double precision = 100d * parser.correct[i] / parser.precision[i];
				double recall    = 100d * parser.correct[i] / parser.recall[i];
				
				System.out.println("Precision: "+precision);
				System.out.println("Recall   : "+recall);
				System.out.println("F1-score : "+SRLEval.getF1(precision, recall));
				System.out.println();
			}
		}
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
		VoiceDevelop developer = new VoiceDevelop();
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
