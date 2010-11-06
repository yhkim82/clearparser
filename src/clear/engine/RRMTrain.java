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
package clear.engine;

import clear.train.algorithm.RRM;

/**
 * Trains RRM (Robust Risk Minimization) model.
 * The main method reads <instance file> containing training instances and saves weight vectors to <code>modelFile.k.mu.eta.c</code>.
 * <pre>
 * Usage: java RRMTrain -i <instance file> -w <model file> [-t <# of threads> -k <K> -m <mu> -e <eta> -c <c>]
 * </pre>
 * @see clear.train.algorithm.RRM
 * @author Jinho D. Choi
 * <b>Last update:</b> 12/09/2009
 */
public class RRMTrain
{
	private String s_instanceFile = null;
	private String s_modelFile    = null;
	private int    i_numThreads   = 1;
	private int    i_K            = 40;
	private double d_mu           = 1.0;
	private double d_eta          = 0.001;
	private double d_c            = 0.1;
	
	public RRMTrain(String[] args)
	{
		if (!initArgs(args))	return;
		new RRM(s_instanceFile, s_modelFile, i_numThreads, i_K, d_mu, d_eta, d_c);
	}
	
	/** Initializes arguments. */
	private boolean initArgs(String[] args)
	{
		if (args.length == 0 || args.length % 2 != 0)
		{
			printUsage();
			return false;
		}
		
		try
		{
			for (int i=0; i<args.length; i+=2)
			{
				if      (args[i].equals("-i"))	s_instanceFile = args[i+1];
				else if (args[i].equals("-w"))	s_modelFile    = args[i+1];
				else if (args[i].equals("-t"))	i_numThreads   = Integer.parseInt   (args[i+1]);
				else if (args[i].equals("-k"))	i_K            = Integer.parseInt   (args[i+1]);
				else if (args[i].equals("-m"))	d_mu           = Double .parseDouble(args[i+1]);
				else if (args[i].equals("-e"))	d_eta          = Double .parseDouble(args[i+1]);
				else if (args[i].equals("-c"))	d_c            = Double .parseDouble(args[i+1]);
				else    { printUsage(); return false; }
			}
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return false;
		}
		
		if (s_instanceFile == null)
		{
			System.err.println("Error: <instance file> must be specified.");
			return false;
		}
		
		if (s_modelFile == null)
		{
			System.err.println("Error: <model file> must be specified.");
			return false;
		}
		
		if (i_numThreads < 1)
		{
			System.err.println("Error: <# of threads> must be greater than 0.");
			return false;
		}
		
		if (i_K < 1)
		{
			System.err.println("Error: <K> must be greater than 0.");
			return false;
		}
		
		return true;
	}

	/** Prints usage. */
	private void printUsage()
	{
		String usage = "Usage: java RRMTrain -i <instance file> -w <model file> [";
		usage += "-t <# of threads = " + i_numThreads + "> ";
		usage += "-k <K = "            + i_K          + "> ";
		usage += "-m <mu = "           + d_mu         + "> ";
		usage += "-e <eta = "          + d_eta        + "> ";
		usage += "-c <c = "            + d_c          + ">]";
		
		System.out.println(usage);
	}
	
	static public void main(String[] args)
	{
		new RRMTrain(args);
	}
}
