package clear.experiment;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.dep.srl.SRLHead;
import clear.reader.SRLReader;
import clear.util.IOUtil;
import clear.util.tuple.JObjectDoubleTuple;

import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class ExtractPBArg
{
	final String TOTAL = "TOTAL";
	
	HashMap<String, ObjectDoubleOpenHashMap<String>> m_prevArg1;
	HashMap<String, ObjectDoubleOpenHashMap<String>> m_nextArg1;
	
	public ExtractPBArg(String filename)
	{
		SRLReader reader = new SRLReader(filename, true);
		DepTree   tree;

		initProb();
		
		while ((tree = reader.nextTree()) != null)
		{
			countPred(tree);
			countArgs(tree);
		}
		
		measureConditionalProb();
		print();
	}
	
	void initProb()
	{
		m_prevArg1 = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
		m_nextArg1 = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
	}
	
	void measureConditionalProb()
	{
		measureConditionalProb(m_prevArg1);
		measureConditionalProb(m_nextArg1);
	}
	
	void print()
	{
		printCP(m_prevArg1, "prev_arg1.txt");
		printCP(m_nextArg1, "next_arg1.txt");
	}
	
	void countPred(DepTree tree)
	{
		int predId = 0;
		String key;
		
		while ((predId = tree.nextPredicateId(predId)) < tree.size())
		{
			key = getKey(tree.get(predId));
			
			incrementPred(m_prevArg1, key);
			incrementPred(m_nextArg1, key);
		}
	}
	
	void incrementPred(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, String key)
	{
		ObjectDoubleOpenHashMap<String> mArg;
		
		if (!mPred.containsKey(key))
		{
			mArg = new ObjectDoubleOpenHashMap<String>();
			mArg .put(TOTAL, 1);
			mPred.put(key, mArg);
		}
		else
		{
			mArg = mPred.get(key);
			mArg.put(TOTAL, mArg.get(TOTAL)+1);
		}
	}
	
	String getKey(DepNode pred)
	{
		String key = pred.lemma, feat;
		
		if ((feat = pred.getFeat(0)) != null && feat.equals("1"))
			return "-"+key;		// passive
		else
			return key;			// active
	}

	void countArgs(DepTree tree)
	{
		String  key;
		DepNode arg, pred;
		HashMap<String, ObjectDoubleOpenHashMap<String>> mPred;
		ObjectDoubleOpenHashMap<String>                  mArg;
		
		for (int i=1; i<tree.size(); i++)
		{
			arg = tree.get(i);
			if (arg.srlInfo.heads.isEmpty())	continue;
			
			for (SRLHead head : arg.srlInfo.heads)
			{
				pred  = tree.get(head.headId);
				key   = getKey(pred);
				mPred = (arg.id < pred.id) ? m_prevArg1 : m_nextArg1;
				mArg  = mPred.get(key);
				mArg.put(head.label, mArg.get(head.label)+1);
			}
		}
	}
	
	void measureConditionalProb(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred)
	{
		ObjectDoubleOpenHashMap<String> mArg;
		double total;	String label;
		
		for (String key : mPred.keySet())
		{
			mArg  = mPred.get(key);
			total = mArg.get(TOTAL);
			
			for (ObjectCursor<String> arg : mArg.keySet())
			{
				label = arg.value;
				mArg.put(label, mArg.get(label)/total);
			}
			
			mArg.remove(TOTAL);
		}
	}
	
	@SuppressWarnings("unchecked")
	void printCP(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, String outputFile)
	{
		ArrayList<String> keys = new ArrayList<String>(mPred.keySet());
		ArrayList<JObjectDoubleTuple<String>> tArgs;
		ObjectDoubleOpenHashMap<String> mArg;
		String label;
		
		Collections.sort(keys);
		PrintStream fout = IOUtil.createPrintFileStream(outputFile);
		
		for (String key : keys)
		{
			tArgs = new ArrayList<JObjectDoubleTuple<String>>();
			mArg  = mPred.get(key);
			
			for (ObjectCursor<String> arg : mArg.keySet())
			{
				label = arg.value;
				tArgs.add(new JObjectDoubleTuple<String>(label, mArg.get(label)));
			}
			
			Collections.sort(tArgs);
			StringBuilder build = new StringBuilder();
			
			build.append(key);
			
			for (JObjectDoubleTuple<String> tup : tArgs)
			{
				build.append(" ");
				build.append(tup.object);
				build.append(":");
				build.append(tup.value);
			}
			
			fout.println(build.toString());
		}
		
		fout.close();
	}
	
	static public void main(String[] args)
	{
		new ExtractPBArg(args[0]);
	}
}
