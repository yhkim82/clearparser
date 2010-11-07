/**
* Copyright (c) 2009, Regents of the University of Colorado
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
package clear.ftr.map;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;

import clear.ftr.xml.AbstractFtrXml;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

/**
 * Abstract feature map.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
abstract public class AbstractFtrMap<FtrXmlType>
{
	/** List of labels. */
	protected ArrayList<String>            a_label;
	/** Takes "label" as a key and its index as a value. */
	protected ObjectIntOpenHashMap<String> m_label;
	/** Contains n-gram features. */
	protected ArrayList<ObjectIntOpenHashMap<String>> m_ngram;
	/** Number of each n-gram feature. */
	protected int[]                                   n_ngram;
	
	/** Initializes empty maps (values to be added later). */
	public AbstractFtrMap(FtrXmlType xml)
	{
		init(xml);
	}
	
	/** Initializes maps from <code>lexiconFile</code>. */
	public AbstractFtrMap(FtrXmlType xml, String lexiconFile)
	{
		load(lexiconFile);
	}
	
	/** This method must be included at the top of {@link AbstractFtrMap#init(Object)}. */
	protected void initDefault(AbstractFtrXml xml)
	{
		int i, n = xml.a_ngram_templates.length;
		
		m_label = new ObjectIntOpenHashMap<String>();
		m_ngram = new ArrayList<ObjectIntOpenHashMap<String>>(n);
		
		for (i=0; i<n; i++)
			m_ngram.add(new ObjectIntOpenHashMap<String>());
	}
	
	/** This method must be included at the top of {@link AbstractFtrMap#load(String)}. */
	protected void loadDefault(BufferedReader fin) throws Exception
	{
		ObjectIntOpenHashMap<String> map;
		int n, m, i, j;
		String key;
		
		// labels
		n = Integer.parseInt(fin.readLine());
		a_label = new ArrayList<String>(n);
		m_label = new ObjectIntOpenHashMap<String>(n);
		
		for (i=1; i<=n; i++)
		{
			key = fin.readLine();
			a_label.add(key);
			m_label.put(key, i);
		}
		
		// n-grams
		m = Integer.parseInt(fin.readLine());
		m_ngram = new ArrayList<ObjectIntOpenHashMap<String>>(m);
		n_ngram = new int[m];
		
		for (j=0; j<m; j++)
		{
			n = Integer.parseInt(fin.readLine());
			map = new ObjectIntOpenHashMap<String>(n);
			
			for (i=1; i<=n; i++)
			{
				key = fin.readLine();
				map.put(key, i);
			}
			
			m_ngram.add(map);
			n_ngram[j] = n + 1;		// 0 is reserved for unseen feature
		}
	}

	/** This method must be included at the top of {@link AbstractFtrMap#save(String)}. */
	public void saveDefault(AbstractFtrXml xml, PrintStream fout, int ngramCutoff)
	{
		ObjectIntOpenHashMap<String> map;
		int j, m, value;
		String key;
		
		// labels
		fout.println(m_label.size());
		
		for (ObjectCursor<String> str : m_label.keySet())
			fout.println(str.value);

		// n-grams
		m = m_ngram.size();
		fout.println(m);

		for (j=0; j<m; j++)
		{
			map = m_ngram.get(j);
			fout.println(countKeys(map, ngramCutoff));
			
			for (ObjectCursor<String> str : map.keySet())
			{
				key   = str.value;
				value = map.get(key);
				if (value > ngramCutoff)	fout.println(key);
			}
		}
	}
	
	protected int countKeys(ObjectIntOpenHashMap<String> map, int cutoff)
	{
		if (cutoff < 1)	return map.size();
		int count = 0, value;
		
		for (ObjectCursor<String> key : map.keySet())
		{
			value = map.get(key.value);
			if (Math.abs(value) > cutoff)	count++;
		}
		
		return count;
	}
	
	/** Adds the class label. */
	public void addLabel(String label)
	{
		m_label.put(label, 1);
	}
	
	/** @return the class label corresponding to the index. */
	public String indexToLabel(int index)
	{
		return a_label.get(index);
	}
	
	/**
	 * Returns the index of the class label.
	 * If the class label does not exist, returns -1.
	 */
	public int labelToIndex(String label)
	{
		return m_label.get(label) - 1;
	}
	
	public void addNgram(int index, String ftr)
	{
		ObjectIntOpenHashMap<String> map = m_ngram.get(index);
		map.put(ftr, map.get(ftr)+1);
	}
	
	public int ngramToIndex(int index, String ftr)
	{
		return m_ngram.get(index).get(ftr);
	}
	
	abstract protected void init(FtrXmlType xml);
	abstract protected void load(String lexiconFile);
	abstract public    void save(FtrXmlType xml, String lexiconFile, int ngramCutoff);
}
