/**
* Copyright (c) 2007, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package clear.propbank;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import clear.treebank.TBReader;
import clear.treebank.TBTree;
import clear.util.IOUtil;

/**
 * Propbank reader.
 * @author Jinho D. Choi
 * <b>Last update:</b> 02/15/2010
 */
public class PBReader
{
	/** Path to the Treebank directory */
	private String st_treeDir;
	/** Name of the current Treebank file */
	private String st_treeFile;
	/** List of trees from {@link PBReader#st_treeFile} */
	private ArrayList<TBTree> ls_tree;
	/** Scanner to read Propbank files */
	private Scanner f_prop;
	/** Number of the current line */
	private int n_lineNum;
	
	/**
	 * Initializes the Propbank reader.
	 * @param propFile name of the Propbank file
	 * @param treeDir path to the Treebank directory.
	 */
	public PBReader(String propFile, String treeDir)
	{
		st_treeDir  = treeDir;
		st_treeFile = "";
		ls_tree     = new ArrayList<TBTree>();
		f_prop      = IOUtil.createFileScanner(propFile);
		n_lineNum   = 0;
	}
		
	/**
	 * Returns the next Propbank instance.
	 * If there is none, return null.
	 * @param onToken if true, returns token IDs for arguments; otherwise, returns terminal IDs
	 * @param offset offset for token IDs (only when <code>onToken</code> is true)
	 */
	public PBInstance nextInstance(boolean onToken, int offset)
	{
		if (!f_prop.hasNextLine())
		{	f_prop.close();	return null;	}
		
		String[] str = f_prop.nextLine().split(PBLib.FIELD_DELIM);
		n_lineNum++;
		
		PBInstance instance  = new PBInstance();
		TBTree     tree;
		instance.treeFile    = str[0];
		instance.treeIndex   = Integer.parseInt(str[1]);
		instance.predicateId = Integer.parseInt(str[2]);
		
		// retrieve trees from new file
		if (!st_treeFile.equals(instance.treeFile))
		{
			TBReader tbreader = new TBReader(st_treeDir + File.separator + instance.treeFile);
			st_treeFile       = instance.treeFile;
			ls_tree.clear();
			
			while ((tree = tbreader.nextTree()) != null)	ls_tree.add(tree);
		}
		
		tree = ls_tree.get(instance.treeIndex);
		if (onToken)
		{
			if (!tree.moveToTerminal(instance.predicateId))
				errorMsg("wrong predicate: "+instance.predicateId);
			
			instance.predicateId = tree.getCurrNode().tokenId + offset;
		}

		instance.rolesetId = str[4];	// str[3] = "gold", str[5] = "-----"
		
		for (int i=6; i<str.length; i++)
		{
			String arg   = str[i];
			int    idx   = arg.indexOf(PBLib.PROP_LABEL_DELIM);
			String label = arg.substring(idx+1);
			String locs  = arg.substring(0, idx);
			
			StringTokenizer tok = new StringTokenizer(locs, PBLib.PROP_ARG_OP);
			TIntArrayList   ids = new TIntArrayList();
			
			while (tok.hasMoreTokens())
			{
				String[]      loc = tok.nextToken().split(PBLib.PROP_LOC_DELIM);
				int terminalIndex = Integer.parseInt(loc[0]);
				int height        = Integer.parseInt(loc[1]);
				
				if (!tree.moveTo(terminalIndex, height))
					errorMsg("wrong argument: "+arg);
				
		//		if (onToken)	ids.addAll(tree.getSubTokenIndices(offset));
		//		else			ids.addAll(tree.getSubTerminalIndices());
			}
			
			if (label.equalsIgnoreCase("rel"))
				ids.remove(instance.predicateId);
			
			if (ids.size() > 0)
			{
				ids.sort();
				instance.addArg(new PBArg(label, ids));
			}
		}
		
		return instance;
	}
	
	public void countTraces()
	{
		int numTraces = 0, numTotal = 0;
		
		while (f_prop.hasNextLine())
		{
			String[] str       = f_prop.nextLine().split(PBLib.FIELD_DELIM);
			String   treeFile  = str[0];
			int      treeIndex = Integer.parseInt(str[1]);
			TBTree   tree;
			
			// retrieve trees from new file
			if (!st_treeFile.equals(treeFile))
			{
				TBReader tbreader = new TBReader(st_treeDir + File.separator + treeFile);
				st_treeFile       = treeFile;
				ls_tree.clear();
				
				while ((tree = tbreader.nextTree()) != null)	ls_tree.add(tree);
			}
			
			tree = ls_tree.get(treeIndex);
			
			outer: for (int i=6; i<str.length; i++)
			{
				String arg   = str[i];
				int    idx   = arg.indexOf(PBLib.PROP_LABEL_DELIM);
				String locs  = arg.substring(0, idx);
				
				StringTokenizer tok = new StringTokenizer(locs, PBLib.PROP_ARG_OP);
				
				while (tok.hasMoreTokens())
				{
					String[]      loc = tok.nextToken().split(PBLib.PROP_LOC_DELIM);
					int terminalIndex = Integer.parseInt(loc[0]);
					int height        = Integer.parseInt(loc[1]);
					
					if (!tree.moveTo(terminalIndex, height))
						errorMsg("wrong argument: "+arg);
					
					if (tree.getCurrNode().isEmptyCategory())
					{
						numTraces++;
						break outer;
					//	break;
					}
				}
				
			//	numTotal++;
			}
		
			numTotal++;
		}
		
		System.out.println(numTraces+" / "+numTotal+" = "+((double)numTraces/numTotal));
		f_prop.close();
	}
	
	/**
	 * Prints the error message and exists the system.
	 * @param msg error message
	 */
	private void errorMsg(String msg)
	{
		System.err.println("error: "+msg+" (line: "+n_lineNum+")");
		System.exit(1);
	}
}
