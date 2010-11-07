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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import clear.ftr.xml.DepFtrXml;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

/**
 * This class contains mappings between indices and features.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
public class DepFtrMap extends AbstractFtrMap<DepFtrXml>
{
	/** Delimiter between joined feature tags for rules */
	static public final String DELIM_RULE = " ";
	
	/** Contains rule features */
	protected ArrayList<ObjectIntOpenHashMap<String>> m_rule;
	/** Takes "punctuation" as a key and its index as a value */
	protected ObjectIntOpenHashMap<String>            m_punctuation;
	
	/** Number of punctuation */
	protected int n_punctuation;
	
	public DepFtrMap(DepFtrXml xml)
	{
		super(xml);
	}
	
	public DepFtrMap(DepFtrXml xml, String lexiconFile)
	{
		super(xml, lexiconFile);
	}
	
	protected void init(DepFtrXml xml)
	{
		initDefault(xml);
		int i, n = xml.a_rule_templates.length;
		
		m_rule = new ArrayList<ObjectIntOpenHashMap<String>>(n);
		
		for (i=0; i<n; i++)
			m_rule.add(new ObjectIntOpenHashMap<String>());
		
		m_punctuation = new ObjectIntOpenHashMap<String>();
	}

	protected void load(String lexiconFile)
	{
		try
		{
			BufferedReader fin = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(lexiconFile))));
			loadDefault(fin);
			
			ObjectIntOpenHashMap<String> map;
			int n, m, i, j;
			String[] arr;
			String   key;
			
			// rules
			m = Integer.parseInt(fin.readLine());
			m_rule = new ArrayList<ObjectIntOpenHashMap<String>>(m);
			
			for (j=0; j<m; j++)
			{
				n = Integer.parseInt(fin.readLine());
				map = new ObjectIntOpenHashMap<String>(n);
				
				for (i=0; i<n; i++)
				{
					key = fin.readLine();
					arr = key.split(DELIM_RULE);
					map.put(arr[0], Integer.parseInt(arr[1]));
				}
				
				m_rule.add(map);
			}
			
			// punctuation
			n = Integer.parseInt(fin.readLine());
			m_punctuation = new ObjectIntOpenHashMap<String>(n);
			
			for (i=1; i<=n; i++)
			{
				key = fin.readLine();
				m_punctuation.put(key, i);
			}
			
			fin.close();
		}
		catch (Exception e) {e.printStackTrace();System.exit(1);}
	}
	
	/** Saves all tags to <code>lexiconFile</code>. */
	public void save(DepFtrXml xml, String lexiconFile, int ngramCutoff)
	{
		try
		{
			PrintStream fout = new PrintStream(new GZIPOutputStream(new FileOutputStream(lexiconFile)));
			saveDefault(xml, fout, ngramCutoff);
			
			ObjectIntOpenHashMap<String> map;
			int j, m, value, ruleCutoff;	String key;
			
			// rules
			m = m_rule.size();
			fout.println(m);
			
			for (j=0; j<m; j++)
			{
				map        = m_rule.get(j);
				ruleCutoff = xml.a_rule_templates[j].cutoff;
				fout.println(countKeys(map, ruleCutoff));
				
				for (ObjectCursor<String> str : map.keySet())
				{
					key   = str.value;
					value = map.get(key);
					
					if (Math.abs(value) > ruleCutoff)
					{
						if      (value < 0)	fout.println(key + DELIM_RULE +"-1");
						else if (value > 0)	fout.println(key + DELIM_RULE + "1");
					}
				}
			}
			
			// punctuation
			fout.println(m_punctuation.size());
			
			for (ObjectCursor<String> str : m_punctuation.keySet())
				fout.println(str.value);
			
			fout.flush();	fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
		
	public void addRule(int index, String ftr, int dir)
	{
		ObjectIntOpenHashMap<String> map = m_rule.get(index);
		map.put(ftr, map.get(ftr)+dir);
	}
	
	public int ruleToIndex(int index, String ftr)
	{
		return m_rule.get(index).get(ftr);
	}	
	
	/** Adds punctuation. */
	public void addPunctuation(String ftr)
	{
		m_punctuation.put(ftr, m_punctuation.get(ftr)+1);
	}
	
	/**
	 * Returns the index of the punctuation.
	 * If the punctuation does not exist, returns -1.
	 */
	public int punctuationToIndex(String ftr)
	{
		return m_punctuation.get(ftr) - 1;
	}
}
