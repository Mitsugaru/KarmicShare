/**
 * KarmicShare CraftBukkit plugin that allows for players to share items via a
 * community pool. Karma system in place so that players cannot leech from the
 * item pool.
 * 
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare;

import java.util.Vector;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mitsugaru.KarmicShare.config.Config;
import com.mitsugaru.KarmicShare.database.DBHandler;
import com.mitsugaru.KarmicShare.listeners.KSBlockListener;
import com.mitsugaru.KarmicShare.listeners.KSInventoryListener;
import com.mitsugaru.KarmicShare.listeners.KSPlayerListener;
import com.mitsugaru.KarmicShare.permissions.PermCheck;
import com.mitsugaru.KarmicShare.questioner.KSQuestion;
import com.mitsugaru.KarmicShare.questioner.KSQuestionsReaper;
import com.mitsugaru.KarmicShare.questioner.KarmicShareQuestionerPlayerListener;

public class KarmicShare extends JavaPlugin
{
	// Class variables
	private DBHandler database;
	public static final String TAG = "[KarmicShare]";
	private Commander commander;
	private Config config;
	private int cleantask;
	public final Vector<KSQuestion> questions = new Vector<KSQuestion>();
	private boolean chest, economyFound;
	private Economy eco;

	// IDEA Score board on karma?
	// TODO Mod commands to remove items

	/**
	 * Method that is called when plugin is disabled
	 */
	@Override
	public void onDisable()
	{
		// Save config
		this.reloadConfig();
		this.saveConfig();
		// Stop cleaner task
		if (cleantask != -1)
		{
			getServer().getScheduler().cancelTask(cleantask);
		}
		// Disconnect from sql database
		if (database.checkConnection())
		{
			// Close connection
			database.close();
		}

	}

	/**
	 * Method that is called when plugin is enabled
	 */
	@Override
	public void onEnable()
	{
		// Config
		config = new Config(this);
		// Database handler
		database = new DBHandler(this, config);
		// Config update
		config.checkUpdate();
		// Initialize permission handler
		PermCheck.init(this);
		// Initialize Karma logic handler
		Karma.init(this);
		// Grab Commander to handle commands
		commander = new Commander(this);
		getCommand("ks").setExecutor(commander);
		// Setup economy
		if (config.economy)
		{
			setupEconomy();
			if (!economyFound)
			{
				getLogger()
						.warning(
								"Economy not setup, but is enabled in config.yml. Reverting to built-in karma system.");
				config.set("karma.useEconomy", false);
				config.reloadConfig();
			}
		}
		// Grab plugin manager
		final PluginManager pm = this.getServer().getPluginManager();
		// Use bundled package of logblockquestioner.
		pm.registerEvents(new KarmicShareQuestionerPlayerListener(questions),
				this);
		this.getServer()
				.getScheduler()
				.scheduleSyncRepeatingTask(this,
						new KSQuestionsReaper(questions), 15000, 15000);
		// Register listeners
		pm.registerEvents(new KSBlockListener(this), this);
		pm.registerEvents(new KSPlayerListener(this), this);
		if (config.chests)
		{
			pm.registerEvents(new KSInventoryListener(this), this);
			chest = true;
		}
		else
		{
			chest = false;
		}
	}

	public Commander getCommander()
	{
		return commander;
	}

	/**
	 * Returns SQLite database
	 * 
	 * @return SQLite database
	 */
	public DBHandler getDatabaseHandler()
	{
		return database;
	}

	/**
	 * Returns Config object
	 * 
	 * @return Config object
	 */
	public Config getPluginConfig()
	{
		return config;
	}

	public String ask(Player respondent, String questionMessage,
			String... answers)
	{
		final KSQuestion question = new KSQuestion(respondent, questionMessage,
				answers);
		questions.add(question);
		return question.ask();
	}

	private void setupEconomy()
	{
		RegisteredServiceProvider<Economy> economyProvider = this.getServer()
				.getServicesManager()
				.getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null)
		{
			eco = economyProvider.getProvider();
			economyFound = true;
		}
		else
		{
			// No economy system found, disable
			getLogger().warning(TAG + " No economy found!");
			this.getServer().getPluginManager().disablePlugin(this);
			economyFound = false;
		}
	}

	public boolean useChest()
	{
		return chest;
	}

	public Economy getEconomy()
	{
		return eco;
	}
	
	/**
	 * Colorizes a given string to Bukkit standards
	 * 
	 * http://forums.bukkit.org/threads/methode-to-colorize.69543/#post-1063437
	 * 
	 * @param string
	 * @return String with appropriate Bukkit ChatColor in them
	 * @author AmberK
	 */
	public static String colorizeText(String string)
	{
		/**
		 * Colors
		 */
		string = string.replaceAll("&0", "" + ChatColor.BLACK);
		string = string.replaceAll("&1", "" + ChatColor.DARK_BLUE);
		string = string.replaceAll("&2", "" + ChatColor.DARK_GREEN);
		string = string.replaceAll("&3", "" + ChatColor.DARK_AQUA);
		string = string.replaceAll("&4", "" + ChatColor.DARK_RED);
		string = string.replaceAll("&5", "" + ChatColor.DARK_PURPLE);
		string = string.replaceAll("&6", "" + ChatColor.GOLD);
		string = string.replaceAll("&7", "" + ChatColor.GRAY);
		string = string.replaceAll("&8", "" + ChatColor.DARK_GRAY);
		string = string.replaceAll("&9", "" + ChatColor.BLUE);
		string = string.replaceAll("&a", "" + ChatColor.GREEN);
		string = string.replaceAll("&b", "" + ChatColor.AQUA);
		string = string.replaceAll("&c", "" + ChatColor.RED);
		string = string.replaceAll("&d", "" + ChatColor.LIGHT_PURPLE);
		string = string.replaceAll("&e", "" + ChatColor.YELLOW);
		string = string.replaceAll("&f", "" + ChatColor.WHITE);
		/**
		 * Formatting
		 */
		string = string.replaceAll("&k", "" + ChatColor.MAGIC);
		string = string.replaceAll("&l", "" + ChatColor.BOLD);
		string = string.replaceAll("&m", "" + ChatColor.STRIKETHROUGH);
		string = string.replaceAll("&n", "" + ChatColor.UNDERLINE);
		string = string.replaceAll("&o", "" + ChatColor.ITALIC);
		string = string.replaceAll("&r", "" + ChatColor.RESET);
		return string;
	}
}