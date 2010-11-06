/**
* Copyright (c) 2010, Regents of the University of Colorado
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
package clear.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import clear.train.kernel.AbstractKernel;

/**
 * One-vs-all model.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
public class OvAModel extends AbstractModel
{
	public OvAModel(String modelFile)
	{
		super(modelFile);
	}

	public OvAModel(AbstractKernel kernel)
	{
		super(kernel);
	}
	
	public void init(AbstractKernel kernel)
	{
		n_labels   = kernel.L;
		n_features = kernel.D;
		d_weights  = new double[n_labels * n_features];
	}
	
	public void load(String modelFile)
	{
		try
		{
			BufferedReader fin = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(modelFile))));
			
			n_labels   = Integer.parseInt(fin.readLine());
			n_features = Integer.parseInt(fin.readLine());
			d_weights  = new double[n_features * n_labels];
			
			readWeights(fin);
			fin.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void save(String modelFile)
	{
		try
		{
			PrintStream fout = new PrintStream(new GZIPOutputStream(new FileOutputStream(modelFile)));
			
			fout.println(n_labels);
			fout.println(n_features);
			printWeights(fout);
			
			fout.flush();	fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void copyWeight(int label, double[] weight)
	{
		int i;
		
		for (i=0; i<n_features; i++)
			d_weights[i * n_labels + label] = weight[i];
	}
	
	public double getScore(int label, int[] x)
	{
		double score = d_weights[label];
		int    i;
		
		for (i=0; i < x.length; i++)
			score += d_weights[x[i] * n_labels + label];
		
		return score;
	}
	
	public double getScore(int label, ArrayList<Integer> x)
	{
		double score = d_weights[label];
		int    i;
		
		for (i=0; i < x.size(); i++)
			score += d_weights[x.get(i) * n_labels + label];
		
		return score;
	}
	
	public double[] getScores(int[] x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, label;
		
		for (i=0; i < x.length; i++)
		{
			for (label=0; label<n_labels; label++)
				scores[label] += d_weights[x[i] * n_labels + label];
		}
		
		return scores;
	}
	
	public double[] getScores(ArrayList<Integer> x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, label;
		
		for (i=0; i < x.size(); i++)
		{
			for (label=0; label<n_labels; label++)
				scores[label] += d_weights[x.get(i) * n_labels + label];
		}
		
		return scores;
	}
}
