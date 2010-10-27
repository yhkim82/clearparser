package clear.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;

import clear.util.IOUtil;


public class JoinLiblinearModels 
{
	protected int n_labels;
	/** Weight vectors for all labels */
	protected float[] d_weights = null;
	/** Joined modelFile */
	protected PrintStream f_out;
	
	public JoinLiblinearModels(String modelFile1, float weight1, String modelFile2, float weight2, String outputFile)
	{
		f_out = IOUtil.createPrintFileStream(outputFile);
		
		try
		{
			init(modelFile1, weight1);
			init(modelFile2, weight2);
			printWeights();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void init(String modelFile, float weight) throws Exception
	{
		BufferedReader fin = new BufferedReader(new FileReader(modelFile));
		String line;	String[] tmp;	int i;
			
		line = fin.readLine();					// solverType
		tmp  = line.split(" ");		
		String solverType = tmp[1];
			
		line = fin.readLine();					// nr_class
		tmp  = line.split(" ");
		int nLabels = Integer.parseInt(tmp[1]);
		if (nLabels == 2 && !solverType.equals("MCSVM_CS"))	nLabels = 1;
			
		line = fin.readLine();					// label
		tmp  = line.split(" ");
		int[] iLabels = new int[nLabels];
		for (i=0; i<nLabels; i++)	iLabels[i] = Integer.parseInt(tmp[i+1]);

		line = fin.readLine();					// nr_feature
		tmp  = line.split(" ");
		int nFeatures = Integer.parseInt(tmp[1]);
			
		fin.readLine();							// bias
		fin.readLine();							// w
		
		if (d_weights == null)
		{
			d_weights = new float[nFeatures * nLabels];
			n_labels  = nLabels;
			
			f_out.println("solver_type "+solverType);
			f_out.println("nr_class "   +nLabels);
			f_out.print  ("label");
			for (i=0; i<nLabels; i++)	f_out.print(" "+i);
			f_out.println();
			f_out.println("nr_feature "+nFeatures);
			f_out.println("bias -1");
		}
		
		int[] buffer = new int[128];
		for (i=0; i<nFeatures; i++)
		{
			for (int j=0; j<nLabels; j++)
			{
				int index = i*n_labels + iLabels[j], b = 0;
				
				while (true)
				{
					int ch = fin.read();
					
					if (ch == ' ')	break;
					else			buffer[b++] = ch;
				}

				d_weights[index] += Float.parseFloat((new String(buffer, 0, b))) * weight;
			}
				
			if (i%1000000 == 0)	System.out.print(".");
		}
		
		fin.close();
	}
	
	private void printWeights()
	{
		f_out.println("w");
		StringBuilder build = new StringBuilder();
		
		for (int i=0; i<d_weights.length; i++)
		{
			if (d_weights[i] == 0)	build.append("0 ");
			else					build.append(d_weights[i]+" ");
		}
		
		f_out.println(build.toString());
	}
	
	static public void main(String[] args)
	{
		String modelFile1 = args[0];
		float  weight1    = Float.parseFloat(args[1]);
		String modelFile2 = args[2];
		float  weight2    = Float.parseFloat(args[3]);
		String outputFile = args[4];
		
		new JoinLiblinearModels(modelFile1, weight1, modelFile2, weight2, outputFile);
	}
}
