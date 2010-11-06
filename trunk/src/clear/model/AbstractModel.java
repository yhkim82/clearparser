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

import clear.train.kernel.AbstractKernel;

/**
 * Abstract model.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
abstract public class AbstractModel
{
	public int      n_labels;
	public int      n_features;
	public double[] d_weights;
	
	/** For decoding. */
	public AbstractModel(String modelFile)
	{
		load(modelFile);
	}
	
	/** For training. */
	public AbstractModel(AbstractKernel kernel)
	{
		init(kernel);
	}
	
	protected void readWeights(BufferedReader fin) throws Exception
	{
		int[] buffer = new int[128];
		int   i, b;
		
		for (i=0; i < d_weights.length; i++)
		{
			b = 0;
			
			while (true)
			{
				int ch = fin.read();
				
				if (ch == ' ')	break;
				else			buffer[b++] = ch;
			}

			d_weights[i] = Double.parseDouble((new String(buffer, 0, b)));
		}
	}
	
	protected void printWeights(PrintStream fout) throws Exception
	{
		StringBuilder build = new StringBuilder();
		int i;
		
		for (i=0; i<d_weights.length; i++)
		{
			build.append(d_weights[i]);
			build.append(' ');
		}
		
		fout.println(build.toString());
	}
	
	abstract public void init(AbstractKernel kernel);
	abstract public void load(String modelFile);
	abstract public void save(String modelFile);
	abstract public void copyWeight(int label, double[] weight);
}