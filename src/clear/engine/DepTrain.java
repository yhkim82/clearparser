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
	private final String TAG_ALGORITHM     = "algorithm";
	private final String TAG_CUTOFF        = "cutoff";
	private final String TAG_CUTOFF_NGRAM  = "ngram";
	
	
	@Option(name="-c", usage="configuration file", required=true, metaVar="REQUIRED")
	private String s_configFile = null;
	@Option(name="-i", usage="training file", required=true, metaVar="REQUIRED")
	private String s_trainFile  = null; 
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile  = null;
	@Option(name="-t", usage="feature template file", required=true, metaVar="REQUIRED")
	protected String  s_featureXml = null;
	@Option(name="-f", usage=ShiftEagerParser.FLAG_PRINT_LEXICON+": train model (default), "+ShiftEagerParser.FLAG_PRINT_TRANSITION+": print transitions", metaVar="OPTIONAL")
	private byte   i_flag       = ShiftEagerParser.FLAG_PRINT_LEXICON;
	/** N-gram cutoff */
	private int    i_ngramCutoff = 0;
	private String s_instanceFile;
	private JarArchiveOutputStream z_out;
	
	public DepTrain(String[] args)
	{
		CmdLineParser cmd  = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
			if (!initConfigElements(s_configFile))	return;
			
			if (i_flag == ShiftEagerParser.FLAG_PRINT_LEXICON)
			{
				printConfig();
				
				z_out = new JarArchiveOutputStream(new FileOutputStream(s_modelFile));
				s_instanceFile = s_modelFile + EXT_INSTANCE_FILE;
				
				trainDepParser(ShiftEagerParser.FLAG_PRINT_LEXICON, null);
				trainDepParser(ShiftEagerParser.FLAG_PRINT_INSTANCE, s_instanceFile);
				trainModel();
				
				z_out.flush();
				z_out.close();
			}
			else
			{
				trainDepParser(ShiftEagerParser.FLAG_PRINT_TRANSITION, s_modelFile);
			}
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/** Trains the dependency parser. */
	private void trainDepParser(byte flag, String outputFile) throws Exception
	{
		AbstractReader<DepNode, DepTree> reader = null;
		
		if (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader  (s_trainFile, true);
		else 											reader = new CoNLLReader(s_trainFile, true);
		
		ShiftEagerParser parser = null;
		
		if (flag == ShiftEagerParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("\n* Save lexica");
			parser = new ShiftEagerParser(flag, s_featureXml);
		}
		else if (flag == ShiftEagerParser.FLAG_PRINT_INSTANCE)
		{
			System.out.println("\n* Print training instances: "+s_instanceFile);
			System.out.println("- loading lexica");
			parser = new ShiftEagerParser(flag, s_featureXml, ENTRY_LEXICA, outputFile);
		}
		else // if (flag == ShiftEagerParser.FLAG_PRINT_TRANSITION)
		{
			System.out.println("\n* Print transitions");
			System.out.println("- from   : "+s_trainFile);
			System.out.println("- to     : "+s_modelFile);
			parser = new ShiftEagerParser(flag, outputFile);
		}
		
		DepTree tree;	int n;
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			if (n % 1000 == 0)	System.out.printf("%s%dK", "\r- parsing: ", n/1000);
		}	System.out.println("\r- parsing: "+n);
		
		if (flag == ShiftEagerParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("- saving");
			System.out.println("- n-gram cutoff = "+i_ngramCutoff);
			parser.saveTags(ENTRY_LEXICA, i_ngramCutoff);
		}
		else if (flag == ShiftEagerParser.FLAG_PRINT_INSTANCE)
		{
			parser.closeOutputStream();
			JarArchiveEntry entry  = new JarArchiveEntry(ENTRY_LEXICA);
			
			z_out.putArchiveEntry(entry);
			IOUtils.copy(new FileInputStream(ENTRY_LEXICA), z_out);
			
			z_out.closeArchiveEntry();
			new File(ENTRY_LEXICA).delete();
		}
	}
	
	/** Trains the LibLinear classifier. */
	private void trainModel() throws Exception
	{
		IAlgorithm algorithm = null;
		
		Element element;
		String name, tmp;
		StringBuilder options = new StringBuilder();
		
		element = getElement(e_config, TAG_ALGORITHM);
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
			return;
		}
		
		int numThreads = 2;
		
		element = getElement(e_config, "threads");
		if (element != null)	numThreads = Integer.parseInt(element.getTextContent().trim());
		
		System.out.println("\n* Train model");
		System.out.println("- algorithm: "+name);
		System.out.println("- options  : "+options.toString());
		System.out.println("- kernel   : "+i_kernel);
		System.out.println("- threads  : "+numThreads);
		System.out.println();
		
		JarArchiveEntry entry = new JarArchiveEntry(ENTRY_MODEL);
		z_out.putArchiveEntry(entry);
		
		long st = System.currentTimeMillis();
		new OneVsAllTrainer(s_instanceFile, new PrintStream(z_out), algorithm, i_kernel, numThreads);
		long time = System.currentTimeMillis() - st;
		System.out.printf("- duration: %d hours, %d minutes\n", time/(1000*3600), time/(1000*60));
		
		z_out.closeArchiveEntry();
	}
	
	protected boolean initElements()
	{
		super.initElements();
		
		Element eCutoff = getElement(e_config, TAG_CUTOFF);
		Element element;
		
		if ((element = getElement(eCutoff, TAG_CUTOFF_NGRAM)) != null)
			i_ngramCutoff = Integer.parseInt(element.getTextContent().trim());
	
		return true;
	}
	
	protected void printConfig()
	{
		super.printConfig();
		
		System.out.println("- feature_xml: "+s_featureXml);
		System.out.println("- train_file : "+s_trainFile);
	}
	
	static public void main(String[] args)
	{
		new DepTrain(args);
	}
}
