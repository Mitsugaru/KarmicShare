package com.mitsugaru.KarmicShare.config;

import java.util.ArrayList;

import com.mitsugaru.KarmicShare.KarmicShare;

public enum ConfigNode {
    // Root nodes
    CHESTS("chests", Type.BOOLEAN, true),
    DISABLED_WORLDS("disabledWorlds", Type.LIST, new ArrayList<String>()),
    EFFECTS("effecs", Type.BOOLEAN, true),
    LIST_LIMIT("listlimit", Type.INTEGER, 10),
    // Karma nodes
    KARMA_STATIC("karma.static", Type.BOOLEAN, false),
    KARMA_DISABLED("karma.disabled", Type.BOOLEAN, false),
    KARMA_UPPER_LIMIT("karma.upper.limit", Type.INTEGER, 200),
    KARMA_UPPER_PERCENT("karma.upper.percent", Type.DOUBLE, 0.85),
    KARMA_LOWER_LIMIT("karma.lower.limit", Type.INTEGER, -200),
    KARMA_LOWER_PERCENT("karma.lower.percent", Type.DOUBLE, 0.15),
    KARMA_PLAYER_DEFAULT("karma.playerDefault", Type.INTEGER, 0),
    KARMA_CHANGE_DEFAULT("karma.changeDefault", Type.INTEGER, 1),
    KARMA_USE_ECONOMY("karma.useEconomy", Type.BOOLEAN, false),
    KARMA_IGNORE_SELF_GROUP("karma.ignoreSelfGroup", Type.BOOLEAN, true),
    // MYSQL
    MYSQL_USE("mysql.use", Type.BOOLEAN, false),
    MYSQL_HOST("mysql.host", Type.STRING, "localhost"),
    MYSQL_PORT("mysql.port", Type.INTEGER, 3306),
    MYSQL_DATABASE("mysql.database", Type.STRING, "minecraft"),
    MYSQL_USER("mysql.user", Type.STRING, "username"),
    MYSQL_PASSWORD("mysql.password", Type.STRING, "pass"),
    MYSQL_TABLE_PREFIX("mysql.tablePrefix", Type.STRING, "ks_"),
    MYSQL_IMPORT("mysql.import", Type.BOOLEAN, false),
    // DEBUG
    DEBUG_TIME("debug.time", Type.BOOLEAN, false),
    DEBUG_DATABASE("debug.database", Type.BOOLEAN, false),
    DEBUG_INVENTORY("debug.inventory", Type.BOOLEAN, false),
    DEBUG_KARMA("debug.karma", Type.BOOLEAN, false),
    DEBUG_ITEM("debug.item", Type.BOOLEAN, false),
    // VERSION
    VERSION("version", Type.DOUBLE, KarmicShare.getInstance().getDescription().getVersion());

    // TODO defaults.put("blacklist", false);
    private String path;
    private Object def;
    private Type type;

    private ConfigNode(String path, Type type, Object def) {
        this.path = path;
        this.type = type;
        this.def = def;
    }

    public String getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public Object getDefaultValue() {
        return def;
    }

    public enum Type {
        STRING,
        INTEGER,
        DOUBLE,
        BOOLEAN,
        LIST;
    }
}
