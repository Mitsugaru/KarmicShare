package com.mitsugaru.KarmicShare.database;

import java.sql.SQLException;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.SQLibrary.MySQL;
import com.mitsugaru.KarmicShare.SQLibrary.SQLite;
import com.mitsugaru.KarmicShare.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.config.Config;


public class DatabaseHandler
{
	// Class Variables
	private KarmicShare plugin;
	private Config config;
	private SQLite sqlite;
	private MySQL mysql;
	private boolean useMySQL;

	public DatabaseHandler(KarmicShare ks, Config conf)
	{
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

	private void checkTables()
	{
		if (useMySQL)
		{
			// Connect to mysql database
			mysql = new MySQL(plugin.getLogger(), KarmicShare.TAG, config.host,
					config.port, config.database, config.user, config.password);
			// Check if item table exists
			if (!mysql.checkTable(Table.ITEMS.getName()))
			{
				plugin.getLogger().info("Created item table");
				mysql.createTable("CREATE TABLE "
						+ Table.ITEMS.getName()
						+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, itemid SMALLINT UNSIGNED, amount INT NOT NULL, data TINYTEXT, durability TINYTEXT, enchantments TEXT, groups TEXT NOT NULL, PRIMARY KEY (id));");
			}
			// Check if player table exists
			if (!mysql.checkTable(Table.PLAYERS.getName()))
			{
				plugin.getLogger().info("Created players table");
				mysql.createTable("CREATE TABLE "
						+ Table.PLAYERS.getName()
						+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL,karma INT NOT NULL, groups TEXT, UNIQUE (playername), PRIMARY KEY (id));");
			}
			// Check if group table exists
			if (!mysql.checkTable(Table.GROUPS.getName()))
			{
				// TODO need to record creator and managers
				// TODO group settings
				plugin.getLogger().info("Created groups table");
				mysql.createTable("CREATE TABLE "
						+ Table.GROUPS.getName()
						+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, groupname varchar(32) NOT NULL, UNIQUE (groupname), PRIMARY KEY (id));");
				mysql.standardQuery("INSERT INTO " + Table.GROUPS.getName()
						+ " (groupname) VALUES ('global');");
			}
		}
		else
		{
			// Connect to sql database
			sqlite = new SQLite(plugin.getLogger(), KarmicShare.TAG, "pool",
					plugin.getDataFolder().getAbsolutePath());
			// Check if item table exists
			if (!sqlite.checkTable(Table.ITEMS.getName()))
			{
				plugin.getLogger().info("Created item table");
				sqlite.createTable("CREATE TABLE "
						+ Table.ITEMS.getName()
						+ " (id INTEGER PRIMARY KEY, itemid SMALLINT UNSIGNED,amount INT NOT NULL,data TEXT,durability TEXT,enchantments TEXT, groups TEXT NOT NULL);");
			}
			// Check if player table exists
			if (!sqlite.checkTable(Table.PLAYERS.getName()))
			{
				plugin.getLogger().info("Created player table");
				sqlite.createTable("CREATE TABLE "
						+ Table.PLAYERS.getName()
						+ " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL,karma INT NOT NULL, groups TEXT, UNIQUE (playername));");
			}
			// Check if groups table exists
			if (!sqlite.checkTable(Table.GROUPS.getName()))
			{
				plugin.getLogger().info("Created groups table");
				sqlite.createTable("CREATE TABLE "
						+ Table.GROUPS.getName()
						+ " (id INTEGER PRIMARY KEY, groupname TEXT NOT NULL, UNIQUE (groupname));");
				sqlite.standardQuery("INSERT INTO " + Table.GROUPS.getName()
						+ " (groupname) VALUES ('global');");
			}
		}
	}

	private void importSQL()
	{
		// Connect to sql database
		try
		{
			StringBuilder sb = new StringBuilder();
			// Grab local SQLite database
			sqlite = new SQLite(plugin.getLogger(), KarmicShare.TAG, "pool",
					plugin.getDataFolder().getAbsolutePath());
			// Copy items
			Query query = sqlite.select("SELECT * FROM "
					+ Table.ITEMS.getName() + ";");
			if (query.getResult().next())
			{
				plugin.getLogger().info("Importing items...");
				do
				{
					boolean hasData = false;
					boolean hasDurability = false;
					boolean hasEnchantments = false;
					final int id = query.getResult().getInt("itemid");
					final int amount = query.getResult().getInt("amount");
					byte data = query.getResult().getByte("data");
					if (!query.getResult().wasNull())
					{
						hasData = true;
					}
					short dur = query.getResult().getShort("durability");
					if (!query.getResult().wasNull())
					{
						hasDurability = true;
					}
					final String enchantments = query.getResult().getString(
							"enchantments");
					if (!query.getResult().wasNull())
					{
						hasEnchantments = true;
					}
					final String groups = query.getResult().getString("groups");
					sb.append("INSERT INTO " + Table.ITEMS.getName()
							+ " (itemid,amount");
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
					final String send = sb.toString();
					mysql.standardQuery(send);
					sb = new StringBuilder();
				} while (query.getResult().next());
			}
			query.closeQuery();
			sb = new StringBuilder();
			// Copy players
			query = sqlite.select("SELECT * FROM " + Table.PLAYERS.getName()
					+ ";");
			if (query.getResult().next())
			{
				plugin.getLogger().info("Importing players...");
				do
				{
					boolean hasGroups = false;
					final String player = query.getResult().getString(
							"playername");
					final int karma = query.getResult().getInt("karma");
					final String groups = query.getResult().getString("groups");
					if (!query.getResult().wasNull())
					{
						hasGroups = true;
					}
					sb.append("INSERT INTO " + Table.PLAYERS.getName()
							+ " (playername,karma");
					if (hasGroups)
					{
						sb.append(",groups");
					}
					sb.append(") VALUES('" + player + "','" + karma + "'");
					if (hasGroups)
					{
						sb.append(",'" + groups + "'");
					}
					sb.append(");");
					final String send = sb.toString();
					mysql.standardQuery(send);
					sb = new StringBuilder();
				} while (query.getResult().next());
			}
			query.closeQuery();
			sb = new StringBuilder();
			// Copy groups
			query = sqlite.select("SELECT * FROM " + Table.GROUPS.getName()
					+ ";");
			if (query.getResult().next())
			{
				plugin.getLogger().info("Importing groups...");
				do
				{
					final String send = "INSERT INTO " + Table.GROUPS.getName()
							+ " (id, groupname) VALUES('"
							+ query.getResult().getInt("id") + "','"
							+ query.getResult().getString("groupname") + "');";
					mysql.standardQuery(send);
				} while (query.getResult().next());
			}
			query.closeQuery();
			plugin.getLogger().info("Done importing SQLite into MySQL");
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning("SQL Exception on Import");
			e.printStackTrace();
		}
	}

	public boolean checkConnection()
	{
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

	public void close()
	{
		if (useMySQL)
		{
			mysql.close();
		}
		else
		{
			sqlite.close();
		}
	}

	public Query select(String query)
	{
		if (useMySQL)
		{
			return mysql.select(query);
		}
		else
		{
			return sqlite.select(query);
		}
	}

	public void standardQuery(String query)
	{
		if (useMySQL)
		{
			mysql.standardQuery(query);
		}
		else
		{
			sqlite.standardQuery(query);
		}
	}

	public void createTable(String query)
	{
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
