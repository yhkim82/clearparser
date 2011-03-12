package clear.dep.srl;

import java.util.ArrayList;

import clear.dep.DepLib;
import clear.reader.AbstractReader;

public class SRLInfo
{
	static final public String DELIM_ARG = ";";
	
	public String         rolesetId;
	public ArrayList<SRLHead> heads;
	
	public SRLInfo()
	{
		rolesetId = DepLib.FIELD_BLANK;
		heads     = new ArrayList<SRLHead>();
	}
	
	/** @param args "headId:label(;headId:label)*" or "_" */
	public SRLInfo(String rolesetId, String args)
	{
		this.rolesetId = rolesetId;
		heads          = new ArrayList<SRLHead>();
		
		if (!args.equals(DepLib.FIELD_BLANK))
		{
			for (String arg : args.split(DELIM_ARG))
				heads.add(new SRLHead(arg));
		}
	}
	
	public SRLHead getHead(int index)
	{
		return heads.get(index);
	}

	public void setRolesetId(String rolesetId)
	{
		this.rolesetId = rolesetId;
	}

	public void addHead(int headId, String label)
	{
		heads.add(new SRLHead(headId, label));
	}
	
	public boolean isPredicate()
	{
		return !rolesetId.equals(DepLib.FIELD_BLANK);
	}
	
	public boolean isEmptyHead()
	{
		return heads.isEmpty();
	}
	
	public boolean isHead(int headId)
	{
		for (SRLHead head : heads)
			if (head.equals(headId))
				return true;
		
		return false;
	}
	
	public boolean isHeadMatch(String regex)
	{
		for (SRLHead head : heads)
			if (head.label.matches(regex))
				return true;
		
		return false;
	}
	
	public String getLabel(int headId)
	{
		for (SRLHead head : heads)
			if (head.equals(headId))
				return head.label;
		
		return null;
	}
	
	public void copy(SRLInfo info)
	{
		rolesetId = info.rolesetId;
		heads     = new ArrayList<SRLHead>();
		
		for (SRLHead head : info.heads)
			heads.add(head.clone());
	}
	
	public SRLInfo clone()
	{
		SRLInfo info = new SRLInfo();
		
		info.copy(this);
		return info;
	}

	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(rolesetId);
		build.append(AbstractReader.FIELD_DELIM);
		
		if (heads.isEmpty())
		{
			build.append(DepLib.FIELD_BLANK);
		}
		else
		{
			build.append(heads.get(0).toString());
			
			for (int i=1; i<heads.size(); i++)
			{
				build.append(DELIM_ARG);
				build.append(heads.get(i).toString());
			}
		}
		
		return build.toString();
	}
}
