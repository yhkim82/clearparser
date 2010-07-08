package clear.engine;


import java.io.File;
import java.io.PrintStream;

import clear.io.FileExtensionFilter;
import clear.propbank.PBArg;
import clear.propbank.PBInstance;
import clear.reader.SrlReader;
import clear.srl.SrlNode;
import clear.srl.SrlTree;
import clear.util.IOUtil;

public class Srl2PBToken {

	public Srl2PBToken(String srlDir, String srlExt, String outputFile)
	{
		File     file     = new File(srlDir);
		String[] filelist = file.list(new FileExtensionFilter(srlExt));
		PrintStream  fout = IOUtil.createPrintFileStream(outputFile);
		
		for (String filename : filelist)
		{
			System.out.print("\r"+filename);
			
			SrlReader reader = new SrlReader(srlDir + File.separator + filename);
			SrlTree   sTree;
			filename         = FileExtensionFilter.getFilenameWithoutExtension(filename);
			
			for (int treeIndex=0; (sTree = reader.nextTree()) != null; treeIndex++)
			{
				sTree.setChildrenIDs();
				sTree.setSubIDs();
				
				for (int currId=1; currId<sTree.size(); currId++)
				{
					SrlNode curr = sTree.get(currId);
					if (!curr.isPredicate())	continue;
					
					PBInstance instance  = new PBInstance();
					instance.treeFile    = filename;
					instance.treeIndex   = treeIndex;
					instance.predicateId = curr.id;
					instance.rolesetId   = curr.rolesetId;
					
					for (PBArg oArg : curr.getArgList())
					{
						PBArg nArg = new PBArg(oArg.label, sTree.getSubIDs(oArg.label, curr.id, oArg.ids));
						instance.addArg(nArg);
					}
					
					instance.removeOverlaps();	// Model 3
					
					fout.println(instance);
				}
			}
		}	System.out.println();
	}
	
	public static void main(String[] args)
	{
		String srlDir     = args[0];
		String srlExt     = args[1];
		String outputFile = args[2];
		
		new Srl2PBToken(srlDir, srlExt, outputFile);
	}

}
