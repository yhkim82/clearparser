package clear.experiment;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import clear.morph.MorphEnAnalyzer;
import clear.propbank.PBArg;
import clear.propbank.PBInstance;
import clear.propbank.PBLib;
import clear.propbank.PBLoc;
import clear.propbank.PBReader;
import clear.srl.SRLHead;
import clear.treebank.TBHeadRules;
import clear.treebank.TBNode;
import clear.treebank.TBPBEnConvert;
import clear.treebank.TBReader;
import clear.treebank.TBTree;
import clear.util.IOUtil;

import com.carrotsearch.hppc.IntOpenHashSet;

public class MergeTreePropBank
{
	HashMap<String,PBInstance> m_pbInstances;
	
	public MergeTreePropBank(String propFile, String treeDir, String mergeDir, String headruleFile, String dictDir)
	{
		readPBInstances(propFile);
		merge(treeDir, mergeDir, headruleFile, dictDir);
	}
	
	/** Reads all PropBank instances and stores them to {@link MergeTreePropBank#m_pbInstances}. */
	public void readPBInstances(String propFile)
	{
		PBReader   reader = new PBReader(propFile);
		PBInstance instance;
		
		m_pbInstances = new HashMap<String,PBInstance>();
		System.out.println("Initialize: "+propFile);
		
		while ((instance = reader.nextInstance()) != null)
			m_pbInstances.put(instance.getKey(), instance);
	}
	
	/** @return list of tree paths as in PropBank instance. */
	private ArrayList<String> getTreePaths()
	{
		HashSet<String> set = new HashSet<String>();
		
		for (String key : m_pbInstances.keySet())
			set.add(key.substring(0, key.indexOf(PBInstance.KEY_DELIM)));

		ArrayList<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		
		return list;
	}
	
	/** @return list of PropBank instances for the tree. */
	private ArrayList<PBInstance> getPBInstances(String treePath, int treeIndex)
	{
		ArrayList<PBInstance> list = new ArrayList<PBInstance>();
		String prefix = treePath + PBInstance.KEY_DELIM + treeIndex + PBInstance.KEY_DELIM;
		
		for (String key : m_pbInstances.keySet())
		{
			if (key.startsWith(prefix))
				list.add(m_pbInstances.get(key));
		}
		
		return list;
	}
	
	public void merge(String treeDir, String mergeDir, String headruleFile, String dictDir)
	{
		TBReader    reader;
		TBTree      tree;
		int         treeIndex;
		String      mergeFile;
	 	PrintStream fout;
		
		TBPBEnConvert   convert   = new TBPBEnConvert();
		TBHeadRules     headrules = new TBHeadRules(headruleFile);
		MorphEnAnalyzer morph     = (dictDir != null) ? new MorphEnAnalyzer(dictDir) : null;
		
	 	treeDir  = treeDir  + File.separator;
		mergeDir = mergeDir + File.separator;
		
		ArrayList<PBInstance> list;
		
		for (String treePath : getTreePaths())
		{
			mergeFile = mergeDir + treePath.substring(treePath.lastIndexOf(File.separator)+1) + ".merge";
			reader    = new TBReader(treeDir + treePath);
			fout      = IOUtil.createPrintFileStream(mergeFile);
			
			System.out.println(mergeFile);
			
			for (treeIndex=0; (tree = reader.nextTree()) != null; treeIndex++)
			{
				list = getPBInstances(treePath, treeIndex);
				if (list.isEmpty())	continue;
				
				tree.setPBLocs();
				tree.setAntecedents();
				
				for (PBInstance instance : list)
					mergeAux(instance, tree);
				
				if (isCyclic(list, tree))
					System.err.println("Cyclic relation: "+treePath+" "+treeIndex);
				else
					fout.println(convert.toSrlTree(tree, headrules, morph)+"\n");
			}
			
			fout.close();
		}
	}
	
	private void mergeAux(PBInstance instance, TBTree tree)
	{
		if (instance.rolesetId.endsWith(".LV"))	return;
	//	if (tree.isUnder(instance.predicateId, TBEnLib.POS_PRN))	return;
		
		ArrayList<PBArg> pbArgs  = instance.getArgs();
		ArrayList<PBArg> delArgs = new ArrayList<PBArg>();
		
		for (PBArg pbArg : pbArgs)
		{
			if (pbArg.isLabel("rel.*"))
			{
				if (processRels(pbArg, instance.predicateId))
					delArgs.add(pbArg);
				
				continue;
			}
			
			if (pbArg.isLabel("LINK.*"))
			{
				if (processLink(pbArgs, pbArg, tree))
					delArgs.add(pbArg);					
				else
					System.err.println("No-achor in "+pbArg.label+": "+instance.toString());
				
				continue;
			}
		}
		
		pbArgs.removeAll(delArgs);
		if (pbArgs.isEmpty())	return;
		
		for (PBArg pbArg : pbArgs)
		{
			if (!processEmtpyCategories(pbArg, tree))
				System.err.println("Wrong location in "+pbArg.label+": "+instance.toString());
		}
		
		if (!instance.rolesetId.endsWith(".DP"))
			tree.getNode(instance.predicateId, 0).rolesetId = instance.rolesetId;
		
		for (PBArg pbArg : pbArgs)
		{
			if (!addPBArgToTBTree(pbArg, tree))
				System.err.println("Wrong location in "+pbArg.label+": "+instance.toString());
		}
	}
	
	/** Removes <code>predId:0</code> from <code>pbArg</code>. */
	private boolean processRels(PBArg pbArg, int predId)
	{
		for (PBLoc loc : pbArg.getLocs())
		{
			if (loc.equals(predId, 0))
			{
				pbArg.removeLoc(loc);
				break;
			}
		}
		
		return pbArg.getLocs().isEmpty();
	}
	
	/** Merges LINK-argument with its anchor-argument. */
	private boolean processLink(ArrayList<PBArg> pbArgs, PBArg currArg, TBTree tree)
	{
		PBLoc  anchor = new PBLoc(null, -1, -1);
		TBNode node, comp;
		
		if (currArg.isLabel("LINK-SLC"))
		{
			// find antecedent
			for (PBLoc pbLoc : currArg.getLocs())
			{
				node = tree.getNode(pbLoc.terminalId, pbLoc.height);
				if (node == null)	return false;
				
				if (node.isPos("WH.*"))
				{
					anchor = pbLoc;
					break;
				}
			}
			
			// find antecedent in height 1
			if (anchor.terminalId == -1)
			{
				for (PBLoc pbLoc : currArg.getLocs())
				{
					node = tree.getNode(pbLoc.terminalId, 1);
					
					if (node != null && node.isPos("WH.*"))
					{
						pbLoc.height = 1;
						anchor = pbLoc;
						break;
					}
				}
			}
		}
		else if (anchor.terminalId == -1)	// normalize empty categories
		{
			for (PBLoc pbLoc : currArg.getLocs())
			{
				node = tree.getNode(pbLoc.terminalId, 0);
				if (node == null)	return false;
				
				if (node.isEmptyCategory())
					pbLoc.height = 0;
			}
		}
		
		for (PBArg pbArg : pbArgs)
		{
			if (!pbArg.isLabel("LINK.*") && pbArg.overlapsLocs(currArg))
			{
				pbArg.putLocs(currArg.getLocs());
				
				// find antecedents of relativizer
				if (anchor.terminalId != -1)
				{
					node = tree.getNode(anchor.terminalId, anchor.height);
					comp = tree.getComplementizer(node);
					
					if (comp.antecedent == null)
					{
						Collections.sort(pbArg.getLocs());
						PBLoc anteLoc = pbArg.getLocs().get(0);
						
						comp.pbLoc.type = PBLib.PROP_OP_COMP;
						comp.antecedent = tree.getNode(anteLoc.terminalId, anteLoc.height);
						
						System.out.println(comp.pbLoc+" "+comp.antecedent.pbLoc+" "+tree.toTree());
					}
					else
						pbArg.putLoc(comp.antecedent.pbLoc);
				}
		
				return true;
			}
		}
		
		return false;
	}
	
	/** Finds empty categories' antecedents. */
	private boolean processEmtpyCategories(PBArg pbArg, TBTree tree)
	{
		ArrayList<PBLoc> addLocs = new ArrayList<PBLoc>();
		ArrayList<PBLoc> delLocs = new ArrayList<PBLoc>();
		TBNode curr;
		
		for (PBLoc pbLoc : pbArg.getLocs())
		{
			curr = tree.getNode(pbLoc.terminalId, pbLoc.height);
			if (curr == null)	return false;
			
			if (tree.isAntecedentOf(curr, "\\*ICH\\*.*"))
				pbLoc.type = ";";
			else if (tree.isAntecedentOf(curr, "\\*RNR\\*.*"))
				pbLoc.type = "&";
			else if (curr.isEmptyCategoryRec())
			{
				if (curr.isPhrase())
					curr = tree.getNode(pbLoc.terminalId, 0);
				
				if (curr.isForm("\\*T\\*.*"))
				{
					delLocs.add(pbLoc);
					addLocs.add(curr.antecedent.pbLoc);
				}
				else if (curr.isForm("\\*PRO\\*-\\d|\\*-\\d"))
				{
					if (curr.getParent().isFollowedBy("VP"))
						addLocs.add(curr.antecedent.pbLoc);
					else
						delLocs.add(pbLoc);
				}
			}
		}
		
		for (PBLoc pbLoc: addLocs)
			pbArg.putLoc(pbLoc);
		
		for (PBLoc pbLoc: delLocs)
			pbArg.removeLocs(pbLoc);
		
		trimEmptyCategories(pbArg, tree);
		
		return true;
	}
	
	private void trimEmptyCategories(PBArg pbArg, TBTree tree)
	{
		ArrayList<PBLoc> pbLocs  = pbArg.getLocs();
		ArrayList<PBLoc> delLocs = new ArrayList<PBLoc>();
		PBLoc pbLoc;
		TBNode curr, ante;
		Collections.sort(pbLocs);
		
		int i, size = pbLocs.size();
		boolean isFound = false;
		
		for (i=size-1; i>=0; i--)
		{
			pbLoc = pbLocs.get(i);
			curr  = tree.getNode(pbLoc.terminalId, pbLoc.height);
			
			if (curr.isEmptyCategoryRec())
			{
				if (curr.isPhrase())
					curr = tree.getNode(pbLoc.terminalId, 0);
				
				if (curr.isForm("\\*PRO\\*.*|\\*.*"))
				{
					if (isFound)
						delLocs.add(pbLoc);
					else
					{
						isFound = true;
						
						if (curr.antecedent == null)
						{
							pbLoc = pbLocs.get(0);
							ante  = tree.getNode(pbLoc.terminalId, pbLoc.height);
							if (!ante.isEmptyCategoryRec())	curr.antecedent = ante;
						}
					}
				}
			}
		}
		
		pbLocs.removeAll(delLocs);
	}
	
	private boolean isCyclic(ArrayList<PBInstance> pbInstances, TBTree tree)
	{
		ArrayList<IntOpenHashSet> list = new ArrayList<IntOpenHashSet>();
		TBNode node;
		
		for (PBInstance instance : pbInstances)
		{
			IntOpenHashSet set = new IntOpenHashSet();
			
			if (!instance.rolesetId.endsWith(".LV"))
			{
				for (PBArg pbArg : instance.getArgs())
				{
					for (PBLoc pbLoc : pbArg.getLocs())
					{
						node = tree.getNode(pbLoc.terminalId, pbLoc.height);
						set.addAll(node.getSubTermainlSet());
					}
				}				
			}
			
			list.add(set);
		}
		
		int i, j, size = pbInstances.size();
		
		for (i=0; i<size; i++)
		{
			PBInstance iInstance = pbInstances.get(i);
			IntOpenHashSet  iSet = list.get(i);
			
			for (j=i+1; j<size; j++)
			{
				PBInstance jInstance = pbInstances.get(j);
				IntOpenHashSet  jSet = list.get(j);
				
				if (iSet.contains(jInstance.predicateId) && jSet.contains(iInstance.predicateId))
					return true;
			}
		}
		
		return false;
	}
	
	private boolean addPBArgToTBTree(PBArg pbArg, TBTree tree)
	{
		TBNode node;
		
		for (PBLoc pbLoc : pbArg.getLocs())
		{
			node = tree.getNode(pbLoc.terminalId, pbLoc.height);
			if (node == null)	return false;
			
			node.addPBArg(new SRLHead(pbArg.predicateId, pbArg.label));
		}
		
		return true;
	}
	
	static public void main(String[] args)
	{
		String propFile = args[0];
		String treeDir  = args[1];
		String mergeDir = args[2];
		String headruleFile = args[3];
		String dictDir  = args[4];
		
		new MergeTreePropBank(propFile, treeDir, mergeDir, headruleFile, dictDir);
	}
}
