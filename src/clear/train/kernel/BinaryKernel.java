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
import com.carrotsearch.hppc.IntOpenHashSet;

/**
 * Linear kernel.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
public class BinaryKernel extends AbstractKernel
{
	public BinaryKernel(String instanceFile)
	{
		super(KERNEL_BINARY, instanceFile);
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
		
		// feature dimension = last feature-index + 1
		D++;
		
		// sort labels;
		a_labels = sLabels.toArray();
		Arrays.sort(a_labels);
		L = a_labels.length;

		System.out.println("- # of instances: " + N);
		System.out.println("- # of labels   : " + L);
		System.out.println("- # of features : " + D);
	}
}