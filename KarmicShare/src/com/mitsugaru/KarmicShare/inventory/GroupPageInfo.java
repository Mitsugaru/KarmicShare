package com.mitsugaru.KarmicShare.inventory;

public class GroupPageInfo
{
	private String group = "global";
	private int page = 0;
	
	public GroupPageInfo(String group, int page)
	{
		this.group = group;
		this.page = page;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof GroupPageInfo)
		{
			final GroupPageInfo info = (GroupPageInfo) obj;
			if(info.getGroup().equals(group) && info.getPage() == page)
			{
				return true;
			}
		}
		return false;
	}
	
	public String getGroup()
	{
		return group;
	}
	
	public int getPage()
	{
		return page;
	}
}
