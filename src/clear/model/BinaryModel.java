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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import clear.train.kernel.AbstractKernel;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Binary model.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
public class BinaryModel extends AbstractModel
{
	public BinaryModel(AbstractKernel kernel)
	{
		super(kernel);
	}
	
	public BinaryModel(String modelFile)
	{
		super(modelFile);
	}

	public void init(AbstractKernel kernel)
	{
		n_features = kernel.D;
		a_labels   = kernel.a_labels;
		d_weights  = new double[n_features];
	}
	
	public void load(String modelFile)
	{
		try
		{
			BufferedReader fin = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(modelFile))));
			
			n_features = Integer.parseInt(fin.readLine());
			a_labels   = new int[2];
			d_weights  = new double[n_features];
			
			readLabels (fin);
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
			
			fout.println(n_features);
			
			printLabels (fout);
			printWeights(fout);
			fout.flush();	fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void copyWeight(double[] weight)
	{
		System.arraycopy(weight, 0, d_weights, 0, n_features);
	}
	
	public double getScore(int[] x)
	{
		double score = d_weights[0];
		int    i;
		
		for (i=0; i < x.length; i++)
			score += d_weights[x[i]];
		
		return score;
	}
	
	public double getScore(IntArrayList x)
	{
		double score = d_weights[0];
		int    i;
		
		for (i=0; i < x.size(); i++)
			score += d_weights[x.get(i)];
		
		return score;
	}
}
