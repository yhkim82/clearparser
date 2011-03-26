package clear.util.cluster;

import java.util.ArrayList;
import java.util.Collections;

import clear.experiment.SRLVerbCluster;
import clear.util.tuple.JIntDoubleTuple;
import clear.util.tuple.JIntIntTuple;
import clear.util.tuple.JObjectDoubleTuple;
import clear.util.tuple.JObjectObjectTuple;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class SRLClusterBuilder
{
	public double d_threshold;
	
	ObjectDoubleOpenHashMap<String> d_similarities;
	ArrayList<ProbCluster>          k_clusters;
	
//	int iter_hm = 2, iter_km = 3;	// brown
//	int iter_hm = 2, iter_km = 2;	// wsj
	int iter_hm = 2, iter_km = 1;	
	
	public SRLClusterBuilder(double threshold)
	{
		d_threshold = threshold;
	}
	
	public ArrayList<ProbCluster> getInitClusters(Prob2dMap map, Prob1dMap mKeyword, double argmWeight)
	{
		ArrayList<ProbCluster> clusters = new ArrayList<ProbCluster>();
		ObjectDoubleOpenHashMap<String> lmap;
		ProbCluster cluster;
		String      label;
		double      weight;
		
		for (String key : map.keySet())
		{
			cluster = new ProbCluster (key);
			lmap    = map.getProb1dMap(key);
			
			for (ObjectCursor<String> cur : lmap.keySet())
			{
				label  = cur.value;
				weight = (mKeyword.containsKey(label)) ? Math.exp(mKeyword.getProb(label)) : 1;
				if (label.contains("AM"))	weight *= argmWeight;
				lmap.put(label, lmap.get(label)*weight);
			}
			
			cluster .add(lmap);
			clusters.add(cluster);
		}
		
		return clusters;
	}
	
	/** @return average cosine similarity between two clusters. */
	public double getAvgSimilarity(ProbCluster cluster1, ProbCluster cluster2, boolean useDynamic)
	{
		String key = getJoinedKey(cluster1, cluster2);
		if (useDynamic && d_similarities.containsKey(key))	return d_similarities.get(key);
		
		double avg = 0;
		
		for (ObjectDoubleOpenHashMap<String> map1 : cluster1)
			for (ObjectDoubleOpenHashMap<String> map2 : cluster2)
				avg += getCosineSimilarity(map1, map2);
		
		avg /= (cluster1.size() + cluster2.size());
		if (useDynamic)	d_similarities.put(key, avg);

		return avg;
	}
	
	/** @return cosine similarity of two maps. */
	public double getCosineSimilarity(ObjectDoubleOpenHashMap<String> map1, ObjectDoubleOpenHashMap<String> map2)
	{
		double dot = 0, scala1 = 0, scala2 = 0, val;
		String key;
		
		for (ObjectCursor<String> cur : map1.keySet())
		{
			key = cur.value;
			val = map1.get(key);
			
			if (map2.containsKey(key))
				dot += (val * map2.get(key));
			
			scala1 += (val * val);
		}
		
		for (ObjectCursor<String> cur : map2.keySet())
		{
			val = map2.get(cur.value);
			scala2 += (val * val);
		}
		
		scala1 = Math.sqrt(scala1);
		scala2 = Math.sqrt(scala2);
		
		return dot / (scala1 * scala2);
	}
	
	/** @return joined key of two clusters. */
	public String getJoinedKey(ProbCluster cluster1, ProbCluster cluster2)
	{
		StringBuilder build = new StringBuilder();
		
	/*	build.append("[");
		build.append(cluster1.key);
		build.append(",");
		build.append(cluster2.key);
		build.append("]");*/
		
		build.append(cluster1.key);
		build.append(",");
		build.append(cluster2.key);
		
		return build.toString();
	}
	
	public void printCluster()
	{
		Collections.sort(k_clusters);
		
		for (ProbCluster cluster : k_clusters)
			System.out.println(cluster.key+" "+cluster.score);
	}
	
// ======================== Hierarchical agglomerative clustering ========================	
	
	public void hmCluster(Prob2dMap map, Prob1dMap mKeyword, double argmWeight)
	{
		d_similarities = new ObjectDoubleOpenHashMap<String>();
		k_clusters     = getInitClusters(map, mKeyword, argmWeight);
		
		hmClusterRec();
		hmClusterTrim();
	}
	
	private void hmClusterRec()
	{
		boolean cont = true;
		
		for (int i=0; i<iter_hm && cont; i++)
		{
			System.out.println("== Iteration: "+i+" ==");
			cont = hmClusterAux();
			if (cont)	printCluster();
		}
	}
	
	private void hmClusterTrim()
	{
		ArrayList<ProbCluster> remove = new ArrayList<ProbCluster>();
		
		for (ProbCluster cluster : k_clusters)
		{
			if (cluster.size() == 1)
				remove.add(cluster);
		}
		
		k_clusters.removeAll(remove);
		d_similarities.clear();
		printCluster();
	}
	
	/** @return true if clustering is performed. */
	@SuppressWarnings("unchecked")
	private boolean hmClusterAux()
	{
		ArrayList<JObjectDoubleTuple<JIntIntTuple>> list = new ArrayList<JObjectDoubleTuple<JIntIntTuple>>();
		ProbCluster cluster1, cluster2;
		
		for (int i=0; i<k_clusters.size(); i++)
		{
			cluster1 = k_clusters.get(i);
			
			for (int j=i+1; j<k_clusters.size(); j++)
			{
				cluster2 = k_clusters.get(j);
				list.add(new JObjectDoubleTuple<JIntIntTuple>(new JIntIntTuple(i,j), getAvgSimilarity(cluster1,cluster2,true)));
			}
		}
		
		IntOpenHashSet      sClustered = new IntOpenHashSet();
		ArrayList<ProbCluster> sRemove = new ArrayList<ProbCluster>();
		JIntIntTuple idx;
		Collections.sort(list);
		
		for (JObjectDoubleTuple<JIntIntTuple> tup : list)
		{
			if (tup.value < d_threshold)	break;
			idx = tup.object;
			if (sClustered.contains(idx.int1) || sClustered.contains(idx.int2))	continue;
			sClustered.add(idx.int1);	sClustered.add(idx.int2);
			
			cluster1 = k_clusters.get(idx.int1);
			cluster2 = k_clusters.get(idx.int2);
			cluster1.addAll(cluster2);
			cluster1.set(getJoinedKey(cluster1, cluster2), tup.value);
			sRemove.add(cluster2);
		}
		
		k_clusters.removeAll(sRemove);
		return !sClustered.isEmpty();
	}
	
// ======================== K-mean clustering ========================
	
	public void kmCluster(Prob2dMap map, Prob1dMap mKeyword, double argmWeight)
	{
		ArrayList<ProbCluster> nClusters = getInitClusters(map, mKeyword, argmWeight);
		ArrayList<JIntIntTuple> list = new ArrayList<JIntIntTuple>();
		ProbCluster kCluster, nCluster;
		JIntDoubleTuple max;
		int i, j, k;	double sim;
		IntOpenHashSet skip = new IntOpenHashSet();
		
		for (k=0; k<iter_km; k++)
		{
			System.out.println("== Iteration: "+k+" ==");
			list.clear();
			
			for (i=0; i<nClusters.size(); i++)
			{
				if (skip.contains(i))	continue;
				nCluster = nClusters.get(i);
				max = new JIntDoubleTuple(-1, -1);
				
				for (j=0; j<k_clusters.size(); j++)
				{
					sim = getAvgSimilarity(nCluster, k_clusters.get(j), false);
					if (max.d < sim)	max.set(j, sim);
				}
				
				if (max.d >= d_threshold)	list.add(new JIntIntTuple(max.i, i));
			}
			
			for (JIntIntTuple tup : list)
			{
				kCluster = k_clusters.get(tup.int1);
				nCluster = nClusters .get(tup.int2);
				kCluster.addAll(nCluster);
				kCluster.set(getJoinedKey(kCluster, nCluster), 1);
				skip.add(tup.int2);
			}
		}
		
		printCluster();
	}
	
	public JObjectObjectTuple<IntIntOpenHashMap, IntIntOpenHashMap> getClusterMaps()
	{
		IntIntOpenHashMap lMap = new IntIntOpenHashMap();
		IntIntOpenHashMap gMap = new IntIntOpenHashMap();
		ProbCluster cluster;
		String[]    ids, key;
		
		for (int i=0; i<k_clusters.size(); i++)
		{
			cluster = k_clusters.get(i);
			ids = cluster.key.split(",");
			
			for (String id : ids)
			{
				key = id.split(":");
				
				if (key[0].equals(SRLVerbCluster.FLAG_LOCAL))
					lMap.put(Integer.parseInt(key[1]), i+1);
				else
					gMap.put(Integer.parseInt(key[1]), i+1);
			}
		}
		
		return new JObjectObjectTuple<IntIntOpenHashMap, IntIntOpenHashMap>(lMap, gMap);
	}
	
	
	
	
	
	
	
	public void cluster(Prob2dMap map, ObjectDoubleOpenHashMap<String> mWeights)
	{
		d_similarities = new ObjectDoubleOpenHashMap<String>();
		k_clusters     = getInitClusters(map, mWeights);
		
		hmClusterRec();
	}
	
	/** Called from {@link SRLClusterBuilder#MapCluster(ProbMap, int)}. */
	private ArrayList<ProbCluster> getInitClusters(Prob2dMap map, ObjectDoubleOpenHashMap<String> mWeights)
	{
		ArrayList<ProbCluster> clusters = new ArrayList<ProbCluster>();
		ObjectDoubleOpenHashMap<String> lmap;
		ProbCluster cluster;
		double weight;
		
		for (String key : map.keySet())
		{
			cluster = new ProbCluster(key);
			lmap    = map.getProb2dMap(key);
			
			for (ObjectCursor<String> cur : lmap.keySet())
			{
				if ((weight = mWeights.get(cur.value)) > 0)
					lmap.put(cur.value, lmap.get(cur.value)*weight);
			}
			
			cluster .add(lmap);
			clusters.add(cluster);
		}
		
		return clusters;
	}
}
