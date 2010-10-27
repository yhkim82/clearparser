package clear.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class OneVsAllModel extends AbstractModel
{
	public OneVsAllModel(String modelFile)
	{
		load(modelFile);
	}

	public OneVsAllModel(int nLabels, int nFeatures)
	{
		n_labels   = nLabels;
		n_features = nFeatures;
		d_weights  = new double[nFeatures * nLabels];
	}
	
	public void load(String modelFile)
	{
		try
		{
			BufferedReader fin = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(modelFile))));
			
			n_labels   = Integer.parseInt(fin.readLine());
			n_features = Integer.parseInt(fin.readLine());
			d_weights  = new double[n_features * n_labels];
			
			readWeights(fin);
			fin.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void save(String modelFile)
	{
		try
		{
			PrintStream fout = new PrintStream(new GZIPOutputStream(new FileOutputStream(modelFile)));
			
			fout.println(n_labels);
			fout.println(n_features);
			printWeights(fout);
			
			fout.flush();	fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void copyWeight(int label, double[] weight)
	{
		int i;
		
		for (i=0; i<n_features; i++)
			d_weights[i * n_labels + label] = weight[i];
	}
	
	public double getScore(int label, int[] x)
	{
		double score = d_weights[label];
		int    i;
		
		for (i=0; i < x.length; i++)
			score += d_weights[x[i] * n_labels + label];
		
		return score;
	}
	
	public double getScore(int label, ArrayList<Integer> x)
	{
		double score = d_weights[label];
		int    i;
		
		for (i=0; i < x.size(); i++)
			score += d_weights[x.get(i) * n_labels + label];
		
		return score;
	}
	
	public double[] getScores(int[] x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, label;
		
		for (i=0; i < x.length; i++)
		{
			for (label=0; label<n_labels; label++)
				scores[label] += d_weights[x[i] * n_labels + label];
		}
		
		return scores;
	}
	
	public double[] getScores(ArrayList<Integer> x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, label;
		
		for (i=0; i < x.size(); i++)
		{
			for (label=0; label<n_labels; label++)
				scores[label] += d_weights[x.get(i) * n_labels + label];
		}
		
		return scores;
	}
}
