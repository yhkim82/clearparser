package clear.engine;

import java.util.Hashtable;

import clear.morph.MorphEnAnalyzer;
import clear.treebank.TBHeadRules;
import clear.treebank.TBReader;
import clear.treebank.TBTree;

public class PhraseToDep
{
	static public void main(String[] args)
	{
		String treeFile     = args[0];
		String headruleFile = args[1];
		String dictDir      = args[2];
		convert(treeFile, headruleFile, dictDir);
	//	check(args);
	}
	
	static public void convert(String treeFile, String headruleFile, String dictDir)
	{
		TBReader       reader = new TBReader(treeFile);
		TBHeadRules headrules = new TBHeadRules(headruleFile);
		MorphEnAnalyzer morph = new MorphEnAnalyzer(dictDir);
		TBTree      tree;

		while ((tree = reader.nextTree()) != null)
			System.out.println(tree.toDepTree(headrules, morph)+"\n");
	}
	
	static public void check(String[] args)
	{
		String treeFile     = args[0];
		TBReader    reader    = new TBReader(treeFile);
		TBTree      tree;

		Hashtable<String,String> hash = new Hashtable<String,String>();
		
		while ((tree = reader.nextTree()) != null)
			tree.checkPhrases(hash);
		
		for (String key : hash.keySet())	System.out.println(key+" "+hash.get(key));
	}
}
