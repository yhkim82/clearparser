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
package clear.model;


import java.util.ArrayList;
import java.util.Collections;

import clear.util.tuple.JIntDoubleTuple;

/**
 * Liblinear decoders.
 * @author Jinho D. Choi
 * <br><b>Last update:</b> 7/1/2010
 */
public class LiblinearVoteModel
{
	protected LiblinearModel[] g_models;
	protected int n_labels;
	
	public LiblinearVoteModel(String modelFile, int nLabels)
	{
		n_labels = nLabels;
		
		try
		{
			init(modelFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void init(String modelFile) throws Exception
	{
		g_models = new LiblinearModel[n_labels*(n_labels-1)/2];
		
		for (int i=0; i<g_models.length; i++)
			g_models[i] = new LiblinearModel(modelFile+"."+i);
	}
	
	/* (non-Javadoc)
	 * @see harvest.model.AbstractModel#predict(java.util.ArrayList)
	 */
	public JIntDoubleTuple predict(ArrayList<Integer> x)
	{
		double[] count = new double[n_labels];
		
		for (LiblinearModel model : g_models)
		{
			JIntDoubleTuple res = model.predict(x);
			if (res.d > 1)	res.d = 1;
			count[res.i] += res.d;
		}

		JIntDoubleTuple max = new JIntDoubleTuple(0, count[0]);
		
		for (int i=1; i<count.length; i++)
		{
			if (count[i] > max.d)
				max.set(i, count[i]);
		}
		
		return max;
	}
	
	/* (non-Javadoc)
	 * @see harvest.model.AbstractModel#predictAll(java.util.ArrayList)
	 */
	public ArrayList<JIntDoubleTuple> predictAll(ArrayList<Integer> x)
	{
		ArrayList<JIntDoubleTuple> list = new ArrayList<JIntDoubleTuple>(n_labels);
		for (int i=0; i<n_labels; i++)	list.add(new JIntDoubleTuple(i, 0));
		
		for (LiblinearModel model : g_models)
		{
			JIntDoubleTuple res = model.predict(x);
			if (res.d > 1)	res.d = 1;
			list.get(res.i).d += res.d;
		}

		Collections.sort(list);
		return list;
	}
}
