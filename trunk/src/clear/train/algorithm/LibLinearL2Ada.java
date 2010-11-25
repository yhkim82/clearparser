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
package clear.train.algorithm;

import java.util.Arrays;

import clear.train.kernel.AbstractKernel;
import clear.util.DSUtil;
import clear.util.tuple.JDoubleDoubleTuple;

/**
 * LibLinear L2-SVM algorithm.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
public class LibLinearL2Ada implements IAlgorithm
{
	private byte   i_lossType;
	private double d_c;
	private double d_eps;
	private double d_bias;
	
	public LibLinearL2Ada(byte lossType, double c, double eps, double bias)
	{
		i_lossType = lossType;
		d_c        = c;
		d_eps      = eps;
		d_bias     = bias;
	}
	
	public double[] getWeight(AbstractKernel kernel, int currLabel)
	{
		double[] gWeight = new double[kernel.D];
		double[] ada     = new double[kernel.N];
		Arrays.fill(ada, (double)1/kernel.N);
		
		double[] weight = new double[kernel.D];
		JDoubleDoubleTuple tup;
		double threshold = 0.5;
		
		for (int i=1; ; i++)
		{
			if (i%100 == 0)	threshold -= 0.01;
			
			Arrays.fill(weight, 0);
			tup = getWeightAux(kernel, currLabel, weight, ada, i-1);
			
			if (tup.d1 >= threshold)
				break;
			if (tup.d1 == 0.0)
			{
				DSUtil.copy(gWeight, weight);
				break;
			}
			
			udpateWeights(gWeight, weight, tup.d2);
			updateAda(kernel, currLabel, tup.d2, weight, ada);
		}
		
		AbstractKernel.normalize(gWeight);
		return gWeight;
	}
	
	public JDoubleDoubleTuple getWeightAux(AbstractKernel kernel, int currLabel, double[] weight, double[] ada, int adaIter)
	{
		final int MAX_ITER = 1000;
		
	//	Random   rand   = new Random(0);
		double[] QD     = new double[kernel.N];
		double[] alpha  = new double[kernel.N];
		double U, G, d, alpha_old;
		
		int [] index = new int [kernel.N];
		byte[] aY    = new byte[kernel.N];
		
		int active_size = kernel.N;
		int i, j, s, iter;
		byte     yi;
		int[]    xi;
		double[] vi = null;
		
		// PG: projected gradient, for shrinking and stopping
		double PG;
		double PGmax_old = Double.POSITIVE_INFINITY;
		double PGmin_old = Double.NEGATIVE_INFINITY;
		double PGmax_new, PGmin_new;
		
		// for loss function
		double[] diag        = {0, 0, 0};
		double[] upper_bound = {d_c, 0, d_c};
		
		if (i_lossType == 2)
		{
			diag[0] = 0.5 / d_c;
			diag[2] = 0.5 / d_c;
			upper_bound[0] = Double.POSITIVE_INFINITY;
			upper_bound[2] = Double.POSITIVE_INFINITY;
		}
		
		for (i=0; i<kernel.N; i++)
		{
			index[i] = i;
			aY   [i] = (kernel.a_ys.get(i) == currLabel) ? (byte)1 : (byte)-1;
			QD   [i] = diag[GETI(aY, i)];
			
			if (kernel.type == AbstractKernel.KERNEL_BINARY)
				QD[i] += kernel.a_xs.get(i).length;
			else
			{
				for (double value : kernel.a_vs.get(i))
					QD[i] += (value * value);
			}
			
			if (d_bias > 0)	QD[i] += (d_bias * d_bias);
		}
		
		for (iter=0; iter<MAX_ITER; iter++)
		{
			PGmax_new = Double.NEGATIVE_INFINITY;
			PGmin_new = Double.POSITIVE_INFINITY;
			
		/*	for (i=0; i<active_size; i++)
			{
				int j = i + r_rand.nextInt(active_size - i);
				swap(index, i, j);
			}*/
			
			for (s=0; s<active_size; s++)
			{
				i  = index[s];
				yi = aY[i];
				xi = kernel.a_xs.get(i);
				if (kernel.type == AbstractKernel.KERNEL_VALUE)
					vi = kernel.a_vs.get(i);

				G = (d_bias > 0) ? weight[0] * d_bias : 0;
				for (j=0; j<xi.length; j++)
				{
					if (kernel.type == AbstractKernel.KERNEL_BINARY)
						G += weight[xi[j]];
					else
						G += (weight[xi[j]] * vi[j]);
				}
				
				G = G * yi - 1;
				G += alpha[i] * diag[GETI(aY, i)];
				U = upper_bound[GETI(aY, i)];
				
				if (alpha[i] == 0)
				{
					if (G > PGmax_old)
					{
						active_size--;
						swap(index, s, active_size);
						s--;
						continue;
					}
					
					PG = Math.min(G, 0);
                }
				else if (alpha[i] == U)
				{
					if (G < PGmin_old)
					{
						active_size--;
						swap(index, s, active_size);
						s--;
						continue;
					}
					
					PG = Math.max(G, 0);
				}
				else
				{
					PG = G;
				}
				
				PGmax_new = Math.max(PGmax_new, PG);
				PGmin_new = Math.min(PGmin_new, PG);
				
				if (Math.abs(PG) > 1.0e-12)
				{
					alpha_old = alpha[i];
					alpha[i] = Math.min(Math.max(alpha[i] - G / QD[i], 0.0), U);
					d = (alpha[i] - alpha_old) * yi * ada[i];
					
					if (d_bias > 0)	weight[0] += d * d_bias;
					
					for (j=0; j<xi.length; j++)
					{
						if (kernel.type == AbstractKernel.KERNEL_BINARY)
							weight[xi[j]] += d;
						else
							weight[xi[j]] += (d * vi[j]);
					}
				}
			}
			
			if (PGmax_new - PGmin_new <= d_eps)
			{
				if (active_size == kernel.N)
					break;
				else
				{
					active_size = kernel.N;
					PGmax_old = Double.POSITIVE_INFINITY;
					PGmin_old = Double.NEGATIVE_INFINITY;
					continue;
				}
			}
			
			PGmax_old = PGmax_new;
			PGmin_old = PGmin_new;
			if (PGmax_old <= 0) PGmax_old = Double.POSITIVE_INFINITY;
			if (PGmin_old >= 0) PGmin_old = Double.NEGATIVE_INFINITY;
		}
		
		double error = 0;
		
		for (i=0; i<kernel.N; i++)
		{
			yi = aY[i];
			xi = kernel.a_xs.get(i);
			
			G = getScore(weight, xi);
			if (G * yi <= 0)	error += ada[i];
		}
		
		StringBuilder build = new StringBuilder();
		double a = getAlpha(error);
		
		build.append("- label = ");
		build.append(currLabel);
		build.append(": ada = ");
		build.append(adaIter);
		build.append(": iter = ");
		build.append(iter);
		build.append(", error = ");
		build.append(error);
		build.append(", alpha = ");
		build.append(a);

		System.out.println(build.toString());
		
		return new JDoubleDoubleTuple(error, a);
	}
	
	private int GETI(byte[] y, int i)
	{
		return y[i] + 1;
	}
	
	private void swap(int[] array, int idxA, int idxB)
	{
		int temp    = array[idxA];
		array[idxA] = array[idxB];
		array[idxB] = temp;
	}
	
	private double getScore(double[] weight, int[] x)
	{
		double score = (d_bias > 0) ? weight[0] * d_bias : 0;
		
		for (int idx : x)
			score += weight[idx];
		
		return score;
	}
	
	private void udpateWeights(double[] gWeight, double[] weight, double alpha)
	{
		int i;
		
		for (i=0; i<gWeight.length; i++)
			gWeight[i] += weight[i] * alpha;
	}
	
	private void updateAda(AbstractKernel kernel, int currLabel, double alpha, double[] weight, double[] ada)
	{
		int i, yi;
		int[]  xi;
		double score, norm = 0;
		
		for (i=0; i<kernel.N; i++)
		{
			yi = (kernel.a_ys.get(i) == currLabel) ? 1 : -1;
			xi = kernel.a_xs.get(i);
			score = getScore(weight, xi);
			
			if (yi*score > 0)	ada[i] *= Math.exp(-alpha);
			else				ada[i] *= Math.exp( alpha);
			
			norm += ada[i];
		}
		
		for (i=0; i<kernel.N; i++)
			ada[i] /= norm; 
	}
	
	private double getAlpha(double error)
	{
		return 0.5 * Math.log((1-error)/error);
	}
}
