package clear.dep.srl;

public class SRLArg
{
	public int    argId;
	public String label;
	public double score;

	public SRLArg(int argId, String label, double score)
	{
		set(argId, label, score);
	}
	
	public void set(int argId, String label, double score)
	{
		this.argId = argId;
		this.label = label;
		this.score = score;
	}
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(argId);
		build.append(":");
		build.append(label);
		
		return build.toString();
	}
}
