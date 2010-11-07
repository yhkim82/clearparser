package clear.experiment;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import clear.propbank.PBArg;
import clear.propbank.PBInstance;
import clear.propbank.PBLoc;
import clear.propbank.PBReader;
import clear.treebank.TBEnLib;
import clear.treebank.TBNode;
import clear.treebank.TBReader;
import clear.treebank.TBTree;
import clear.util.IOUtil;

public class MergeTreePropBank
{
	HashMap<String,PBInstance> m_prop;
	String tree_path = null;
	
	public MergeTreePropBank(String treeDir, String propFile)
	{
		String filename = treeDir+"/"+propFile.substring(propFile.lastIndexOf('/')+1, propFile.lastIndexOf('.'));
		
		getPBInstances(propFile);
		merge(filename);
	}
	
	private void getPBInstances(String propFile)
	{
		PBReader   reader = new PBReader(propFile, "");
		PBInstance instance;
		m_prop = new HashMap<String,PBInstance>();
		
		while ((instance = reader.nextInstance()) != null)
		{
			String key = instance.treeIndex+"_"+instance.predicateId;
			m_prop.put(key, instance);
			if (tree_path == null)	tree_path = instance.treeFile;
		}
	}
	
	private void merge(String filename)
	{
		PrintStream outFile = IOUtil.createPrintFileStream(filename+".merge");
		PrintStream errFile = IOUtil.createPrintFileStream(filename+".miss");
		TBReader    reader  = new TBReader(filename+".parse"); 
		TBTree      tree;
		
		for (int treeIndex=0; (tree = reader.nextTree()) != null; treeIndex++)
		{
			ArrayList<Integer> predicateIDs = tree.getAllVerbIDs(TBEnLib.POS_VB+".*");
			if (predicateIDs.size() == 0)	continue;	// no predicate in this tree
			boolean isComplete = true;					// if all predicate-argument annotations exist for the tree
			
			for (int predicateID : predicateIDs)
			{
				String key = treeIndex+"_"+predicateID;
				
				if (m_prop.containsKey(key))
				{
					PBInstance instance = m_prop.get(key);
					
					for (PBArg pbarg : instance.getArgs())
					{
						ArrayList<TBNode> nodes = new ArrayList<TBNode>();
						HashSet<Integer>  coidx = new HashSet<Integer>();
						
						for (PBLoc loc : pbarg.getLocs())
						{
							tree.moveTo(loc.terminalId, loc.height);
							TBNode curr = tree.getCurrNode();
							nodes.add(curr);
							
							int idx;
							if ((idx = curr.getEmptyCategoryCoIndex()) != -1)
								coidx.add(idx);
						}
						
						String label = "";
						if (pbarg.label.length() < 5)	label = "A"+pbarg.label.substring(3);
						else							label = pbarg.label.substring(5);
						
						for (TBNode node : nodes)
						{
							if (node.terminalId == instance.predicateId)	continue;
							node.addPBArg(new PBArg(label, pbarg.predicateId));
							coidx.remove(node.coIndex);
						}
						
						for (int idx : coidx)
						{
							TBNode node = tree.getCoIndexedNode(idx);
							if (node != null)	node.addPBArg(new PBArg(label, pbarg.predicateId));
						}
					}
				}
				else
				{
					tree.moveTo(predicateID, 0);
					TBNode node   = tree.getCurrNode();
					TBNode parent = node.getParent();
					String form   = node.form.toLowerCase();
					
					if (parent.isPos(TBEnLib.POS_VP) && !isAux(form))// && !isLightVerb(form))
					{
						String lemma = node.form.toLowerCase()+"_"+node.pos;
						errFile.println(tree_path+" "+treeIndex+" "+predicateID+" "+lemma+" "+predicateID+":0-rel");
						isComplete = false;
					}
				}
			}
			
			if (isComplete)	outFile.println(tree.toTree()+"\n");
		}
	}
	
	private boolean isAux(String form)
	{
		return form.equals("be") || form.equals("been") || form.equals("being") ||
		form.equals("am") || form.equals("is") || form.equals("was") ||
		form.equals("are") || form.equals("were") ||
		form.equals("have") || form.equals("has") || form.equals("had") || form.equals("having") ||
		form.equals("'m") || form.equals("'s") || form.equals("'re") ||
		form.equals("'ve") || form.equals("'d");
	}
	
	private boolean isLightVerb(String form)
	{
		return form.equals("take") || form.equals("takes") || form.equals("took") || form.equals("taken") || form.equals("taking") ||
		form.equals("give") || form.equals("gives") || form.equals("gave") || form.equals("given") || form.equals("giving") ||
		form.equals("make") || form.equals("makes") || form.equals("made") || form.equals("making") ||
		form.equals("do") || form.equals("does") || form.equals("did") || form.equals("done") || form.equals("doing") ||
		form.equals("have") || form.equals("has") || form.equals("had") || form.equals("having");
	}
	
	static public void main(String[] args)
	{
		new MergeTreePropBank(args[0], args[1]);
	}
}
