package com.mitsugaru.KarmicShare.database;

import com.mitsugaru.KarmicShare.config.Config;

public enum Table
{
	ITEMS(Config.tablePrefix + "items"), PLAYERS(Config.tablePrefix + "players"), GROUPS(Config.tablePrefix + "groups");
	
	private final String table;
	
	private Table(String table)
	{
		this.table = table;
	}
	
	public String getName()
	{
		return table;
	}
	
	@Override
	public String toString()
	{
		return table;
	}
}
