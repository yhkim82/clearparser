package clear.experiment;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLHead;
import clear.dep.srl.SRLInfo;
import clear.reader.SRLReader;

public class CheckSpan
{
	public CheckSpan(String inputFile)
	{
		SRLReader reader = new SRLReader(inputFile, true);
		DepTree   tree;
		
		while ((tree = reader.nextTree()) != null)
			gather(tree);
	}
	
	void gather(DepTree tree)
	{
		IntObjectOpenHashMap<IntOpenHashSet> map = new IntObjectOpenHashMap<IntOpenHashSet>();
		DepNode node;
		SRLInfo info;
		
		int i, size = tree.size();
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			info = node.srlInfo;
			
			for (SRLHead head : info.heads)
			{
				
			}
		}
	}
	
	public static void main(String[] args)
	{

	}
}
