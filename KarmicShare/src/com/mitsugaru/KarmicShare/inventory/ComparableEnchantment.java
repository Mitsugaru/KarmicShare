package com.mitsugaru.KarmicShare.inventory;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;

/**
 * Wrapper class so that Enchantments can be ordered / comparable
 * 
 * @author Tokume
 *
 */
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
