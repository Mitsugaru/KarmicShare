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

import java.util.Vector;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KarmicShare extends JavaPlugin {
	// Class variables
	private DBHandler database;
	public static final String prefix = "[KarmicShare]";
	private Commander commander;
	private Config config;
	private PermCheck perm;
	private Karma karma;
	private int cleantask;
	public final Vector<KSQuestion> questions = new Vector<KSQuestion>();
	public boolean hasSpout;

	// IDEA Score board on karma?
	// TODO Mod commands to remove items

	/**
	 * Method that is called when plugin is disabled
	 */
	@Override
	public void onDisable() {
		// Save config
		this.reloadConfig();
		this.saveConfig();
		// Stop cleaner task
		if (cleantask != -1) {
			getServer().getScheduler().cancelTask(cleantask);
		}
		// Disconnect from sql database
		if (database.checkConnection()) {
			// Close connection
			database.close();
		}
		getLogger().info(prefix + " Plugin disabled");

	}

	/**
	 * Method that is called when plugin is enabled
	 */
	@Override
	public void onEnable() {
		// Config
		config = new Config(this);
		// Database handler
		database = new DBHandler(this, config);
		// Config update
		config.checkUpdate();

		// Create permission handler
		perm = new PermCheck(this);

		// Create Karma logic handler
		karma = new Karma(this);

		// Grab Commander to handle commands
		commander = new Commander(this);
		getCommand("ks").setExecutor(commander);

		// Grab plugin manager
		final PluginManager pm = this.getServer().getPluginManager();

		// Use bundled package of logblockquestioner.
		this.getServer()
				.getPluginManager()
				.registerEvents(
						new KarmicShareQuestionerPlayerListener(questions),
						this);
		this.getServer()
				.getScheduler()
				.scheduleSyncRepeatingTask(this,
						new KSQuestionsReaper(questions), 15000, 15000);

		// Generate listeners
		KSBlockListener blockListener = new KSBlockListener(this);
		KSPlayerListener playerListener = new KSPlayerListener(this);
		pm.registerEvents(blockListener, this);
		pm.registerEvents(playerListener, this);
		if (config.chests) {
			// Check for Spout plugin
			if (pm.isPluginEnabled("Spout")) {
				KSInventoryListener invListener = new KSInventoryListener(this);
				pm.registerEvents(invListener, this);
				hasSpout = true;
				getLogger().warning(prefix
						+ " Hooked into Spout.");
			} else {
				hasSpout = false;
				getLogger().warning(prefix
						+ " Spout not found. Cannot use physical chests.");
			}
		}
		// Create cleaner task
		cleantask = getServer().getScheduler().scheduleAsyncRepeatingTask(this,
				new CleanupTask(), 1200, 1200);
		if (cleantask == -1) {
			getLogger().warning(prefix + " Could not create cleaner task.");
		}
		getLogger().info(prefix + " KarmicShare v"
				+ this.getDescription().getVersion() + " enabled");
	}

	public Commander getCommander() {
		return commander;
	}

	public PermCheck getPermissionHandler() {
		return perm;
	}

	/**
	 * Returns SQLite database
	 * 
	 * @return SQLite database
	 */
	public DBHandler getDatabaseHandler() {
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

	public String ask(Player respondent, String questionMessage,
			String... answers) {
		final KSQuestion question = new KSQuestion(respondent, questionMessage,
				answers);
		questions.add(question);
		return question.ask();
	}

	class CleanupTask implements Runnable {

		public CleanupTask() {
		}

		@Override
		public void run() {
			// Drop bad entries
			getDatabaseHandler().standardQuery(
					"DELETE FROM " + config.tablePrefix
							+ "items WHERE amount<='0';");
		}
	}

	public Karma getKarma() {
		return karma;
	}
}