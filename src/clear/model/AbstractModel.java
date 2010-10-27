package clear.model;

import java.io.BufferedReader;
import java.io.PrintStream;

public class AbstractModel
{
	public int      n_labels;
	public int      n_features;
	public double[] d_weights;

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
			if ((i+1)%n_features == 0)	System.out.print(".");
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
}
