package com.mitsugaru.KarmicShare.questioner.tasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;

public class ConfirmRemoveGroup implements Runnable
{
	private KarmicShare plugin;
	private String group;
	private Player sender;

	public ConfirmRemoveGroup(KarmicShare plugin, Player sender, String group)
	{
		this.plugin = plugin;
		this.sender = sender;
		this.group = group;
	}

	@Override
	public void run()
	{
		String answer = plugin.ask(sender, ChatColor.YELLOW + KarmicShare.TAG
				+ ChatColor.DARK_AQUA + " Remove group " + ChatColor.GOLD
				+ group + ChatColor.DARK_AQUA + "? ", ChatColor.GREEN + "yes",
				ChatColor.RED + "no");
		if (answer.equals("yes"))
		{
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " This could take a while...");
			int i = plugin
					.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(plugin,
							new RemoveGroupTask(plugin, sender, group));
			if (i == -1)
			{
				sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
						+ " Could not schedule task");
			}
			plugin.getDatabaseHandler().standardQuery(
					"DELETE FROM " + Table.ITEMS.getName() + " WHERE groups='"
							+ group + "';");
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Removed all items of group: " + ChatColor.GOLD + group);
		}
		else
		{
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ ChatColor.DARK_AQUA + " Cancelled removal of "
					+ ChatColor.GOLD + group);
		}
	}
}
