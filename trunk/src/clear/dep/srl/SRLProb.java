package clear.dep.srl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import clear.dep.DepNode;
import clear.dep.DepTree;
import clear.parse.AbstractSRLParser;
import clear.util.IOUtil;
import clear.util.tuple.JObjectDoubleTuple;

import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class SRLProb
{
	static public final String TOTAL = "TOTAL";
	static public final String SHIFT = "SHIFT";
	
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_prevProb1d;
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_nextProb1d;
	
	public double d_smooth = Double.MIN_VALUE;
	public double d_shift  = 0.15;
	
	public SRLProb()
	{
		m_prevProb1d = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
		m_nextProb1d = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
	}
	
	public String getKey(DepNode pred)
	{
		String key = pred.lemma, feat;
		
		if ((feat = pred.getFeat(0)) != null && feat.equals("1"))
			return "-"+key;		// passive
		else
			return key;			// active
	}
	
//	============================= Count 1st-degree =============================
	
	public void countPred(DepTree tree)
	{
		int predId = 0;
		String key;
		
		while ((predId = tree.nextPredicateId(predId)) < tree.size())
		{
			key = getKey(tree.get(predId));
			
			incrementPred(m_prevProb1d, key);
			incrementPred(m_nextProb1d, key);
		}
	}
	
	public void countArgs(DepTree tree)
	{
		DepNode arg, pred;	String key;
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
				mPred = (arg.id < pred.id) ? m_prevProb1d : m_nextProb1d;
				mArg  = mPred.get(key);
				mArg.put(head.label, mArg.get(head.label)+1);
			}
		}
	}
	
	/** Called from {@link SRLProb#countPred(DepTree)}. */
	private void incrementPred(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, String key)
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
	
//	============================= Compute Probabilities =============================
	
	/** Must be called before any probability is used. */
	public void computeProb()
	{
		computeConditionalProb(m_prevProb1d);
		computeConditionalProb(m_nextProb1d);
	}
	
	/** Called from {@link SRLProb#computeConditionalProb(HashMap)}. */
	void computeConditionalProb(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred)
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
	
	public ObjectDoubleOpenHashMap<String> getProb1d(DepNode pred, byte dir)
	{
		if (dir == AbstractSRLParser.DIR_LEFT)
			return getPrevProb1d(pred);
		else
			return getNextProb1d(pred);
	}
	
	private ObjectDoubleOpenHashMap<String> getPrevProb1d(DepNode pred)
	{
		return m_prevProb1d.get(getKey(pred));
	}
	
	private ObjectDoubleOpenHashMap<String> getNextProb1d(DepNode pred)
	{
		return m_nextProb1d.get(getKey(pred));
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

}
