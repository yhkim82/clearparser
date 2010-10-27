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
package clear.propbank;

import java.util.ArrayList;

/**
 * Propbank argument.
 * @author Jinho D. Choi
 * <b>Last update:</b> 9/30/2010
 */
public class PBArg
{
	/** Propbank argument-label */
	public String label;
	/** Predicate ID of this argument */
	public int    predicateId;
	/** Propbank locations */
	private ArrayList<PBLoc> pb_locs = null;
	
	/** Initializes the Propbank argument. */
	public PBArg(String label, int predicateId)
	{
		init(label, predicateId);
	}
	
	public PBArg(String labelPredicatId)
	{
		String[] tmp = labelPredicatId.split(PBLib.LABEL_DELIM);
		init(tmp[0], Integer.parseInt(tmp[1]));
	}
	
	private void init(String label, int predicateId)
	{
		this.label       = label;
		this.predicateId = predicateId;
		this.pb_locs     = new ArrayList<PBLoc>();
	}
	
	/** Adds a location. */
	public void addLoc(PBLoc loc)
	{
		pb_locs.add(loc);
	}
	
	public ArrayList<PBLoc> getLocs()
	{
		return pb_locs;
	}

	/** @return string representation of the argument. */
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		
		for (PBLoc loc : pb_locs)
			buff.append(loc.toString());
		
		buff.append(PBLib.PROP_LABEL_DELIM);
		buff.append(label);
		
		return buff.toString();
	}

	public String toStringLabelPredicateId()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(label);
		buff.append(PBLib.LABEL_DELIM);
		buff.append(predicateId);
		
		return buff.toString();
	}
}
