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

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.model.OneVsAllModel;
import clear.parse.AbstractDepParser;
import clear.parse.ShiftEagerParser;
import clear.parse.ShiftPopParser;
import clear.reader.AbstractReader;
import clear.reader.CoNLLXReader;
import clear.reader.DepReader;

/**
 * Trains conditional dependency parser.
 * <b>Last update:</b> 11/19/2010
 * @author Jinho D. Choi
 */
public class DepTrain extends AbstractTrain
{
	@Option(name="-t", usage="feature template file", required=true, metaVar="REQUIRED")
	private String s_featureXml = null;
	@Option(name="-i", usage="training file", required=true, metaVar="REQUIRED")
	private String s_trainFile  = null;
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile  = null;
	@Option(name="-n", usage="bootstrapping level (default = 0)", required=false, metaVar="OPTIONAL")
	private int    n_boot       = 0;
	
	private DepFtrXml     t_xml   = null;
	private DepFtrMap     t_map   = null;
	private OneVsAllModel m_model = null;
	
	public void initElements() {}
	
	public void train() throws Exception
	{
		printConfig();
		
		String instanceFile = "instaces.ftr";
		String modelFile    = s_modelFile;
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		
		trainDepParser(ShiftPopParser.FLAG_PRINT_LEXICON , null,         null);
		trainDepParser(ShiftPopParser.FLAG_PRINT_INSTANCE, instanceFile, zout);
		m_model = (OneVsAllModel)trainModel(instanceFile, zout);
		zout.flush();	zout.close();
		
		for (int i=1; i<=n_boot; i++)
		{
			modelFile = s_modelFile + ".boot" + i;
			System.out.print("\n== Bootstrapping: "+i+" ==\n");

			zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
			trainDepParser(ShiftPopParser.FLAG_TRAIN_CONDITIONAL, instanceFile, zout);
			m_model = null;
			m_model = (OneVsAllModel)trainModel(instanceFile, zout);
			zout.flush();	zout.close();
		}
		
		new File(ENTRY_LEXICA).delete();
		new File(instanceFile).delete();
	}
	
	/** Trains the dependency parser. */
	private void trainDepParser(byte flag, String outputFile, JarArchiveOutputStream zout) throws Exception
	{
		AbstractDepParser parser  = null;
		OneVsAllDecoder   decoder = null;
		
		if (flag == ShiftPopParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("\n* Save lexica");
			
			if (s_depParser.equals(AbstractDepParser.ALG_SHIFT_EAGER))
				parser = new ShiftEagerParser(flag, s_featureXml);
			else if (s_depParser.equals(AbstractDepParser.ALG_SHIFT_POP))
				parser = new ShiftPopParser  (flag, s_featureXml);
		}
		else if (flag == ShiftPopParser.FLAG_PRINT_INSTANCE)
		{
			System.out.println("\n* Print training instances");
			System.out.println("- loading lexica");
			
			if (s_depParser.equals(AbstractDepParser.ALG_SHIFT_EAGER))
				parser = new ShiftEagerParser(flag, t_xml, ENTRY_LEXICA, outputFile);
			else if (s_depParser.equals(AbstractDepParser.ALG_SHIFT_POP))
				parser = new ShiftPopParser  (flag, t_xml, ENTRY_LEXICA, outputFile);
		}
		else if (flag == ShiftPopParser.FLAG_TRAIN_CONDITIONAL)
		{
			System.out.println("\n* Train conditional");
			decoder = new OneVsAllDecoder(m_model);
			
			if (s_depParser.equals(AbstractDepParser.ALG_SHIFT_EAGER))
				parser = new ShiftEagerParser(flag, t_xml, t_map, decoder, outputFile);
			else if (s_depParser.equals(AbstractDepParser.ALG_SHIFT_POP))
				parser = new ShiftPopParser  (flag, t_xml, t_map, decoder, outputFile);
		}
		
		AbstractReader<DepNode, DepTree> reader = null;
		DepTree tree;	int n;
		
		if      (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader   (s_trainFile, true);
		else if (s_format.equals(AbstractReader.FORMAT_CONLLX))	reader = new CoNLLXReader(s_trainFile, true);
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			
			if (n % 1000 == 0)
				System.out.printf("\r- parsing: %dK", n/1000);
		}
		
		System.out.println("\r- parsing: "+n);
		
		if (flag == ShiftPopParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("- saving");
			parser.saveTags(ENTRY_LEXICA);
			t_xml = parser.getDepFtrXml();
		}
		else if (flag == ShiftPopParser.FLAG_PRINT_INSTANCE || flag == ShiftPopParser.FLAG_TRAIN_CONDITIONAL)
		{
			parser.closeOutputStream();
			
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE));
			IOUtils.copy(new FileInputStream(s_featureXml), zout);
			zout.closeArchiveEntry();
			
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_LEXICA));
			IOUtils.copy(new FileInputStream(ENTRY_LEXICA), zout);
			zout.closeArchiveEntry();
			
			if (flag == ShiftPopParser.FLAG_PRINT_INSTANCE)
				t_map = parser.getDepFtrMap();
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
		System.out.println("- model_file : "+s_modelFile);
		System.out.println("- n_boots    : "+n_boot);
	}
	
	static public void main(String[] args)
	{
		DepTrain  trainer = new DepTrain();
		CmdLineParser cmd = new CmdLineParser(trainer);
		
		try
		{
			cmd.parseArgument(args);
			trainer.init();
			trainer.train();
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
