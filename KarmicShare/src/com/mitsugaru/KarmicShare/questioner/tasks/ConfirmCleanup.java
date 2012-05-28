package com.mitsugaru.KarmicShare.questioner.tasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;

public class ConfirmCleanup implements Runnable
{
	private KarmicShare plugin;
	private Player sender;

	public ConfirmCleanup(KarmicShare plugin, Player sender)
	{
		this.plugin = plugin;
		this.sender = sender;
	}

	@Override
	public void run()
	{
		String answer = plugin.ask(sender, ChatColor.YELLOW + KarmicShare.TAG
				+ ChatColor.DARK_AQUA + " Run cleanup task?", ChatColor.GREEN
				+ "yes", ChatColor.RED + "no");
		if (answer.equals("yes"))
		{
			plugin.getDatabaseHandler().standardQuery(
					"DELETE FROM " + Table.ITEMS.getName()
							+ " WHERE amount<='0';");
			plugin.getLogger()
					.info(KarmicShare.TAG + " Cleanup task executed.");
			sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
					+ " Cleanup task executed.");
		}
		else
		{
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Cleanup task cancelled.");
		}
	}

}
