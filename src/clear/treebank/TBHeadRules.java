package clear.treebank;

import java.util.HashMap;
import java.util.Scanner;

import clear.util.IOUtil;

public class TBHeadRules
{
	static public final String FIELD_DELIM = "\t";
	static public final String HEAD_DELIM  = ";";
	
	private HashMap<String, TBHeadRule> m_headrules;
	
	public TBHeadRules(String inputFile)
	{
		Scanner scan = IOUtil.createFileScanner(inputFile);
		m_headrules  = new HashMap<String, TBHeadRule>();
		
		while (scan.hasNextLine())
		{
			String[] ls = scan.nextLine().split(FIELD_DELIM);
			m_headrules.put(ls[0], new TBHeadRule(ls[1], ls[2].split(HEAD_DELIM)));
		}
	}
	
	public TBHeadRule getHeadRule(String phrasePos)
	{
		return m_headrules.get(phrasePos);
	}
}
