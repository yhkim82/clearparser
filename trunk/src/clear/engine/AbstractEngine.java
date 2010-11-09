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
package clear.engine;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import clear.reader.AbstractReader;

/**
 * <b>Last update:</b> 11/8/2010
 * @author Jinho D. Choi
 */
public class AbstractEngine
{
	protected final String TAG_COMMON          = "common";
	protected final String TAG_COMMON_LANGUAGE = "language";
	protected final String TAG_COMMON_FORMAT   = "format";
	
	protected final String ENTRY_LEXICA  = "lexica";
	protected final String ENTRY_MODEL   = "model";
	protected final String ENTRY_FEATURE = "feature";
	
	/** Language */
	protected String  s_language = AbstractReader.LANG_EN;
	/** Format */
	protected String  s_format   = AbstractReader.FORMAT_DEP;
	/** Configuration element */
	protected Element e_config;
	
	/** Initializes <configuration> element. */
	public boolean initConfigElements(String configXml)
	{
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		
		try
		{
			DocumentBuilder builder = dFactory.newDocumentBuilder();
			Document        doc     = builder.parse(new File(configXml));
			
			e_config = doc.getDocumentElement();
			return initElements();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/** Initializes <common> elements. */	
	protected boolean initElements()
	{
		Element eCommon = getElement(e_config, TAG_COMMON);
		Element element;
		
		if ((element = getElement(eCommon, TAG_COMMON_LANGUAGE)) != null)
			s_language = element.getTextContent().trim();
		
		if ((element = getElement(eCommon, TAG_COMMON_FORMAT)) != null)
			s_format = element.getTextContent().trim();
		
		return true;
	}
	
	protected Element getElement(Element parent, String name)
	{
		NodeList list = parent.getElementsByTagName(name);
		return (list.getLength() > 0) ? (Element)list.item(0) : null;
	}
}
