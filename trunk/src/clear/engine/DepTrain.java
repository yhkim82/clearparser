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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.parse.ShiftEagerParser;
import clear.reader.AbstractReader;
import clear.reader.CoNLLReader;
import clear.reader.DepReader;
import clear.train.OneVsAllTrainer;
import clear.train.algorithm.IAlgorithm;
import clear.train.algorithm.LibLinearL2;
import clear.train.algorithm.RRM;

/**
 * Trains dependency parser.
 * <b>Last update:</b> 6/29/2010
 * @author Jinho D. Choi
 */
public class DepTrain extends AbstractEngine
{
	private final String EXT_INSTANCE_FILE = ".ftr";
	
	@Option(name="-c", usage="configuration file", required=true, metaVar="REQUIRED")
	private String s_configFile = null;
	@Option(name="-t", usage="training file", required=true, metaVar="REQUIRED")
	private String s_trainFile  = null; 
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile  = null;
	@Option(name="-f", usage=ShiftEagerParser.FLAG_PRINT_LEXICON+": train model (default), "+ShiftEagerParser.FLAG_PRINT_TRANSITION+": print transitions", metaVar="OPTIONAL")
	private byte   i_flag       = ShiftEagerParser.FLAG_PRINT_LEXICON;
	/** Lexicon file */
	private String s_lexiconFile;
	/** Instance file */
	private String s_instanceFile;
	
	public DepTrain(String[] args)
	{
		CmdLineParser cmd  = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);

			if (!initConfigElement(s_configFile))	return;
			s_lexiconFile  = s_modelFile + EXT_LEXICON_FILE;
			s_instanceFile = s_modelFile + EXT_INSTANCE_FILE;
			
			if (i_flag == ShiftEagerParser.FLAG_PRINT_LEXICON)
			{
				printCommonConfig();
				System.out.println("- train_file : "+s_trainFile);
				
				System.out.println("\nPrint lexicon file: "+s_lexiconFile);
				trainDepParser(ShiftEagerParser.FLAG_PRINT_LEXICON, s_instanceFile);
				
				System.out.println("\nPrint training instances: "+s_instanceFile);
				trainDepParser(ShiftEagerParser.FLAG_PRINT_INSTANCE, s_instanceFile);
				
				System.out.println("\nTrain learning model: "+s_modelFile);
				trainModel();
			}
			else
				trainDepParser(ShiftEagerParser.FLAG_PRINT_TRANSITION, s_modelFile);
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
	}
	
	/** Trains the dependency parser. */
	private void trainDepParser(byte flag, String outputFile)
	{
		AbstractReader<DepNode, DepTree> reader = null;
		
		if (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader  (s_trainFile, true);
		else 											reader = new CoNLLReader(s_trainFile, true);
		
		ShiftEagerParser parser = new ShiftEagerParser(flag, s_featureXml, s_lexiconFile, outputFile);
		DepTree   tree;
		
		System.out.print("Parsing: ");	int n;
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			if (n % 1000 == 0)	System.out.printf("%s%dK", "\r- Parsing: ", n/1000);
		}	System.out.println("\r- Parsing: "+n);
		
		if (flag == ShiftEagerParser.FLAG_PRINT_LEXICON)		parser.saveTags(s_lexiconFile);
		else if (flag == ShiftEagerParser.FLAG_PRINT_INSTANCE)	parser.close();
	}
	
	/** Trains the LibLinear classifier. */
	private void trainModel()
	{
		IAlgorithm algorithm = null;
		
		Element element;
		String name, tmp;
		
		element = getElement(e_config, "algorithm");
		name    = element.getAttribute("name").trim();
		
		if (name.equals(IAlgorithm.LIBLINEAR_L2))
		{
			byte lossType = 1;
			double c = 0.1, eps = 0.1, bias = -1;
			
			if ((tmp = element.getAttribute("l").trim()).length() > 0)
				lossType = Byte.parseByte(tmp);
			
			if ((tmp = element.getAttribute("c").trim()).length() > 0)
				c = Double.parseDouble(tmp);
			
			if ((tmp = element.getAttribute("e").trim()).length() > 0)
				eps = Double.parseDouble(tmp);
			
			if ((tmp = element.getAttribute("b").trim()).length() > 0)
				bias = Double.parseDouble(tmp);
			
			algorithm = new LibLinearL2(lossType, c, eps, bias);
		}
		else if (name.equals(IAlgorithm.RRM))
		{
			int k = 40;
			double mu = 1.0, eta = 0.001, c = 0.1;
			
			if ((tmp = element.getAttribute("k").trim()).length() > 0)
				k = Integer.parseInt(tmp);
			
			if ((tmp = element.getAttribute("m").trim()).length() > 0)
				mu = Double.parseDouble(tmp);
			
			if ((tmp = element.getAttribute("e").trim()).length() > 0)
				eta = Double.parseDouble(tmp);
			
			if ((tmp = element.getAttribute("c").trim()).length() > 0)
				c = Double.parseDouble(tmp);
			
			algorithm = new RRM(k, mu, eta, c);
		}
		
		if (algorithm == null)	return;
		
		int numThreads = 2;
		
		element = getElement(e_config, "threads");
		if (element != null)	numThreads = Integer.parseInt(element.getTextContent().trim());
		
		System.out.println("- algorithm: "+name);
		System.out.println("- kernel   : "+i_kernel);
		System.out.println("- threads  : "+numThreads);
		System.out.println();
		
		long st = System.currentTimeMillis();
		new OneVsAllTrainer(s_instanceFile, s_modelFile, algorithm, i_kernel, numThreads);
		long time = System.currentTimeMillis() - st;
		System.out.printf("- Duration: %d hours, %d minutes\n", time/(1000*60*60), time/(1000*60));
	}
	
	static public void main(String[] args)
	{
		new DepTrain(args);
	}
}
