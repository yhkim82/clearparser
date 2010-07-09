package clear.engine;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;

import clear.dep.DepTree;
import clear.io.FileExtensionFilter;
import clear.propbank.PBArg;
import clear.propbank.PBInstance;
import clear.propbank.PBReader;
import clear.reader.CoNLLReader;
import clear.reader.DepReader;
import clear.srl.SrlNode;
import clear.srl.SrlTree;
import clear.util.IOUtil;

public class Dep2Srl
{
	private final String KEY_DELIM = "_";
	private HashMap<String, PBInstance> m_propbank;

	public Dep2Srl(String pbFile, String tokenFile, String tbDir, String depDir, String depExt, String srlDir, String srlExt)
	{
		initPropbank(pbFile, tbDir);
		printTokenFile(tokenFile);
		processDepTrees(depDir, depExt, srlDir, srlExt);
	}
	
	private void initPropbank(String pbFile, String tbDir)
	{
		PBReader   reader = new PBReader(pbFile, tbDir);
		PBInstance instance;	int n;
		
		System.out.println("Load: "+pbFile);
		m_propbank = new HashMap<String, PBInstance>();
		
		for (n=0; (instance = reader.nextInstance(true, 1)) != null; n++)
		{
			instance.treeFile = FileExtensionFilter.getFilenameWithoutExtension(instance.treeFile);
			String key = instance.treeFile + KEY_DELIM + instance.treeIndex + KEY_DELIM + instance.predicateId;
			
			if (m_propbank.containsKey(key))	m_propbank.get(key).addArgs(instance.getArgs());
			else								m_propbank.put(key, instance);
			
			if (n % 1000 == 0)	System.out.print("\r"+n);
		}	System.out.println("\r"+n);
	}
	
	private void printTokenFile(String tokenFile)
	{
		PrintStream fout = IOUtil.createPrintFileStream(tokenFile);
		
		System.out.println("Save: "+tokenFile);
		for (String key : m_propbank.keySet())
			fout.println(m_propbank.get(key));
		
		fout.close();
	}
	
	private void processDepTrees(String depDir, String depExt, String srlDir, String srlExt)
	{
		String[] filelist = new File(depDir).list(new FileExtensionFilter(depExt));

		for (String depFile : filelist)
		{
			System.out.print("\rProcess: "+depFile);
			DepReader reader = new DepReader(depDir + File.separator + depFile, true);
			DepTree   dTree;
			
			String  filename = FileExtensionFilter.getFilenameWithoutExtension(depFile);
			PrintStream fout = IOUtil.createPrintFileStream(srlDir + File.separator + filename+"."+srlExt);
			
			for (int treeIndex=0; (dTree = reader.nextTree()) != null; treeIndex++)
			{
				SrlTree sTree = new SrlTree(dTree);
				sTree.setChildrenIDs();
				sTree.setSubIDs();
				
				for (int currId=1; currId<sTree.size(); currId++)
				{
					SrlNode curr = sTree.get(currId);
					if (curr.isPos("VBG") && curr.isDeprel("NMOD"))	continue;
					
					String key = filename + KEY_DELIM + treeIndex + KEY_DELIM + currId;
					PBInstance instance = m_propbank.get(key);
					if (instance == null)	continue;
					
					curr.rolesetId = instance.rolesetId;
					
					for (PBArg oArg : instance.getArgs())
					{
						PBArg nArg = new PBArg(oArg.label, sTree.getHeadIDs(oArg.ids));
						curr.addArg(nArg);
						sTree.injectArg(nArg);
					}
				}
				
				fout.println(sTree+"\n");
			}
			
			fout.close();
			reader.close();
		}	System.out.println();
	}
	
	static public void main(String[] args)
	{
		String pbFile    = args[0];
		String tokenFile = args[1];
		String tbDir     = args[2];
		String depDir    = args[3];
		String depExt    = args[4];
		String srlDir    = args[5];
		String srlExt    = args[6];
		
		new Dep2Srl(pbFile, tokenFile, tbDir, depDir, depExt, srlDir, srlExt);
	}
}
