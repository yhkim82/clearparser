package clear.engine;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.reader.CoNLLReader;

public class CheckNonPorjective
{
	public CheckNonPorjective(String inputFile)
	{
		CoNLLReader reader = new CoNLLReader(inputFile, true);
		DepTree   tree;
		
		while ((tree = reader.nextTree()) != null)
		{
			if (!isProjective(tree))
			{
				System.out.println(tree);
				break;
			}
		}
		
	}

	private boolean isProjective(DepTree tree)
	{
		for (int i=1; i<tree.size(); i++)
		{
			DepNode curr = tree.get(i);
			DepNode head = tree.get(curr.headId);
			
			int sId = (curr.id < head.id) ? curr.id : head.id;
			int eId = (curr.id < head.id) ? head.id : curr.id;
			
			for (int j=sId+1; j<eId; j++)
			{
				DepNode node = tree.get(j);
				if (node.headId < sId || node.headId > eId)
				{
					System.out.println(sId+" "+node.id+" "+eId+" "+node.headId);
					return false;
				}
			}
		}
		
		return true;
	}
	
	static public void main(String[] args)
	{
		new CheckNonPorjective(args[0]);
	}
}
