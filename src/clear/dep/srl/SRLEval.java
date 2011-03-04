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
package clear.dep.srl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import clear.dep.DepNode;
import clear.dep.DepTree;

/**
 * Compare two dependency trees.
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/26/2010
 */
public class SRLEval
{
	private final String TOTAL = "TOTAL";
	private HashMap<String,int[]> m_score;
	
	public SRLEval()
	{
		m_score = new HashMap<String, int[]>();
		
		int[] value = new int[4];
		m_score.put(TOTAL, value);
	}

	public void evaluate(DepTree gold, DepTree sys)
	{
		for (int i=1; i<gold.size(); i++)
		{
			DepNode gNode = gold.get(i);
			DepNode sNode = sys .get(i);
			
			measure(gNode, sNode);
		}
	}
	
	private void measure(DepNode gNode, DepNode sNode)
	{
		ArrayList<SRLHead> gHeads = gNode.srlInfo.heads;
		ArrayList<SRLHead> sHeads = sNode.srlInfo.heads;
		int[] total = m_score.get(TOTAL);
		int[] gArg, sArg;
		
		for (SRLHead gHead : gHeads)
		{
			gArg = getArray(gHead.label);
			
			for (SRLHead sHead : sHeads)
			{
				if (sHead.equals(gHead.headId))
				{
					total[0]++;
					
					if (sHead.equals(gHead.label))
					{
						total[1]++;
						gArg [0]++;
					}
					
					break;
				}
			}
			
			gArg[2]++;		// recall
		}
		
		total[2] += sHeads.size();	// precision
		total[3] += gHeads.size();	// recall

		for (SRLHead sHead : sHeads)
		{
			sArg = getArray(sHead.label);
			sArg[1]++;		// precision
		}
	}
	
	private int[] getArray(String label)
	{
		if (m_score.containsKey(label))
		{
			return m_score.get(label);
		}
		else
		{
			int[] value = new int[3];
			m_score.put(label, value);
			
			return value;
		}
	}
	
	
	
	public void print()
	{
		System.out.println("----------------------------------------");
		System.out.printf("%10s%10s%10s%10s\n", "LABEL","P","R","F1");
		System.out.println("----------------------------------------");
		printTotal();
		System.out.println("----------------------------------------");
		
		ArrayList<String> labels = new ArrayList<String>(m_score.keySet());
		Collections.sort(labels);
		
		for (String label : labels)
		{
			if (!label.equals(TOTAL))
				printLocal(label);
		}
		System.out.println("----------------------------------------");
	}
	
	private void printTotal()
	{
		int[] value = m_score.get(TOTAL);
		
		double precision = 100d * value[0] / value[2];
		double recall    = 100d * value[0] / value[3];
		double f1        = getF1(precision, recall);
		
		printEach("UAS", precision, recall, f1);
		
		precision = 100d * value[1] / value[2];
		recall    = 100d * value[1] / value[3];
		f1        = getF1(precision, recall);
		
		printEach("LAS", precision, recall, f1);
	}
	
	private void printLocal(String label)
	{
		int[] value = m_score.get(label);
		
		double precision = 100d * value[0] / value[1];
		double recall    = 100d * value[0] / value[2];
		double f1        = getF1(precision, recall);
		
		printEach(label, precision, recall, f1);
	}
	
	private void printEach(String label, double precision, double recall, double f1)
	{
		System.out.printf("%10s%10.2f%10.2f%10.2f\n", label, precision, recall, f1);
	}
	
	public double getF1()
	{
		int[] value = m_score.get(TOTAL);
		
		double precision = 100d * value[1] / value[2];
		double recall    = 100d * value[1] / value[3];
		
		return getF1(precision, recall);
	}
	
	static public double getF1(double precision, double recall)
	{
		return 2 * (precision * recall) / (precision + recall);
	}
}
