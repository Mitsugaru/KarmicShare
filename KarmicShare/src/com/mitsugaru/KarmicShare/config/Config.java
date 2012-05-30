/**
 * Config file mimicking DiddiZ's Config class file in LB. Tailored for this
 * plugin.
 * 
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.inventory.Item;

public class Config
{
	// Class variables
	private KarmicShare plugin;
	public String host, port, database, user, password;
	public static String tablePrefix;
	public static boolean debugDatabase;
	public boolean useMySQL, statickarma, effects, debugTime, karmaDisabled,
			chests, importSQL, economy, blacklist;
	public int upper, lower, listlimit, playerKarmaDefault, karmaChange;
	public double upperPercent, lowerPercent;
	public final Map<Item, Integer> karma = new HashMap<Item, Integer>();
	public final Set<String> disabledWorlds = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

	// TODO ability to change config in-game

	// IDEA Ability to change the colors for all parameters
	// such as item name, amount, data value, id value, enchantment name,
	// enchantment lvl, page numbers, maybe even header titles
	/**
	 * Constructor and initializer
	 * 
	 * @param KarmicShare
	 *            plugin
	 */
	public Config(KarmicShare ks)
	{
		plugin = ks;
		// Grab config
		final ConfigurationSection config = ks.getConfig();
		// Hashmap of defaults
		final Map<String, Object> defaults = new LinkedHashMap<String, Object>();
		defaults.put("chests", true);
		defaults.put("disabledWorlds", new ArrayList<String>());
		defaults.put("effects", true);
		defaults.put("listlimit", 10);
		defaults.put("karma.static", false);
		defaults.put("karma.disabled", false);
		defaults.put("karma.upper.limit", 200);
		defaults.put("karma.upper.percent", 0.85);
		defaults.put("karma.lower.limit", -200);
		defaults.put("karma.lower.percent", 0.15);
		defaults.put("karma.playerDefault", 0);
		defaults.put("karma.changeDefault", 1);
		defaults.put("karma.useEconomy", false);
		defaults.put("mysql.use", false);
		defaults.put("mysql.host", "localhost");
		defaults.put("mysql.port", 3306);
		defaults.put("mysql.database", "minecraft");
		defaults.put("mysql.user", "username");
		defaults.put("mysql.password", "pass");
		defaults.put("mysql.tablePrefix", "ks_");
		defaults.put("mysql.import", false);
		defaults.put("debug.time", false);
		defaults.put("debug.database", false);
		defaults.put("version", ks.getDescription().getVersion());
		// TODO defaults.put("blacklist", false);
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
		/**
		 * Database info
		 */
		useMySQL = config.getBoolean("mysql.use", false);
		host = config.getString("mysql.host", "localhost");
		port = config.getString("mysql.port", "3306");
		database = config.getString("mysql.database", "minecraft");
		user = config.getString("mysql.user", "user");
		password = config.getString("mysql.password", "password");
		tablePrefix = config.getString("mysql.prefix", "ks_");
		importSQL = config.getBoolean("mysql.import", false);
		statickarma = config.getBoolean("karma.static", false);
		//reload to load other settings
		reloadConfig();
	}

	public void set(String path, Object o)
	{
		final ConfigurationSection config = plugin.getConfig();
		config.set(path, o);
		plugin.saveConfig();
	}

	/**
	 * Loads the per-item karma values into a hashmap for later usage
	 */
	private void loadKarmaMap()
	{
		// Load karma file
		final YamlConfiguration karmaFile = this.karmaFile();
		// Load custom karma file into map
		for (final String entry : karmaFile.getKeys(false))
		{
			try
			{
				// Attempt to parse the nodes
				int key = Integer.parseInt(entry);
				// If it has child nodes, parse those as well
				if (karmaFile.isConfigurationSection(entry))
				{
					ConfigurationSection sec = karmaFile
							.getConfigurationSection(entry);
					for (final String dataValue : sec.getKeys(false))
					{
						int secondKey = Integer.parseInt(dataValue);
						int secondValue = sec.getInt(dataValue);
						if (key != 373)
						{
							karma.put(
									new Item(key, Byte
											.parseByte("" + secondKey),
											(short) secondKey), secondValue);
						}
						else
						{
							karma.put(new Item(key, Byte.parseByte("" + 0),
									(short) secondKey), secondValue);
						}
					}
				}
				else
				{
					int value = karmaFile.getInt(entry);
					karma.put(new Item(key, Byte.valueOf("" + 0), (short) 0),
							value);
				}
			}
			catch (final NumberFormatException ex)
			{
				plugin.getLogger().warning("Non-integer value for: " + entry);
				ex.printStackTrace();
			}
		}
		plugin.getLogger().info("Loaded custom karma values");
	}

	private void loadBlacklist()
	{
		// final YamlConfiguration blacklistFile = blacklistFile();
		// Load info into set
		// TODO test
		// final List<String> list = (List<String>)
		// blacklistFile.getList("blacklist", new ArrayList<String>());
	}

	/**
	 * Reloads info from yaml file(s)
	 */
	public void reloadConfig()
	{
		// Initial relaod
		plugin.reloadConfig();
		// Grab config
		loadSettings(plugin.getConfig());
		// Load config for item specific karma if not using static karma
		if (!statickarma && !karmaDisabled)
		{
			// Clear old mappings
			karma.clear();
			// Reload karma mappings
			this.loadKarmaMap();
		}
		if (blacklist)
		{
			this.loadBlacklist();
		}
		// Check bounds
		this.boundsCheck();
	}
	
	private void loadSettings(ConfigurationSection config)
	{
		upper = config.getInt("karma.upper.limit", 200);
		lower = config.getInt("karma.lower.limit", -200);
		upperPercent = config.getDouble("karma.upper.percent", 0.85);
		lowerPercent = config.getDouble("karma.lower.percent", 0.15);
		playerKarmaDefault = config.getInt("karma.playerDefault", 0);
		karmaChange = config.getInt("karma.changeDefault", 1);
		effects = config.getBoolean("effects", true);
		chests = config.getBoolean("chests", false);
		listlimit = config.getInt("listlimit", 10);
		debugTime = config.getBoolean("debug.time", false);
		debugDatabase = config.getBoolean("debug.database", false);
		karmaDisabled = config.getBoolean("karma.disabled", false);
		economy = config.getBoolean("karma.useEconomy", false);
		blacklist = config.getBoolean("blacklist", false);
		/**
		 * Disabled worlds
		 */
		final List<String> worlds = config.getStringList("disabledWorlds");
		if(worlds != null && !worlds.isEmpty())
		{
			disabledWorlds.addAll(worlds);
		}
	}

	/**
	 * Check the bounds on the parameters to make sure that all config variables
	 * are legal and usable by the plugin
	 */
	private void boundsCheck()
	{
		// Check upper and lower limits
		if (upper < lower)
		{
			upper = 200;
			lower = -200;
			plugin.getLogger()
					.warning(
							"Upper limit is smaller than lower limit. Reverting to defaults.");
		}
		// Check that we don't go beyond what the database can handle, via
		// smallint
		else if (Math.abs(upper) >= 30000 || Math.abs(lower) >= 30000)
		{
			upper = 200;
			lower = -200;
			plugin.getLogger()
					.warning(
							"Upper/lower limit is beyond bounds. Reverting to defaults.");
		}
		// Check percentages
		if (upperPercent < lowerPercent)
		{
			upperPercent = 0.85;
			lowerPercent = 0.15;
			plugin.getLogger()
					.warning(
							"Upper %-age is smaller than lower %-age. Reverting to defaults.");
		}
		else if (upperPercent > 1.0 || lowerPercent < 0)
		{
			upperPercent = 0.85;
			lowerPercent = 0.15;
			plugin.getLogger()
					.warning(
							"Upper %-age and/or lower %-age are beyond bounds. Reverting to defaults.");
		}
		// Check that the default karma is actually in range.
		if (playerKarmaDefault < lower || playerKarmaDefault > upper)
		{
			// Average out valid bounds to create valid default
			playerKarmaDefault = upper - ((lower + upper) / 2);
			plugin.getLogger()
					.warning(
							"Player karma default is out of range. Using average of the two.");
		}
		// Check that default karma change is not negative.
		if (karmaChange < 0)
		{
			karmaChange = 1;
			plugin.getLogger().warning(
					"Default karma rate is negative. Using default.");
		}
		// Check that list is actually going to output something, based on limit
		// given
		if (listlimit < 1)
		{
			listlimit = 10;
			plugin.getLogger().warning(
					"List limit is lower than 1. Using default.");
		}
	}

	/**
	 * Loads the karma file. Contains default values If the karma file isn't
	 * there, or if its empty, then load defaults.
	 * 
	 * @return YamlConfiguration file
	 */
	private YamlConfiguration karmaFile()
	{
		final File file = new File(plugin.getDataFolder().getAbsolutePath()
				+ "/karma.yml");
		final YamlConfiguration karmaFile = YamlConfiguration
				.loadConfiguration(file);
		if (karmaFile.getKeys(false).isEmpty())
		{
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
				plugin.getLogger().warning(
						"File I/O Exception on saving karma list");
				e1.printStackTrace();
			}
		}
		return karmaFile;
	}

	@SuppressWarnings("unused")
	private YamlConfiguration blacklistFile()
	{
		final File file = new File(plugin.getDataFolder().getAbsolutePath()
				+ "/blacklist.yml");
		final YamlConfiguration blacklistFile = YamlConfiguration
				.loadConfiguration(file);
		if (!file.exists())
		{
			try
			{
				// Save the file
				blacklistFile.save(file);
			}
			catch (IOException e1)
			{
				plugin.getLogger().warning(
						"File I/O Exception on saving blacklist");
				e1.printStackTrace();
			}
		}
		return blacklistFile;
	}
}
