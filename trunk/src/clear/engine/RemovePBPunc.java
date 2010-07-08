package clear.engine;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import clear.dep.DepTree;
import clear.propbank.PBArg;
import clear.propbank.PBInstance;
import clear.propbank.PBLib;
import clear.reader.CoNLLReader;
import clear.util.IOUtil;

public class RemovePBPunc
{
	ArrayList<DepTree> ls_tree;
	String             st_treeFile;
	
	public RemovePBPunc(String propFile, String depDir, String outFile)
	{
		ls_tree     = new ArrayList<DepTree>();
		st_treeFile = "";
		
		process(propFile, depDir, outFile);
	}
	
	void process(String propFile, String depDir, String outFile)
	{
		Scanner scan = IOUtil.createFileScanner(propFile);
		PrintStream fout = IOUtil.createPrintFileStream(outFile);
		
		while (scan.hasNextLine())
		{
			String[] str = scan.nextLine().split(PBLib.FIELD_DELIM);
			PBInstance instance  = new PBInstance();
			DepTree tree;
			instance.treeFile    = str[0];
			instance.treeIndex   = Integer.parseInt(str[1]);
			instance.predicateId = Integer.parseInt(str[2]);
			
			// retrieve trees from new file
			if (!st_treeFile.equals(instance.treeFile))
			{
				CoNLLReader reader = new CoNLLReader(depDir + File.separator + instance.treeFile + ".dep", true);
				st_treeFile      = instance.treeFile;
				ls_tree.clear();
				
				while ((tree = reader.nextTree()) != null)	ls_tree.add(tree);
			}
			
			tree = ls_tree.get(instance.treeIndex);
			
			instance.rolesetId = str[3];	// str[3] = "gold", str[5] = "-----"
			
			for (int i=4; i<str.length; i++)
			{
				String[] arg   = str[i].split(PBLib.LABEL_DELIM);
				String   label = arg[0];
				TIntArrayList ids = new TIntArrayList();
				
				if (arg.length > 1)
				{
					
					String[] locs  = arg[1].split(PBLib.ID_DELIM);
					
					
					for (String loc : locs)
					{
						int id = Integer.parseInt(loc);
						if (!tree.get(id).isDeprel("P"))	ids.add(id);
					}
					
					ids.sort();
				}
				
				instance.addArg(new PBArg(label, ids));
			}
			
			fout.println(instance);
		}
	}

	static public void main(String[] args)
	{
		String depDir   = args[0];
		String propFile = args[1];
		String outFile  = args[2];
		
		new RemovePBPunc(propFile, depDir, outFile);
	}
}
