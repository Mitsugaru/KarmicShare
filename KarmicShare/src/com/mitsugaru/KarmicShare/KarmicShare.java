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

import org.bukkit.plugin.java.JavaPlugin;

public class KarmicShare extends JavaPlugin {
	// Class variables
	private SQLite database;
	private Logger syslog;
	private final static String prefix = "[KarmicShare]";
	private Commander commander;
	private Config config;
	private PermCheck perm;

	// IDEA max amount of total items in pool?
	// TODO Enchantments
	// IDEA Score board on karma?
	// TODO Mod commands to remove items

	/**
	 * Method that is called when plugin is disabled
	 */
	@Override
	public void onDisable() {
		// TODO IDK of anything else to do :\
		// maybe clear out memory by setting stuff to null?
		// dunno how safe that is
		//Save config
		this.saveConfig();
		// Disconnect from sql database? Dunno if necessary
		if (database.checkConnection())
		{
			// Close connection
			database.close();
		}
		syslog.info(prefix + " Plugin disabled");

	}

	// TODO implement onLoad() ?
	// Gets called before enable
	// DiddiZ has it so that it does SQL and config
	// As well as self-updater
	@Override
	public void onLoad()
	{
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
			database.createTable("CREATE TABLE `items` (`itemid` SMALLINT UNSIGNED,`amount` INT,`data` TEXT,`enchantments` TEXT);");
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
		//Config update
		config.checkUpdate();

		//Create permission handler
		perm = new PermCheck();

		// Grab Commander to handle commands
		commander = new Commander(this);
		getCommand("ks").setExecutor(commander);

		syslog.info(prefix + " KarmicShare v" + this.getDescription().getVersion() + " enabled");
	}

	public PermCheck getPermissionHandler()
	{
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
	 * Returns the plugin's prefix
	 *
	 * @return String of plugin prefix
	 */
	public String getPluginPrefix() {
		return prefix;
	}

	/**
	 *  Returns SQLite database
	 *
	 *  @return SQLite database
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