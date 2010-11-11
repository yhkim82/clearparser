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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.model.OneVsAllModel;
import clear.parse.ShiftEagerParser;
import clear.reader.AbstractReader;
import clear.reader.CoNLLReader;
import clear.reader.DepReader;
import clear.train.OneVsAllTrainer;
import clear.train.algorithm.IAlgorithm;
import clear.train.algorithm.LibLinearL2;
import clear.train.algorithm.RRM;
import clear.train.kernel.BinaryKernel;
import clear.util.IOUtil;

/**
 * Trains conditional dependency parser.
 * <b>Last update:</b> 6/29/2010
 * @author Jinho D. Choi
 */
public class DepTrainDev extends AbstractEngine
{
	private final int MAX_ITER = 10;
	
	private final String TAG_TRAIN           = "train";
	private final String TAG_TRAIN_ALGORITHM = "algorithm";
	private final String TAG_TRAIN_CUTOFF    = "cutoff";
	private final String EXT_INSTANCE_FILE   = ".ftr";	
	
	@Option(name="-c", usage="configuration file", required=true, metaVar="REQUIRED")
	private String s_configFile = null;
	@Option(name="-i", usage="training file", required=true, metaVar="REQUIRED")
	private String s_trainFile = null; 
	@Option(name="-d", usage="development file", required=true, metaVar="REQUIRED")
	private String s_devFile = null; 
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile = null;
	@Option(name="-t", usage="feature template file", required=true, metaVar="REQUIRED")
	private String s_featureXml = null;
	/** N-gram cutoff */
	private int    i_ngramCutoff = 0;
	
	private StringBuilder s_build = null;
	private DepFtrXml     t_xml   = null;
	private DepFtrMap     t_map   = null;
	private OneVsAllModel m_model = null;
	
	public DepTrainDev(String[] args)
	{
		CmdLineParser cmd  = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
			if (!initConfigElements(s_configFile))	return;
			printConfig();
			
			int    i = 0;
			String modelFile    = s_modelFile + "." + i;
			String instanceFile = modelFile + EXT_INSTANCE_FILE;
			String log          = "\n== Interation: "+i+" ==\n";
			
			s_build = new StringBuilder();
			s_build.append(log);
			System.out.print(log);
			
			JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
			
			trainDepParser(ShiftEagerParser.FLAG_PRINT_LEXICON , null, null);
			trainDepParser(ShiftEagerParser.FLAG_PRINT_INSTANCE, instanceFile, zout);
			m_model = trainModel(instanceFile, zout);
			zout.flush();	zout.close();
			
			double prevAcc = 0, currAcc;
			
			do
			{
				currAcc = trainDepParser(ShiftEagerParser.FLAG_PREDICT, s_devFile+".parse."+i, null);
				
				modelFile    = s_modelFile + "." + (++i);
				instanceFile = modelFile + EXT_INSTANCE_FILE;
				
				trainDepParser(ShiftEagerParser.FLAG_TRAIN_CONDITIONAL, instanceFile, null);
				
				if (currAcc < prevAcc)	break;
				prevAcc = currAcc;
				
				log = "\n== Interation: "+i+" ==\n";
				s_build.append(log);
				System.out.print(log);

				zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
				zout.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE));
				IOUtils.copy(new FileInputStream(s_featureXml), zout);
				zout.closeArchiveEntry();
				
				zout.putArchiveEntry(new JarArchiveEntry(ENTRY_LEXICA));
				IOUtils.copy(new FileInputStream(ENTRY_LEXICA), zout);
				zout.closeArchiveEntry();
				
				m_model = null;
				m_model = trainModel(instanceFile, zout);
				zout.flush();	zout.close();
			}
			while (i < MAX_ITER);
			
			new File(ENTRY_LEXICA).delete();
			System.out.println(s_build.toString());
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
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
			parser = new ShiftEagerParser(flag, s_featureXml, ENTRY_LEXICA, outputFile);
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
		
		if (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader  (inputFile, isTrain);
		else 											reader = new CoNLLReader(inputFile, isTrain);
		
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
			System.out.println("- cutoff: n-gram = "+i_ngramCutoff);
			parser.saveTags(ENTRY_LEXICA, i_ngramCutoff);
		}
		else if (flag == ShiftEagerParser.FLAG_PRINT_INSTANCE)
		{
			parser.closeOutputStream();
			
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE));
			IOUtils.copy(new FileInputStream(s_featureXml), zout);
			zout.closeArchiveEntry();
			
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_LEXICA));
			IOUtils.copy(new FileInputStream(ENTRY_LEXICA), zout);
			zout.closeArchiveEntry();
			
			t_xml = parser.getDepFtrXml();
			t_map = parser.getDepFtrMap();
		}
		else if (flag == ShiftEagerParser.FLAG_PREDICT)
		{
			fout.close();
			
			String[] args = {"-g", s_devFile, "-s", outputFile};
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
			
			String log = "\n* Training accuracy\n";
			double acc = parser.getAccuracy();
			
			System.out.println(log+": "+acc);
			System.out.println("- "+acc);
			
			s_build.append(log);
			s_build.append("- "+parser.getAccuracy()+"\n");
		}
		
		return 0;
	}
	
	/** Trains the LibLinear classifier. */
	private OneVsAllModel trainModel(String instanceFile, JarArchiveOutputStream zout) throws Exception
	{
		Element eTrain  = getElement(e_config, TAG_TRAIN);
		Element element = getElement(eTrain, TAG_TRAIN_ALGORITHM);
		String  name    = element.getAttribute("name").trim();
		
		StringBuilder options   = new StringBuilder();
		IAlgorithm    algorithm = null;		
		String        tmp;
		
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
			
			options.append("loss_type = ");	options.append(lossType);
			options.append(", c = ");		options.append(c);
			options.append(", eps = ");		options.append(eps);
			options.append(", bias = ");	options.append(bias);
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
			
			options.append("K = ");		options.append(k);
			options.append(", mu = ");	options.append(mu);
			options.append(", eta = ");	options.append(eta);
			options.append(", c = ");	options.append(c);
		}
		
		if (algorithm == null)
		{
			System.err.println("Learning algorithm is not specified in '"+s_featureXml+"'");
			return null;
		}
		
		int numThreads = 2;
		
		element = getElement(eTrain, "threads");
		if (element != null)	numThreads = Integer.parseInt(element.getTextContent().trim());
		
		System.out.println("\n* Train model");
		System.out.println("- algorithm: "+name);
		System.out.println("- options  : "+options.toString());
		System.out.println("- threads  : "+numThreads);
		System.out.println();
		
		JarArchiveEntry entry = new JarArchiveEntry(ENTRY_MODEL);
		zout.putArchiveEntry(entry);
		
		long st = System.currentTimeMillis();
		OneVsAllTrainer trainer = new OneVsAllTrainer(new PrintStream(zout), algorithm, new BinaryKernel(instanceFile), numThreads);
		long time = System.currentTimeMillis() - st;
		System.out.printf("- duration: %d hours, %d minutes\n", time/(1000*3600), time/(1000*60));
		zout.closeArchiveEntry();
		
		return trainer.getModel();
	}
	
	protected boolean initElements()
	{
		if (!super.initElements())	return false;
		
		Element eTrain = getElement(e_config, TAG_TRAIN);
		Element element;
		
		if ((element = getElement(eTrain, TAG_TRAIN_CUTOFF)) != null)
		{
			if (element.hasAttribute("ngram"))
				i_ngramCutoff = Integer.parseInt(element.getAttribute("ngram"));
		}
	
		return true;
	}
	
	protected void printConfig()
	{
		System.out.println("* Configurations");
		System.out.println("- language   : "+s_language);
		System.out.println("- format     : "+s_format);
		System.out.println("- feature_xml: "+s_featureXml);
		System.out.println("- train_file : "+s_trainFile);
	}
	
	static public void main(String[] args)
	{
		new DepTrainDev(args);
	}
}
