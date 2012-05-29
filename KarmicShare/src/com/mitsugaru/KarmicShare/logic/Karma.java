package com.mitsugaru.KarmicShare.logic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.inventory.GroupPageInfo;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.inventory.KSInventoryHolder;

public class Karma
{
	private static KarmicShare plugin;
	public static final Map<String, String> selectedGroup = new HashMap<String, String>();
	public static final Map<GroupPageInfo, KSInventoryHolder> inventories = new HashMap<GroupPageInfo, KSInventoryHolder>();
	public static final Map<Item, Integer> cache = new HashMap<Item, Integer>();
	public static final Map<String, Integer> chestPage = new HashMap<String, Integer>();

	public static void init(KarmicShare ks)
	{
		plugin = ks;
	}

	/**
	 * Updates the player's karma
	 * 
	 * @param Name
	 *            of player
	 * @param Amount
	 *            of karma to add
	 * @param Boolean
	 *            if to add to current karma, or to set it as given value
	 * @throws SQLException
	 */
	public static void updatePlayerKarma(String name, int k)
			throws SQLException
	{
		try
		{
			// Retrieve karma from database
			int karma = getPlayerKarma(name);
			// Add to existing value
			karma += k;
			String query;
			// Check updated karma value to limits in config
			if (karma <= plugin.getPluginConfig().lower)
			{
				// Updated karma value is beyond lower limit, so set to min
				query = "UPDATE " + Table.PLAYERS.getName() + " SET karma='"
						+ plugin.getPluginConfig().lower
						+ "' WHERE playername='" + name + "';";
			}
			else if (karma >= plugin.getPluginConfig().upper)
			{
				// Updated karma value is beyond upper limit, so set to max
				query = "UPDATE " + Table.PLAYERS.getName() + " SET karma='"
						+ plugin.getPluginConfig().upper
						+ "' WHERE playername='" + name + "';";
			}
			else
			{
				// Updated karma value is within acceptable range
				query = "UPDATE " + Table.PLAYERS.getName() + " SET karma='"
						+ karma + "' WHERE playername='" + name + "';";
			}
			plugin.getDatabaseHandler().standardQuery(query);
		}
		catch (SQLException e)
		{
			throw e;
		}
	}

	/**
	 * Retrieves karma value of a player from the database. Forces player to be
	 * added to database if they don't exist
	 * 
	 * @param Player
	 *            name
	 * @return karma value associated with name
	 */
	public static int getPlayerKarma(String name) throws SQLException
	{
		String query = "SELECT * FROM " + Table.PLAYERS.getName()
				+ " WHERE playername='" + name + "';";
		final Query rs = plugin.getDatabaseHandler().select(query);
		int karma = plugin.getPluginConfig().playerKarmaDefault;
		boolean has = false;
		// Retrieve karma from database
		try
		{
			if (rs.getResult().next())
			{
				do
				{
					// Grab player karma value
					karma = rs.getResult().getInt("karma");
					has = true;
				} while (rs.getResult().next());
			}
			rs.closeQuery();
			if (!has)
			{
				// Player not in database, therefore add them
				query = "INSERT INTO " + Table.PLAYERS.getName()
						+ " (playername,karma) VALUES ('" + name + "','"
						+ karma + "');";
				plugin.getDatabaseHandler().standardQuery(query);
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		return karma;
	}

	public static boolean validGroup(CommandSender sender, String group)
	{
		if (group.equals("global"))
		{
			return true;
		}
		else if (group.equals("self_" + sender.getName()))
		{
			return true;
		}
		boolean valid = false;
		try
		{
			final Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.GROUPS.getName()
							+ " WHERE groupname='" + group + "';");
			if (rs.getResult().next())
			{
				valid = true;
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
		return valid;
	}

	public static boolean playerHasGroup(CommandSender sender, String name,
			String group)
	{
		if (group.equals("global"))
		{
			return true;
		}
		else if (group.equals("self_" + sender.getName()))
		{
			return true;
		}
		boolean has = false;
		try
		{
			// Insures that the player is added to the database
			getPlayerKarma(name);
			String groups = "";
			final Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName()
							+ " WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				if (!rs.getResult().wasNull())
				{
					if (groups.contains("&"))
					{
						// they have multiple groups
						for (String s : groups.split("&"))
						{
							try
							{
								// grab id of given group and compare against
								// ids
								final String groupName = getGroupName(Integer
										.parseInt(s));
								if (groupName.equalsIgnoreCase(group))
								{
									has = true;
								}
							}
							catch (NumberFormatException n)
							{
								// bad group id
							}
						}
					}
					else
					{
						try
						{
							final String groupName = getGroupName(Integer
									.parseInt(groups));
							// they only have one group
							if (groupName.equalsIgnoreCase(group))
							{
								has = true;
							}
						}
						catch (NumberFormatException n)
						{
							// bad group id
						}
					}
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
		return has;
	}

	public static List<String> getPlayerGroups(CommandSender sender, String name)
	{
		List<String> list = new ArrayList<String>();
		boolean wasNull = false;
		try
		{
			String groups = "";
			Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName()
							+ " WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				wasNull = rs.getResult().wasNull();
			}
			rs.closeQuery();
			if (wasNull)
			{
				// No groups, add in the global and self
				plugin.getDatabaseHandler().standardQuery(
						"INSERT INTO " + Table.GROUPS.getName()
								+ " (groupname) VALUES ('self_"
								+ name.toLowerCase() + "');");
				groups = getGroupId("global") + "&"
						+ getGroupId("self_" + name);
				// Set groups for future reference
				plugin.getDatabaseHandler()
						.standardQuery(
								"UPDATE " + Table.PLAYERS.getName()
										+ " SET groups='" + groups
										+ "' WHERE playername='" + name + "';");
			}
			String[] split = groups.split("&");
			for (String s : split)
			{
				try
				{
					list.add(getGroupName(Integer.parseInt(s)));
				}
				catch (NumberFormatException n)
				{
					plugin.getLogger().severe("Bad group id '" + s + "'");
					n.printStackTrace();
				}
			}
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
		return list;
	}

	public static int getGroupId(String group)
	{
		int id = -1;
		try
		{
			final Query query = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.GROUPS.getName()
							+ " WHERE groupname='" + group + "';");
			if (query.getResult().next())
			{
				id = query.getResult().getInt("id");
				if (query.getResult().wasNull())
				{
					id = -1;
				}
			}
			query.closeQuery();
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					"SQL Exception on getting group '" + group + "' id");
			e.printStackTrace();
		}
		return id;
	}

	public static String getGroupName(int id)
	{
		String group = "NONE";
		try
		{
			final Query query = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.GROUPS.getName() + " WHERE id='"
							+ id + "';");
			if (query.getResult().next())
			{
				group = query.getResult().getString("groupname");
				if (query.getResult().wasNull())
				{
					group = "NONE";
				}
			}
			query.closeQuery();
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					"SQL Exception on getting group name for  id '" + id + "'");
			e.printStackTrace();
		}
		return group;
	}
}