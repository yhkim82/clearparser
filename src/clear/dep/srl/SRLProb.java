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
	static public String SYM_PREV    = "<";
	static public String SYM_NEXT    = ">";
	static public String SYM_ACTIVE  = "a";
	static public String SYM_PASSIVE = "p";
	
	private final String TOTAL = "TOTAL";
	private final String NONE  = "NONE";
	private final String END   = "END";
	
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_prob1d;
	private HashMap<String, ObjectDoubleOpenHashMap<String>> m_prob2d;
	
	public double d_smooth = Double.MIN_VALUE;
	public double d_shift  = 0.15;
	
	public SRLProb()
	{
		m_prob1d = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
		m_prob2d = new HashMap<String, ObjectDoubleOpenHashMap<String>>();
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
		ObjectDoubleOpenHashMap<String> lArg = increment1dPred(m_prob1d, getKey(pred, SRLParser.DIR_LEFT));
		ObjectDoubleOpenHashMap<String> rArg = increment1dPred(m_prob1d, getKey(pred, SRLParser.DIR_RIGHT));

		for (String label : sArgs)
		{
			if (isPrevArg(label))	lArg.put(label, lArg.get(label)+1);
			else					rArg.put(label, rArg.get(label)+1);
		}
	}
	
	/** Called from {@link SRLProb#add1dArgs(DepNode, HashSet)}. */
	private ObjectDoubleOpenHashMap<String> increment1dPred(HashMap<String, ObjectDoubleOpenHashMap<String>> mPred, String key)
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
		ArrayList<String> prevArgs = new ArrayList<String>();
		ArrayList<String> nextArgs = new ArrayList<String>();
		
		for (SRLArg arg : lsArgs)
		{
			if (isPrevArg(arg.label))	prevArgs.add(arg.label);
			else						nextArgs.add(arg.label);
		}
		
		String prevArg = NONE;
		
		for (String currArg : prevArgs)
		{
			add2dArgsAux(pred, prevArg, currArg, SRLParser.DIR_LEFT);
			prevArg = currArg;
		}
		
		add2dArgsAux(pred, prevArg, END, SRLParser.DIR_LEFT);
		prevArg = NONE;
		
		for (String currArg : nextArgs)
		{
			add2dArgsAux(pred, prevArg, currArg, SRLParser.DIR_RIGHT);
			prevArg = currArg;
		}
		
		add2dArgsAux(pred, prevArg, END, SRLParser.DIR_RIGHT);
	}
	
	/** Called from {@link SRLProb#add1dArgs(DepNode, HashSet)}. */
	private void add2dArgsAux(DepNode pred, String prevArg, String currArg, byte dir)
	{
		ObjectDoubleOpenHashMap<String> mArg = increment1dPred(m_prob2d, getKey(pred, prevArg, dir));
		
		mArg.put(currArg, mArg.get(currArg)+1);
	}
	
//	============================= Compute Probabilities =============================
	
	/** Must be called before any probability is used. */
	public void computeProb()
	{
		computeConditionalProb(m_prob1d);
		computeConditionalProb(m_prob2d);
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
	
//	============================= Print =============================
	
	public void printAll(String filename)
	{
		printCP(m_prob1d, filename+".p1d");
		printCP(m_prob2d, filename+".p2d");
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
