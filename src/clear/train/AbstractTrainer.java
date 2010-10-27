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
package clear.train;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import clear.util.DSUtil;
import clear.util.IOUtil;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.set.hash.TShortHashSet;

/**
 * Super class for trainer classes.
 * @author Jinho D. Choi
 * <b>Last update:</b> 2/1/2010
 */
abstract public class AbstractTrainer
{
	/** Delimiter between index and value (e.g. 3:0.12) */
	public static final String FTR_DELIM = ":";
	/** Delimiter between columns (e.g. 0:0.12 3:0.45) */
	public static final String COL_DELIM = " ";
	
	/** Total number of training instances */
	protected int   N; 
	/** Features dimension */
	protected int   D;
	/** Set of labels */
	protected TShortHashSet    s_labels;
	/** Training labels */
	protected TShortArrayList  a_labels;
	/** Training feature-vectors */
	protected ArrayList<int[]> a_features;
		
	/**
	 * Initializes all member fields with training instances in <code>instanceFile</code>.
	 * @see AbstractTrainer#init(String, String, int)
	 * @param instanceFile name of the file containing training instances
	 */
	public AbstractTrainer(String instanceFile)
	{
		init(instanceFile);
	}
	
	/**
	 * Initializes all member fields.
	 * Reads training instances from <code>instanceFile</code> and stores to 
	 * {@link AbstractTrainer#a_labels} and {@link AbstractTrainer#a_features}. 
	 * @param instanceFile name of the file containing training instances
	 */
	private void init(String instanceFile)
	{
		final int NUM = 1000000;
		Scanner scan  = IOUtil.createFileScanner(instanceFile);
		s_labels      = new TShortHashSet();
		a_labels      = new TShortArrayList (NUM);
		a_features    = new ArrayList<int[]>(NUM);
		
		for (N=0; scan.hasNextLine(); N++)
		{
			StringTokenizer tok = new StringTokenizer(scan.nextLine());
			short label         = Short.parseShort(tok.nextToken());
			int[] feature       = DSUtil.toIntArray(tok);
			
			// add label and feature
			a_labels  .add(label  );
			a_features.add(feature);

			// indices in feature are in ascending order
			if (label >= 0)	s_labels.add(label);
			D = Math.max(D, feature[feature.length-1]);
			
			if (N%100000 == 0)	System.out.print("\r* Initializing  : "+(N/1000)+"K");
		}
		
		scan.close();
		a_labels  .trimToSize();
		a_features.trimToSize();
		D++;	// feature dimension = last feature-index + 1

		System.out.println();
		System.out.println("- # of instances: " + N);
		System.out.println("- # of labels   : " + s_labels.size());
		System.out.println("- # of features : " + D);
	}
	
	/**
	 * Normalizes the weight vector.
	 * @param weight weight vector
	 */
	static public void normalize(double[] weight)
	{
		double norm = 0;
		
		for (int i=0; i<weight.length; i++)
			norm += (weight[i] * weight[i]);
		
		norm = Math.sqrt(norm);
		
		for (int i=0; i<weight.length; i++)
			weight[i] /= norm;
	}
	
	/**
	 * Prints the label and the weight vector to <code>fout</code>.
	 * @param fout   file to print
	 * @param label  label of the weight vector
	 * @param weight weight vector
	 */
	static public void printWeight(PrintStream fout, short label, double[] weight)
	{
		fout.print(label);
		
		for (int i=0; i<weight.length; i++)
			if (weight[i] != 0)	fout.print(COL_DELIM + i + FTR_DELIM + weight[i]);
	
		fout.println();
	}
	
	/**
	 * Prints the label and the weight vector to <code>filename</code>.
	 * @param filename name of the file to print
	 * @param label    label of the weight vector
	 * @param weight   weight vector
	 */
	static public void printWeight(String filename, short label, double[] weight)
	{
		PrintStream fout = IOUtil.createPrintFileStream(filename);
		
		printWeight(fout, label, weight);
		fout.close();
	}
	
	/**
	 * Prints the labels and the weight vectors to <code>filename</code>.
	 * @param filename name of the file to print
	 * @param labels   labels of the weight vectors
	 * @param weights  weight vectors
	 */
	static public void printWeight(String filename, short[] labels, double[][] weights)
	{
		PrintStream fout = IOUtil.createPrintFileStream(filename);
		
		for (short label : labels)
			printWeight(fout, label, weights[label]);
		
		fout.close();
	}
	
	/**
	 * Returns the score of a training instance <code>x</code> using the weight vector.
	 * @param weight weight vector
	 * @param x      training instance (indices start from 1)
	 */
	static public double getScore(double[] weight, int[] x)
	{
		double score = weight[0];
		
		for (int idx : x)	score += weight[idx];
		return score;
	}
}
