package com.mitsugaru.KarmicShare.config;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.bukkit.enchantments.EnchantmentWrapper;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.inventory.ComparableEnchantment;

public class Update
{
	private static KarmicShare plugin;

	public static void init(KarmicShare ks)
	{
		plugin = ks;
	}

	/**
	 * This method is called to make the appropriate changes, most likely only
	 * necessary for database schema modification, for a proper update.
	 */
	public static void update()
	{
		// Grab current version
		final double ver = Double.parseDouble(plugin.getConfig().getString(
				"version"));
		String query = "";
		// Updates to alpha 0.08
		if (ver < 0.08)
		{
			// Add enchantments column
			plugin.getLogger().info(
					"Altering items table to add enchantments column.");
			query = "ALTER TABLE items ADD enchantments TEXT;";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		if (ver < 0.09)
		{
			// Add back durability column
			plugin.getLogger().info(
					"Altering items table to add durability column.");
			query = "ALTER TABLE items ADD durability TEXT;";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		if (ver < 0.14)
		{
			// Revamp item table
			try
			{
				plugin.getLogger().info("Revamping item table");
				query = "SELECT * FROM items;";
				final List<ZeroPointFourteenItemObject> fourteen = new ArrayList<ZeroPointFourteenItemObject>();
				Query rs = plugin.getDatabaseHandler().select(query);
				if (rs.getResult().next())
				{
					do
					{
						String enchantments = rs.getResult().getString(
								"enchantments");
						if (!rs.getResult().wasNull())
						{
							fourteen.add(new ZeroPointFourteenItemObject(rs
									.getResult().getInt("itemid"), rs
									.getResult().getInt("amount"), rs
									.getResult().getByte("data"), rs
									.getResult().getShort("durability"),
									enchantments));
						}
						else
						{
							fourteen.add(new ZeroPointFourteenItemObject(rs
									.getResult().getInt("itemid"), rs
									.getResult().getInt("amount"), rs
									.getResult().getByte("data"), rs
									.getResult().getShort("durability"), ""));
						}

					} while (rs.getResult().next());
				}
				rs.closeQuery();
				// Drop item table
				plugin.getDatabaseHandler().standardQuery("DROP TABLE items;");
				// Create new table
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE `items` (`id` INTEGER PRIMARY KEY, `itemid` SMALLINT UNSIGNED,`amount` INT,`data` TEXT,`durability` TEXT,`enchantments` TEXT, `groups` TEXT);");
				// Add back items
				for (ZeroPointFourteenItemObject bak : fourteen)
				{
					String fourteenItemQuery = "";
					if (bak.enchantments.equals(""))
					{
						fourteenItemQuery = "INSERT INTO items (itemid,amount,data,durability,groups) VALUES ('"
								+ bak.itemid
								+ "','"
								+ bak.amount
								+ "','"
								+ bak.data
								+ "','"
								+ bak.durability
								+ "','global');";
					}
					else
					{
						fourteenItemQuery = "INSERT INTO items (itemid,amount,data,durability,enchantments,groups) VALUES ('"
								+ bak.itemid
								+ "','"
								+ bak.amount
								+ "','"
								+ bak.data
								+ "','"
								+ bak.durability
								+ "','"
								+ bak.enchantments + "','global');";
					}
					plugin.getDatabaseHandler()
							.standardQuery(fourteenItemQuery);
				}
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning("SQL Exception");
				e.printStackTrace();
			}
			// Add groups to players table
			plugin.getLogger().info(
					"Altering player table to add groups column.");
			query = "ALTER TABLE players ADD groups TEXT;";
			plugin.getDatabaseHandler().standardQuery(query);
			// Add the GLOBAL group
			plugin.getLogger().info("Adding global group to groups table.");
			query = "INSERT INTO groups (groupname) VALUES ('global');";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		if (ver < 0.2)
		{
			// Drop newly created tables
			plugin.getLogger().info("Dropping empty tables.");
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.ITEMS.getName() + ";");
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.PLAYERS.getName() + ";");
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.GROUPS.getName() + ";");
			// Update tables to have prefix
			plugin.getLogger().info(
					"Renaming items table to '" + Table.ITEMS.getName() + "'.");
			query = "ALTER TABLE items RENAME TO " + Table.ITEMS.getName()
					+ ";";
			plugin.getDatabaseHandler().standardQuery(query);
			plugin.getLogger().info(
					"Renaming players table to '" + Table.PLAYERS.getName()
							+ "'.");
			query = "ALTER TABLE players RENAME TO " + Table.PLAYERS.getName()
					+ ";";
			plugin.getDatabaseHandler().standardQuery(query);
			plugin.getLogger().info(
					"Renaming groups table to '" + Table.GROUPS.getName()
							+ "'.");
			query = "ALTER TABLE groups RENAME TO " + Table.GROUPS.getName()
					+ ";";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		if (ver < 0.3)
		{
			/**
			 * Rebuild groups table
			 */
			plugin.getLogger().info("Rebuilding groups table...");
			// Save old groups
			final List<String> groups = new ArrayList<String>();
			// Add global to be known
			groups.add("global");
			try
			{
				Query rs = plugin.getDatabaseHandler().select(
						"SELECT * FROM " + Table.GROUPS.getName() + ";");
				if (rs.getResult().next())
				{
					do
					{
						groups.add(rs.getResult().getString("groupname"));
					} while (rs.getResult().next());
				}
				rs.closeQuery();
			}
			catch (SQLException sql)
			{
				plugin.getLogger().warning("SQL Exception");
				sql.printStackTrace();
			}
			// Drop previous table
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.GROUPS.getName() + ";");
			// Recreate table
			if (plugin.getPluginConfig().useMySQL)
			{
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE "
										+ Table.GROUPS.getName()
										+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, groupname varchar(32) NOT NULL, UNIQUE (groupname), PRIMARY KEY (id));");
			}
			else
			{
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE "
										+ Table.GROUPS.getName()
										+ " (id INTEGER PRIMARY KEY, groupname TEXT NOT NULL, UNIQUE (groupname));");
			}
			// Add back in groups
			for (final String group : groups)
			{
				plugin.getDatabaseHandler().standardQuery(
						"INSERT INTO " + Table.GROUPS.getName()
								+ " (groupname) VALUES('" + group + "');");
			}
			/**
			 * Rebuild player table
			 */
			plugin.getLogger().info("Rebuilding player table...");
			// Save old players
			List<ZeroPointTwoSixTwoPlayerObject> playerList = new ArrayList<ZeroPointTwoSixTwoPlayerObject>();
			try
			{
				Query rs = plugin.getDatabaseHandler().select(
						"SELECT * FROM " + Table.PLAYERS.getName());
				if (rs.getResult().next())
				{
					do
					{
						final String playerGroups = rs.getResult().getString(
								"groups");
						playerList.add(new ZeroPointTwoSixTwoPlayerObject(rs
								.getResult().getString("playername"), rs
								.getResult().getInt("karma"), playerGroups));
					} while (rs.getResult().next());
				}
				rs.closeQuery();
			}
			catch (SQLException sql)
			{
				plugin.getLogger().warning("SQL Exception");
				sql.printStackTrace();
			}
			// Drop previous table
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.PLAYERS.getName() + ";");
			// Recreate table
			if (plugin.getPluginConfig().useMySQL)
			{
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE "
										+ Table.PLAYERS.getName()
										+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL,karma INT NOT NULL, groups TEXT, UNIQUE (playername), PRIMARY KEY (id));");
			}
			else
			{
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE "
										+ Table.PLAYERS.getName()
										+ " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL,karma INT NOT NULL, groups TEXT, UNIQUE (playername));");
			}
			// Add them back in
			for (final ZeroPointTwoSixTwoPlayerObject player : playerList)
			{
				if (player.groups == null)
				{
					query = "INSERT INTO " + Table.PLAYERS.getName()
							+ " (playername, karma) VALUES('"
							+ player.playername + "','" + player.karma + "','"
							+ "');";
				}
				else
				{
					// Translate groups into ids
					final StringBuilder sb = new StringBuilder();
					if (player.groups.contains("&"))
					{
						for (String s : player.groups.split("&"))
						{
							int id = plugin.getDatabaseHandler().getGroupId(s);
							sb.append(id + "&");
						}
						// Remove extra &
						sb.deleteCharAt(sb.length() - 1);
					}
					else
					{
						sb.append(plugin.getDatabaseHandler().getGroupId(
								player.groups));
					}
					query = "INSERT INTO " + Table.PLAYERS.getName()
							+ " (playername, karma, groups) VALUES ('"
							+ player.playername + "','" + player.karma + "','"
							+ sb.toString() + "');";
				}
				plugin.getDatabaseHandler().standardQuery(query);
			}
			/**
			 * Rebuild items table
			 */
			plugin.getLogger().info("Rebuilding items table...");
			// Save old items
			List<ZeroPointTwoSixTwoItemObject> itemList = new ArrayList<ZeroPointTwoSixTwoItemObject>();
			try
			{
				Query rs = plugin.getDatabaseHandler().select(
						"SELECT * FROM " + Table.ITEMS.getName() + ";");
				if (rs.getResult().next())
				{
					do
					{
						final int id = rs.getResult().getInt("itemid");
						final int amount = rs.getResult().getInt("amount");
						final byte data = rs.getResult().getByte("data");
						final short dur = rs.getResult().getShort("durability");
						final String enchantments = rs.getResult().getString(
								"enchantments");
						final String itemGroups = rs.getResult().getString(
								"groups");
						itemList.add(new ZeroPointTwoSixTwoItemObject(id,
								amount, data, dur, enchantments, itemGroups));
					} while (rs.getResult().next());
				}
			}
			catch (SQLException sql)
			{
				plugin.getLogger().warning("SQL Exception");
				sql.printStackTrace();
			}
			// Drop previous table
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.ITEMS.getName() + ";");
			// Recreate table
			if (plugin.getPluginConfig().useMySQL)
			{
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE "
										+ Table.ITEMS.getName()
										+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, itemid SMALLINT UNSIGNED, amount INT NOT NULL, data TINYTEXT, durability TINYTEXT, enchantments TEXT, groups TEXT NOT NULL, PRIMARY KEY (id));");
			}
			else
			{
				plugin.getDatabaseHandler()
						.createTable(
								"CREATE TABLE "
										+ Table.ITEMS.getName()
										+ " (id INTEGER PRIMARY KEY, itemid SMALLINT UNSIGNED,amount INT NOT NULL,data TEXT,durability TEXT,enchantments TEXT, groups TEXT NOT NULL);");
			}
			// Add them back in
			for (ZeroPointTwoSixTwoItemObject item : itemList)
			{
				final int groupid = plugin.getDatabaseHandler().getGroupId(
						item.groups);
				query = "INSERT INTO " + Table.ITEMS.getName()
						+ " (itemid,amount,data,durability,groups) VALUES ('"
						+ item.itemid + "','" + item.amount + "','" + item.data
						+ "','" + item.enchantments + "','" + groupid + "');";
				if (item.enchantments != null)
				{
					if (!item.enchantments.equalsIgnoreCase("null"))
					{
						// Order
						/**
						 * http://stackoverflow.com/questions/922528/how-to-sort-map-values-by-key-in-java
						 */
						final Map<ComparableEnchantment, Integer> map = new HashMap<ComparableEnchantment, Integer>();
						TreeSet<ComparableEnchantment> keys = new TreeSet<ComparableEnchantment>(
								map.keySet());
						String[] cut = item.enchantments.split("i");
						for (int i = 0; i < cut.length; i++)
						{
							try
							{
								//Attempt to recover as many of the enchantments
								String[] cutter = cut[i].split("v");
								EnchantmentWrapper e = new EnchantmentWrapper(
										Integer.parseInt(cutter[0]));
								map.put(new ComparableEnchantment(e
										.getEnchantment()), Integer
										.parseInt(cutter[1]));
							}
							catch (NumberFormatException n)
							{
								// INGORE
							}
						}
						StringBuilder sb = new StringBuilder();
						for (ComparableEnchantment key : keys)
						{
							sb.append(key.getId() + "v"
									+ map.get(key).intValue() + "i");
						}
						try
						{
							sb.deleteCharAt(sb.length() - 1);
							item.enchantments = sb.toString();
							query = "INSERT INTO "
									+ Table.ITEMS.getName()
									+ " (itemid,amount,data,durability,enchantments,groups) VALUES ('"
									+ item.itemid + "','" + item.amount + "','"
									+ item.data + "','" + item.durability
									+ "','" + item.enchantments + "','"
									+ groupid + "');";
						}
						catch (StringIndexOutOfBoundsException s)
						{
							// IGNORE
						}
					}
				}
				plugin.getDatabaseHandler().standardQuery(query);
			}
			// Set old config options to new format
			plugin.getConfig().set("karma.upper.limit",
					plugin.getConfig().getInt("karma.upperlimit", 200));
			plugin.getConfig().set("karma.lower.limit",
					plugin.getConfig().getInt("karma.lowerlimit", -200));
			plugin.getConfig().set("karma.lower.percent",
					plugin.getConfig().getDouble("karma.upperPercent", 0.85));
			plugin.getConfig().set("karma.lower.percent",
					plugin.getConfig().getDouble("karma.lowerPercent", 0.15));
			// Remove old entries
			plugin.getConfig().set("karma.upperlimit", null);
			plugin.getConfig().set("karma.lowerlimit", null);
			plugin.getConfig().set("karma.upperPercent", null);
			plugin.getConfig().set("karma.lowerPercent", null);
			plugin.saveConfig();
			plugin.getPluginConfig().reloadConfig();
		}
		// Update version number in config.yml
		plugin.getConfig().set("version", plugin.getDescription().getVersion());
		plugin.saveConfig();
		plugin.getLogger().info("Upgrade complete");
	}

	static class ZeroPointFourteenItemObject
	{
		public int itemid, amount;
		public byte data;
		public short durability;
		public String enchantments;

		public ZeroPointFourteenItemObject(int id, int quantity, byte dv,
				short dur, String en)
		{
			this.itemid = id;
			this.amount = quantity;
			this.data = dv;
			this.durability = dur;
			this.enchantments = en;
		}
	}

	static class ZeroPointTwoSixTwoPlayerObject
	{
		public String playername, groups;
		public int karma;

		public ZeroPointTwoSixTwoPlayerObject(String playername, int karma,
				String groups)
		{
			this.playername = playername;
			this.karma = karma;
			this.groups = groups;
		}
	}

	static class ZeroPointTwoSixTwoItemObject extends
			ZeroPointFourteenItemObject
	{
		public String groups;

		public ZeroPointTwoSixTwoItemObject(int id, int quantity, byte dv,
				short dur, String en, String groups)
		{
			super(id, quantity, dv, dur, en);
			this.groups = groups;
		}
	}
}
