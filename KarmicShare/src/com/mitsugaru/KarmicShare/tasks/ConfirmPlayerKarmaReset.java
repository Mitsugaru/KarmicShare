package com.mitsugaru.KarmicShare.tasks;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.Karma;
import com.mitsugaru.KarmicShare.KarmicShare;

public class ConfirmPlayerKarmaReset implements Runnable
{
	private KarmicShare plugin;
	private String name;
	private Player sender;

	public ConfirmPlayerKarmaReset(KarmicShare plugin, Player player,
			String name)
	{
		this.plugin = plugin;
		this.name = name;
		sender = player;
	}

	@Override
	public void run()
	{
		String answer = plugin.ask(sender, ChatColor.YELLOW + KarmicShare.TAG
				+ ChatColor.DARK_AQUA + " Reset " + ChatColor.GOLD + name
				+ ChatColor.DARK_AQUA + "'s karma?", ChatColor.GREEN + "yes",
				ChatColor.RED + "no");
		if (answer.equals("yes"))
		{
			try
			{
				Karma.updatePlayerKarma(name,
						plugin.getPluginConfig().playerKarmaDefault);
				sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " "
						+ name + "'s karma reset");
			}
			catch (SQLException e)
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ "Could not reset " + name + "'s karma");
				e.printStackTrace();
			}
		}
		else
		{
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ ChatColor.DARK_AQUA + " Karma reset for "
					+ ChatColor.GOLD + name + ChatColor.DARK_AQUA
					+ " cancelled.");
		}
	}
}
