/**
 * KarmicShare CraftBukkit plugin that allows for players to share items via a
 * community pool. Karma system in place so that players cannot leech from the
 * item pool.
 * 
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mitsugaru.KarmicShare.commands.Commander;
import com.mitsugaru.KarmicShare.config.KarmaConfig;
import com.mitsugaru.KarmicShare.config.RootConfig;
import com.mitsugaru.KarmicShare.config.ConfigNode;
import com.mitsugaru.KarmicShare.database.DatabaseHandler;
import com.mitsugaru.KarmicShare.listeners.KSBlockListener;
import com.mitsugaru.KarmicShare.listeners.KSInventoryListener;
import com.mitsugaru.KarmicShare.listeners.KSPlayerListener;
import com.mitsugaru.KarmicShare.logic.ItemLogic;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.permissions.PermissionHandler;
import com.mitsugaru.KarmicShare.update.Update;

public class KarmicShare extends JavaPlugin {
    // Class variables
    private DatabaseHandler database;
    public static final String TAG = "[KarmicShare]";
    private int cleantask;
    private boolean chest, economyFound;
    private Economy eco;

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

    }

    /**
     * Method that is called when plugin is enabled
     */
    @Override
    public void onEnable() {
	// Config
	RootConfig.init(this);
	// Database handler
	database = new DatabaseHandler(this);
	// Initialize permission handler
	PermissionHandler.init(this);
	// Initialize karma values config
	KarmaConfig.init(this);
	// Initialize Karma logic handler
	Karma.init(this);
	// Initialize ItemLogic handler
	ItemLogic.init(this);
	// Updater
	Update.init(this);
	Update.checkUpdate();
	// Grab Commander to handle commands
	getCommand("ks").setExecutor(new Commander(this));
	// Setup economy
	if (RootConfig.getBoolean(ConfigNode.KARMA_ECONOMY)) {
	    setupEconomy();
	    if (!economyFound) {
		getLogger()
			.warning(
				"Economy not setup, but is enabled in config.yml. Reverting to built-in karma system.");
		RootConfig.set("karma.useEconomy", false);
		RootConfig.reload();
	    }
	}
	// Grab plugin manager
	final PluginManager pm = this.getServer().getPluginManager();
	// Register listeners
	pm.registerEvents(new KSBlockListener(this), this);
	pm.registerEvents(new KSPlayerListener(this), this);
	if (RootConfig.getBoolean(ConfigNode.CHESTS)) {
	    pm.registerEvents(new KSInventoryListener(this), this);
	    chest = true;
	} else {
	    chest = false;
	}
    }

    /**
     * Returns SQLite database
     * 
     * @return SQLite database
     */
    public DatabaseHandler getDatabaseHandler() {
	return database;
    }

    private void setupEconomy() {
	RegisteredServiceProvider<Economy> economyProvider = this.getServer()
		.getServicesManager()
		.getRegistration(net.milkbowl.vault.economy.Economy.class);
	if (economyProvider != null) {
	    eco = economyProvider.getProvider();
	    economyFound = true;
	} else {
	    // No economy system found, disable
	    getLogger().warning(TAG + " No economy found!");
	    this.getServer().getPluginManager().disablePlugin(this);
	    economyFound = false;
	}
    }

    public boolean useChest() {
	return chest;
    }

    public Economy getEconomy() {
	return eco;
    }

    /**
     * Colorizes a given string to Bukkit standards
     * 
     * http://forums.bukkit.org/threads/multiple-classes-config-colours.79719/#
     * post-1154761
     * 
     * @param string
     * @return String with appropriate Bukkit ChatColor in them
     * @author Njol
     */
    public static String colorizeText(String string) {
	if (string == null) {
	    return "";
	}
	return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Attempts to look up full name based on who's on the server Given a
     * partial name
     * 
     * @author Frigid, edited by Raphfrk and petteyg359
     */
    public String expandName(String Name) {
	int m = 0;
	String Result = "";
	for (int n = 0; n < getServer().getOnlinePlayers().length; n++) {
	    String str = getServer().getOnlinePlayers()[n].getName();
	    if (str.matches("(?i).*" + Name + ".*")) {
		m++;
		Result = str;
		if (m == 2) {
		    return null;
		}
	    }
	    if (str.equalsIgnoreCase(Name))
		return str;
	}
	if (m == 1)
	    return Result;
	if (m > 1) {
	    return null;
	}
	return Name;
    }
}