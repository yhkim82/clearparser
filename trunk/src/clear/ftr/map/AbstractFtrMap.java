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
	/** Contains labels */
	protected ArrayList<String>                       a_label;
	/** Takes "label" as a key and its index as a value */
	protected ObjectIntOpenHashMap<String>            m_label;
	/** Contains ngram features */
	protected ArrayList<ObjectIntOpenHashMap<String>> am_ngram;
	
	/** Last n_gram index + 1 */
	public int n_ngram;
	
	/** Initializes empty maps (values to be added later). */
	public AbstractFtrMap(FtrXmlType xml)
	{
		init(xml);
	}
	
	/** Initializes maps from <code>lexiconFile</code>. */
	public AbstractFtrMap(FtrXmlType xml, String lexiconFile)
	{
		init(xml);
		load(lexiconFile);
	}
	
	/** This method must be included at the top of {@link AbstractFtrMap#init(Object)}. */
	protected void initDefault(AbstractFtrXml xml)
	{
		a_label = new ArrayList<String>();
		m_label = new ObjectIntOpenHashMap<String>();
		
		int n = xml.a_ngram.size(), i;
		am_ngram = new ArrayList<ObjectIntOpenHashMap<String>>(n);
		
		for (i=0; i<n; i++)
			am_ngram.add(new ObjectIntOpenHashMap<String>());
	}
	
	/** This method must be included at the top of {@link AbstractFtrMap#load(String)}. */
	protected void loadDefault(BufferedReader fin) throws Exception
	{
		int n, m, i, j;
		String key;
		
		n = Integer.parseInt(fin.readLine());	// labels
		for (i=1; i<=n; i++)
		{
			key = fin.readLine();
			a_label.add(key);
			m_label.put(key, i);
		}
		
		n_ngram = 1;							// n-grams
		m       = am_ngram.size();
		for (j=0; j<m; j++)
		{
			n = Integer.parseInt(fin.readLine());
			for (i=0; i<n; i++)
			{
				key = fin.readLine();
				am_ngram.get(j).put(key, n_ngram++);
			}
		}
	}

	/** This method must be included at the top of {@link AbstractFtrMap#save(String)}. */
	public void saveDefualt(AbstractFtrXml xml, PrintStream fout)
	{
		ObjectIntOpenHashMap<String> map;
		int j, m, value, cutoff;	String key;
		
		fout.println(m_label.size());			// labels
		for (ObjectCursor<String> str : m_label.keySet())
			fout.println(str.value);

		m = am_ngram.size();					// n-grams
		for (j=0; j<m; j++)
		{
			cutoff = xml.a_ngram.get(j).cutoff;
			map    = am_ngram.get(j);
			fout.println(map.size());
			
			for (ObjectCursor<String> str : map.keySet())
			{
				key   = str.value;
				value = map.get(key);
				
				if (value > cutoff)
					fout.println(str.value);
			}
		}
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
		ObjectIntOpenHashMap<String> map = am_ngram.get(index);
		map.put(ftr, map.get(ftr)+1);
	}
	
	public int ngramToIndex(int index, String ftr)
	{
		return am_ngram.get(index).get(ftr);
	}
	
	abstract protected void init(FtrXmlType xml);
	abstract protected void load(String lexiconFile);
	abstract public    void save(FtrXmlType xml, String lexiconFile);
}
