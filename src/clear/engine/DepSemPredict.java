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


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import clear.decode.AbstractDecoder;
import clear.decode.OneVsAllDecoder;
import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.ftr.map.DepFtrMap;
import clear.ftr.xml.DepFtrXml;
import clear.parse.DepSemParser;
import clear.parse.ShiftEagerParser;
import clear.reader.AbstractReader;
import clear.reader.RichReader;
import clear.util.IOUtil;

/**
 * Predicts dependency trees.
 * @author Jinho D. Choi
 * <b>Last update:</b> 6/29/2010
 */
public class DepSemPredict extends AbstractCommon
{
	private final String TAG_PREDICT            = "predict";
	private final String TAG_PREDICT_MORPH_DICT = "morph_dict";
	
	@Option(name="-i", usage="input file", required=true, metaVar="REQUIRED")
	private String s_inputFile  = null;
	@Option(name="-o", usage="output file", required=true, metaVar="REQUIRED")
	private String s_outputFile = null;
	@Option(name="-m", usage="model file", required=true, metaVar="REQUIRED")
	private String s_modelFile  = null;
	/** Morphological dictionary directory */
	private String s_morphDict  = null;
	/** Flag to choose parsing algorithm */
	private byte   i_flag       = ShiftEagerParser.FLAG_PREDICT;
	
	private int[]    n_size_total = new int[10];
	private double[] d_time       = new double[10];
	private double   d_time_total = 0;
	
	public DepSemPredict(String[] args)
	{
		CmdLineParser cmd  = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
			initConfigElements();
			
			ZipInputStream zin = new ZipInputStream(new FileInputStream(s_modelFile));
			ZipEntry zEntry;
			
			DepFtrXml       xml     = null;
			DepFtrMap       map     = null;
			AbstractDecoder decoder = null;
			
			printConfig();
			System.out.println("\n* Predict");
			
			while ((zEntry = zin.getNextEntry()) != null)
			{
				if (zEntry.getName().equals(ENTRY_FEATURE))
				{
					System.out.println("- loading feature template");
					
					BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
					StringBuilder  build  = new StringBuilder();
					String string;

					while ((string = reader.readLine()) != null)
					{
						build.append(string);
						build.append("\n");
					}
					
					xml = new DepFtrXml(new ByteArrayInputStream(build.toString().getBytes()));
				}
				
				if (zEntry.getName().equals(ENTRY_LEXICA))
				{
					System.out.println("- loading lexica");
					map = new DepFtrMap(xml, new BufferedReader(new InputStreamReader(zin)));
				}
				else if (zEntry.getName().equals(ENTRY_MODEL))
				{
					System.out.println("- loading model");
					decoder = new OneVsAllDecoder(new BufferedReader(new InputStreamReader(zin)));
				}
			}
			
			AbstractReader<DepNode, DepTree> reader = null;
			
			reader = new RichReader(s_inputFile, true);
			
			DepSemParser parser = new DepSemParser(i_flag, xml, map, decoder);
			PrintStream      fout   = IOUtil.createPrintFileStream(s_outputFile);
		//	PrintStream      fplot  = JIO.createPrintFileOutputStream("plot.txt");
			DepTree     tree;
			
			long st, et;
			int  n = 0;

			while (true)
			{
				st   = System.currentTimeMillis();
				tree = reader.nextTree();
				if (tree == null)	break;
				parser.parse(tree);	n++;
				et   = System.currentTimeMillis();
				fout.println(tree.toStringRich()+"\n");
			//	fout.println(tree+"\n");
				if (n%100 == 0)	System.out.print("\r- parsing: "+n);
				
				int index = (tree.size() >= 101) ? 9 : (tree.size()-1) / 10;
				d_time [index]     += (et - st);
				d_time_total       += (et - st);
				n_size_total[index]++;
			//	fplot.println(tree.size()+"\t"+tree.n_trans);
			}	System.out.println("\r- parsing: "+n);
			
			System.out.println("\n* Parsing time per sentence length");
			for (int i=0; i<d_time.length; i++)
				System.out.printf("<= %3d: %4.2f (ms)\n", (i+1)*10, d_time[i]/n_size_total[i]);
			
			System.out.printf("\nAverage parsing time: %4.2f (ms)\n", d_time_total/n);
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	protected void initElements()
	{
		initCommonElements();
		
		if (s_format.equals(AbstractReader.FORMAT_POS))
		{
			Element ePredict = getElement(e_config, TAG_PREDICT);
			Element element;
			
			if ((element = getElement(ePredict, TAG_PREDICT_MORPH_DICT)) != null)
				s_morphDict = element.getTextContent().trim();
		}
	}
	
	protected void printConfig()
	{
		System.out.println("* Configurations");
		System.out.println("- language  : "+s_language);
		System.out.println("- format    : "+s_format);
		if (s_morphDict != null)
			System.out.println("- morph_dict: "+s_morphDict);
		System.out.println("- input_file: "+s_inputFile);
	}
	
	static public void main(String[] args)
	{
		new DepSemPredict(args);
	}
}