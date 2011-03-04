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

import clear.ftr.xml.SRLFtrXml;
import clear.util.IOUtil;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

/**
 * This class contains mappings between indices and features.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
public class SRLFtrMap extends AbstractFtrMap<SRLFtrXml>
{
	public SRLFtrMap(SRLFtrXml xml)
	{
		super(xml);
	}
	
	public SRLFtrMap(SRLFtrXml xml, String lexiconFile)
	{
		super(xml, lexiconFile);
	}
	
	public SRLFtrMap(SRLFtrXml xml, BufferedReader fin)
	{
		super(xml, fin);
	}
	
	protected void init(SRLFtrXml xml)
	{
		initDefault(xml);
	}

	protected void load(String lexiconFile)
	{
		try
		{
			BufferedReader fin = IOUtil.createBufferedFileReader(lexiconFile);
			loadAux(fin);
			fin.close();
		}
		catch (Exception e) {e.printStackTrace();System.exit(1);}
	}
	
	protected void load(BufferedReader fin)
	{
		try
		{
			loadAux(fin);
		}
		catch (Exception e) {e.printStackTrace();System.exit(1);}
	}
	
	private void loadAux(BufferedReader fin) throws Exception
	{
		loadDefault(fin);
	}
	
	protected ObjectIntOpenHashMap<String> loadFreqMap(BufferedReader fin) throws Exception
	{
		int i, n = Integer.parseInt(fin.readLine());
		ObjectIntOpenHashMap<String> map = new ObjectIntOpenHashMap<String>(n);
		String[] tmp;
		
		for (i=1; i<=n; i++)
		{
			tmp = fin.readLine().split(" ");
			map.put(tmp[0], Integer.parseInt(tmp[1]));
		}
		
		return map;
	}
	
	/** Saves all tags to <code>lexiconFile</code>. */
	public void save(SRLFtrXml xml, String lexiconFile)
	{
		try
		{
			PrintStream fout = IOUtil.createPrintFileStream(lexiconFile);
			saveAux(xml, fout);
			fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/** Saves all tags to <code>fout</code>. */
	public void save(SRLFtrXml xml, PrintStream fout)
	{
		try
		{
			saveAux(xml, fout);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/** Saves all tags to <code>lexiconFile</code>. */
	private void saveAux(SRLFtrXml xml, PrintStream fout)
	{
		saveDefault(xml, fout);
	}
	
	protected void saveFreqMap(PrintStream fout, ObjectIntOpenHashMap<String> map, int cutoff)
	{
		String key;	int value;
		fout.println(countKeys(map, cutoff));
		
		for (ObjectCursor<String> str : map.keySet())
		{
			key   = str.value;
			value = map.get(key);
			if (value > cutoff)	fout.println(key+" "+value);
		}
	}
}
