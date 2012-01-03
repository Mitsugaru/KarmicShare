package com.mitsugaru.KarmicShare;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.event.inventory.InventoryClickEvent;
import org.getspout.spoutapi.event.inventory.InventoryCloseEvent;
import org.getspout.spoutapi.event.inventory.InventoryListener;
import org.getspout.spoutapi.event.inventory.InventoryOpenEvent;

import com.splatbang.betterchest.BetterChest;

public class KSInventoryListener extends InventoryListener {
	private KarmicShare plugin;

	public KSInventoryListener(KarmicShare karmicShare) {
		plugin = karmicShare;
	}

	@Override
	public void onInventoryClose(InventoryCloseEvent event) {
		plugin.getLogger().info("InventoryCloseEvent");
	}

	@Override
	public void onInventoryClick(InventoryClickEvent event) {
		// This will verify that it was a block
		if (event.getLocation() != null)
		{
			// Verify that it is a chest
			final Block block = event.getLocation().getBlock();
			if (block.getType().equals(Material.CHEST))
			{
				// Don't know if getInventory null check is necessary
				if (event.getInventory() != null)
				{
					if (!event.getInventory().equals(
							event.getPlayer().getInventory()))
					{
						// Player is working on inventory that is not theirs
						// Verify that it is one of our chests
						boolean kschest = false;
						final BetterChest chest = new BetterChest((Chest) block.getState());
						if(block.getRelative(BlockFace.UP).getType().equals(Material.WALL_SIGN))
						{
							final Sign sign = (Sign) block.getRelative(BlockFace.UP).getState();
							if(ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[KarmicShare]"))
							{
								kschest = true;
							}
						}
						else if(chest.isDoubleChest())
						{
							if(chest.attachedBlock().getRelative(BlockFace.UP).getType().equals(Material.WALL_SIGN))
							{
								final Sign sign = (Sign) block.getRelative(BlockFace.UP).getState();
								if(ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[KarmicShare]"))
								{
									kschest = true;
								}
							}
						}
						if (kschest)
						{
							if (event.getItem() != null
									&& event.getCursor() != null)
							{
								// Switching items
								if (event.getItem().getType()
										.equals(event.getCursor().getType()))
								{
									// they're adding to existing stack
									plugin.getLogger().info(
											"Adding to existing stack");
								}
								else
								{
									plugin.getLogger().info("Switching");
								}
							}
							else if (event.getItem() != null)
							{
								// they clicked on an item
								plugin.getLogger().info("Grab");
							}
							else if (event.getCursor() != null)
							{
								// they placed an item
								plugin.getLogger().info("Put");
							}
							// TODO repopulate
							// TODO shift click
						}
					}
				}
			}
		}
	}

	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {
		plugin.getLogger().info("InventoryOpenEvent");
	}
}
