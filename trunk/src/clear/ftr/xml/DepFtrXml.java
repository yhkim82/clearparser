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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads dependency feature templates from a xml file.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class DepFtrXml extends AbstractFtrXml
{
	static public final String RULE		= "rule";
	static public final char   LAMBDA	= 'l';
	static public final char   BETA		= 'b';
	static public final String R_HD		= "hd";
	static public final String R_LM		= "lm";
	static public final String R_RM		= "rm";
	static public final String F_FORM	= "f";
	static public final String F_LEMMA	= "m";
	static public final String F_POS	= "p";
	static public final String F_DEPREL	= "d";
	
	/** Rule feature templates */
	public FtrTemplate[] a_rule_templates;
	
	public DepFtrXml(String featureXml)
	{
		super(featureXml);
	}	
	
	protected void initFeatures(Document doc) throws Exception
	{
		NodeList eList = doc.getElementsByTagName(RULE);
		int i, n = eList.getLength();
		Element eFeature;

		a_rule_templates = new FtrTemplate[n];
		
		for (i=0; i<n; i++)
		{
			eFeature = (Element)eList.item(i);
			if (eFeature.getAttribute(VISIBLE).trim().equals("false"))	continue;
			
			a_rule_templates[i] = getFtrTemplate(eFeature);
		}
	}
	
	protected boolean validSource(char token)
	{
		return token == LAMBDA || token == BETA;
	}
	
	protected boolean validRelation(String relation)
	{
		return relation.equals(R_HD) || relation.equals(R_LM) || relation.equals(R_RM);
	}
	
	protected boolean validField(String field)
	{
		return field.equals(F_FORM) || field.equals(F_LEMMA) || field.equals(F_POS) || field.equals(F_DEPREL);
	}
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		int i, j;
		
		build.append("<"+TEMPLATE+">\n");
		
		for (i=0; i<a_ngram_templates.length; i++)
			for (j=0; j<a_ngram_templates[i].length; j++)
				toStringAux(build, NGRAM, a_ngram_templates[i][j]);
		
		for (i=0; i<a_rule_templates.length; i++)
			toStringAux(build, RULE, a_rule_templates[i]);
		
		build.append("</"+TEMPLATE+">");
		
		return build.toString();
	}
	
/*	static public void main(String[] args)
	{
		DepFtrXml xml = new DepFtrXml(args[0]);
		
		System.out.println(xml.toString());
	}*/
}
