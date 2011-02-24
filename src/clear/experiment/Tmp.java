package clear.experiment;
import java.util.ArrayList;
import java.util.Collections;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.feat.FeatCzech;
import clear.reader.DepReader;
import clear.util.tuple.JObjectDoubleTuple;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class Tmp
{
	static public void main(String[] args)
	{
		DepReader reader = new DepReader(args[0],true);
		DepTree   tree;
		reader.setLanguage(DepReader.LANG_CZ);
		int[] n_count = new int[FeatCzech.STR_TITLES.length];
		int[] n_total = new int[FeatCzech.STR_TITLES.length];
	//	ObjectIntOpenHashMap<String> mTotal = new ObjectIntOpenHashMap<String>();
		int total = 0;
		ObjectIntOpenHashMap<String> set = new ObjectIntOpenHashMap<String>();
		
		while ((tree = reader.nextTree()) != null)
		{
			for (int i=1; i<tree.size(); i++)
			{
				DepNode node = tree.get(i);
				if (!node.isDeprel("Coord"))	continue;
				if (!node.getFeat(9).equals("^"))
				{
					set.put(node.lemma, set.get(node.lemma)+1);
					total++;
				}
				
				
			/*	if (!tree.get(i).isDeprel("Coord"))	continue;
				
				for (int j=i+1; j<tree.size(); j++)
				{
					DepNode next = tree.get(j);
					String nextPos = next.getFeat(9);
					if (!nextPos.equals("f"))	continue;
					
					if (next.headId == i && next.deprel.endsWith("_M"))
					{
						for (int k=i-1; k>0; k--)
						{
							DepNode prev = tree.get(k);
							
							if (prev.headId == i && prev.deprel.equals(next.deprel) && nextPos.equals(prev.getFeat(9)))
							{
							//	mTotal.put(nextPos, mTotal.get(nextPos)+1);
							//	total++;
								
								String[] prevFeats = prev.feats.feats;
								String[] nextFeats = next.feats.feats;
								
								for (int l=0; l<nextFeats.length; l++)
								{
									if (nextFeats[l] == null || l == 9)	continue;
									n_total[l]++;
									
									if (prevFeats[l] != null && prevFeats[l].equals(nextFeats[l]))
										n_count[l]++;
								}
								
								break;
							}
						}
						
						break;
					}
				}*/
			}
		}
		
		ArrayList<JObjectDoubleTuple<String>> list = new ArrayList<JObjectDoubleTuple<String>>();
		
		for (ObjectCursor<String> key : set.keySet())
			list.add(new JObjectDoubleTuple<String>(key.value, 100d*set.get(key.value)/total));
		
		Collections.sort(list);
		
		for (JObjectDoubleTuple<String> tup : list)
			System.out.println(tup.object+"\t"+tup.value);
		
	/*	double[] scores = new double[n_total.length];
		double   prior  = 0.03140233533612969, normal = 0;
		
		for (int i=0; i<scores.length; i++)
		{
			if (n_total[i] != 0)
			{
				scores[i] = prior * n_count[i] / n_total[i];
				normal += scores[i];
			}
		}
		
		for (int i=0; i<scores.length; i++)
			scores[i] /= normal;
		
		System.out.println(Arrays.toString(scores));*/
		
	//	for (ObjectCursor<String> item : mTotal.keySet())
	//		System.out.println(item.value+"\t"+mTotal.get(item.value)+"\t"+total+"\t"+(double)mTotal.get(item.value)/total);
	}
}

