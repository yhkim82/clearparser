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
	protected ArrayList<ObjectIntOpenHashMap<String>> am_rule;
	/** Takes "punctuation" as a key and its index as a value */
	protected ObjectIntOpenHashMap<String>            m_punctuation;
	
	/** Size of {@link DepFtrMap#m_punctuation} */
	public int n_punctuation;
	
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
		
		int i, n;
		
		n = xml.a_rule.size();
		am_rule = new ArrayList<ObjectIntOpenHashMap<String>>(n);
		
		for (i=0; i<n; i++)
			am_rule.add(new ObjectIntOpenHashMap<String>());
		
		m_punctuation = new ObjectIntOpenHashMap<String>();
	}

	protected void load(String lexiconFile)
	{
		try
		{
			BufferedReader fin = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(lexiconFile))));
			loadDefault(fin);
			
			int n, m, i, j;
			String   key;
			String[] arr;
			ObjectIntOpenHashMap<String> map;
			
			m = am_rule.size();						// rules
			for (j=0; j<m; j++)
			{
				map = am_rule.get(j);
				n   = Integer.parseInt(fin.readLine());
				for (i=0; i<n; i++)
				{
					arr = fin.readLine().split(DELIM_RULE);
					map.put(arr[0], Integer.parseInt(arr[1]));
				} 
			}
			
			n = Integer.parseInt(fin.readLine());	// punctuation
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
	public void save(DepFtrXml xml, String lexiconFile)
	{
		try
		{
			PrintStream fout = new PrintStream(new GZIPOutputStream(new FileOutputStream(lexiconFile)));
			saveDefualt(xml, fout);
			
			ObjectIntOpenHashMap<String> map;
			int j, m, value, cutoff;	String key;
			
			m = am_rule.size();						// rules
			for (j=0; j<m; j++)
			{
				cutoff = xml.a_rule.get(j).cutoff;
				map    = am_rule.get(j);
				fout.println(map.size());
				
				for (ObjectCursor<String> str : map.keySet())
				{
					key   = str.value;
					value = map.get(key);
					
					if (Math.abs(value) > cutoff)
					{
						if      (value < 0)	fout.println(key + DELIM_RULE +"-1");
						else if (value > 0)	fout.println(key + DELIM_RULE + "1");
					}
				}
			}
			
			fout.println(m_punctuation.size());		// punctuation
			for (ObjectCursor<String> str : m_punctuation.keySet())
				fout.println(str.value);
			
			fout.flush();	fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
		
	public void addRule(int index, String ftr, int dir)
	{
		ObjectIntOpenHashMap<String> map = am_rule.get(index);
		map.put(ftr, map.get(ftr)+dir);
	}
	
	public int ruleToIndex(int index, String ftr)
	{
		return am_rule.get(index).get(ftr);
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
