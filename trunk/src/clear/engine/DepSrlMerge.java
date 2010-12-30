package clear.engine;

import clear.reader.MergeReader;
import clear.srl.merge.MergeTree;

public class DepSrlMerge
{
	static public void main(String[] args)
	{
		MergeReader reader = new MergeReader(args[0]);
		MergeTree   tree;
		
		while ((tree = reader.nextTree()) != null)
		{
			tree.mapArgs();
		}
		
	}

}
