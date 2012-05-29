package com.mitsugaru.KarmicShare.inventory;

import com.mitsugaru.KarmicShare.logic.Karma;

public class GroupPageInfo
{
	private String group = "global";
	private int page = 0, viewers = 0;
	
	
	public GroupPageInfo(String group, int page)
	{
		this.group = group;
		this.page = page;
	}
	
	@Override
	public int hashCode()
	{
		return group.hashCode() + page;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof GroupPageInfo)
		{
			final GroupPageInfo info = (GroupPageInfo) obj;
			if(info.getGroup().equalsIgnoreCase(group) && info.getPage() == page)
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
	
	public void addViewer()
	{
		viewers += 1;
	}
	
	public void removeViewer()
	{
		viewers -= 1;
		if(viewers <= 0)
		{
			//Remove from inventories hashmap as there are no more viewers
			Karma.inventories.remove(this);
		}
	}
}
