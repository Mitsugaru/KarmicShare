package com.mitsugaru.KarmicShare.tasks;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Repopulate implements Runnable
{
	int slot;
	ItemStack item;
	Inventory inventory;
	boolean clear;

	public Repopulate(Inventory inv, ItemStack i)
	{
		slot = -999;
		inventory = inv;
		item = i;
	}

	public Repopulate(Inventory inv, ItemStack i, int s, boolean c)
	{
		inventory = inv;
		item = i;
		slot = s;
		clear = c;
	}

	@Override
	public void run()
	{
		if (slot >= 0)
		{
			if (clear)
			{
				inventory.clear(slot);
			}
			else
			{
				inventory.setItem(slot, item);
			}
		}
		else
		{
			inventory.addItem(item);
		}
	}
}
