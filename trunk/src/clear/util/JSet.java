package clear.util;

import gnu.trove.TIntCollection;
import gnu.trove.set.hash.TIntHashSet;

public class JSet
{
	static public TIntHashSet intersection(TIntHashSet set, TIntCollection col)
	{
		TIntHashSet inter = new TIntHashSet(set);
		
		inter.retainAll(col);
		return inter;
	}
}
