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
import java.io.IOException;
import java.util.ArrayList;

import clear.model.vector.SparseVector;
import clear.train.AbstractTrainer;
import clear.util.tuple.JIntDoubleTuple;

/**
 * One-vs-all RRM model.
 * @author Jinho D. Choi
 * <br><b>Last update:</b> 4/27/2010
 */
public class RRMModel extends AbstractModel
{
	/** List of labels */
	private int[]          i_labels;
	/** Weight vectors for all labels */
	private SparseVector[] s_weight;
	
	/**
	 * Calls {@link RRMModel#init(String, byte)}.
	 * @param modelFile name of the file containing RRM model
	 * @param flag      0: dense-vector, 1: sparse-vector
	 */
	public RRMModel(String modelFile, byte flag)
	{
		init(modelFile, flag);
	}
	
	/**
	 * Initializes weight vectors for all labels.
	 * @param modelFile name of the file containing RRM model
	 * @param flag      0: dense-vector, 1: sparse-vector
	 */
	private void init(String modelFile, byte flag)
	{
		System.out.print("Initializing model: ");
		
		try
		{
			BufferedReader fin = new BufferedReader(new FileReader(modelFile));
		//	BufferedReader fin = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(modelFile))));
			String line;   int i;
			
			n_labels   = Integer.parseInt(fin.readLine());
			i_labels   = new int[n_labels];
			n_features = Integer.parseInt(fin.readLine());
			
			s_weight = new SparseVector[n_labels];
			
			for (i=0; (line = fin.readLine()) != null; i++)
			{
				String[] vector = line.split(AbstractTrainer.COL_DELIM);
				int       label = Integer.parseInt(vector[0]);
				int   [] aIndex = new int   [vector.length-1];
				double[] aValue = new double[vector.length-1];
				
				for (int j=1; j<vector.length; j++)
				{
					String[] str = vector[j].split(AbstractTrainer.FTR_DELIM);
					aIndex[j-1]  = Integer.parseInt   (str[0]);
					aValue[j-1]  = Double .parseDouble(str[1]);
				}
				
				i_labels[i] = label;
				s_weight[i] = new SparseVector(aIndex, aValue);
				if (i%10 == 0)	System.out.print("\rInitializing model: "+i);
			}	System.out.println("\rInitializing model: "+i);
		}
		catch (IOException e) {e.printStackTrace();}
	}
	
	/* (non-Javadoc)
	 * @see harvest.decode.AbstractLinearDecoder#get(java.util.ArrayList)
	 */
	public JIntDoubleTuple predict(ArrayList<Integer> x)
	{
		JIntDoubleTuple best = new JIntDoubleTuple(-1, -0.05);
		
		for (int i=0; i<n_labels; i++)
		{
			double score = s_weight[i].getScore(x);
			if (score >= best.d)	best.set(i_labels[i], score);
		}
		
		if (best.d > 1)	best.d = 1;
		return best;
	}
	
	/* (non-Javadoc)
	 * @see harvest.decode.AbstractLinearDecoder#getAll(java.util.ArrayList)
	 */
	public ArrayList<JIntDoubleTuple> predictAll(ArrayList<Integer> x)
	{
		ArrayList<JIntDoubleTuple> aRes = new ArrayList<JIntDoubleTuple>();
		
		outer: for (int i=0; i<n_labels; i++)
		{
			double score = s_weight[i].getScore(x);
			if (score > 1)	score = 1;
			
			for (int j=0; j<aRes.size(); j++)
			{
				if (aRes.get(j).d < score)
				{
					aRes.add(j, new JIntDoubleTuple(i_labels[i], score));
					continue outer;
				}
			}
			
			aRes.add(new JIntDoubleTuple(i_labels[i], score));
		}
		
		return aRes;
	}
}
