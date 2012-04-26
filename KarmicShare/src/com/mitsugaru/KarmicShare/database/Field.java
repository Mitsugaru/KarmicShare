package com.mitsugaru.KarmicShare.database;

import com.mitsugaru.KarmicShare.database.Table;

public enum Field
{
	ITEMS_ITEMID(Table.ITEMS, "itemid", Type.INTEGER), ITEMS_AMOUNT(
			Table.ITEMS, "amount", Type.INTEGER), ITEMS_DATA(Table.ITEMS,
			"data", Type.STRING), ITEMS_DURABILITY(Table.ITEMS, "durability",
			Type.STRING), ITEMS_ENCHANTMENTS(Table.ITEMS, "enchantments",
			Type.STRING), ITEMS_GROUPS(Table.ITEMS, "groups", Type.STRING), PLAYERS_NAME(
			Table.PLAYERS, "playername", Type.STRING), PLAYERS_KARMA(
			Table.PLAYERS, "karma", Type.INTEGER), PLAYERS_GROUPS(
			Table.PLAYERS, "groups", Type.STRING), GROUPS_NAME(Table.GROUPS,
			"groupname", Type.STRING);
	private final Table table;
	private final String columnname;
	private final Type type;

	private Field(Table table, String columnname, Type type)
	{

		this.table = table;
		this.columnname = columnname;
		this.type = type;
	}

	public Table getTable()
	{
		return table;
	}

	public String getColumnName()
	{
		return columnname;
	}
	
	public Type getType()
	{
		return type;
	}
}
