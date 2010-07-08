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
package clear.dep;

/**
 * This class contains constant variables for dependency parsing.
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/26/2010
 */
public class DepLib
{
	/** Flag to print configuration files */
	static public final byte FLAG_PRINT_LEXICON     = 0;
	/** Flag to print training instances */
	static public final byte FLAG_PRINT_INSTANCE   = 1;
	/** Flag to print transitions */
	static public final byte FLAG_PRINT_TRANSITION = 2;
	/** Flag to predict with greedy search */
	static public final byte FLAG_PREDICT_GREEDY   = 3;
	/** Flag to predict with k-best search */
	static public final byte FLAG_PREDICT_BEST     = 4;
	
	/** ID of the root node */
	static public final int ROOT_ID      =  0;
	/** ID of a null node */
	static public final int NULL_ID      = -1;
	/** Head ID of a null/root node */
	static public final int NULL_HEAD_ID = -2;

	/** Feature tag for root nodes */
	static public final String ROOT_TAG   = "#$ROOT$#";
	/** Dependency label of root nodes */
	static public final String ROOT_LABEL = "ROOT";
}
