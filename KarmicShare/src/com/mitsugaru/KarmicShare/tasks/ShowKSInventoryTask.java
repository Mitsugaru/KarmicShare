package com.mitsugaru.KarmicShare.tasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.mitsugaru.KarmicShare.KarmicShare;

public class ShowKSInventoryTask implements Runnable
{
	private final KarmicShare plugin;
	private final Player player;
	private final Inventory inventory;

	public ShowKSInventoryTask(KarmicShare plugin, Player player,
			Inventory inventory)
	{
		this.plugin = plugin;
		this.player = player;
		this.inventory = inventory;
	}

	@Override
	public void run()
	{
		player.closeInventory();
		final int i = plugin.getServer().getScheduler()
				.scheduleSyncDelayedTask(plugin, new Runnable() {

					@Override
					public void run()
					{
						player.openInventory(inventory);
					}

				}, 1);
		if (i == -1)
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Could not open inventory!");
		}
	}

}
