package com.mitsugaru.KarmicShare.tasks;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.database.SQLibrary.Query;
import com.mitsugaru.KarmicShare.logic.Karma;

public class RemoveGroupTask implements Runnable
{
	private KarmicShare plugin;
	private CommandSender sender;
	private String group, groupId;
	private Map<String, String> queries = new HashMap<String, String>();

	public RemoveGroupTask(KarmicShare plugin, CommandSender sender,
			String group)
	{
		this.plugin = plugin;
		this.sender = sender;
		this.group = group;
		this.groupId = "" + Karma.getGroupId(group);
	}

	@Override
	public void run()
	{
		try
		{
			final Map<String, String> map = new HashMap<String, String>();
			Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName() + ";");
			if (rs.getResult().next())
			{
				do
				{
					final String groups = rs.getResult().getString("groups");
					if (!rs.getResult().wasNull())
					{
						map.put(rs.getResult().getString("playername"), groups);
					}
				} while (rs.getResult().next());
			}
			rs.closeQuery();
			for (Map.Entry<String, String> entry : map.entrySet())
			{
				boolean has = false;
				if (entry.getValue().contains("&"))
				{
					// they have multiple groups
					for (String s : entry.getValue().split("&"))
					{
						// grab id of given group and compare against
						// ids
						if (s.equalsIgnoreCase(groupId))
						{
							has = true;
						}
					}
				}
				else
				{
					// they only have one group
					if (entry.getValue().equalsIgnoreCase(groupId))
					{
						has = true;
					}
				}
				if (has)
				{
					if (entry.getValue().contains("&"))
					{
						// Multigroup
						StringBuilder sb = new StringBuilder();
						for (String s : entry.getValue().split("&"))
						{
							//plugin.getLogger().info(s);
							// Add back all groups excluding
							// specified group
							if (!s.equals(groupId))
							{
								sb.append(s + "&");
							}
						}
						// Remove trailing ampersand
						sb.deleteCharAt(sb.length() - 1);
						queries.put(entry.getKey(), sb.toString());
					}
					else
					{
						// Else, it was their only group, so clear it.
						queries.put(entry.getKey(), "");
					}
				}
			}
			for (Map.Entry<String, String> entry : queries.entrySet())
			{
				plugin.getDatabaseHandler().standardQuery(
						"UPDATE " + Table.PLAYERS.getName() + " SET groups='"
								+ entry.getValue() + "' WHERE playername='"
								+ entry.getKey() + "';");
			}
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Done removing group " + ChatColor.GOLD + group
					+ ChatColor.YELLOW + " from all players.");
			// Lastly, remove the actual group
			plugin.getDatabaseHandler().standardQuery(
					"DELETE FROM " + Table.GROUPS.getName() + " WHERE id='"
							+ groupId + "';");
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Removed group " + ChatColor.GOLD + group
					+ ChatColor.YELLOW + " from known groups table.");
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " SQL error");
			e.printStackTrace();
		}
	}
}
