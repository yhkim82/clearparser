/**
* Copyright (c) 2007, Regents of the University of Colorado
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
package clear.treebank;

import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * Treebank library.
 * @author Jinho D. Choi
 * <b>Last update:</b> 7/28/2010
 */
public class TBLib
{
	static final public String LRB = "(";
	static final public String RRB = ")";
	
	// phrase level pos-tags
	static final public String POS_ADJP   = "ADJP";
	static final public String POS_ADVP   = "ADVP";
	static final public String POS_CONJP  = "CONJP";
	static final public String POS_EDITED = "EDITED";
	static final public String POS_NML    = "NML";
	static final public String POS_NP     = "NP";
	static final public String POS_NX     = "NX";
	static final public String POS_META   = "META";
	static final public String POS_PP     = "PP";
	static final public String POS_PRN    = "PRN";
	static final public String POS_RRC    = "RRC";
	static final public String POS_SBAR   = "SBAR";
	static final public String POS_TOP    = "TOP";
	static final public String POS_UCP    = "UCP";
	static final public String POS_VP     = "VP";
	static final public String POS_WHADVP = "WHADVP";
	static final public String POS_WHNP   = "WHNP";
	static final public String POS_WHPP   = "WHPP";
	
	// word-level pos-tags
	static final public String POS_CC    = "CC";
	static final public String POS_CD    = "CD";
	static final public String POS_IN    = "IN";
	static final public String POS_JJ    = "JJ";
	static final public String POS_NONE  = "-NONE-";
	static final public String POS_NN    = "NN";
	static final public String POS_MD    = "MD";
	static final public String POS_PRP   = "PRP";
	static final public String POS_QP    = "QP";
	static final public String POS_TO    = "TO";
	static final public String POS_VB    = "VB";
	static final public String POS_WRB   = "WRB";
	static final public String POS_XX    = "XX";
	
	// punctuation pos-tags
	static final public String POS_COLON  = ":";
	static final public String POS_COMMA  = ",";
	static final public String POS_HYPH   = "HYPH";
	static final public String POS_LDQ    = "``";
	static final public String POS_LRB    = "-LRB-";
	static final public String POS_NFP    = "NFP";
	static final public String POS_PERIOD = ".";
	static final public String POS_RDQ    = "''";
	static final public String POS_RRB    = "-RRB-";
	static final public String POS_SYM    = "SYM";
		
	// function tags
	static final public String TAG_NOM = "NOM";
	static final public String TAG_SBJ = "SBJ";
	static final public String TAG_ETC = "ETC";
	
	static public boolean isConjunction(String pos)
	{
		return isWordConjunction(pos) || isPuncConjunction(pos);
	}
	
	static public boolean isWordConjunction(String pos)
	{
		return pos.equals(POS_CC) || pos.equals(POS_CONJP);
	}
	
	static public boolean isPuncConjunction(String pos)
	{
		return pos.equals(POS_COMMA) || pos.equals(POS_COLON);
	}
	
	static public boolean isNounLike(String pos)
	{
		return isNoun(pos) || pos.equals(POS_NP) || pos.equals(POS_NML) || pos.equals(POS_WHNP) || pos.contains(TAG_NOM);
	}
	
	static public boolean isNoun(String pos)
	{
		return pos.startsWith(POS_NN) || pos.equals(POS_PRP);
	}
	
	static public boolean isVerb(String pos)
	{
		return pos.startsWith(POS_VB) || pos.equals(POS_MD);
	}
	
	static public boolean isAdjectiveLike(String pos)
	{
		return isAdjective(pos) || pos.equals(POS_ADJP) || pos.equals(POS_NML);
	}
	
	static public boolean isAdjective(String pos)
	{
		return pos.startsWith(POS_JJ);
	}
	
	static public boolean isWhAdjectiveLike(String pos)
	{
		return pos.equals(POS_WHADVP) || pos.equals(POS_WRB) || pos.equals(POS_WHPP) || pos.equals(POS_IN);
	}
	
	static public boolean isPunctuation(String pos)
	{
		return pos.equals(POS_COLON) || pos.equals(POS_COMMA) || pos.equals(POS_PERIOD) || pos.equals(POS_NFP) || pos.equals(POS_HYPH) || pos.equals(POS_SYM)
	        || pos.equals(POS_LDQ)   || pos.equals(POS_RDQ)   || pos.equals(POS_LRB)    || pos.equals(POS_RRB);
	}
	
	
	
	
	
	
	static public boolean isVerbPhrase(String pos)
	{
		return pos.startsWith(POS_VP);
	}
	
	static public boolean isPreposition(String pos)
	{
		return pos.startsWith(POS_IN);
	}
	
	static public boolean isCardinal(String pos)
	{
		return pos.startsWith(POS_CD);
	}
	
	static public boolean isHyphen(String pos)
	{
		return pos.startsWith(POS_HYPH);
	}
	
	static public boolean isRelativeClause(String pos)
	{
		return pos.startsWith(POS_RRC);
	}
	
	static public boolean isCorrelativeConjunction(String words)
	{
		words = words.toLowerCase();
		return words.equals("either") || words.equals("neither") || words.equals("whether") || words.equals("both") || words.equals("not only"); 
	}
	
	static public boolean isSubject(String pos)
	{
		return pos.contains(TAG_SBJ);
	}
	
	
	
	static public boolean isLeftAttachedPunctuation(String pos)
	{
		return pos.equals(POS_COLON) || pos.equals(POS_COMMA) || pos.equals(POS_HYPH) || pos.equals(POS_SYM);
	}
	
	static public boolean isCoordination(String poss)
	{
		ArrayList<Pattern> ls_pattern = new ArrayList<Pattern>();
		String[] aPos = {"JJ", "JJR", "JJS", "VBN", "VBG", "NML", "PRP\\$", "ADJP", "NNP", "DT", "ADVP", "RB", "IN", "RBR", "PP", "VB", "NP", "CD", "VP", "S", "RP", "VBP", "VBD", "VBG", "NN", "MD"};
		
		String mod = "(-LRB-|-RRB-|RB|ADVP|EDITED|INTJ|PRN|SBAR|PP|NFP|''|DT|CODE|X|XX)";
		String coord = "(,|:|CC)";
		
		for (String pos : aPos)
		{
			pos = "("+pos+"(\\w)*)";
			ls_pattern.add(Pattern.compile(String.format("%s( %s %s)*(( %s)? %s( %s)?)*(( %s)? CC( %s)?)(( %s)? %s( %s)?)*( %s)", pos, coord, pos, coord, mod, coord, coord, coord, coord, mod, coord, pos)));
		}
		
		ls_pattern.add(Pattern.compile("[A-Z]+ HYPH CC HYPH [A-Z]+"));
	//	ls_pattern.add(Pattern.compile("JJ|VBG|VBN CC( NN)* JJ|VBG|VBN"));
	//	ls_pattern.add(Pattern.compile("JJ.?+ CC JJ.?+"));

		for (Pattern p : ls_pattern)
			if (p.matcher(poss).find())
				return true;
		
		return false;
	}
}
