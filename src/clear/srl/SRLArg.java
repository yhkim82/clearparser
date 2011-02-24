package clear.srl;

import clear.experiment.MergeTreePropBank;
import clear.util.JArrays;

public class SRLArg implements Comparable<SRLArg>
{
	static public String DELIM = ",";
	
	public String  label;
	public float[] ids;
	
	/** Called only from {@link MergeTreePropBank#addPBArgToTBTree()}. */
	public SRLArg(String label, int[] ids)
	{
		set(label, ids);
	}
	
	public SRLArg(String label, float[] ids)
	{
		set(label, ids);
	}
	
	/** Called only from {@link MergeTreePropBank#addPBArgToTBTree()}. */
	public void set(String label, int[] ids)
	{
		this.label = label;
		this.ids   = new float[ids.length];
		
		for (int i=0; i<ids.length; i++)
			this.ids[i] = ids[i] + 1;
	}
	
	public void set(String label, float[] ids)
	{
		this.label = label;
		this.ids   = ids;
	}
	
	public boolean isLabel(String regex)
	{
		return label.matches(regex);
	}

	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(label);	build.append(SRLHead.DELIM);
		build.append(JArrays.join(ids, DELIM));
		
		return build.toString();
	}
	
	@Override
	public int compareTo(SRLArg arg)
	{
		return label.compareTo(arg.label);
	}
}
