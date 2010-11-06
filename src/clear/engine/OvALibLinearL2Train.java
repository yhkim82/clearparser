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
package clear.engine;

import clear.train.OvALibLinearL2Trainer;

/**
 * Trains one-vs-all LibLinear L2-* classifier.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/3/2010
 */
public class OvALibLinearL2Train
{
	private String s_instanceFile = null;
	private String s_modelFile    = null;
	private int    i_numThreads   = 2;
	private byte   i_lossType     = 1;
	private double d_c            = 0.1;
	private double d_eps          = 0.1;
	private double d_bias         = -1;
	
	public OvALibLinearL2Train(String[] args)
	{
		s_instanceFile = args[0];
		s_modelFile    = args[1];
		new OvALibLinearL2Trainer(s_instanceFile, s_modelFile, i_numThreads, i_lossType, d_c, d_eps, d_bias);
	}
	
	static public void main(String[] args)
	{
		new OvALibLinearL2Train(args);
	}
}
