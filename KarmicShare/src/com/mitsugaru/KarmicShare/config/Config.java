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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.inventory.Item;

public class Config {
    // Class variables
    private static KarmicShare plugin;
    public static final Map<Item, Integer> karma = new HashMap<Item, Integer>();
    private static final EnumMap<ConfigNode, Object> OPTIONS = new EnumMap<ConfigNode, Object>(
            ConfigNode.class);

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
    public Config(KarmicShare ks) {
        plugin = ks;
        // Grab config
        final ConfigurationSection config = ks.getConfig();
        // Insert defaults into config file if they're not present
        for (ConfigNode node : ConfigNode.values()) {
            if (!config.contains(node.getPath())) {
                config.set(node.getPath(), node.getDefaultValue());
            }
        }
        // Save config
        ks.saveConfig();
        /**
         * Database info
         */
        updateOption(ConfigNode.MYSQL_USE);
        updateOption(ConfigNode.MYSQL_HOST);
        updateOption(ConfigNode.MYSQL_PORT);
        updateOption(ConfigNode.MYSQL_DATABASE);
        updateOption(ConfigNode.MYSQL_USER);
        updateOption(ConfigNode.MYSQL_PASSWORD);
        updateOption(ConfigNode.MYSQL_TABLE_PREFIX);
        updateOption(ConfigNode.MYSQL_IMPORT);
        // reload to load other settings
        reloadConfig();
    }

    @SuppressWarnings("unchecked")
    public void updateOption(ConfigNode node) {
        final ConfigurationSection config = plugin.getConfig();
        switch (node.getType()) {
            case LIST: {
                List<String> list = config.getStringList(node.getPath());
                if (list == null) {
                    list = (List<String>) node.getDefaultValue();
                }
                OPTIONS.put(node, list);
                break;
            }
            case DOUBLE: {
                OPTIONS.put(
                        node,
                        config.getDouble(node.getPath(),
                                (Double) node.getDefaultValue()));
                break;
            }
            case STRING: {
                OPTIONS.put(
                        node,
                        config.getString(node.getPath(),
                                (String) node.getDefaultValue()));
                break;
            }
            case INTEGER: {
                OPTIONS.put(
                        node,
                        config.getInt(node.getPath(),
                                (Integer) node.getDefaultValue()));
                break;
            }
            case BOOLEAN: {
                OPTIONS.put(
                        node,
                        config.getBoolean(node.getPath(),
                                (Boolean) node.getDefaultValue()));
                break;
            }
            default: {
                OPTIONS.put(node,
                        config.get(node.getPath(), node.getDefaultValue()));
            }
        }
    }

    public void set(ConfigNode node, Object o) {
        set(node.getPath(), o);
    }

    public void set(String path, Object o) {
        final ConfigurationSection config = plugin.getConfig();
        config.set(path, o);
        plugin.saveConfig();
    }

    public static int getInt(ConfigNode node) {
        int i = -1;
        switch (node.getType()) {
            case INTEGER: {
                i = ((Integer) OPTIONS.get(node)).intValue();
                break;
            }
            default: {
                // TODO throw exception
                break;
            }
        }
        return i;
    }

    public static String getString(ConfigNode node) {
        String out = "";
        switch (node.getType()) {
            case STRING: {
                out = (String) OPTIONS.get(node);
                break;
            }
            default: {
                // TODO throw exception
                break;
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getStringList(ConfigNode node) {
        List<String> list = new ArrayList<String>();
        switch (node.getType()) {
            case LIST: {
                final ConfigurationSection config = plugin.getConfig();
                list = config.getStringList(node.getPath());
                if (list == null) {
                    list = (List<String>) node.getDefaultValue();
                }
                break;
            }
            default: {
                // TODO throw exception
                break;
            }
        }
        return list;
    }

    public static double getDouble(ConfigNode node) {
        double d = 0.0;
        switch (node.getType()) {
            case DOUBLE: {
                d = ((Double) OPTIONS.get(node)).doubleValue();
                break;
            }
            default: {
                // TODO throw exception
                break;
            }
        }
        return d;
    }

    public static boolean getBoolean(ConfigNode node) {
        boolean b = false;
        switch (node.getType()) {
            case BOOLEAN: {
                b = ((Boolean) OPTIONS.get(node)).booleanValue();
            }
            default: {
                // TODO throw exception
                break;
            }
        }
        return b;
    }

    /**
     * Loads the per-item karma values into a hashmap for later usage
     */
    private void loadKarmaMap() {
        // Load karma file
        final YamlConfiguration karmaFile = this.karmaFile();
        // Load custom karma file into map
        for (final String entry : karmaFile.getKeys(false)) {
            try {
                // Attempt to parse the nodes
                int key = Integer.parseInt(entry);
                // If it has child nodes, parse those as well
                if (karmaFile.isConfigurationSection(entry)) {
                    ConfigurationSection sec = karmaFile
                            .getConfigurationSection(entry);
                    for (final String dataValue : sec.getKeys(false)) {
                        int secondKey = Integer.parseInt(dataValue);
                        int secondValue = sec.getInt(dataValue);
                        if (key != 373) {
                            karma.put(
                                    new Item(key, Byte
                                            .parseByte("" + secondKey),
                                            (short) secondKey), secondValue);
                        } else {
                            karma.put(new Item(key, Byte.parseByte("" + 0),
                                    (short) secondKey), secondValue);
                        }
                    }
                } else {
                    int value = karmaFile.getInt(entry);
                    karma.put(new Item(key, Byte.valueOf("" + 0), (short) 0),
                            value);
                }
            } catch (final NumberFormatException ex) {
                plugin.getLogger().warning("Non-integer value for: " + entry);
                ex.printStackTrace();
            }
        }
        plugin.getLogger().info("Loaded custom karma values");
    }

    @SuppressWarnings("unused")
    private void loadBlacklist() {
        // final YamlConfiguration blacklistFile = blacklistFile();
        // Load info into set
        // TODO test
        // final List<String> list = (List<String>)
        // blacklistFile.getList("blacklist", new ArrayList<String>());
    }

    /**
     * Reloads info from yaml file(s)
     */
    public void reloadConfig() {
        // Initial relaod
        plugin.reloadConfig();
        // Grab config
        loadSettings(plugin.getConfig());
        // Load config for item specific karma if not using static karma
        if (!getBoolean(ConfigNode.KARMA_STATIC)
                && !getBoolean(ConfigNode.KARMA_DISABLED)) {
            // Clear old mappings
            karma.clear();
            // Reload karma mappings
            this.loadKarmaMap();
        }
        // TODO
        // if (blacklist) {
        // this.loadBlacklist();
        // }
        // Check bounds
        this.boundsCheck();
    }

    private void loadSettings(ConfigurationSection config) {

        updateOption(ConfigNode.CHESTS);
        updateOption(ConfigNode.DISABLED_WORLDS);
        updateOption(ConfigNode.EFFECTS);
        updateOption(ConfigNode.LIST_LIMIT);
        updateOption(ConfigNode.KARMA_DISABLED);
        updateOption(ConfigNode.KARMA_STATIC);
        updateOption(ConfigNode.KARMA_UPPER_LIMIT);
        updateOption(ConfigNode.KARMA_UPPER_PERCENT);
        updateOption(ConfigNode.KARMA_LOWER_LIMIT);
        updateOption(ConfigNode.KARMA_LOWER_PERCENT);
        updateOption(ConfigNode.KARMA_PLAYER_DEFAULT);
        updateOption(ConfigNode.KARMA_CHANGE_DEFAULT);
        updateOption(ConfigNode.KARMA_USE_ECONOMY);
        updateOption(ConfigNode.KARMA_IGNORE_SELF_GROUP);
        updateOption(ConfigNode.DEBUG_TIME);
        updateOption(ConfigNode.DEBUG_DATABASE);
        updateOption(ConfigNode.DEBUG_INVENTORY);
        updateOption(ConfigNode.DEBUG_KARMA);
        updateOption(ConfigNode.DEBUG_ITEM);
    }

    /**
     * Check the bounds on the parameters to make sure that all config variables
     * are legal and usable by the plugin
     */
    private void boundsCheck() {
        // Check upper and lower limits
        if (getInt(ConfigNode.KARMA_UPPER_LIMIT) < getInt(ConfigNode.KARMA_LOWER_LIMIT)) {
            OPTIONS.put(ConfigNode.KARMA_UPPER_LIMIT,
                    ConfigNode.KARMA_UPPER_LIMIT.getDefaultValue());
            OPTIONS.put(ConfigNode.KARMA_LOWER_LIMIT,
                    ConfigNode.KARMA_LOWER_LIMIT.getDefaultValue());
            plugin.getLogger()
                    .warning(
                            "Upper limit is smaller than lower limit. Reverting to defaults.");
        }
        // Check that we don't go beyond what the database can handle, via
        // smallint
        else if (Math.abs(getInt(ConfigNode.KARMA_UPPER_LIMIT)) >= 30000
                || Math.abs(getInt(ConfigNode.KARMA_LOWER_LIMIT)) >= 30000) {
            OPTIONS.put(ConfigNode.KARMA_UPPER_LIMIT,
                    ConfigNode.KARMA_UPPER_LIMIT.getDefaultValue());
            OPTIONS.put(ConfigNode.KARMA_LOWER_LIMIT,
                    ConfigNode.KARMA_LOWER_LIMIT.getDefaultValue());
            plugin.getLogger()
                    .warning(
                            "Upper/lower limit is beyond bounds. Reverting to defaults.");
        }
        // Check percentages
        if (getDouble(ConfigNode.KARMA_UPPER_PERCENT) < getDouble(ConfigNode.KARMA_LOWER_PERCENT)) {
            OPTIONS.put(ConfigNode.KARMA_UPPER_PERCENT,
                    ConfigNode.KARMA_UPPER_PERCENT.getDefaultValue());
            OPTIONS.put(ConfigNode.KARMA_LOWER_PERCENT,
                    ConfigNode.KARMA_LOWER_PERCENT.getDefaultValue());
            plugin.getLogger()
                    .warning(
                            "Upper %-age is smaller than lower %-age. Reverting to defaults.");
        } else if (getDouble(ConfigNode.KARMA_UPPER_PERCENT) > 1.0
                || getDouble(ConfigNode.KARMA_LOWER_PERCENT) < 0) {
            OPTIONS.put(ConfigNode.KARMA_UPPER_PERCENT,
                    ConfigNode.KARMA_UPPER_PERCENT.getDefaultValue());
            OPTIONS.put(ConfigNode.KARMA_LOWER_PERCENT,
                    ConfigNode.KARMA_LOWER_PERCENT.getDefaultValue());
            plugin.getLogger()
                    .warning(
                            "Upper %-age and/or lower %-age are beyond bounds. Reverting to defaults.");
        }
        // Check that the default karma is actually in range.
        if (getInt(ConfigNode.KARMA_PLAYER_DEFAULT) < getInt(ConfigNode.KARMA_LOWER_LIMIT)
                || getInt(ConfigNode.KARMA_PLAYER_DEFAULT) > getInt(ConfigNode.KARMA_UPPER_LIMIT)) {
            // Average out valid bounds to create valid default
            OPTIONS.put(
                    ConfigNode.KARMA_PLAYER_DEFAULT,
                    getInt(ConfigNode.KARMA_UPPER_LIMIT)
                            - ((getInt(ConfigNode.KARMA_LOWER_LIMIT) + getInt(ConfigNode.KARMA_UPPER_LIMIT)) / 2));
            plugin.getLogger()
                    .warning(
                            "Player karma default is out of range. Using average of the two.");
        }
        // Check that default karma change is not negative.
        if (getInt(ConfigNode.KARMA_CHANGE_DEFAULT) < 0) {
            OPTIONS.put(ConfigNode.KARMA_CHANGE_DEFAULT,
                    ConfigNode.KARMA_CHANGE_DEFAULT.getDefaultValue());
            plugin.getLogger().warning(
                    "Default karma rate is negative. Using default.");
        }
        // Check that list is actually going to output something, based on limit
        // given
        if (getInt(ConfigNode.LIST_LIMIT) < 1) {
            OPTIONS.put(ConfigNode.LIST_LIMIT,
                    ConfigNode.LIST_LIMIT.getDefaultValue());
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
    private YamlConfiguration karmaFile() {
        final File file = new File(plugin.getDataFolder().getAbsolutePath()
                + "/karma.yml");
        final YamlConfiguration karmaFile = YamlConfiguration
                .loadConfiguration(file);
        if (karmaFile.getKeys(false).isEmpty()) {
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
            try {
                // Save the file
                karmaFile.save(file);
            } catch (IOException e1) {
                plugin.getLogger().warning(
                        "File I/O Exception on saving karma list");
                e1.printStackTrace();
            }
        }
        return karmaFile;
    }

    @SuppressWarnings("unused")
    private YamlConfiguration blacklistFile() {
        final File file = new File(plugin.getDataFolder().getAbsolutePath()
                + "/blacklist.yml");
        final YamlConfiguration blacklistFile = YamlConfiguration
                .loadConfiguration(file);
        if (!file.exists()) {
            try {
                // Save the file
                blacklistFile.save(file);
            } catch (IOException e1) {
                plugin.getLogger().warning(
                        "File I/O Exception on saving blacklist");
                e1.printStackTrace();
            }
        }
        return blacklistFile;
    }
}
