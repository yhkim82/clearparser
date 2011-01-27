package clear.parse;

import java.util.ArrayList;

import clear.treebank.TBEnLib;
import clear.treebank.TBNode;

public class VoiceDetector
{
	static public int getPassive(TBNode predicate)
	{
		if (!predicate.pos.matches("VBN"))
			return 0;
       
		// Ordinary passive:
		// 1. Parent is VP, closest verb sibling of any VP ancestor is passive auxiliary (be verb)
		{
			TBNode currNode = predicate;
			
			while (currNode.getParent() != null && currNode.getParent().isPos("VP"))
			{
				if (findAux(currNode))	return 1;
				currNode = currNode.getParent();
			}
		}
		
		// 2. ancestor path is (ADVP->)*VP, closest verb sibling of the VP is passive auxiliary (be verb)
		{
			TBNode currNode = predicate;
			
			while (currNode.getParent() != null && currNode.getParent().isPos("ADJP"))
				currNode = currNode.getParent();
			
			if (currNode != predicate && currNode.isPos("VP"))
			{
				if (findAux(currNode))	return 2;
			}
		}
		
		//Reduced Passive:
		//1. Parent and nested ancestors are VP,
		//   none of VP ancestor's preceding siblings is verb
		//   parent of oldest VP ancestor is NP
		{
			TBNode currNode = predicate;
			boolean   found = true;
			
			while (currNode.getParent() != null && currNode.getParent().isPos("VP"))
			{
				ArrayList<TBNode> siblings = currNode.getParent().getChildren();
				
				for (int i=currNode.childId-1; i>=0; --i)
				{
					if (!siblings.get(i).isToken()) continue;
					
					if (siblings.get(i).pos.matches("VB.*|AUX|MD"))
					{
						found = false;
						break;
					}
				}
				
				if (!found) break;
				currNode = currNode.getParent();
			}
			
			if (found && currNode != predicate && currNode.getParent() != null && currNode.getParent().isPos("NP"))
				return 3;
		}
		
		//2. Parent is PP
		{
			TBNode parent = predicate.getParent();
			
			if (parent != null && parent.isPos("PP"))
				return 4;
		}
		
		//3. Parent is VP, grandparent is clause, and great grandparent is clause, NP, VP or PP
		{
			TBNode parent = predicate.getParent();
			TBNode grandParent = (parent != null) ? parent.getParent() : null;
			TBNode greatParent = (grandParent != null) ? grandParent.getParent() : null;
			
			if (parent      != null && parent.isPos("VP") &&
				grandParent != null && grandParent.isPos("S.*") &&
				greatParent != null && greatParent.isPos("S.*|NP|VP|PP"))
				return 5;
		}
		
		//4. ancestors are ADVP, no preceding siblings of oldest ancestor is DET, no following siblings is a noun or NP
		{
			TBNode currNode = predicate;
			
			while (currNode.getParent() != null && currNode.getParent().isPos("ADJP"))
				currNode = currNode.getParent();
			
			if (currNode != predicate)
			{
				boolean found = true;
				ArrayList<TBNode> siblings = currNode.getParent().getChildren();
				
				for (int i=currNode.childId-1; i>=0; i--)
				{
					if (siblings.get(i).isPos("DT"))
					{
						found = false;
						break;
					}
				}
				
				for (int i=currNode.childId+1; i<siblings.size(); ++i)
				{
					if (siblings.get(i).isPos("N.*"))
					{
						found = false;
						break;
					}
				}
				
				if (found) return 6;
			}
		}
		
		return 0;
    }
	
	static private boolean findAux(TBNode currNode)
	{
		ArrayList<TBNode> siblings = currNode.getParent().getChildren();
		TBNode node;
		
		for (int i=currNode.childId-1; i>=0; --i)
		{
			node = siblings.get(i);
			if (!node.isToken()) continue;
			
			// find auxiliary verb if verb, if not, stop
			if (node.isPos("VB.*|AUX"))
			{
				String form = node.form.toLowerCase();
				return TBEnLib.isBe(form) || TBEnLib.isGet(form);
			}
		}
		
		return false;
	}
}
