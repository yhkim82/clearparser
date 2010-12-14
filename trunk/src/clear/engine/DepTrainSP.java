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
import org.kohsuke.args4j.Option;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.xml.DepFtrXml;
import clear.parse.ShiftPopParser;
import clear.reader.AbstractReader;
import clear.reader.CoNLLReader;
import clear.reader.DepReader;

/**
 * Trains dependency parser.
 * <b>Last update:</b> 6/29/2010
 * @author Jinho D. Choi
 */
public class DepTrainSP extends AbstractTrain
{
	private final String EXT_INSTANCE_FILE = ".ftr";	
	
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile = null;
	@Option(name="-f", usage=ShiftPopParser.FLAG_PRINT_LEXICON+": train model (default), "+ShiftPopParser.FLAG_PRINT_TRANSITION+": print transitions", metaVar="OPTIONAL")
	private byte   i_flag = ShiftPopParser.FLAG_PRINT_LEXICON;
	
	private String                 s_instanceFile;
	private DepFtrXml              t_xml;
	private JarArchiveOutputStream z_out;
	
	public DepTrainSP(String[] args)
	{
		super(args);
	}
	
	protected void train() throws Exception
	{
		if (i_flag == ShiftPopParser.FLAG_PRINT_LEXICON)
		{
			printConfig();
			
			z_out = new JarArchiveOutputStream(new FileOutputStream(s_modelFile));
			s_instanceFile = s_modelFile + EXT_INSTANCE_FILE;
			
			trainDepParser(ShiftPopParser.FLAG_PRINT_LEXICON , null);
			trainDepParser(ShiftPopParser.FLAG_PRINT_INSTANCE, s_instanceFile);
			
			trainModel(s_instanceFile, z_out);
			new File(s_instanceFile).delete();
			
			z_out.flush();
			z_out.close();
		}
		else if (i_flag == ShiftPopParser.FLAG_PRINT_TRANSITION)
		{
			trainDepParser(ShiftPopParser.FLAG_PRINT_TRANSITION, s_modelFile);
		}
	}
	
	/** Trains the dependency parser. */
	private void trainDepParser(byte flag, String outputFile) throws Exception
	{
		ShiftPopParser parser = null;
		
		if (flag == ShiftPopParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("\n* Save lexica");
			parser = new ShiftPopParser(flag, s_featureXml);
		}
		else if (flag == ShiftPopParser.FLAG_PRINT_INSTANCE)
		{
			System.out.println("\n* Print training instances: "+s_instanceFile);
			System.out.println("- loading lexica");
			parser = new ShiftPopParser(flag, t_xml, ENTRY_LEXICA, outputFile);
		}
		else if (flag == ShiftPopParser.FLAG_PRINT_TRANSITION)
		{
			System.out.println("\n* Print transitions");
			System.out.println("- from   : "+s_trainFile);
			System.out.println("- to     : "+s_modelFile);
			parser = new ShiftPopParser(flag, outputFile);
		}
		
		AbstractReader<DepNode, DepTree> reader = null;
		DepTree tree;	int n;
		
		if (s_format.equals(AbstractReader.FORMAT_DEP))	reader = new DepReader  (s_trainFile, true);
		else 											reader = new CoNLLReader(s_trainFile, true);
		
		for (n=0; (tree = reader.nextTree()) != null; n++)
		{
			parser.parse(tree);
			if (n % 1000 == 0)	System.out.printf("\r- parsing: %dK", n/1000);
		}	System.out.println("\r- parsing: "+n);
		
		if (flag == ShiftPopParser.FLAG_PRINT_LEXICON)
		{
			System.out.println("- saving");
			parser.saveTags(ENTRY_LEXICA);
			t_xml = parser.getDepFtrXml();
		}
		else if (flag == ShiftPopParser.FLAG_PRINT_INSTANCE)
		{
			parser.closeOutputStream();
			
			z_out.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE));
			IOUtils.copy(new FileInputStream(s_featureXml), z_out);
			z_out.closeArchiveEntry();
			
			z_out.putArchiveEntry(new JarArchiveEntry(ENTRY_LEXICA));
			IOUtils.copy(new FileInputStream(ENTRY_LEXICA), z_out);
			z_out.closeArchiveEntry();
			new File(ENTRY_LEXICA).delete();
		}
		else if (flag == ShiftPopParser.FLAG_PRINT_TRANSITION)
		{
			parser.closeOutputStream();
		}
	}
	
	protected void printConfig()
	{
		super.printConfig();
		System.out.println("- model_file : "+s_modelFile);		
	}
	
	static public void main(String[] args)
	{
		new DepTrainSP(args);
	}
}
