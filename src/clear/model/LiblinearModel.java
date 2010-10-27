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
package clear.model;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import clear.decode.AbstractDecoder;
import clear.util.tuple.JIntDoubleTuple;

/**
 * Liblinear decoders.
 * @author Jinho D. Choi
 * <br><b>Last update:</b> 10/19/2010
 */
public class LiblinearModel extends AbstractDecoder
{
	/** Total number of labels. */
	protected int     n_labels;
	/** Total number of features. */
	protected int     n_features;
	/** List of labels */
	protected int[]   i_labels;
	/** Weight vectors for all labels */
	protected float[] d_weights;
	/** Bias */
	protected float   d_bias;
	
	public LiblinearModel(String modelFile)
	{
		try
		{
			init(modelFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void init(String modelFile) throws Exception
	{
		BufferedReader fin = new BufferedReader(new FileReader(modelFile));
		String[] tmp;	int i;
			
		tmp = fin.readLine().split(" ");		// solverType
		String solverType = tmp[1];
			
		tmp = fin.readLine().split(" ");		// nr_class
		n_labels = Integer.parseInt(tmp[1]);
			
		tmp = fin.readLine().split(" ");		// label
		i_labels = new int[n_labels];
		for (i=0; i<n_labels; i++)	i_labels[i] = Integer.parseInt(tmp[i+1]);
		
		if (n_labels == 2 && !solverType.equals("MCSVM_CS"))
			n_labels = 1;
					
		tmp = fin.readLine().split(" ");		// nr_feature
		n_features = Integer.parseInt(tmp[1]);
			
		tmp = fin.readLine().split(" ");		// bias
		d_bias = Float.parseFloat(tmp[1]);
		if (d_bias >= 0)	n_features++;
			
		fin.readLine();							// w
		d_weights = new float [n_features * n_labels];
			
		int[] buffer = new int[128];
		for (i=0; i<n_features; i++)
		{
			for (int j=0; j<n_labels; j++)
			{
				int index = i*n_labels + j, b = 0;
				
				while (true)
				{
					int ch = fin.read();
					
					if (ch == ' ')	break;
					else			buffer[b++] = ch;
				}

				d_weights[index] = Float.parseFloat((new String(buffer, 0, b)));
			}
				
			if (i%1000000 == 0)	System.out.print(".");
		}
		
		fin.close();
	}
	
	/** @return scores of all labels for <code>x</code> */
	public double[] getScores(ArrayList<Integer> x)
	{
		double[] scores = new double[n_labels];
		
		for (int idx : x)
		{
			if (idx > n_features)	break;
			
			for (int i=0; i<n_labels; i++)
				scores[i] += d_weights[(idx - 1) * n_labels + i];
		}
		
		if (d_bias >= 0)
		{
			for (int i=0; i<n_labels; i++)
				scores[i] += d_weights[(n_features - 1) * n_labels + i];
		}
		
		return scores;
	}
	
	/* (non-Javadoc)
	 * @see harvest.model.AbstractModel#predict(java.util.ArrayList)
	 */
	public JIntDoubleTuple predict(ArrayList<Integer> x)
	{
		double[] scores = getScores(x);
		
		if (n_labels == 1)
		{
			if (scores[0] > 0)	return new JIntDoubleTuple(i_labels[0], scores[0]);
			else				return new JIntDoubleTuple(i_labels[1], scores[0]*-1);
		}
		
		JIntDoubleTuple max = new JIntDoubleTuple(i_labels[0], scores[0]);
		
		for (int i=1; i < n_labels; i++)
			if (scores[i] > max.d) max.set(i_labels[i], scores[i]);

		return max;
	}
	
	/* (non-Javadoc)
	 * @see harvest.model.AbstractModel#predictAll(java.util.ArrayList)
	 */
	public ArrayList<JIntDoubleTuple> predictAll(ArrayList<Integer> x)
	{
		double[] scores = getScores(x);
		ArrayList<JIntDoubleTuple> aRes = new ArrayList<JIntDoubleTuple>();
		
		if (n_labels == 1)
		{
			if (scores[0] > 0)	aRes.add(new JIntDoubleTuple(i_labels[0], scores[0]));
			else				aRes.add(new JIntDoubleTuple(i_labels[1], scores[0]*-1));
			
			return aRes;
		}
		
		outer: for (short i=0; i<n_labels; i++)
		{
			for (int j=0; j<aRes.size(); j++)
			{
				if (aRes.get(j).d < scores[i])
				{
					aRes.add(j, new JIntDoubleTuple(i_labels[i], scores[i]));
					continue outer;
				}
			}
			
			aRes.add(new JIntDoubleTuple(i_labels[i], scores[i]));
		}
		
		return aRes;
	}
	
	public String joinedLabel()
	{
		if (i_labels[0] < i_labels[1])	return i_labels[0] + "_" + i_labels[1];
		else							return i_labels[1] + "_" + i_labels[0];
	}
	
	public void countPositive(ArrayList<Integer> x, int[] counts)
	{
		double[] scores = getScores(x);
		
		for (int i=0; i<n_labels; i++)
			if (scores[i] > 0)	counts[i_labels[i]]++;
	}
	
	public void addScores(ArrayList<Integer> x, double[] counts, double weight)
	{
		double[] scores = getScores(x);
		
		for (int i=0; i<n_labels; i++)
			counts[i_labels[i]] += (scores[i] * weight);
	}
	
	public boolean containsLabel(int label)
	{
		for (int iLabel : i_labels)
			if (iLabel == label)	return true;
		
		return false;
	}
}
