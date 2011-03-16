package clear.dep.srl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import clear.dep.DepNode;
import clear.parse.SRLParser;
import clear.util.IOUtil;
import clear.util.tuple.JObjectDoubleTuple;

import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class SRLProb
{
	static public final String SYM_PREV    = "<";
	static public final String SYM_NEXT    = ">";
	static public final String SYM_ACTIVE  = "a";
	static public final String SYM_PASSIVE = "p";
	static public final String ARG_NONE    = "NONE";
	static public final String ARG_END     = "END";
	
	private final String TOTAL = "TOTAL";
	public double SMOOTH_1A = Double.MIN_VALUE;
	public double SMOOTH_2A = Double.MIN_VALUE; 
	public double SMOOTH_2N = Double.MIN_VALUE;
	
	
	
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_prob1a;
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_prob2a;
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_prob2n;
	
	public SRLProb()
	{
		m_prob1a = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
		m_prob2a = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
		m_prob2n = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
	}
	
	public ObjectDoubleOpenHashMap<String> get1aProb(DepNode pred, byte dir)
	{
		return m_prob1a.get(getKey(pred, dir));
	}
	
	public ObjectDoubleOpenHashMap<String> get2aProb(DepNode pred, String prevArg, byte dir)
	{
		return m_prob2a.get(getKey(pred, prevArg, dir));
	}
	
	public ObjectDoubleOpenHashMap<String> get2nProb(DepNode pred, String prevArg, byte dir)
	{
		return m_prob2n.get(getKey(pred, prevArg, dir));
	}
	
//	============================= Retrieve Key =============================
	
	public String getKey(DepNode pred, byte dir)
	{
		String postfix, feat;
		
		if (dir == SRLParser.DIR_LEFT)	postfix = SYM_PREV;
		else							postfix = SYM_NEXT;
		
		if ((feat = pred.getFeat(0)) != null && feat.equals("1"))
			postfix += SYM_PASSIVE;
		else
			postfix += SYM_ACTIVE;
		
		return pred.lemma + postfix;
	}
	
	public String getKey(DepNode pred, String prevArg, byte dir)
	{
		return getKey(pred, dir) + "|" + prevArg;
	}
	
	public boolean isPrevArg(String label)
	{
		return (label.startsWith(SYM_PREV));
	}
	
//	============================= Count 1st-degree =============================
	
	/** For training. */
	public void add1dArgs(DepNode pred, HashSet<String> sArgs)
	{
		HashSet<String> sPrev = new HashSet<String>();
		HashSet<String> sNext = new HashSet<String>();
		
		for (String label : sArgs)
		{
			if (isPrevArg(label))	sPrev.add(label);
			else					sNext.add(label);
		}
		
		ObjectDoubleOpenHashMap<String> mArg = incrementPred(m_prob1a, getKey(pred, SRLParser.DIR_LEFT));
		
		if  (sPrev.isEmpty())		mArg.put(ARG_END, mArg.get(ARG_END)+1);
		for (String label : sPrev)	mArg.put(label  , mArg.get(label  )+1);
		
		mArg = incrementPred(m_prob1a, getKey(pred, SRLParser.DIR_RIGHT));
		
		if  (sNext.isEmpty())		mArg.put(ARG_END, mArg.get(ARG_END)+1);
		for (String label : sNext)	mArg.put(label  , mArg.get(label  )+1);
	}
	
	/** Called from {@link SRLProb#add1dArgs(DepNode, HashSet)}. */
	private ObjectDoubleOpenHashMap<String> incrementPred(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, String key)
	{
		ObjectDoubleOpenHashMap<String> mArg;
		
		if (mPred.containsKey(key))
		{
			mArg = mPred.get(key);
			mArg.put(TOTAL, mArg.get(TOTAL)+1);
			
		}
		else
		{
			mArg = new ObjectDoubleOpenHashMap<String>();
			mArg .put(TOTAL, 1);
			mPred.put(key, mArg);	
		}
		
		return mArg;
	}

//	============================= Count 2nd-degree =============================
	
	/** For training. */
	public void add2dArgs(DepNode pred, ArrayList<SRLArg> lsArgs)
	{
		ArrayList<String> sPrev = new ArrayList<String>();
		ArrayList<String> sNext = new ArrayList<String>();
		
		for (SRLArg arg : lsArgs)
		{
			if (isPrevArg(arg.label))	sPrev.add(arg.label);
			else						sNext.add(arg.label);
		}
		
		String prevArgA = ARG_NONE;
		String prevArgN = ARG_NONE;
		
		for (String label : sPrev)
		{
			add2dArgsAux(m_prob2a, pred, prevArgA, label, SRLParser.DIR_LEFT);
			add2dArgsAux(m_prob2n, pred, prevArgN, label, SRLParser.DIR_LEFT);
			
			prevArgA = label;
			if (label.substring(1).matches("A\\d"))	prevArgN = label;
		}
		
		add2dArgsAux(m_prob2a, pred, prevArgA, ARG_END, SRLParser.DIR_LEFT);
		add2dArgsAux(m_prob2n, pred, prevArgN, ARG_END, SRLParser.DIR_LEFT);
		
		for (String label : sNext)
		{
			add2dArgsAux(m_prob2a, pred, prevArgA, label, SRLParser.DIR_RIGHT);
			add2dArgsAux(m_prob2n, pred, prevArgN, label, SRLParser.DIR_RIGHT);
			
			prevArgA = label;
			if (label.substring(1).matches("A\\d"))	prevArgN = label;
		}
		
		add2dArgsAux(m_prob2a, pred, prevArgA, ARG_END, SRLParser.DIR_RIGHT);
		add2dArgsAux(m_prob2n, pred, prevArgN, ARG_END, SRLParser.DIR_RIGHT);
	}
	
	/** Called from {@link SRLProb#add1dArgs(DepNode, HashSet)}. */
	private void add2dArgsAux(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, DepNode pred, String prevArg, String currArg, byte dir)
	{
		ObjectDoubleOpenHashMap<String> mArg = incrementPred(mPred, getKey(pred, prevArg, dir));
		
		mArg.put(currArg, mArg.get(currArg)+1);
	}
	
//	============================= Compute Probabilities =============================
	
	/** Must be called before any probability is used. */
	public void computeProb()
	{
		SMOOTH_1A = computeConditionalProb(m_prob1a);
		SMOOTH_2A = computeConditionalProb(m_prob2a);
		SMOOTH_2N = computeConditionalProb(m_prob2n);
	}
	
	/** Called from {@link SRLProb#computeConditionalProb(HashMap)}. */
	private double computeConditionalProb(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred)
	{
		ObjectDoubleOpenHashMap<String> mArg;
		double total, score, min = 1;	String label;
		
		for (String key : mPred.keySet())
		{
			mArg  = mPred.get(key);
			total = mArg.get(TOTAL);
			
			for (ObjectCursor<String> arg : mArg.keySet())
			{
				label = arg.value;
				score = mArg.get(label)/total;
				min   = Math.min(min, score);
				mArg.put(label, score);
			}
			
			mArg.remove(TOTAL);
		}
		
		return min;
	}
	
	public double getScore(DepNode pred, ArrayList<SRLArg> lsArgs)
	{
		double score = 1;
		
		for (SRLArg arg : lsArgs)
		{
			score *= arg.score;
		}

		return score;
	}
	
//	============================= Print =============================
	
	public void printAll(String filename)
	{
		printCP(m_prob1a, filename+".p1d");
		printCP(m_prob2a, filename+".p2d");
	}
	
	@SuppressWarnings("unchecked")
	private void printCP(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, String outputFile)
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
