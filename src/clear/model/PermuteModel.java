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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import clear.train.kernel.AbstractKernel;
import clear.train.kernel.PermuteKernel;
import clear.util.IOUtil;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

/**
 * One-vs-all model.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
public class PermuteModel extends AbstractMultiModel
{
	ObjectIntOpenHashMap<String> m_perm;
	
	public PermuteModel(PermuteKernel kernel)
	{
		super(kernel);
		m_perm = kernel.m_perm;
	}
	
	public PermuteModel(String modelFile)
	{
		super(modelFile);
	}
	
	public PermuteModel(BufferedReader fin)
	{
		super(fin);
	}
	
	public PermuteModel(int nLabels, int nFeatures, int[] aLabels, double[] dWeights)
	{
		super(nLabels, nFeatures, aLabels, dWeights);
	}
	
	public void init(AbstractKernel kernel)
	{
		n_labels   = kernel.L;
		n_features = kernel.D;
		a_labels   = kernel.a_labels;
		d_weights  = new double[n_labels * n_features];
	}
	
	public void load(String modelFile)
	{
		try
		{
			BufferedReader fin = IOUtil.createBufferedFileReader(modelFile);
			
			loadAux(fin);
			fin.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void load(BufferedReader fin)
	{
		try
		{
			loadAux(fin);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void loadAux(BufferedReader fin) throws Exception
	{
		n_labels   = Integer.parseInt(fin.readLine());
		n_features = Integer.parseInt(fin.readLine());
		a_labels   = new int[n_labels];
		d_weights  = new double[n_labels * n_features];
		
		readLabels (fin);
		readPermMap(fin);
		readWeights(fin);
	}
	
	protected void readPermMap(BufferedReader fin) throws Exception
	{
		String[] tmp = fin.readLine().split(" ");
		int i, length = tmp.length;
		
		m_perm = new ObjectIntOpenHashMap<String>();
		
		for (i=0; i<length; i+=2)
			m_perm.put(tmp[i], Integer.parseInt(tmp[i+1]));
	}
	
	public void save(String modelFile)
	{
		try
		{
			PrintStream fout = IOUtil.createPrintFileStream(modelFile);
			
			saveAux(fout);
			fout.flush();
			fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void save(PrintStream fout)
	{
		try
		{
			saveAux(fout);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void saveAux(PrintStream fout) throws Exception
	{
		fout.println(n_labels);
		fout.println(n_features);
		printLabels (fout);
		printPermMap(fout);
		printWeights(fout);
	}
	
	protected void printPermMap(PrintStream fout) throws Exception
	{
		StringBuilder build = new StringBuilder();
		
		for (ObjectCursor<String> cur : m_perm.keySet())
		{
			build.append(cur.value);
			build.append(" ");
			build.append(m_perm.get(cur.value));
		}
		
		fout.println(build.toString());
	}
	
	private int getBeginIndex(int label, int index)
	{
		return index * n_labels + label;
	}
	
	public void copyWeight(int label, double[] weight)
	{
		int i;
		
		for (i=0; i<n_features; i++)
			d_weights[getBeginIndex(label, i)] = weight[i];
	}
	
	public double[] getScores(int[] x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, idx, label;
		
		for (String key : PermuteKernel.getPerm(x))
		{
			if ((i = m_perm.get(key)) == 0)	continue;
			
			for (label=0; label<n_labels; label++)
			{
				if ((idx = getBeginIndex(label, i)) < d_weights.length)
					scores[label] += d_weights[idx];
			}
		}
		
		return scores;
	}
	
	public double[] getScores(IntArrayList x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, idx, label;
		
		for (String key : PermuteKernel.getPerm(x))
		{
			if ((i = m_perm.get(key)) == 0)	continue;
			
			for (label=0; label<n_labels; label++)
			{
				if ((idx = getBeginIndex(label, i)) < d_weights.length)
					scores[label] += d_weights[idx];
			}
		}
		
		return scores;
	}
	
	public double[] getScores(ArrayList<JIntDoubleTuple> x)
	{
		return null;
	}
}
