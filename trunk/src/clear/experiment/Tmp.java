package clear.experiment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import clear.treebank.TBNode;
import clear.treebank.TBReader;
import clear.treebank.TBTree;

public class Tmp
{
	HashSet<String> set;
	
	public Tmp(String filename)
	{
		TBReader reader = new TBReader(filename);
		TBTree tree;
		int n = 0;
		
		set = new HashSet<String>();
		
		while ((tree = reader.nextTree()) != null)
		{
			 retrieve(tree, tree.getRootNode());
			 n++;
		}
		
		System.out.println(n);
		System.out.println(set.toString());
	}
	
	void retrieve(TBTree tree, TBNode node)
	{
		String ppos;
	//	String tag = "PRN";
		
		if (node.isPhrase())
		{
			ppos = node.pos.split(";")[0];
		//	set.add(ppos);
			
		/*	boolean isFound = false;
			
			for (TBNode child : node.getChildren())
			{
				cpos = child.pos.split(";")[0];
				
				if ((ppos.equals(cpos) || cpos.equals("X") || (ppos.equals("S") && cpos.matches("VP|VNP"))) && child.isTag(tag))	
				{
					isFound = true;
					break;
				}
			}
			
			int n = 0;
		
			for (TBNode child : node.getChildren())
			{
				if (child.isTag(tag))	
					n++;
			}
			
			if (n > 1)
			{
				System.out.println(ppos+"-"+node.tags+" "+node.toPosTags());
				System.out.println(node.toPosWords());
			//	System.out.println(tree.getRootNode().toWords());
			}*/
			
		/*	if (node.containsTag(tag))
			{
				int n = 0;
				
				for (TBNode child : node.getChildren())
				{
					cpos = child.pos.split(";")[0];
					
					if (!cpos.equals("X"))
					{
						if (!child.isTag(tag))
							n++;
						else if (ppos.equals(cpos))
							n++;						
					}
				}
				
				if (n > 1)
				{
					System.out.println(ppos+"-"+node.tags+" "+node.toPosTags());
					System.out.println(node.toPosWords());
				}
			}*/
			
			if (ppos.equals("L"))
			{
				System.out.println(ppos+"-"+node.tags+" "+node.toPosTags());
				System.out.println(node.toPosWords());
			}
				
			
			
			for (TBNode child : node.getChildren())
				retrieve(tree, child);
		}
	}
	
	static public List<String> toList()
	{
		System.out.println("Function");
		return Arrays.asList("s","a","c");
	}
	
	public static void main(String[] args)
	{
		for (String s : toList())
		{
			System.out.println(s);
		}
		
	/*	String s = "+a/A+//SF++/SS";
		s = "디자인/NNG+하/XSV+ᆯ/ETM";
		
		for (JObjectObjectTuple<String, String> tup : TBKrLib.splitMorphem(s))
			System.out.println(tup.o1+":"+tup.o2);*/
	}
}
