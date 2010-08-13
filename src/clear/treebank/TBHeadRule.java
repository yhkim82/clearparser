package clear.treebank;

public class TBHeadRule
{
	public int      dir;
	public String[] rules;
	
	public TBHeadRule(String dir, String[] heads)
	{
		this.dir   = (dir.equals("l")) ? -1 : 1;
		this.rules = heads;
	}
}
