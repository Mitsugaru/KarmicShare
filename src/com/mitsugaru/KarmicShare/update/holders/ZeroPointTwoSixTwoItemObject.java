package com.mitsugaru.KarmicShare.update.holders;

public class ZeroPointTwoSixTwoItemObject extends ZeroPointFourteenItemObject
{
	public String groups;

	public ZeroPointTwoSixTwoItemObject(int id, int quantity, byte dv,
			short dur, String en, String groups)
	{
		super(id, quantity, dv, dur, en);
		this.groups = groups;
	}
}
