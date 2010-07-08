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

import clear.util.DSUtil;
import clear.util.JString;
import gnu.trove.list.array.TIntArrayList;

/**
 * Propbank argument.
 * @author Jinho D. Choi
 * <b>Last update:</b> 02/17/2010
 */
public class PBArg
{
	/** Propbank argument-label */
	public String        label;
	/** List of terminal/token IDs */
	public TIntArrayList ids = null;
	
	/**
	 * Initializes the Propbank argument.
	 * @param label {@link PBArg#label}
	 * @param ids   {@link PBArg#ids}
	 */
	public PBArg(String label, TIntArrayList ids)
	{
		this.label = label;
		this.ids   = ids;
	}
	
	/**
	 * Initializes the Propbank argument with values in <code>str</code>.
	 * @param str <label>{@link PBLib#LABEL_DELIM}(<id>{@link PBLib#ID_DELIM}>)+
	 */
	public PBArg(String str)
	{
		String[] arg = str.split(PBLib.LABEL_DELIM);
		
		label = arg[0];
		ids   = DSUtil.toTIntArrayList(arg[1].split(PBLib.ID_DELIM));
	}
	
	/** @return true if <code>arg</code> is equal to the current argument */
	public boolean equals(PBArg arg)
	{
		if (label.equals(arg.label))
			return (ids.size() == arg.ids.size()) && ids.containsAll(arg.ids);
		
		return false;
	}
	
	/** @return string representation of the argument. */
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		
		buff.append(label);
		buff.append(PBLib.LABEL_DELIM);
		buff.append(JString.join(ids, PBLib.ID_DELIM));
		
		return buff.toString();
	}
}
