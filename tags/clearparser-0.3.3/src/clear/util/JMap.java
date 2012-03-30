package clear.util;

import java.util.ArrayList;
import java.util.Collections;

import clear.util.tuple.JObjectIntTuple;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class JMap
{
	static public <T>ArrayList<JObjectIntTuple<T>> getSortedTuples(ObjectIntOpenHashMap<T> map)
	{
		ArrayList<JObjectIntTuple<T>> list = new ArrayList<JObjectIntTuple<T>>();
		
		for (ObjectCursor<T> cur : map.keys())
			list.add(new JObjectIntTuple<T>(cur.value, map.get(cur.value)));

		Collections.sort(list);
		return list;
	}

}
