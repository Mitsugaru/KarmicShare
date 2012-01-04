/**
 * KarmicShare
 * CraftBukkit plugin that allows for players to
 * share items via a community pool. Karma system
 * in place so that players cannot leech from the
 * item pool.
 *
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare;

import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KarmicShare extends JavaPlugin {
	// Class variables
	private SQLite database;
	private Logger syslog;
	public static final String prefix = "[KarmicShare]";
	private Commander commander;
	private Config config;
	private PermCheck perm;

	// IDEA Score board on karma?
	// TODO Mod commands to remove items

	/**
	 * Method that is called when plugin is disabled
	 */
	@Override
	public void onDisable() {
		// Save config
		this.saveConfig();
		// Disconnect from sql database? Dunno if necessary
		if (database.checkConnection())
		{
			// Close connection
			database.close();
		}
		syslog.info(prefix + " Plugin disabled");

	}

	@Override
	public void onLoad() {
		// Logger
		syslog = this.getServer().getLogger();
		// Config
		config = new Config(this);
		// TODO MySQL support
		// Connect to sql database
		database = new SQLite(syslog, prefix, "pool", this.getDataFolder()
				.getAbsolutePath());
		// Check if item table exists
		if (!database.checkTable("items"))
		{
			syslog.info(prefix + " Created item table");
			database.createTable("CREATE TABLE `items` (`itemid` SMALLINT UNSIGNED,`amount` INT,`data` TEXT,`durability` TEXT,`enchantments` TEXT);");
		}
		// Check if player table exists
		if (!database.checkTable("players"))
		{
			syslog.info(prefix + " Created player table");
			// Schema: playername, karma
			// Karma works with 0 being neutral, postive and negative :: good
			// and bad.
			// Past certain boundary, do not increase/decrease.
			// Boundary must be within 30000 high or low, as per SMALLINT
			database.createTable("CREATE TABLE `players` (`playername` varchar(32) NOT NULL,`karma` INT NOT NULL,UNIQUE (`playername`));");
		}
	}

	/**
	 * Method that is called when plugin is enabled
	 */
	@Override
	public void onEnable() {
		// Config update
		config.checkUpdate();

		// Create permission handler
		perm = new PermCheck(this);

		// Grab Commander to handle commands
		commander = new Commander(this);
		getCommand("ks").setExecutor(commander);

		// Grab plugin manager
		final PluginManager pm = this.getServer().getPluginManager();

		//Generate listeners
		KSBlockListener blockListener = new KSBlockListener(this);
		KSPlayerListener playerListener = new KSPlayerListener(this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener,
				Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener,
				Priority.Normal, this);
		if (config.chests)
		{
			//Check for Spout plugin
			if (pm.isPluginEnabled("Spout"))
			{
				KSInventoryListener invListener = new KSInventoryListener(this);
				pm.registerEvent(Event.Type.CUSTOM_EVENT, invListener,
						Priority.Normal, this);
			}
			else
			{
				syslog.warning(prefix
						+ " Spout not found. Cannot use physical chests.");
			}
		}
		syslog.info(prefix + " KarmicShare v"
				+ this.getDescription().getVersion() + " enabled");
	}

	public Commander getCommander()
	{
		return commander;
	}

	public PermCheck getPermissionHandler() {
		return perm;
	}

	/**
	 * Returns the console log object
	 *
	 * @return Logger object
	 */
	public Logger getLogger() {
		return syslog;
	}

	/**
	 * Returns SQLite database
	 *
	 * @return SQLite database
	 */
	public SQLite getLiteDB() {
		return database;
	}

	/**
	 * Returns Config object
	 *
	 * @return Config object
	 */
	public Config getPluginConfig() {
		return config;
	}
}