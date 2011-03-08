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

import clear.train.kernel.AbstractKernel;
import clear.train.kernel.PolynomialKernel;
import clear.util.IOUtil;
import clear.util.tuple.JIntDoubleTuple;

import com.carrotsearch.hppc.IntArrayList;

/**
 * One-vs-all model.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/5/2010
 */
public class OneVsAllPolyModel extends AbstractMultiModel
{
	SupportVectorModel[] s_models;
	int                  i_degree;
	double               d_gamma;
	double               d_coef;
	
	public OneVsAllPolyModel(AbstractKernel kernel)
	{
		super(kernel);
	}
	
	public OneVsAllPolyModel(String modelFile)
	{
		super(modelFile);
	}
	
	public OneVsAllPolyModel(BufferedReader fin)
	{
		super(fin);
	}
		
	public void init(AbstractKernel kernel)
	{
		PolynomialKernel poly = (PolynomialKernel)kernel;
		
		n_labels = kernel.L;
		i_degree = poly.i_degree;
		d_gamma  = poly.d_gamma;
		d_coef   = poly.d_coef;
		
		a_labels = kernel.a_labels;
		s_models = new SupportVectorModel[n_labels];
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
		n_labels = Integer.parseInt  (fin.readLine());
		i_degree = Integer.parseInt  (fin.readLine());
		d_gamma  = Double.parseDouble(fin.readLine());
		d_coef   = Double.parseDouble(fin.readLine());

		a_labels = new int[n_labels];
		readLabels(fin);
		
		s_models = new SupportVectorModel[n_labels];
		for (int i=0; i<n_labels; i++)
		{
			s_models[i] = new SupportVectorModel();
			s_models[i].load(fin);
		}
	}
	
	public void save(String modelFile)
	{
		try
		{
			PrintStream fout = IOUtil.createPrintFileStream(modelFile);
			
			saveAux(fout);
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
		fout.println(i_degree);
		fout.println(d_gamma);
		fout.println(d_coef);
		
		printLabels(fout);
		
		for (SupportVectorModel model : s_models)
			model.print(fout);
	}
	
	public void copyWeight(int label, double[] weight) {}
	
	public void copySupportVectors(int label, SupportVectorModel model)
	{
		s_models[label] = model;
	}
	
	public double[] getScores(int[] x)
	{
		double[] scores = new double[n_labels];
		int      label;
		
		for (label=0; label<n_labels; label++)
			scores[label] = s_models[label].getScore(x, d_gamma, d_coef, i_degree);
		
		return scores;
	}
	
	public double[] getScores(IntArrayList x)
	{
		double[] scores = new double[n_labels];
		int      label;
		
		for (label=0; label<n_labels; label++)
			scores[label] = s_models[label].getScore(x, d_gamma, d_coef, i_degree);
		
		return scores;
	}
	
	public double[] getScores(ArrayList<JIntDoubleTuple> x)
	{
		double[] scores = new double[n_labels];
		int      label;
		
		for (label=0; label<n_labels; label++)
			scores[label] = s_models[label].getScore(x, d_gamma, d_coef, i_degree);
		
		return scores;
	}
}
