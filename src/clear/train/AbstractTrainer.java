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
package clear.train;

import java.io.PrintStream;

import clear.train.algorithm.IAlgorithm;
import clear.train.kernel.AbstractKernel;
import clear.train.kernel.LinearKernel;
import clear.train.kernel.PermutationKernel;

/**
 * Abstract trainer.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
abstract public class AbstractTrainer
{
	static public final byte   ST_BINARY       = 0;
	static public final byte   ST_ONE_VS_ALL   = 1;
	
	protected String         s_modelFile;
	protected PrintStream    f_out;
	protected IAlgorithm     a_algorithm;
	protected AbstractKernel k_kernel;
	protected int            i_numThreads;
	
	public AbstractTrainer(String instanceFile, String modelFile, IAlgorithm algorithm, byte kernel, int numThreads)
	{
		s_modelFile  = modelFile;
		f_out        = null;
		a_algorithm  = algorithm;
		i_numThreads = numThreads;
		
		if (kernel == AbstractKernel.LINEAR)
			k_kernel = new LinearKernel     (instanceFile);
		else
			k_kernel = new PermutationKernel(instanceFile);
		
		initModel();
		train();
	}
	
	public AbstractTrainer(String instanceFile, PrintStream fout, IAlgorithm algorithm, byte kernel, int numThreads)
	{
		s_modelFile  = null;
		f_out        = fout;
		a_algorithm  = algorithm;
		i_numThreads = numThreads;
		
		if (kernel == AbstractKernel.LINEAR)
			k_kernel = new LinearKernel     (instanceFile);
		else
			k_kernel = new PermutationKernel(instanceFile);
		
		initModel();
		train();
	}
	
	abstract protected void initModel();
	abstract protected void train();
}
