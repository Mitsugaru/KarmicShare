package com.mitsugaru.KarmicShare.update.holders;

public class ZeroPointFourteenItemObject
{
	public int itemid, amount;
	public byte data;
	public short durability;
	public String enchantments;

	public ZeroPointFourteenItemObject(int id, int quantity, byte dv,
			short dur, String en)
	{
		this.itemid = id;
		this.amount = quantity;
		this.data = dv;
		this.durability = dur;
		this.enchantments = en;
	}
}
