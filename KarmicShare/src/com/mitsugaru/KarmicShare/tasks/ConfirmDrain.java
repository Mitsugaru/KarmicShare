package com.mitsugaru.KarmicShare.tasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;

public class ConfirmDrain implements Runnable
{
	private KarmicShare plugin;
	private Player player;
	private String group;

	public ConfirmDrain(KarmicShare plugin, Player player, String group)
	{
		this.plugin = plugin;
		this.player = player;
		this.group = group;
	}

	public void run()
	{
		String answer = plugin.ask(player, ChatColor.YELLOW + KarmicShare.TAG
				+ ChatColor.DARK_AQUA + " Delete ALL items in "
				+ ChatColor.GOLD + group + ChatColor.DARK_AQUA
				+ " pool? No recovery...", ChatColor.GREEN + "yes",
				ChatColor.RED + "no");
		if (answer.equals("yes"))
		{
			// Wipe table
			final String query = "DELETE FROM " + Table.ITEMS.getName()
					+ " WHERE groups='" + group + "';";
			plugin.getDatabaseHandler().standardQuery(query);
			plugin.getLogger().info("'" + group + "'" + " items table cleared");
			player.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " "
					+ ChatColor.GOLD + group + ChatColor.GREEN
					+ " item pool emptied.");
		}
		else
		{
			player.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Drain cancelled.");
		}
	}
}