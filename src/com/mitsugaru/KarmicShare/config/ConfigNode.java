package com.mitsugaru.KarmicShare.config;

import java.util.ArrayList;

public enum ConfigNode {
    
    // Root nodes
    CHESTS("chests", VarType.BOOLEAN, true),
    DISABLED_WORLDS("disabledWorlds", VarType.LIST, new ArrayList<String>()),
    EFFECTS("effecs", VarType.BOOLEAN, true),
    LIST_LIMIT("listlimit", VarType.INTEGER, 10),
    ENDER_REPLACE("enderchestreplace", VarType.BOOLEAN, false),
    // Karma nodes
    KARMA_STATIC("karma.static", VarType.BOOLEAN, false),
    KARMA_ECONOMY("karma.useEconomy", VarType.BOOLEAN, true),
    KARMA_DISABLED("karma.disabled", VarType.BOOLEAN, false),
    KARMA_UPPER_LIMIT("karma.upper.limit", VarType.INTEGER, 200),
    KARMA_UPPER_PERCENT("karma.upper.percent", VarType.DOUBLE, 0.85),
    KARMA_LOWER_LIMIT("karma.lower.limit", VarType.INTEGER, -200),
    KARMA_LOWER_PERCENT("karma.lower.percent", VarType.DOUBLE, 0.15),
    KARMA_PLAYER_DEFAULT("karma.playerDefault", VarType.INTEGER, 0),
    KARMA_CHANGE_GIVE("karma.change.give", VarType.INTEGER, 1),
    KARMA_CHANGE_TAKE("karma.change.take", VarType.INTEGER, 1),
    KARMA_IGNORE_SELF_GROUP("karma.ignoreSelfGroup", VarType.BOOLEAN, true),
    // MYSQL
    MYSQL_USE("mysql.use", VarType.BOOLEAN, false),
    MYSQL_HOST("mysql.host", VarType.STRING, "localhost"),
    MYSQL_PORT("mysql.port", VarType.INTEGER, 3306),
    MYSQL_DATABASE("mysql.database", VarType.STRING, "minecraft"),
    MYSQL_USER("mysql.user", VarType.STRING, "username"),
    MYSQL_PASSWORD("mysql.password", VarType.STRING, "pass"),
    MYSQL_TABLE_PREFIX("mysql.tablePrefix", VarType.STRING, "ks_"),
    MYSQL_IMPORT("mysql.import", VarType.BOOLEAN, false),
    // DEBUG
    DEBUG_TIME("debug.time", VarType.BOOLEAN, false),
    DEBUG_DATABASE("debug.database", VarType.BOOLEAN, false),
    DEBUG_INVENTORY("debug.inventory", VarType.BOOLEAN, false),
    DEBUG_KARMA("debug.karma", VarType.BOOLEAN, false),
    DEBUG_ITEM("debug.item", VarType.BOOLEAN, false),
    DEBUG_CONFIG("debug.config", VarType.BOOLEAN, false),
    DEBUG_ECONOMY("debug.economy", VarType.BOOLEAN, false),
    // VERSION
    VERSION("version", VarType.STRING, "0.317");

    // TODO defaults.put("blacklist", false);
    
    private String path;
    private Object def;
    private VarType vartype;

    private ConfigNode(String path, VarType vartype, Object def) {
        this.path = path;
        this.vartype = vartype;
        this.def = def;
    }

    public String getPath() {
        return this.path;
    }

    public VarType getVarType() {
        return this.vartype;
    }

    public Object getDefaultValue() {
        return this.def;
    }

    public enum VarType {
        STRING,
        INTEGER,
        DOUBLE,
        BOOLEAN,
        LIST;
    }
}
