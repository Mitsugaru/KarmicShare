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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.Config;
import com.mitsugaru.KarmicShare.config.ConfigNode;
import com.mitsugaru.KarmicShare.permissions.PermissionHandler;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;
import com.splatbang.betterchest.BetterChest;

public class KSBlockListener implements Listener
{
	private KarmicShare plugin;
	private static final BlockFace[] nav = { BlockFace.NORTH, BlockFace.SOUTH,
			BlockFace.EAST, BlockFace.WEST };

	// IDEA Player karma signs
	public KSBlockListener(KarmicShare karmicShare)
	{
		plugin = karmicShare;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(final SignChangeEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		if (!(ChatColor.stripColor(event.getLine(1))
				.equalsIgnoreCase(KarmicShare.TAG)))
		{
			// Not ours
			return;
		}
		// Check permission
		if (!PermissionHandler.has(event.getPlayer(), PermissionNode.SIGN))
		{
			event.getPlayer().sendMessage(
					ChatColor.RED + KarmicShare.TAG + " Lack permission: "
							+ PermissionNode.SIGN);
			// Cancel event
			event.setCancelled(true);
			return;
		}
		//Check world
		final String world = event.getBlock().getWorld().getName();
		if(Config.getStringList(ConfigNode.DISABLED_WORLDS).contains(world))
		{
			event.getPlayer().sendMessage(
					ChatColor.RED + KarmicShare.TAG + " KarmicShare access disabled for this world.");
			event.setCancelled(true);
			return;
		}
		if (!ChatColor.stripColor(event.getLine(2)).equals(""))
		{
			// TODO Player sign
		}
		else
		{
			// Check if its a chest
			if (Config.getBoolean(ConfigNode.CHESTS) && plugin.useChest())
			{
				// Thanks to Wolvereness for the following code
				if (event.getBlock().getRelative(BlockFace.DOWN).getType()
						.equals(Material.CHEST))
				{
					// Reformat sign
					event.setLine(1, ChatColor.AQUA + KarmicShare.TAG);
					event.setLine(2, "Page:");
					event.setLine(3, "1");
					event.getPlayer().sendMessage(
							ChatColor.GREEN + KarmicShare.TAG
									+ " Chest linked to cloud storage.");
				}
				else
				{
					// Reformat sign
					event.setLine(1, ChatColor.DARK_RED + KarmicShare.TAG);
					event.setLine(2, "Page:");
					event.setLine(3, "1");
					event.getPlayer().sendMessage(
							ChatColor.YELLOW + KarmicShare.TAG
									+ " No chest found!");
				}
			}
			else
			{
				event.getPlayer().sendMessage(
						ChatColor.RED + KarmicShare.TAG
								+ " Chests access disabled");
				// Cancel event
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlace(final BlockPlaceEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		final Material material = event.getBlock().getType();
		if (material.equals(Material.SIGN)
				|| material.equals(Material.WALL_SIGN)
				|| material.equals(Material.SIGN_POST))
		{
			boolean has = false;
			for (BlockFace face : nav)
			{
				if (event.getBlock().getRelative(face).getType()
						.equals(Material.WALL_SIGN))
				{
					Sign sign = (Sign) event.getBlock().getRelative(face)
							.getState();
					for (String s : sign.getLines())
					{
						if (ChatColor.stripColor(s).equalsIgnoreCase(
								KarmicShare.TAG))
						{
							has = true;
						}
					}
				}
			}
			if (has)
			{
				event.getPlayer().sendMessage(
						ChatColor.RED + KarmicShare.TAG
								+ " Cannot have a sign next to a link sign!");
				event.setCancelled(true);
			}
		}
		else if (material.equals(Material.CHEST))
		{
			final Block block = event.getBlock();
			final BetterChest chest = new BetterChest((Chest) block.getState());
			Sign sign = null;
			boolean has = false;
			if (block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
			{
				sign = (Sign) block.getRelative(BlockFace.UP).getState();
				for (String s : sign.getLines())
				{
					if (ChatColor.stripColor(s).equalsIgnoreCase(
							KarmicShare.TAG))
					{
						has = true;
						// TODO check that it isn't a player karma sign
					}
				}
			}
			else if (chest.isDoubleChest())
			{
				if (chest.attachedBlock().getRelative(BlockFace.UP).getType()
						.equals(Material.WALL_SIGN))
				{
					sign = (Sign) chest.attachedBlock()
							.getRelative(BlockFace.UP).getState();
					for (String s : sign.getLines())
					{
						if (ChatColor.stripColor(s).equalsIgnoreCase(
								KarmicShare.TAG))
						{
							// Sign already exists
							has = true;
						}
					}
				}
			}
			if(has)
			{
				//Check world
				final String world = block.getWorld().getName();
				if(Config.getStringList(ConfigNode.DISABLED_WORLDS).contains(world))
				{
					event.getPlayer().sendMessage(
							ChatColor.RED + KarmicShare.TAG + " KarmicShare access disabled for this world.");
					event.setCancelled(true);
					return;
				}
				// Reformat sign
				sign.setLine(1, ChatColor.AQUA + KarmicShare.TAG);
				sign.setLine(2, "Page:");
				sign.setLine(3, "1");
				sign.update();
				event.getPlayer().sendMessage(
						ChatColor.GREEN + KarmicShare.TAG
								+ " Chest linked to cloud storage.");
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(final BlockBreakEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		final Material material = event.getBlock().getType();
		if (material.equals(Material.CHEST))
		{
			final Block block = event.getBlock();
			final BetterChest chest = new BetterChest((Chest) block.getState());
			if (block.getRelative(BlockFace.UP).getType()
					.equals(Material.WALL_SIGN))
			{
				Sign sign = (Sign) block.getRelative(BlockFace.UP).getState();
				if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
						KarmicShare.TAG))
				{
					// Update sign
					sign.setLine(1, ChatColor.DARK_RED + KarmicShare.TAG);
					sign.update();
					event.getPlayer().sendMessage(
							ChatColor.YELLOW + KarmicShare.TAG
									+ " Chest unlinked from cloud storage.");
				}
			}
			else if (chest.isDoubleChest())
			{
				if (chest.attachedBlock().getRelative(BlockFace.UP).getType()
						.equals(Material.WALL_SIGN))
				{
					final Sign sign = (Sign) chest.attachedBlock()
							.getRelative(BlockFace.UP).getState();
					if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
							KarmicShare.TAG))
					{
						// Update sign to reset page
						sign.setLine(3, "1");
						sign.update();
					}
				}
			}
		}
		else if (material.equals(Material.WALL_SIGN))
		{
			final Sign sign = (Sign) event.getBlock().getState();
			boolean has = false;
			for (String s : sign.getLines())
			{
				if (ChatColor.stripColor(s).equalsIgnoreCase(KarmicShare.TAG))
				{
					// Sign already exists
					has = true;
				}
			}
			if (has)
			{
				if (PermissionHandler.has(event.getPlayer(),
						PermissionNode.SIGN))
				{
					if (event.getBlock().getRelative(BlockFace.DOWN).getType()
							.equals(Material.CHEST))
					{
						event.getPlayer()
								.sendMessage(
										ChatColor.YELLOW
												+ KarmicShare.TAG
												+ " Chest unlinked from cloud storage.");
					}
				}
				else
				{
					event.getPlayer().sendMessage(
							ChatColor.RED + KarmicShare.TAG
									+ " Lack permission: "
									+ PermissionNode.SIGN.getNode());
					event.setCancelled(true);
				}
			}
		}
	}
}
