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

import java.util.ArrayList;
import org.w3c.dom.Document;

/**
 * Reads dependency feature templates from a xml file.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/4/2010
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
	
	public ArrayList<FtrTemplate> a_rule;	// rule-based features
	
	public DepFtrXml(String featureXml)
	{
		super(featureXml);
	}
	
	protected void initFeatures(Document doc) throws Exception
	{
		a_rule = new ArrayList<FtrTemplate>();
		getFeatures(doc.getElementsByTagName(RULE), a_rule);
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
		
		build.append("<"+TEMPLATE+">\n");
		
		for (FtrTemplate ftr : a_ngram)
			toStringAux(build, NGRAM, ftr);
		
		for (FtrTemplate ftr : a_rule)
			toStringAux(build, RULE, ftr);
		
		build.append("</"+TEMPLATE+">");
		
		return build.toString();
	}
}
