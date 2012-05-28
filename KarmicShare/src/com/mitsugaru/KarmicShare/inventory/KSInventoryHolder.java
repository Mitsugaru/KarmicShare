package com.mitsugaru.KarmicShare.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class KSInventoryHolder implements InventoryHolder
{
	private Inventory inventory = null;
	private GroupPageInfo info = null;
	
	public KSInventoryHolder(GroupPageInfo info)
	{
		this.info = info;
	}
	
	public void setInventory(Inventory inventory)
	{
		//TODO populate inventory
	}
	
	@Override
	public Inventory getInventory()
	{
		return inventory;
	}
	
	public GroupPageInfo getInfo()
	{
		return info;
	}

}
