package clear.experiment;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.reader.DepReader;

public class ExtractRules
{
	public ExtractRules(String filename)
	{
		DepReader reader = new DepReader(filename, true);
		DepTree   tree;
		
		while ((tree = reader.nextTree()) != null)
		{
			
		}
	}
	
	private void extract(DepTree tree)
	{
		int i, size = tree.size();
		DepNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
