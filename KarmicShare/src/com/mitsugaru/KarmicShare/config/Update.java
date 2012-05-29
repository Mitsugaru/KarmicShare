package com.mitsugaru.KarmicShare.config;

public class Update
{
	static class ZeroPointFourteenItemObject
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
	
	static class ZeroPointTwoSixTwoPlayerObject
	{
		public String playername, groups;
		public int karma;
		
		public ZeroPointTwoSixTwoPlayerObject(String playername, int karma, String groups)
		{
			this.playername = playername;
			this.karma = karma;
			this.groups = groups;
		}
	}
	
	static class ZeroPointTwoSixTwoItemObject extends ZeroPointFourteenItemObject
	{
		public String groups;
		
		public ZeroPointTwoSixTwoItemObject(int id, int quantity, byte dv,
				short dur, String en, String groups)
		{
			super(id, quantity, dv, dur, en);
			this.groups = groups;
		}
	}
}
