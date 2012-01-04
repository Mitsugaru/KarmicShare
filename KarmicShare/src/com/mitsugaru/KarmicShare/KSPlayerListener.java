package com.mitsugaru.KarmicShare;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.splatbang.betterchest.BetterChest;

public class KSPlayerListener extends PlayerListener {
	private KarmicShare plugin;
	private static final BlockFace[] nav = { BlockFace.NORTH, BlockFace.SOUTH,
			BlockFace.EAST, BlockFace.WEST };

	public KSPlayerListener(KarmicShare karmicShare) {
		plugin = karmicShare;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			final Block block = event.getClickedBlock();
			if (block.getType().equals(Material.CHEST))
			{
				if (block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
				{
					Sign sign = (Sign) block.getRelative(BlockFace.UP)
							.getState();
					if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
							"[KarmicShare]"))
					{
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
							BetterChest chest = new BetterChest(
									(Chest) block.getState());
							if (chest.isDoubleChest())
							{
								BetterChest adj = new BetterChest(
										(Chest) chest.attached());
								chest = adj;
							}
							chest.getInventory().clear();
							chest.update();
							if (plugin.getPluginConfig().chests)
							{
								int page = 1;
								try
								{
									page = Integer.parseInt(sign.getLine(3));
									populateChest(chest.getInventory(), page, chest.isDoubleChest());
									chest.update();
								}
								catch(NumberFormatException n)
								{
									event.getPlayer()
									.sendMessage(
											ChatColor.RED
													+ KarmicShare.prefix
													+ " Sign has wrong formatting. Remake sign.");
								}
							}
						}
						else
						{
							event.getPlayer()
									.sendMessage(
											ChatColor.RED
													+ KarmicShare.prefix
													+ " Lack permission: KarmicShare.chest");
							event.setCancelled(true);
						}
					}
				}
				else
				{
					// Check all 4 directions for adjacent chest
					for (BlockFace face : nav)
					{
						if (block.getRelative(face).getType()
								.equals(Material.CHEST))
						{
							final Block adjBlock = block.getRelative(face);
							if (adjBlock.getRelative(BlockFace.UP).getType()
									.equals(Material.WALL_SIGN))
							{
								Sign sign = (Sign) adjBlock.getRelative(
										BlockFace.UP).getState();
								if (ChatColor.stripColor(sign.getLine(1))
										.equalsIgnoreCase("[KarmicShare]"))
								{
									if (plugin.getPermissionHandler()
											.checkPermission(event.getPlayer(),
													"KarmicShare.chest"))
									{
										// populate chests
										BetterChest chest = new BetterChest(
												(Chest) block.getState());
										chest.getInventory().clear();
										chest.update();
										if (plugin.getPluginConfig().chests)
										{
											int page = 1;
											try
											{
												page = Integer.parseInt(sign.getLine(3));
												populateChest(chest.getInventory(), page, chest.isDoubleChest());
												chest.update();
											}
											catch(NumberFormatException n)
											{
												event.getPlayer()
												.sendMessage(
														ChatColor.RED
																+ KarmicShare.prefix
																+ " Sign has wrong formatting. Remake sign.");
											}
										}
									}
									else
									{
										event.getPlayer()
												.sendMessage(
														ChatColor.RED
																+ KarmicShare.prefix
																+ " Lack permission: KarmicShare.chest");
										event.setCancelled(true);
									}
								}
							}
						}
					}
				}

			}
		}
		else if (event.getAction() == Action.LEFT_CLICK_BLOCK)
		{
			final Block block = event.getClickedBlock();
			if (block.getType().equals(Material.CHEST))
			{
				if (block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
				{
					Sign sign = (Sign) block.getRelative(BlockFace.UP)
							.getState();
					if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
							"[KarmicShare]"))
					{
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
							BetterChest chest = new BetterChest(
									(Chest) block.getState());
							if (chest.isDoubleChest())
							{
								try
								{
									int page = grabNextPage(
											Integer.parseInt(""
													+ sign.getLine(3)), 54);
									sign.setLine(3, "" + page);
									sign.update();
								}
								catch (NumberFormatException e)
								{
									event.getPlayer()
											.sendMessage(
													ChatColor.RED
															+ KarmicShare.prefix
															+ " Sign has wrong formatting. Remake sign.");
								}
							}
							else
							{
								try
								{
									int page = grabNextPage(
											Integer.parseInt(""
													+ sign.getLine(3)), 27);
									sign.setLine(3, "" + page);
									sign.update();
								}
								catch (NumberFormatException e)
								{
									event.getPlayer()
											.sendMessage(
													ChatColor.RED
															+ KarmicShare.prefix
															+ " Sign has wrong formatting. Remake sign.");
								}
							}
							// TODO clear + repopulate
						}
						else
						{
							event.getPlayer()
									.sendMessage(
											ChatColor.RED
													+ KarmicShare.prefix
													+ " Lack permission: KarmicShare.chest");
							event.setCancelled(true);
						}
					}
				}
				else
				{
					// Check all 4 directions for adjacent chest
					for (BlockFace face : nav)
					{
						if (block.getRelative(face).getType()
								.equals(Material.CHEST))
						{
							final Block adjBlock = block.getRelative(face);
							if (adjBlock.getRelative(BlockFace.UP).getType()
									.equals(Material.WALL_SIGN))
							{
								Sign sign = (Sign) adjBlock.getRelative(
										BlockFace.UP).getState();
								if (ChatColor.stripColor(sign.getLine(1))
										.equalsIgnoreCase("[KarmicShare]"))
								{
									if (plugin.getPermissionHandler()
											.checkPermission(event.getPlayer(),
													"KarmicShare.chest"))
									{
										BetterChest chest = new BetterChest(
												(Chest) block.getState());
										if (chest.isDoubleChest())
										{
											try
											{
												int page = grabNextPage(
														Integer.parseInt(""
																+ sign.getLine(3)),
														54);
												sign.setLine(3, "" + page);
												sign.update();
											}
											catch (NumberFormatException e)
											{
												event.getPlayer()
														.sendMessage(
																ChatColor.RED
																		+ KarmicShare.prefix
																		+ " Sign has wrong formatting. Remake sign.");
											}
										}
										else
										{
											try
											{
												int page = grabNextPage(
														Integer.parseInt(""
																+ sign.getLine(3)),
														27);
												sign.setLine(3, "" + page);
												sign.update();
											}
											catch (NumberFormatException e)
											{
												event.getPlayer()
														.sendMessage(
																ChatColor.RED
																		+ KarmicShare.prefix
																		+ " Sign has wrong formatting. Remake sign.");
											}
										}
										// TODO clear + repopulate
									}
									else
									{
										event.getPlayer()
												.sendMessage(
														ChatColor.RED
																+ KarmicShare.prefix
																+ " Lack permission: KarmicShare.chest");
										event.setCancelled(true);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private int grabNextPage(int current, int limit) {
		int page = 1;
		// Grab total items
		ResultSet count = plugin.getLiteDB().select(
				"SELECT COUNT(*) FROM items;");
		try
		{
			if (count.next())
			{
				int total = count.getInt(1);
				if (!count.wasNull())
				{
					// Grab number of pages based off of what the chest can
					// store
					int num = total / limit;
					double rem = (double) total % (double) limit;
					if (rem != 0)
					{
						num++;
					}
					// increment current page
					page = current + 1;
					if (page < 0)
					{
						// Was negative... return it to first page
						page = 1;
					}
					else if (page > num)
					{
						// Going to page beyond the total items, cycle back to
						// first
						page = 1;
					}
					// Otherwise, its a valid page number, so send it off
				}
			}
			// Close select
			count.close();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			plugin.getLogger().warning(
					ChatColor.RED + KarmicShare.prefix + "SQL error.");
			e.printStackTrace();
		}
		return page;
	}

	private void populateChest(Inventory inventory, int page, boolean isDouble) {
		try
		{
			int count = 0;
			int limit = 27;
			if (isDouble)
			{
				limit = 54;
			}
			int start = (page - 1) * limit;
			ResultSet itemList = plugin.getLiteDB().select(
					"SELECT * FROM items;");
			if (itemList.next())
			{
				boolean done = false;
				do
				{
					if (count >= start)
					{
						// Generate item
						int id = itemList.getInt("itemid");
						byte data = itemList.getByte("data");
						short dur = itemList.getShort("durability");
						ItemStack item = new ItemStack(id, 1, dur, data);
						Item meta = new Item(id, data, dur);
						// If tool
						if (meta.isTool())
						{
							// Check for enchantments
							String enchantments = itemList
									.getString("enchantments");
							if (!itemList.wasNull())
							{
								String[] cut = enchantments.split("i");
								for (int i = 0; i < cut.length; i++)
								{
									String[] cutter = cut[i].split("v");
									EnchantmentWrapper e = new EnchantmentWrapper(
											Integer.parseInt(cutter[0]));
									item.addUnsafeEnchantment(
											e.getEnchantment(),
											Integer.parseInt(cutter[1]));
								}
							}
						}
						if (meta.isPotion())
						{
							// Remove data for full potion compatibility
							item = new ItemStack(id, 1, dur);
						}
						HashMap<Integer, ItemStack> residual = inventory
								.addItem(item);
						if (!residual.isEmpty())
						{
							done = true;
						}
					}
					count++;
				}
				while (itemList.next() && !done);
			}
			else
			{
				// No items to add.
				inventory.clear();
			}
			// Close select
			itemList.close();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			plugin.getLogger().warning(
					ChatColor.RED + KarmicShare.prefix + "SQL error.");
			e.printStackTrace();
		}
	}
}
