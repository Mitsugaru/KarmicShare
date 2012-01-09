/**
 * Config file mimicking DiddiZ's Config class
 * file in LB. Tailored for this plugin.
 *
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {
	// Class variables
	private KarmicShare plugin;
	/* public String url, user, password; */
	public boolean /* useMySQL, */statickarma, effects, debugTime, karmaDisabled, chests;
	public int upper, lower, listlimit, playerKarmaDefault, karmaChange;
	public double upperPercent, lowerPercent;
	public final Map<Item, Integer> karma = new HashMap<Item, Integer>();

	// TODO ability to change config in-game

	// IDEA Ability to change the colors for all parameters
	// such as item name, amount, data value, id value, enchantment name,
	// enchantment lvl, page numbers, maybe even header titles
	/**
	 * Constructor and initializer
	 *
	 * @param KarmicShare plugin
	 */
	public Config(KarmicShare ks) {
		plugin = ks;
		// Grab config
		ConfigurationSection config = ks.getConfig();
		// Hashmap of defaults
		final Map<String, Object> defaults = new HashMap<String, Object>();
		defaults.put("version", ks.getDescription().getVersion());
		/*
		 * defaults.put("mysql.host", "localhost"); defaults.put("mysql.port",
		 * 3306); defaults.put("mysql.database", "minecraft");
		 * defaults.put("mysql.user","username");
		 * defaults.put("mysql.password","pass"); defaults.put("useMySQL",
		 * false);
		 */
		defaults.put("karma.upperlimit", 200);
		defaults.put("karma.upperPercent", 0.85);
		defaults.put("karma.lowerlimit", -200);
		defaults.put("karma.lowerPercent", 0.15);
		defaults.put("karma.playerDefault", 0);
		defaults.put("karma.changeDefault", 1);
		defaults.put("karma.static", false);
		defaults.put("karma.disabled", false);
		defaults.put("effects", true);
		defaults.put("listlimit", 10);
		defaults.put("chests", true);
		// Insert defaults into config file if they're not present
		for (final Entry<String, Object> e : defaults.entrySet())
		{
			if (!config.contains(e.getKey()))
			{
				config.set(e.getKey(), e.getValue());
			}
		}
		// Save config
		ks.saveConfig();
		// Load variables from config
		/*
		 * useMySQL = config.getBoolean("useMySQL", false); url =
		 * "jdbc:mysql://" + config.getString("mysql.host") + ":" +
		 * config.getInt("mysql.port") + "/" +
		 * config.getString("mysql.database"); user =
		 * config.getString("mysql.user"); password =
		 * config.getString("mysql.password");
		 */
		statickarma = config.getBoolean("karma.static", false);
		upper = config.getInt("karma.upperlimit", 200);
		lower = config.getInt("karma.lowerlimit", -200);
		upperPercent = config.getDouble("karma.upperPercent", 0.85);
		lowerPercent = config.getDouble("karma.lowerPercent", 0.15);
		playerKarmaDefault = config.getInt("karma.playerDefault", 0);
		karmaChange = config.getInt("karma.changeDefault", 1);
		effects = config.getBoolean("effects", true);
		chests = config.getBoolean("chests", true);
		listlimit = config.getInt("listlimit", 10);
		debugTime = config.getBoolean("debugTime", false);
		karmaDisabled = config.getBoolean("karma.disabled", false);
		// Load config for item specific karma if not using static karma
		if (!statickarma && !karmaDisabled)
		{
			this.loadKarmaMap();
		}
		// Finally, do a bounds check on parameters to make sure they are legal
	}

	/**
	 * Loads the per-item karma values into a hashmap
	 * for later usage
	 */
	private void loadKarmaMap() {
		// Load karma file
		final YamlConfiguration karmaFile = this.karmaFile();
		// Load custom karma file into map
		for (final String entry : karmaFile.getKeys(false))
		{
			try
			{
				// Attempt to parse the nodes
				int key = Integer.parseInt(entry);
				//If it has child nodes, parse those as well
				if (karmaFile.isConfigurationSection(entry))
				{
					ConfigurationSection sec = karmaFile
							.getConfigurationSection(entry);
					for (final String dataValue : sec.getKeys(false))
					{
						int secondKey = Integer.parseInt(dataValue);
						int secondValue = sec.getInt(dataValue);
						if(key!=373)
						{
							karma.put(
									new Item(key, Byte.parseByte("" + secondKey), (short) secondKey),
									secondValue);
						}
						else
						{
							karma.put(
									new Item(key, Byte.parseByte("" + 0), (short) secondKey),
									secondValue);
						}
					}
				}
				else
				{
					int value = karmaFile.getInt(entry);
					karma.put(new Item(key, Byte.valueOf("" + 0), (short) 0), value);
				}
			}
			catch (final NumberFormatException ex)
			{
				plugin.getLogger().warning("Non-integer value in karma list");
				ex.printStackTrace();
			}
		}
		plugin.getLogger().info(
				KarmicShare.prefix + " Loaded custom karma values");
	}

	/**
	 * Check if updates are necessary
	 */
	public void checkUpdate()
	{
		// Check if need to update
		ConfigurationSection config = plugin.getConfig();
		if (Double.parseDouble(plugin.getDescription().getVersion()) > Double
				.parseDouble(config.getString("version")))
		{
			// Update to latest version
			plugin.getLogger().info(
					KarmicShare.prefix + " Updating to v"
							+ plugin.getDescription().getVersion());
			this.update();
		}
	}

	/**
	 * This method is called to make the appropriate changes, most likely only
	 * necessary for database schema modification, for a proper update.
	 */
	private void update() {
		//Grab current version
		double ver = Double.parseDouble(plugin.getConfig().getString("version"));
		String query = "";
		//Updates to alpha 0.08
		if(ver < 0.08)
		{
			//Add enchantments column
			plugin.getLogger().info(KarmicShare.prefix + " Altering items table to add enchantments column.");
			query = "ALTER TABLE items ADD enchantments TEXT;";
			plugin.getLiteDB().standardQuery(query);
		}
		if(ver < 0.09)
		{
			//Add back durability column
			plugin.getLogger().info(KarmicShare.prefix + " Altering items table to add durability column.");
			query = "ALTER TABLE items ADD durability TEXT;";
			plugin.getLiteDB().standardQuery(query);
		}
		if(ver < 0.14)
		{
			//Revamp item table
			try
			{
				plugin.getLogger().info(KarmicShare.prefix + " Revamping item table");
				query = "SELECT * FROM items;";
				final List<ZeroPointFourteenItemObject> fourteen = new ArrayList<ZeroPointFourteenItemObject>();
				ResultSet rs = plugin.getLiteDB().select(query);
				if(rs.next())
				{
					do
					{
						String enchantments = rs.getString("enchantments");
						if(!rs.wasNull())
						{
							fourteen.add(new ZeroPointFourteenItemObject(rs.getInt("itemid"), rs.getInt("amount"), rs.getByte("data"), rs.getShort("durability"), enchantments));
						}
						else
						{
							fourteen.add(new ZeroPointFourteenItemObject(rs.getInt("itemid"), rs.getInt("amount"), rs.getByte("data"), rs.getShort("durability"), ""));
						}

					}while(rs.next());
				}
				rs.close();
				//Drop item table
				plugin.getLiteDB().standardQuery("DROP TABLE items;");
				//Create new table
				plugin.getLiteDB().createTable("CREATE TABLE `items` (`id` INTEGER PRIMARY KEY, `itemid` SMALLINT UNSIGNED,`amount` INT,`data` TEXT,`durability` TEXT,`enchantments` TEXT, `groups` TEXT);");
				//Add back items
				for(ZeroPointFourteenItemObject bak : fourteen)
				{
					String fourteenItemQuery = "";
					if(bak.enchantments.equals(""))
					{
						fourteenItemQuery = "INSERT INTO items (itemid,amount,data,durability,groups) VALUES ('" + bak.itemid + "','" + bak.amount + "','" + bak.data + "','" + bak.durability + "','global');";
					}
					else
					{
						fourteenItemQuery = "INSERT INTO items (itemid,amount,data,durability,enchantments,groups) VALUES ('" + bak.itemid + "','" + bak.amount + "','" + bak.data + "','" + bak.durability + "','" + bak.enchantments + "','global');";
					}
					plugin.getLiteDB().standardQuery(fourteenItemQuery);
				}
			}
			catch (SQLException e)
			{
				// INFO Auto-generated catch block
				plugin.getLogger().warning(KarmicShare.prefix + " SQL Exception");
				e.printStackTrace();
			}
			//Add groups to players table
			plugin.getLogger().info(KarmicShare.prefix + " Altering player table to add groups column.");
			query = "ALTER TABLE players ADD groups TEXT;";
			plugin.getLiteDB().standardQuery(query);
			//Add the GLOBAL group
			plugin.getLogger().info(KarmicShare.prefix + " Adding global group to groups table.");
			query = "INSERT INTO groups (groupname) VALUES ('global');";
			plugin.getLiteDB().standardQuery(query);
		}
		// Update version number in config.yml
		plugin.getConfig().set("version", plugin.getDescription().getVersion());
		plugin.saveConfig();
	}

	/**
	 * Reloads info from yaml file(s)
	 */
	public void reloadConfig() {
		// Initial relaod
		plugin.reloadConfig();
		// Grab config
		ConfigurationSection config = plugin.getConfig();
		upper = config.getInt("karma.upperlimit", 200);
		lower = config.getInt("karma.lowerlimit", -200);
		upperPercent = config.getDouble("karma.upperPercent", 0.85);
		lowerPercent = config.getDouble("karma.lowerPercent", 0.15);
		playerKarmaDefault = config.getInt("karma.playerDefault", 0);
		karmaChange = config.getInt("karma.changeDefault", 1);
		effects = config.getBoolean("effects", true);
		chests = config.getBoolean("chests", false);
		listlimit = config.getInt("listlimit", 10);
		debugTime = config.getBoolean("debugTime", false);
		karmaDisabled = config.getBoolean("karma.disabled", false);
		// Load config for item specific karma if not using static karma
		if (!statickarma && !karmaDisabled)
		{
			// Clear old mappings
			karma.clear();
			// Reload karma mappings
			this.loadKarmaMap();
		}
		// Check bounds
		this.boundsCheck();
		plugin.getLogger().info(KarmicShare.prefix + " Config reloaded");
	}

	/**
	 * Check the bounds on the parameters to make sure that
	 * all config variables are legal and usable by the plugin
	 */
	private void boundsCheck() {
		// Check upper and lower limits
		if (upper < lower)
		{
			upper = 200;
			lower = -200;
			plugin.getLogger()
					.warning(
							KarmicShare.prefix
									+ " Upper limit is smaller than lower limit. Reverting to defaults.");
		}
		//Check that we don't go beyond what the database can handle, via smallint
		else if (Math.abs(upper) >= 30000 || Math.abs(lower) >= 30000)
		{
			upper = 200;
			lower = -200;
			plugin.getLogger()
					.warning(
							KarmicShare.prefix
									+ " Upper/lower limit is beyond bounds. Reverting to defaults.");
		}
		// Check percentages
		if (upperPercent < lowerPercent)
		{
			upperPercent = 0.85;
			lowerPercent = 0.15;
			plugin.getLogger()
					.warning(
							KarmicShare.prefix
									+ " Upper %-age is smaller than lower %-age. Reverting to defaults.");
		}
		else if (upperPercent > 1.0 || lowerPercent < 0)
		{
			upperPercent = 0.85;
			lowerPercent = 0.15;
			plugin.getLogger()
					.warning(
							KarmicShare.prefix
									+ " Upper %-age and/or lower %-age are beyond bounds. Reverting to defaults.");
		}
		// Check that the default karma is actually in range.
		if (playerKarmaDefault < lower || playerKarmaDefault > upper)
		{
			// Average out valid bounds to create valid default
			playerKarmaDefault = upper - ((lower + upper) / 2);
			plugin.getLogger()
					.warning(
							KarmicShare.prefix
									+ " Player karma default is out of range. Using average of the two.");
		}
		// Check that default karma change is not negative.
		if (karmaChange < 0)
		{
			karmaChange = 1;
			plugin.getLogger()
					.warning(
							KarmicShare.prefix
									+ " Default karma rate is negative. Using default.");
		}
		// Check that list is actually going to output something, based on limit
		// given
		if (listlimit < 1)
		{
			listlimit = 10;
			plugin.getLogger().warning(
					KarmicShare.prefix
							+ " List limit is lower than 1. Using default.");
		}
	}

	/**
	 * Loads the karma file. Contains default values
	 * If the karma file isn't there, or if its empty,
	 * then load defaults.
	 * @return YamlConfiguration file
	 */
	private YamlConfiguration karmaFile() {
		final File file = new File(plugin.getDataFolder().getAbsolutePath()
				+ "/karma.yml");
		final YamlConfiguration karmaFile = YamlConfiguration
				.loadConfiguration(file);
		if (karmaFile.getKeys(false).isEmpty())
		{
			// TODO all-inclusive defaults
			// Defaults
			karmaFile.set("14", 5);
			karmaFile.set("15", 2);
			karmaFile.set("17.0", 2);
			karmaFile.set("17.1", 2);
			karmaFile.set("17.2", 2);
			karmaFile.set("19", 10);
			karmaFile.set("20", 3);
			karmaFile.set("22", 36);
			karmaFile.set("24", 2);
			karmaFile.set("35.0", 2);
			karmaFile.set("35.1", 2);
			karmaFile.set("35.2", 2);
			karmaFile.set("35.3", 2);
			karmaFile.set("35.4", 2);
			karmaFile.set("35.5", 2);
			karmaFile.set("35.6", 2);
			karmaFile.set("35.7", 2);
			karmaFile.set("35.8", 2);
			karmaFile.set("35.9", 2);
			karmaFile.set("35.10", 2);
			karmaFile.set("35.11", 2);
			karmaFile.set("35.12", 2);
			karmaFile.set("35.13", 2);
			karmaFile.set("35.14", 2);
			karmaFile.set("35.15", 2);
			karmaFile.set("41", 54);
			karmaFile.set("45", 6);
			karmaFile.set("47", 6);
			karmaFile.set("49", 6);
			karmaFile.set("57", 225);
			karmaFile.set("89", 4);
			karmaFile.set("102", 12);
			karmaFile.set("264", 25);
			karmaFile.set("265", 3);
			karmaFile.set("266", 6);
			karmaFile.set("322", 10);
			karmaFile.set("331", 2);
			karmaFile.set("351.4", 4);
			// Insert defaults into config file if they're not present
			try
			{
				// Save the file
				karmaFile.save(file);
			}
			catch (IOException e1)
			{
				// INFO Auto-generated catch block
				plugin.getLogger().warning(
						KarmicShare.prefix
								+ " File I/O Exception on saving karma list");
				e1.printStackTrace();
			}
		}
		return karmaFile;
	}

	static class ZeroPointFourteenItemObject
	{
		public int itemid, amount;
		public byte data;
		public short durability;
		public String enchantments;

		public ZeroPointFourteenItemObject(int id, int quantity, byte dv, short dur, String en)
		{
			itemid = id;
			amount = quantity;
			data = dv;
			durability = dur;
			enchantments = en;
		}
	}
}
