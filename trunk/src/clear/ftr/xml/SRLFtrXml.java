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

import java.io.InputStream;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads dependency feature templates from a xml file.
 * @author Jinho D. Choi
 * <b>Last update:</b> 11/6/2010
 */
public class SRLFtrXml extends AbstractFtrXml
{
	static public final char   LAMBDA	= 'l';
	static public final char   BETA		= 'b';
	static public final String R_HD		= "hd";
	static public final String R_LM		= "lm";		// leftmost dependent
	static public final String R_RM		= "rm";		// rightmost dependent
	static public final String R_LS		= "ls";		// left sibling
	static public final String R_RS		= "rs";		// right sibling
	
	static public final String F_FORM	= "f";
	static public final String F_LEMMA	= "m";
	static public final String F_POS	= "p";
	static public final String F_DEPREL	= "d";

	static public final Pattern P_FEAT 		= Pattern.compile("^ft(\\d+)$");
	static public final Pattern P_SUBCAT_D	= Pattern.compile("^scd(\\d+)$");
	static public final Pattern P_SUBCAT_P	= Pattern.compile("^scp(\\d+)$");
	static public final Pattern P_PATH_D	= Pattern.compile("^ptd(\\d+)$");
	static public final Pattern P_PATH_P	= Pattern.compile("^ptp(\\d+)$");
	static public final Pattern P_REL		= Pattern.compile(R_HD+"|"+R_LM+"|"+R_RM+"|"+R_LS+"|"+R_RS);
	static public final Pattern P_FIELD		= Pattern.compile(F_FORM+"|"+F_LEMMA+"|"+F_POS+"|"+F_DEPREL); 
	
	public SRLFtrXml(String featureXml)
	{
		super(featureXml);
	}
	
	public SRLFtrXml(InputStream fin)
	{
		super(fin);
	}
	
	protected void initCutoffs(Document doc) throws Exception
	{
		NodeList eList = doc.getElementsByTagName(CUTOFF);
		if (eList.getLength() <= 0)	return;
		
		Element eCutoff = (Element)eList.item(0);
		n_cutoff_label  = (eCutoff.hasAttribute(LABEL)) ? Integer.parseInt(eCutoff.getAttribute(LABEL)) : 0;
		n_cutoff_ngram  = (eCutoff.hasAttribute(NGRAM)) ? Integer.parseInt(eCutoff.getAttribute(NGRAM)) : 0;
	}
	
	protected void initFeatures(Document doc) throws Exception {}
	
	protected boolean validSource(char token)
	{
		return token == LAMBDA || token == BETA;
	}
	
	protected boolean validRelation(String relation)
	{
		return P_REL.matcher(relation).matches();
	}
	
	protected boolean validField(String field)
	{
		return P_FIELD   .matcher(field).matches() ||  
		       P_FEAT    .matcher(field).matches() ||
		       P_SUBCAT_D.matcher(field).matches() ||
		       P_SUBCAT_P.matcher(field).matches() ||
		       P_PATH_D  .matcher(field).matches() ||
		       P_PATH_P  .matcher(field).matches();
	}
}
