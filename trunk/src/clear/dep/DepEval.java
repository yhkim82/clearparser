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
 * Compare two dependency trees.
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/26/2010
 */
public class DepEval
{
	private int  n_las;
	private int  n_uas;
	private int  n_ls;
	private int  n_dep;
	
	private double d_las;
	private double d_uas;
	private double d_ls;
	private int    n_tree;

	private byte b_skip;
	
	public DepEval(byte skip)
	{
		n_las   = 0;
		n_uas   = 0;
		n_ls    = 0;
		n_dep   = 0;
		b_skip  = skip;
	}

	public void evaluate(DepTree gold, DepTree sys)
	{
		int[] acc = new int[3];
		
		for (int i=1; i<gold.size(); i++)
		{
			DepNode gNode = gold.get(i);
			DepNode sNode = sys .get(i);
			
			if (b_skip == 1 && sNode.headId == DepLib.NULL_HEAD_ID)
				continue;
			
			measure(gNode, sNode, acc);
		}
		
		int total = gold.size() - 1;
		
		n_las += acc[0];
		n_uas += acc[1];
		n_ls  += acc[2];
		n_dep += total;
		
		d_las += (double)acc[0] / total;
		d_uas += (double)acc[1] / total;
		d_ls  += (double)acc[2] / total;
		n_tree++;
	}
	
	static public double getLas(DepTree gold, DepTree sys)
	{
		int correct = 0, size = gold.size();
		
		for (int i=1; i<size; i++)
		{
			DepNode gNode = gold.get(i);
			DepNode sNode = sys .get(i);
			
			if (gNode.isDeprel(sNode.deprel) && gNode.headId == sNode.headId)
				correct++;
		}
		
		return (double)correct/(size-1);
	}
	
	private void measure(DepNode gNode, DepNode sNode, int[] acc)
	{
		if (gNode.isDeprel(sNode.deprel))
		{
			acc[2]++;
			if (gNode.headId == sNode.headId)
				acc[0]++;
		}
		
		if (gNode.headId == sNode.headId)
			acc[1]++;
	}
	
	public double getLas()
	{
		return (double)n_las / n_dep;
	}
	
	public double getUas()
	{
		return (double)n_uas / n_dep;
	}
	
	public double getLs()
	{
		return (double)n_ls / n_dep;
	}
	
	public void printTotal()
	{
		System.out.println("== Micro ==");
		System.out.printf("LAS: %4.2f%% (%d/%d)\n", getLas()*100, n_las, n_dep);
		System.out.printf("UAS: %4.2f%% (%d/%d)\n", getUas()*100, n_uas, n_dep);
		System.out.printf("LS : %4.2f%% (%d/%d)\n", getLs ()*100, n_ls , n_dep);
		System.out.println();
		System.out.println("== Macro ==");
		System.out.printf("LAS: %4.2f%%\n", (d_las/n_tree)*100);
		System.out.printf("UAS: %4.2f%%\n", (d_uas/n_tree)*100);
		System.out.printf("LS : %4.2f%%\n", (d_ls /n_tree)*100);
	}
}
