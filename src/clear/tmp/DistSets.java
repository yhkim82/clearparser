package clear.tmp;

import java.io.PrintStream;
import java.util.ArrayList;

import clear.dep.DepTree;
import clear.reader.DepReader;
import clear.util.IOUtil;

public class DistSets
{
	public DistSets(String inputFile)
	{
		DepReader reader = new DepReader(inputFile, true);
		DepTree   tree;
		ArrayList<DepTree> ls = new ArrayList<DepTree>();
		
		while ((tree = reader.nextTree()) != null)
			if (tree.size() > 1)	ls.add(tree);
		
		long trainSize = Math.round((double)ls.size() * 1.0);
		PrintStream fTrain = IOUtil.createPrintFileStream(inputFile+".trn");
		PrintStream fTest  = IOUtil.createPrintFileStream(inputFile+".tst");
		
		for (int i=0; i<ls.size(); i++)
		{
			if (i < trainSize)	fTrain.println(ls.get(i)+"\n");
			else				fTest .println(ls.get(i)+"\n");
		}
	}

	public static void main(String[] args)
	{
		new DistSets(args[0]);
	}
}
