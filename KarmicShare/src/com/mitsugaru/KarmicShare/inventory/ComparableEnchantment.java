package com.mitsugaru.KarmicShare.inventory;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;

public class ComparableEnchantment extends EnchantmentWrapper implements Comparable<ComparableEnchantment>
{

	public ComparableEnchantment(Enchantment enchantment)
	{
		super(enchantment.getId());
	}

	@Override
	public int compareTo(ComparableEnchantment o)
	{
		return getId() - o.getId();
	}

}
