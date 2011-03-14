package clear.experiment;

import clear.dep.DepTree;
import clear.dep.srl.SRLProb;
import clear.parse.SRLParser;
import clear.reader.SRLReader;

public class ExtractPBArg
{
	public ExtractPBArg(String filename)
	{
		SRLParser parser = new SRLParser(SRLParser.FLAG_TRAIN_PROBABILITY);
		SRLReader reader = new SRLReader(filename, true);
		DepTree   tree;
		
		while ((tree = reader.nextTree()) != null)
			parser.parse(tree);

		SRLProb prob = parser.getSRLProb();
		prob.computeProb();
		prob.printAll(filename);
	}
		
	static public void main(String[] args)
	{
		new ExtractPBArg(args[0]);
	}
}
