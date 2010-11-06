/**
* Copyright (c) 2010, Regents of the University of Colorado
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
package clear.ftr.xml;

import java.io.FileInputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Read abstract features from a xml file.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
 */
abstract public class AbstractFtrXml
{
	static public final String TEMPLATE	= "feature_template";
	static public final String NGRAM	= "ngram";
	static public final String N		= "n";			// # of tokens
	static public final String C		= "c";			// cutoff
	static public final String F		= "f";			// field
	static public final String VISIBLE	= "visible";	// "true"|"false"
	static public final String DELIM_F	= ":";			// field    delimiter (e.g., l+1.f)
	static public final String DELIM_R	= "_";			// relation delimiter (e.g., l_hd)
	
	public ArrayList<FtrTemplate> a_ngram;				// n-gram features
	
	public AbstractFtrXml(String featureXml)
	{
		init(featureXml);
	}
	
	public void init(String featureXml)
	{
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		
		try
		{
			DocumentBuilder builder = dFactory.newDocumentBuilder();
			Document        doc     = builder.parse(new FileInputStream(featureXml));
			
			initNgrams  (doc);
			initFeatures(doc);
		}
		catch (Exception e) {e.printStackTrace();System.exit(1);}
	}
	
	protected void initNgrams(Document doc) throws Exception
	{
		a_ngram = new ArrayList<FtrTemplate>();
		getFeatures (doc.getElementsByTagName(NGRAM), a_ngram);
	}
	
	protected void getFeatures(NodeList lsNgram, ArrayList<FtrTemplate> list) throws Exception
	{
		int i, j, n, c;
		
		for (i=0; i<lsNgram.getLength(); i++)
		{
			Element eFeature = (Element)lsNgram.item(i);
			String  visible  = eFeature.getAttribute(VISIBLE).trim();
			if (visible.equals("false"))	continue;
			
			n = Integer.parseInt(eFeature.getAttribute(N));
			c = (eFeature.hasAttribute(C)) ? Integer.parseInt(eFeature.getAttribute(C)) : 0;
			
			FtrTemplate ftr = new FtrTemplate(n, c);
			
			for (j=0; j<n; j++)
				ftr.addFtrToken(j, getFtrToken(eFeature.getAttribute(F + j)));
			
			list.add(ftr);
		}
		
		list.trimToSize();
	}
	
	/** @param ftr (e.g., "l.f", "l+1.m", "l-1.p", "l0_hd.d") */
	protected FtrToken getFtrToken(String ftr) throws Exception
	{
		String[] aField    = ftr      .split(DELIM_F);	// {"l-1_hd", "p"}
		String[] aRelation = aField[0].split(DELIM_R);	// {"l-1", "hd"} 
		
		char source = aRelation[0].charAt(0);
		if (!validSource(source))	xmlError(ftr);
		
		int offset = 0;
		if (aRelation[0].length() >= 2)
		{
			if (aRelation[0].charAt(1) == '+')	offset = Integer.parseInt(aRelation[0].substring(2)); 
			else								offset = Integer.parseInt(aRelation[0].substring(1));
		}
		
		String relation = null;
		if (aRelation.length > 1)
		{
			relation = aRelation[1];
			if (!validRelation(relation))	xmlError(ftr);
		}
		
		String field = aField[1];
		if (!validField(field))	xmlError(ftr);

		return new FtrToken(source, offset, relation, field);
	}
	
	/** Prints system error and exits. */
	protected void xmlError(String error)
	{
		System.err.println("Invalid feature: "+error);
		System.exit(1);
	}
	
	abstract protected void    initFeatures(Document doc) throws Exception;
	abstract protected boolean validSource(char source);
	abstract protected boolean validRelation(String relation);
	abstract protected boolean validField(String filed);
	
	protected void toStringAux(StringBuilder build, String type, FtrTemplate ftr)
	{
		build.append("    <");
		build.append(type);
		build.append(" ");
		build.append(ftr.toString());
		build.append("/>\n");	
	}
}
