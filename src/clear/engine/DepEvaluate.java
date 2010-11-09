package clear.engine;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import clear.dep.DepEval;
import clear.dep.DepTree;
import clear.reader.DepReader;

public class DepEvaluate
{
	@Option(name="-g", usage="gold-standard file", required=true, metaVar="REQUIRED")
	private String s_goldFile;
	@Option(name="-s", usage="system file", required=true, metaVar="REQUIRED")
	private String s_sysFile;
	
	public DepEvaluate(String args[])
	{
		CmdLineParser cmd  = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
			
			DepReader gReader = new DepReader(s_goldFile, true);
			DepReader sReader = new DepReader(s_sysFile , true);
			DepEval   eval = new DepEval();
			DepTree   gTree, sTree;
			
			while ((gTree = gReader.nextTree()) != null)
			{
				sTree = sReader.nextTree();
				if (sTree == null)
				{
					System.err.println("More tree needed in '"+s_sysFile+"'");
					System.exit(1);
				}
				
				eval.evaluate(gTree, sTree);
			}
			
			eval.print();
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
		}
	}

	static public void main(String[] args)
	{
		new DepEvaluate(args);
	}
}
