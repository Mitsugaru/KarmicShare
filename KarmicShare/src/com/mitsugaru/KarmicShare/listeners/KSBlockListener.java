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
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

import com.mitsugaru.KarmicShare.Karma;
import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.permissions.Permission;
import com.splatbang.betterchest.BetterChest;

public class KSBlockListener implements Listener {
	private KarmicShare plugin;
	private static final BlockFace[] nav = { BlockFace.NORTH, BlockFace.SOUTH,
			BlockFace.EAST, BlockFace.WEST };

	// IDEA Player karma signs
	public KSBlockListener(KarmicShare karmicShare) {
		plugin = karmicShare;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(final SignChangeEvent event) {
		if (!event.isCancelled())
		{
			if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
					"[KarmicShare]"))
			{
				if (plugin.getPermissionHandler().checkPermission(
						event.getPlayer(), Permission.SIGN.getNode()))
				{
					if (!ChatColor.stripColor(event.getLine(2)).equals(""))
					{
						// Player sign
					}
					else
					{
						String groupName = "global";
						if(!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase(""))
						{
							//Group chest
							if(Karma.validGroup(event.getPlayer(), ChatColor.stripColor(event.getLine(0)).toLowerCase()))
							{
								groupName = event.getLine(0);
							}
							else
							{
								event.getPlayer().sendMessage(
										ChatColor.YELLOW + KarmicShare.prefix
												+ " Group " + ChatColor.GRAY + event.getLine(0) + ChatColor.YELLOW + " not found");
							}
						}
						//Check if its a chest
						if (plugin.getPluginConfig().chests && plugin.useChest())
						{
							// Thanks to Wolvereness for the following code
							if (event.getBlock().getRelative(BlockFace.DOWN)
									.getType().equals(Material.CHEST))
							{
								// Reformat sign
								event.setLine(0, groupName);
								event.setLine(1, ChatColor.AQUA
										+ "[KarmicShare]");
								event.setLine(2, "Page:");
								event.setLine(3, "1");
								if(!ChatColor.stripColor(groupName).equalsIgnoreCase("global"))
								{
									event.getPlayer().sendMessage(
											ChatColor.GREEN + KarmicShare.prefix
													+ " Chest linked to " + ChatColor.GRAY + ChatColor.stripColor(groupName).toLowerCase());
								}
								else
								{
									event.getPlayer().sendMessage(
											ChatColor.GREEN + KarmicShare.prefix
													+ " Chest linked to pool.");
								}
							}
							else
							{
								// Reformat sign
								event.setLine(0, groupName);
								event.setLine(1, ChatColor.DARK_RED
										+ "[KarmicShare]");
								event.setLine(2, "Page:");
								event.setLine(3, "1");
								event.getPlayer().sendMessage(
										ChatColor.YELLOW + KarmicShare.prefix
												+ " No chest found!");
							}
						}
						else
						{
							event.getPlayer().sendMessage(
									ChatColor.RED + KarmicShare.prefix
											+ " Chests access disabled");
							// Cancel event
							event.setCancelled(true);
						}
					}
				}
				else
				{
					event.getPlayer().sendMessage(
							ChatColor.RED + KarmicShare.prefix
									+ " Lack permission: " + Permission.SIGN.getNode());
					// Cancel event
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlace(final BlockPlaceEvent event) {
		if (!event.isCancelled())
		{
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
									"[KarmicShare]"))
							{
								has = true;
							}
						}
					}
				}
				if (has)
				{
					event.getPlayer()
							.sendMessage(
									ChatColor.RED
											+ KarmicShare.prefix
											+ " Cannot have a sign next to a link sign!");
					event.setCancelled(true);
				}
			}
			else if (material.equals(Material.CHEST))
			{
				final Block block = event.getBlock();
				final BetterChest chest = new BetterChest(
						(Chest) block.getState());

				if (block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
				{
					boolean has = false;
					Sign sign = (Sign) block.getRelative(BlockFace.UP)
							.getState();
					for (String s : sign.getLines())
					{
						if (ChatColor.stripColor(s).equalsIgnoreCase(
								"[KarmicShare]"))
						{
							has = true;
							//TODO check that it isn't a player karma sign
						}
					}
					if (has)
					{
						// Reformat sign
						sign.setLine(1, ChatColor.AQUA + "[KarmicShare]");
						sign.setLine(2, "Page:");
						sign.setLine(3, "1");
						sign.update();
						if(!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("global"))
						{
							event.getPlayer().sendMessage(
									ChatColor.GREEN + KarmicShare.prefix
											+ " Chest linked to " + ChatColor.GRAY + ChatColor.stripColor(sign.getLine(0)).toLowerCase());
						}
						else
						{
							event.getPlayer().sendMessage(
									ChatColor.GREEN + KarmicShare.prefix
											+ " Chest linked to pool.");
						}
					}
				}
				else if (chest.isDoubleChest())
				{
					if (chest.attachedBlock().getRelative(BlockFace.UP)
							.getType().equals(Material.WALL_SIGN))
					{
						boolean exists = false;
						final Sign sign = (Sign) chest.attachedBlock()
								.getRelative(BlockFace.UP).getState();
						for (String s : sign.getLines())
						{
							if (ChatColor.stripColor(s).equalsIgnoreCase(
									"[KarmicShare]"))
							{
								// Sign already exists
								exists = true;
							}
						}
						if (exists)
						{
							// Reformat sign
							sign.setLine(1, ChatColor.AQUA + "[KarmicShare]");
							sign.setLine(2, "Page:");
							sign.setLine(3, "1");
							sign.update();
							if(!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("global"))
							{
								event.getPlayer().sendMessage(
										ChatColor.GREEN + KarmicShare.prefix
												+ " Chest linked to " + ChatColor.GRAY + ChatColor.stripColor(sign.getLine(0)).toLowerCase());
							}
							else
							{
								event.getPlayer().sendMessage(
										ChatColor.GREEN + KarmicShare.prefix
												+ " Chest linked to pool.");
							}
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(final BlockBreakEvent event) {
		if (!event.isCancelled())
		{
			final Material material = event.getBlock().getType();
			if (material.equals(Material.CHEST))
			{
				final Block block = event.getBlock();
				final BetterChest chest = new BetterChest(
						(Chest) block.getState());
				if (block.getRelative(BlockFace.UP).getType()
						.equals(Material.WALL_SIGN))
				{
					Sign sign = (Sign) block.getRelative(BlockFace.UP)
							.getState();
					if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
							"[KarmicShare]"))
					{
						// Empty chest as it has spawned items
						chest.getInventory().clear();
						chest.update();
						// Update sign
						sign.setLine(1, ChatColor.DARK_RED + "[KarmicShare]");
						sign.update();
						if(!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("global"))
						{
							event.getPlayer().sendMessage(
									ChatColor.YELLOW + KarmicShare.prefix
											+ " Chest unlinked from " + ChatColor.GRAY + ChatColor.stripColor(sign.getLine(0)).toLowerCase());
						}
						else
						{
							event.getPlayer().sendMessage(
									ChatColor.YELLOW + KarmicShare.prefix
											+ " Chest unlinked from pool.");
						}
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
								.equalsIgnoreCase("[KarmicShare]"))
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
			else if (material.equals(Material.WALL_SIGN))
			{
				final Sign sign = (Sign) event.getBlock().getState();
				boolean has = false;
				for (String s : sign.getLines())
				{
					if (ChatColor.stripColor(s).equalsIgnoreCase(
							"[KarmicShare]"))
					{
						// Sign already exists
						has = true;
					}
				}
				if (has)
				{
					if (plugin.getPermissionHandler().checkPermission(
							event.getPlayer(), Permission.SIGN.getNode()))
					{
						if (event.getBlock().getRelative(BlockFace.DOWN)
								.getType().equals(Material.CHEST))
						{

							BetterChest chest = new BetterChest((Chest) sign
									.getBlock().getRelative(BlockFace.DOWN)
									.getState());
							chest.getInventory().clear();
							chest.update();
							if(!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("global"))
							{
								event.getPlayer().sendMessage(
										ChatColor.YELLOW + KarmicShare.prefix
												+ " Chest unlinked from " + ChatColor.GRAY + ChatColor.stripColor(sign.getLine(0)).toLowerCase());
							}
							else
							{
								event.getPlayer().sendMessage(
										ChatColor.YELLOW + KarmicShare.prefix
												+ " Chest unlinked from pool.");
							}
						}

					}
					else
					{
						event.getPlayer().sendMessage(
								ChatColor.RED + KarmicShare.prefix
										+ " Lack permission: " + Permission.SIGN.getNode());
						event.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Thanks to Ribesg for the following method
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Material material = event.getBlock().getType();
		if (material.equals(Material.SIGN_POST)
				|| material.equals(Material.WALL_SIGN))
		{
			Sign sign = (Sign) event.getBlock().getState();
			if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
					"[KarmicShare]"))
			{
				// Find chest
				if (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.CHEST)
				{
					// Clear
					BetterChest chest = new BetterChest((Chest) sign.getBlock()
							.getRelative(BlockFace.DOWN).getState());
					chest.getInventory().clear();
					chest.update();
				}
			}
		}
	}
}
