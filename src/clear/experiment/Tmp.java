package clear.experiment;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLHead;
import clear.dep.srl.SRLInfo;
import clear.reader.SRLReader;
import clear.util.cluster.Prob1dMap;
import clear.util.tuple.JObjectDoubleTuple;

public class Tmp
{
	public Tmp(String inputFile)
	{
		SRLReader reader = new SRLReader(inputFile, true);
		DepTree tree;
		DepNode node;
		SRLInfo info;
		int dir, dis;
		
		Prob1dMap map = new Prob1dMap();
		
		while ((tree = reader.nextTree()) != null)
		{
			for (int i=1; i<tree.size(); i++)
			{
				node = tree.get(i);
				info = node.srlInfo;
				
				for (SRLHead head : info.heads)
				{
					dir = (node.id < head.headId) ? -1 : 1;
					dis = Math.abs(node.id - head.headId);
					
					if      (dis <=  5)	dis =  1;
					else if (dis <= 10)	dis =  2;
					else if (dis <= 15)	dis =  3;
					else if (dis <= 20)	dis =  4;
					else if (dis <= 25)	dis =  5;
					else if (dis <= 30)	dis =  6;
					else if (dis <= 35)	dis =  7;
					else if (dis <= 40)	dis =  8;
					else if (dis <= 45)	dis =  9;
					else if (dis <= 50)	dis = 10;
					else				dis = 11;
					
					dis *= dir;
					map.increment(Integer.toString(dis));
				}
			}
		}
		
		for (JObjectDoubleTuple<String> cur : map.getProbList())
			System.out.println(cur.object+"\t"+cur.value);
	}
	
	
	static public void main(String[] args)
	{
		String inputFile = args[0];
		new Tmp(inputFile);
	}
}
