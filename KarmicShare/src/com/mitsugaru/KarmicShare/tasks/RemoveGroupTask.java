package com.mitsugaru.KarmicShare.tasks;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;

public class RemoveGroupTask implements Runnable
{
	private KarmicShare plugin;
	private CommandSender sender;
	private String group;
	private Map<String, String> queries = new HashMap<String, String>();

	public RemoveGroupTask(KarmicShare plugin, CommandSender sender, String group)
	{
		this.plugin = plugin;
		this.sender = sender;
		this.group = group;
	}

	@Override
	public void run()
	{
		try
		{
			Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName() + ";");
			if (rs.getResult().next())
			{
				do
				{
					boolean has = false;
					String groups = rs.getResult().getString("groups");
					if (!rs.getResult().wasNull())
					{
						if (groups.contains("&"))
						{
							// they have multiple groups
							for (String s : groups.split("&"))
							{
								if (s.equals(group))
								{
									has = true;
								}
							}
						}
						else
						{
							// they only have one group
							if (groups.equals(group))
							{
								has = true;
							}
						}
						if (has)
						{
							if (groups.contains("&"))
							{
								// Multigroup
								StringBuilder sb = new StringBuilder();
								for (String s : groups.split("&"))
								{
									plugin.getLogger().info(s);
									// Add back all groups excluding
									// specified group
									if (!s.equals(group))
									{
										sb.append(s + "&");
									}
								}
								// Remove trailing ampersand
								sb.deleteCharAt(sb.length() - 1);
								groups = sb.toString();
								queries.put(
										rs.getResult().getString(
												"playername"), groups);
							}
							// Else, it was their only group, so clear it.
							queries.put(
									rs.getResult().getString("playername"),
									"");
						}
					}
				} while (rs.getResult().next());
			}
			rs.closeQuery();
			for (Map.Entry<String, String> entry : queries.entrySet())
			{
				plugin.getDatabaseHandler().standardQuery(
						"UPDATE " + Table.PLAYERS.getName()
								+ " SET groups='" + entry.getValue()
								+ "' WHERE playername='" + entry.getKey()
								+ "';");
			}
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Done removing group " + ChatColor.GRAY + group
					+ ChatColor.YELLOW + " from all players.");
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " SQL error");
			e.printStackTrace();
		}
	}
}
