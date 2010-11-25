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

import java.io.PrintStream;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import clear.model.AbstractModel;
import clear.train.AbstractTrainer;
import clear.train.BinaryTrainer;
import clear.train.OneVsAllKeepTrainer;
import clear.train.algorithm.IAlgorithm;
import clear.train.algorithm.LibLinearL2;
import clear.train.algorithm.RRM;
import clear.train.kernel.AbstractKernel;
import clear.train.kernel.BinaryKernel;
import clear.train.kernel.ValueKernel;

/**
 * Trains dependency parser.
 * <b>Last update:</b> 11/16/2010
 * @author Jinho D. Choi
 */
abstract public class AbstractTrain extends AbstractCommon
{
	protected final String TAG_TRAIN           = "train";
	protected final String TAG_TRAIN_ALGORITHM = "algorithm";
	
	@Option(name="-i", usage="training file", required=true, metaVar="REQUIRED")
	protected String s_trainFile  = null; 
	@Option(name="-t", usage="feature template file", required=true, metaVar="REQUIRED")
	protected String s_featureXml = null;
	
	protected byte kernel_type  = AbstractKernel.KERNEL_BINARY;
	protected byte trainer_type = AbstractTrainer.ST_ONE_VS_ALL;
	
	public AbstractTrain(String[] args)
	{
		CmdLineParser cmd  = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
			if (!initConfigElements())	return;
			
			train();
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	abstract protected void train() throws Exception;
	
	/** Trains the LibLinear classifier. */
	protected AbstractModel trainModel(String instanceFile, JarArchiveOutputStream zout) throws Exception
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
		
		int numThreads = 1;
		
		element = getElement(eTrain, "threads");
		if (element != null)	numThreads = Integer.parseInt(element.getTextContent().trim());
		
		System.out.println("\n* Train model");
		System.out.println("- algorithm: "+name);
		System.out.println("- options  : "+options.toString());
		System.out.println("- threads  : "+numThreads);
		System.out.println();
		
		PrintStream fout = null;
		if (zout != null)
		{
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_MODEL));
			fout = new PrintStream(zout);
		}
		
		long st = System.currentTimeMillis();
		AbstractKernel kernel = (kernel_type == AbstractKernel.KERNEL_BINARY) ? new BinaryKernel(instanceFile) : new ValueKernel(instanceFile);
		AbstractTrainer trainer = (trainer_type == AbstractTrainer.ST_BINARY) ? new BinaryTrainer(fout, algorithm, kernel, numThreads) : new OneVsAllKeepTrainer(fout, algorithm, kernel, numThreads);
		long time = System.currentTimeMillis() - st;
		System.out.printf("- duration: %d hours, %d minutes\n", time/(1000*3600), time/(1000*60));
		
		if (zout != null)	zout.closeArchiveEntry();
		
		return trainer.getModel();
	}
	
	protected void initElements()
	{
		initCommonElements();
	}
	
	protected void printConfig()
	{
		System.out.println("* Configurations");
		System.out.println("- language   : "+s_language);
		System.out.println("- format     : "+s_format);
		System.out.println("- feature_xml: "+s_featureXml);
		System.out.println("- train_file : "+s_trainFile);
	}
}
