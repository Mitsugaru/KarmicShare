package com.mitsugaru.KarmicShare.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.splatbang.betterchest.BetterChest;

/*
 * This will no longer be necessary as we're no longer manipulating the chest
 * inventory
 */
@Deprecated
public class KSEntityListener implements Listener
{
	@SuppressWarnings("unused")
	private KarmicShare plugin;

	public KSEntityListener(KarmicShare ks)
	{
		plugin = ks;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityExplode(EntityExplodeEvent event)
	{
		for (Block block : event.blockList())
		{
			final Material material = block.getType();
			if (material.equals(Material.SIGN_POST)
					|| material.equals(Material.WALL_SIGN))
			{
				Sign sign = (Sign) block.getState();
				if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
						KarmicShare.TAG))
				{
					// Find chest
					if (block.getRelative(BlockFace.DOWN).getType() == Material.CHEST)
					{
						// Clear
						BetterChest chest = new BetterChest((Chest) sign
								.getBlock().getRelative(BlockFace.DOWN)
								.getState());
						chest.getInventory().clear();
						chest.update();
					}
				}
			}
			else if (material.equals(Material.CHEST))
			{
				final BetterChest chest = new BetterChest(
						(Chest) block.getState());
				if (block.getRelative(BlockFace.UP).getType()
						.equals(Material.WALL_SIGN))
				{
					Sign sign = (Sign) block.getRelative(BlockFace.UP)
							.getState();
					if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
							KarmicShare.TAG))
					{
						// Empty chest as it has spawned items
						chest.getInventory().clear();
						chest.update();
						// Update sign
						sign.setLine(1, ChatColor.DARK_RED + KarmicShare.TAG);
						sign.update();
					}
				}
				else if (chest.isDoubleChest())
				{
					if (chest.attachedBlock().getRelative(BlockFace.UP)
							.getType().equals(Material.WALL_SIGN))
					{
						final Sign sign = (Sign) chest.attachedBlock()
								.getRelative(BlockFace.UP).getState();
						if (ChatColor.stripColor(sign.getLine(1))
								.equalsIgnoreCase(KarmicShare.TAG))
						{
							// Empty chest as it has spawned items
							chest.getInventory().clear();
							chest.update();
							// Update sign to reset page
							sign.setLine(3, "1");
							sign.update();
						}
					}
				}
			}
		}
	}
}
