package com.mitsugaru.KarmicShare;

import java.sql.ResultSet;
import java.sql.SQLException;

import lib.PatPeter.SQLibrary.MySQL;
import lib.PatPeter.SQLibrary.SQLite;

public class DBHandler {
	// Class Variables
	private KarmicShare plugin;
	private Config config;
	private SQLite sqlite;
	private MySQL mysql;
	private boolean useMySQL;

	public DBHandler(KarmicShare ks, Config conf) {
		plugin = ks;
		config = conf;
		useMySQL = config.useMySQL;
		checkTables();
		if (config.importSQL)
		{
			if (useMySQL)
			{
				importSQL();
			}
			config.set("mysql.import", false);
		}
	}

	private void checkTables() {
		if (useMySQL)
		{
			// Connect to mysql database
			mysql = new MySQL(plugin.getLogger(), KarmicShare.prefix,
					config.host, config.port, config.database, config.user,
					config.password);
			// Check if item table exists
			if (!mysql.checkTable(config.tablePrefix + "items"))
			{
				plugin.getLogger().info(
						KarmicShare.prefix + " Created item table");
				mysql.createTable("CREATE TABLE `"
						+ config.tablePrefix
						+ "items` (`id` INT UNSIGNED NOT NULL AUTO_INCREMENT, `itemid` SMALLINT UNSIGNED, `amount` INT NOT NULL, `data` TINYTEXT, `durability` TINYTEXT, `enchantments` TEXT, `groups` TINYTEXT NOT NULL, PRIMARY KEY (id));");
			}
			// Check if player table exists
			if (!mysql.checkTable(config.tablePrefix + "players"))
			{
				plugin.getLogger().info(
						KarmicShare.prefix + " Created players table");
				mysql.createTable("CREATE TABLE `"
						+ config.tablePrefix
						+ "players` (`playername` varchar(32) NOT NULL,`karma` INT NOT NULL, `groups` TEXT, UNIQUE (`playername`));");
			}
			// Check if group table exists
			if (!mysql.checkTable(config.tablePrefix + "groups"))
			{
				plugin.getLogger().info(
						KarmicShare.prefix + " Created groups table");
				mysql.createTable("CREATE TABLE `"
						+ config.tablePrefix
						+ "groups` (`groupname` varchar(32) NOT NULL, UNIQUE (`groupname`));");
			}
		}
		else
		{
			// Connect to sql database
			sqlite = new SQLite(plugin.getLogger(), KarmicShare.prefix, "pool",
					plugin.getDataFolder().getAbsolutePath());
			// Check if item table exists
			if (!sqlite.checkTable(config.tablePrefix + "items"))
			{
				plugin.getLogger().info(
						KarmicShare.prefix + " Created item table");
				sqlite.createTable("CREATE TABLE `"
						+ config.tablePrefix
						+ "items` (`id` INTEGER PRIMARY KEY, `itemid` SMALLINT UNSIGNED,`amount` INT NOT NULL,`data` TEXT,`durability` TEXT,`enchantments` TEXT, `groups` TEXT NOT NULL);");
			}
			// Check if player table exists
			if (!sqlite.checkTable(config.tablePrefix + "players"))
			{
				plugin.getLogger().info(
						KarmicShare.prefix + " Created player table");
				sqlite.createTable("CREATE TABLE `"
						+ config.tablePrefix
						+ "players` (`playername` varchar(32) NOT NULL,`karma` INT NOT NULL, `groups` TEXT, UNIQUE (`playername`));");
			}
			// Check if grups table exists
			if (!sqlite.checkTable(config.tablePrefix + "groups"))
			{
				plugin.getLogger().info(
						KarmicShare.prefix + " Created groups table");
				sqlite.createTable("CREATE TABLE `"
						+ config.tablePrefix
						+ "groups` (`groupname` TEXT NOT NULL, UNIQUE (`groupname`));");
			}
		}
	}

	private void importSQL() {
		// Connect to sql database
		try
		{
			StringBuilder sb = new StringBuilder();
			// Grab local SQLite database
			sqlite = new SQLite(plugin.getLogger(), KarmicShare.prefix, "pool",
					plugin.getDataFolder().getAbsolutePath());
			// Copy items
			ResultSet rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "items;");
			if (rs.next())
			{
				plugin.getLogger().info(KarmicShare.prefix + " Importing items...");
				do
				{
					boolean hasData = false;
					boolean hasDurability = false;
					boolean hasEnchantments = false;
					final int id = rs.getInt("itemid");
					final int amount = rs.getInt("amount");
					byte data = rs.getByte("data");
					if (!rs.wasNull())
					{
						hasData = true;
					}
					short dur = rs.getShort("durability");
					if (!rs.wasNull())
					{
						hasDurability = true;
					}
					final String enchantments = rs.getString("enchantments");
					if (!rs.wasNull())
					{
						hasEnchantments = true;
					}
					final String groups = rs.getString("groups");
					sb.append("INSERT INTO " + config.tablePrefix
							+ "items (itemid,amount");
					if (hasData)
					{
						sb.append(",data");
					}
					if (hasDurability)
					{
						sb.append(",durability");
					}
					if (hasEnchantments)
					{
						sb.append(",enchantments");
					}
					sb.append(",groups) VALUES('" + id + "','" + amount + "','");
					if (hasData)
					{
						sb.append(data + "','");
					}
					if (hasDurability)
					{
						sb.append(dur + "','");
					}
					if (hasEnchantments)
					{
						sb.append(enchantments + "','");
					}
					sb.append(groups + "');");
					final String query = sb.toString();
					mysql.standardQuery(query);
					sb = new StringBuilder();
				}
				while (rs.next());
			}
			rs.close();
			sb = new StringBuilder();
			// Copy players
			rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "players;");
			if(rs.next())
			{
				plugin.getLogger().info(KarmicShare.prefix + " Importing players...");
				do
				{
					boolean hasGroups = false;
					final String player = rs.getString("playername");
					final int karma = rs.getInt("karma");
					final String groups = rs.getString("groups");
					if(!rs.wasNull())
					{
						hasGroups = true;
					}
					sb.append("INSERT INTO " + config.tablePrefix
							+ "players (playername,karma");
					if(hasGroups)
					{
						sb.append(",groups");
					}
					sb.append(") VALUES('" + player + "','" + karma + "'");
					if(hasGroups)
					{
						sb.append(",'" + groups + "'");
					}
					sb.append(");");
					final String query = sb.toString();
					mysql.standardQuery(query);
					sb = new StringBuilder();
				}while(rs.next());
			}
			rs.close();
			sb = new StringBuilder();
			// Copy groups
			rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "groups;");
			if(rs.next())
			{
				plugin.getLogger().info(KarmicShare.prefix + " Importing groups...");
				do
				{
					final String query = "INSERT INTO " + config.tablePrefix + "groups (groupname) VALUES('" + rs.getString("groupname") + "');";
					mysql.standardQuery(query);
				}while(rs.next());
			}
			rs.close();
			plugin.getLogger().info(KarmicShare.prefix + " Done importing SQLite into MySQL");
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					KarmicShare.prefix + " SQL Exception on Import");
			e.printStackTrace();
		}

	}

	public boolean checkConnection() {
		boolean connected = false;
		if (useMySQL)
		{
			connected = mysql.checkConnection();
		}
		else
		{
			connected = sqlite.checkConnection();
		}
		return connected;
	}

	public void close() {
		if (useMySQL)
		{
			mysql.close();
		}
		else
		{
			sqlite.close();
		}
	}

	public ResultSet select(String query) {
		if (useMySQL)
		{
			return mysql.select(query);
		}
		else
		{
			return sqlite.select(query);
		}
	}

	public void standardQuery(String query) {
		if (useMySQL)
		{
			mysql.standardQuery(query);
		}
		else
		{
			sqlite.standardQuery(query);
		}
	}

	public void createTable(String query) {
		if (useMySQL)
		{
			mysql.createTable(query);
		}
		else
		{
			sqlite.createTable(query);
		}
	}
}
