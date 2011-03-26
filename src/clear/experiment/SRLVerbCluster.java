package clear.experiment;

import java.io.PrintStream;
import java.util.HashSet;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.feat.FeatEnglish;
import clear.dep.srl.SRLHead;
import clear.dep.srl.SRLInfo;
import clear.morph.MorphEnAnalyzer;
import clear.reader.SRLReader;
import clear.util.IOUtil;
import clear.util.cluster.Prob1dMap;
import clear.util.cluster.Prob2dMap;
import clear.util.cluster.SRLClusterBuilder;
import clear.util.tuple.JObjectObjectTuple;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;

public class SRLVerbCluster
{
	static public final String FLAG_LOCAL  = "b";
	static public final String FLAG_GLOBAL = "w";
	
	MorphEnAnalyzer m_morph;
	Prob2dMap       m_prob;
	Prob1dMap       m_keyword;
	int             i_verbId;
	
	public SRLVerbCluster(String dicFile)
	{
		m_morph   = new MorphEnAnalyzer(dicFile);
		m_prob    = new Prob2dMap();
		m_keyword = new Prob1dMap();
		i_verbId  = 0;
	}
	
	public void lemmatize(DepTree tree)
	{
		DepNode node;
		
		for (int i=1; i<tree.size(); i++)
		{
			node = tree.get(i);
			if (node.isPredicate())
				node.lemma = m_morph.getLemma(node.form, "VB");
			else if (node.isPosx("NN.*"))
				node.lemma = m_morph.getLemma(node.form, "NN");
		}
	}
	
	private String getArgLemma(DepTree tree, DepNode node, SRLHead head)
	{
		DepNode tmp;
		
		if (head.equals("A0") && node.isLemma("by") && (tmp = tree.getRightNearestDependent(node.id)) != null)
			return tmp.lemma;
		else
			return node.lemma;
	}
	
	public void retrieveArgs(DepTree tree, String flag)
	{
		IntObjectOpenHashMap<HashSet<String>> map = getArgMap(tree, flag);
		HashSet<String> set;
		DepNode node;
		String  key;
		
		for (int i=1; i<tree.size(); i++)
		{
			if ((node = tree.get(i)).isPredicate())
			{
			//	key = flag+":"+i_verbId;
				key = flag+":"+i_verbId+":"+node.lemma;
				set = map.get(node.id);
				
				if (set != null)	m_prob.increment(key, set);
				i_verbId++;
			}
		}
	}
	
	private IntObjectOpenHashMap<HashSet<String>> getArgMap(DepTree tree, String flag)
	{
		IntObjectOpenHashMap<HashSet<String>> map = new IntObjectOpenHashMap<HashSet<String>>();
		HashSet<String> set;
		DepNode node;
		SRLInfo info;
		String  arg;
		
		for (int i=1; i<tree.size(); i++)
		{
			node = tree.get(i);
			info = node.srlInfo;
	
			for (SRLHead head : info.heads)
			{
				if (head.label.matches("AM-MOD|AM-NEG"))	continue;
				
				if (map.containsKey(head.headId))
					set = map.get(head.headId);
				else
				{
					set = new HashSet<String>();
					map.put(head.headId, set);
				}
				
				arg = getArgLemma(tree, node, head)+":"+head.label;
				if (flag.equals(FLAG_LOCAL) && node.isPosx("NN.*"))	m_keyword.increment(arg);
				set.add(arg);
			}
		}
		
		return map;
	}
	
	public void retrieveLocalCluster(SRLClusterBuilder build, double argmWeight)
	{
		build.hmCluster(m_prob, m_keyword, argmWeight);
		m_prob.clear();
	}
	
	public void retrieveGlobalCluster(SRLClusterBuilder build, double argmWeight)
	{
		build.kmCluster(m_prob, m_keyword, argmWeight);
		m_prob.clear();
	}
	
	public void initVerbId()
	{
		i_verbId = 0;
	}
	
	public void assignCluster(DepTree tree, IntIntOpenHashMap map)
	{
		DepNode node;
		int clusterId;
		
		for (int i=1; i<tree.size(); i++)
		{
			if ((node = tree.get(i)).isPredicate())
			{
				if ((clusterId = map.get(i_verbId)) > 0)
				{
					if (node.feats == null)	node.feats = new FeatEnglish();
					node.feats.feats[2] = Integer.toString(clusterId);
				}
				
				i_verbId++;
			}
		}
	}
	
	static public void main(String[] args)
	{
		String dicFile    = args[0];
		String localFile  = args[1];
		String globalFile = args[2];
		
		SRLVerbCluster cluster = new SRLVerbCluster(dicFile);
		SRLReader      reader  = new SRLReader(localFile, true);
		DepTree        tree;

		System.out.println("== Retrieve local arguments ==");
		
		while ((tree = reader.nextTree()) != null)
		{
			cluster.lemmatize(tree);
			cluster.retrieveArgs(tree, FLAG_LOCAL);
		}
		
		reader.close();
		
		System.out.println("== Retrieve local clusters ==");
		
		double clusterThreshold = 0.38, argmWeight = 0.5;	// brown
	//	double clusterThreshold = 0.42, argmWeight = 0.5;	// wsj
		SRLClusterBuilder build = new SRLClusterBuilder(clusterThreshold);
		cluster.retrieveLocalCluster(build, argmWeight);
		
		System.out.println("== Retrieve global arguments ==");
		reader.open(globalFile);
		
		while ((tree = reader.nextTree()) != null)
		{
			cluster.lemmatize(tree);
			cluster.retrieveArgs(tree, FLAG_GLOBAL);
		}
		
		reader.close();
		
		System.out.println("== Retrieve global clusters ==");
		cluster.retrieveGlobalCluster(build, argmWeight);
		
		JObjectObjectTuple<IntIntOpenHashMap, IntIntOpenHashMap> maps = build.getClusterMaps();
		IntIntOpenHashMap lMap = maps.o1;
		IntIntOpenHashMap gMap = maps.o2;

		System.out.println("== Print local clusters ==");
		PrintStream fout = IOUtil.createPrintFileStream(localFile+".ct");
		cluster.initVerbId();
		reader.open(localFile);
		
		while ((tree = reader.nextTree()) != null)
		{
			cluster.assignCluster(tree, lMap);
			fout.println(tree+"\n");
		}
		
		reader.close();
		fout.close();

		System.out.println("== Print global clusters ==");
		fout = IOUtil.createPrintFileStream(globalFile+".ct");
		cluster.initVerbId();
		reader.open(globalFile);
		
		while ((tree = reader.nextTree()) != null)
		{
			cluster.assignCluster(tree, gMap);
			fout.println(tree+"\n");
		}
		
		reader.close();
		fout.close();
	}
}
