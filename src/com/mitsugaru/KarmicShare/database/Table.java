package com.mitsugaru.KarmicShare.database;

import com.mitsugaru.KarmicShare.config.RootConfig;
import com.mitsugaru.KarmicShare.config.ConfigNode;

public enum Table {
    ITEMS("items"),
    PLAYERS("players"),
    GROUPS("groups");

    private final String prefix = RootConfig
            .getString(ConfigNode.MYSQL_TABLE_PREFIX);
    private final String table;

    private Table(String table) {
        this.table = prefix + table;
    }

    public String getName() {
        return table;
    }

    @Override
    public String toString() {
        return table;
    }
}
