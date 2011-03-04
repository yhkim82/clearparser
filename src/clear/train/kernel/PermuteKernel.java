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
package clear.train.kernel;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;

import clear.util.DSUtil;
import clear.util.IOUtil;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

/**
 * Linear kernel.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
public class PermuteKernel extends AbstractKernel
{
	public  ObjectIntOpenHashMap<String> m_perm;
	private double d_threshold;
	
	public PermuteKernel(String instanceFile, double threshold)
	{
		super(KERNEL_PERMUTE, instanceFile);
		
		d_threshold = threshold;
	}
	
	/**
	 * Reads training instances from <code>instanceFile</code> and stores to 
	 * {@link AbstractKernel#a_ys} and {@link AbstractKernel#a_xs}. 
	 * @param instanceFile name of a file containing training instances
	 */
	protected void init(String instanceFile) throws Exception
	{
		final int NUM = 1000000;
		
		BufferedReader fin = IOUtil.createBufferedFileReader(instanceFile);
		
		a_ys = new IntArrayList    (NUM);
		a_xs = new ArrayList<int[]>(NUM);
		a_vs = null;
		
		IntOpenHashSet sLabels = new IntOpenHashSet();
		String line;
		String[] tok;	int y;	int[] x;
		
		for (N=0; (line = fin.readLine()) != null; N++)
		{
			tok = line.split(COL_DELIM);
			y   = Integer.parseInt (tok[0]);
			x   = DSUtil.toIntArray(tok, 1);
			
			// add label and feature
			a_ys.add(y);
			a_xs.add(x);

			// indices in feature are in ascending order
			D = Math.max(D, x[x.length-1]);
			sLabels.add(y);
			
			if (N%100000 == 0)	System.out.print("\r* Initializing  : "+(N/1000)+"K");
		}	System.out.println("\r* Initializing  : " + instanceFile);
		
		fin.close();
		a_ys.trimToSize();
		a_xs.trimToSize();
		
		// sort labels;
		a_labels = sLabels.toArray();
		Arrays.sort(a_labels);
		L = a_labels.length;
		
		System.out.println("- # of instances: " + N);
		System.out.println("- # of labels   : " + L);
		System.out.println("- # of original features: " + D);
		
		permute();
		System.out.println("- # of permuted features: " + D);
	}
	
	static public String[] getPerm(int[] x)
	{
		int i, j, k = 0, size = x.length;
		String[] list = new String[(size*(size+1))/2];
		
		for (i=0; i<size; i++)
		{
			list[k++] = Integer.toString(x[i]);
			
			for (j=i+1; j<size; j++)
				list[k++] = x[i]+"_"+x[j];
		}
		
		return list;
	}
	
	static public String[] getPerm(IntArrayList x)
	{
		int i, j, k = 0, size = x.size();
		String[] list = new String[(size*(size+1))/2];
		
		for (i=0; i<size; i++)
		{
			list[k++] = Integer.toString(x.get(i));
			
			for (j=i+1; j<size; j++)
				list[k++] = x.get(i)+"_"+x.get(j);
		}
		
		return list;
	}
	
	private void permute()
	{
		final String TOTAL = "T";
		int i, y;	int[] x;
		
		IntObjectOpenHashMap<ObjectIntOpenHashMap<String>> mYX = new IntObjectOpenHashMap<ObjectIntOpenHashMap<String>>();
		
		for (int label : a_labels)
			mYX.put(label, new ObjectIntOpenHashMap<String>());
		
		ObjectIntOpenHashMap<String> mY;
		ObjectIntOpenHashMap<String> mX = new ObjectIntOpenHashMap<String>();
		
		for (i=0; i<N; i++)
		{
			y = a_ys.get(i);
			x = a_xs.get(i);
		
			mY = mYX.get(y);
			mY.put(TOTAL, mY.get(TOTAL)+1);
			
			for (String key : getPerm(x))
			{
				mY.put(key  , mY.get(key)  +1);
				mX.put(key  , mX.get(key)  +1);
				mX.put(TOTAL, mX.get(TOTAL)+1);
			}
			
			if (i%100000 == 0)	System.out.print("\r  - Initialize perm-map : "+(i/1000)+"K");
		}	System.out.println("\r  - Initialize perm-map : "+i);

		m_perm = new ObjectIntOpenHashMap<String>();
		D      = 1;
		int    tY, tX = mX.get(TOTAL);
		double pmi, avg = 0;
		
		for (int label : a_labels)
		{
			mY = mYX.get(label);
			tY = mY .get(TOTAL);
			
			for (ObjectCursor<String> key : mY.keySet())
			{
				if (m_perm.containsKey(key.value))	continue;
				
				pmi = Math.log(((double)mY.get(key.value) / tY) / ((double)mX.get(key.value) / tX));
				if (pmi > d_threshold)
				{
					m_perm.put(key.value, D++);
					avg += pmi;
				}
			}
			
			avg /= (D - 1);
			System.out.print("\r  - Compute PMI         : "+label);
		}	System.out.println("\r  - Compute PMI         : "+avg);
		
		IntArrayList list;
		mYX.clear();
		
		for (i=0; i<N; i++)
		{
			list = new IntArrayList();
			
			for (String key : getPerm(a_xs.get(i)))
			{
				if ((y = m_perm.get(key)) != 0)
					list.add(y);
			}
			
			x = list.toArray();
			Arrays.sort(x);
			a_xs.set(i, x);
			
			if (i%100000 == 0)	System.out.print("\r  - Permuate instances  : "+(i/1000)+"K");
		}	System.out.println("\r  - Permuate instances  : "+i);
	}
}
