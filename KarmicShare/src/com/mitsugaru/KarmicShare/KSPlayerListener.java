package com.mitsugaru.KarmicShare;

import java.sql.SQLException;
import java.util.HashMap;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.splatbang.betterchest.BetterChest;

public class KSPlayerListener implements Listener {
	private KarmicShare plugin;
	private Karma karma;
	private static final BlockFace[] nav = { BlockFace.NORTH, BlockFace.SOUTH,
			BlockFace.EAST, BlockFace.WEST };

	public KSPlayerListener(KarmicShare karmicShare) {
		plugin = karmicShare;
		karma = plugin.getKarma();
	}
	
	public void onEntityInteract(EntityInteractEvent event)
	{
		plugin.getLogger().info("Entity interact");
	}

	@EventHandler(priority = EventPriority.NORMAL)
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
						final String group = ChatColor.stripColor(
								sign.getLine(0)).toLowerCase();
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
							if (karma.playerHasGroup(event.getPlayer(), event.getPlayer().getName(),
									group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
							{
								BetterChest chest = new BetterChest(
										(Chest) block.getState());
								if (chest.isDoubleChest())
								{
									BetterChest adj = new BetterChest(chest.attached());
									chest = adj;
								}
								chest.getInventory().clear();
								chest.update();
								if (plugin.getPluginConfig().chests && plugin.useChest())
								{
									int page = 1;
									try
									{
										page = Integer
												.parseInt(sign.getLine(3));
										populateChest(chest.getInventory(),
												page, chest.isDoubleChest(),
												group);
										chest.update();
									}
									catch (NumberFormatException n)
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
									event.getPlayer().sendMessage(ChatColor.RED + KarmicShare.prefix
											+ " Spout not found. Cannot use physical chests.");
								}
							}
							else
							{
								event.getPlayer().sendMessage(
										ChatColor.RED + KarmicShare.prefix
												+ " Not part of group "
												+ ChatColor.GRAY + group);
								event.setCancelled(true);
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
									final String group = ChatColor.stripColor(
											sign.getLine(0)).toLowerCase();
									if (plugin.getPermissionHandler()
											.checkPermission(event.getPlayer(),
													"KarmicShare.chest"))
									{
										if (karma.playerHasGroup(event.getPlayer(), event.getPlayer()
												.getName(), group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
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
													page = Integer
															.parseInt(sign
																	.getLine(3));
													populateChest(
															chest.getInventory(),
															page,
															chest.isDoubleChest(),
															group);
													chest.update();
												}
												catch (NumberFormatException n)
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
																	+ " Not part of group "
																	+ ChatColor.GRAY
																	+ group);
											event.setCancelled(true);
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
			else if (block.getType().equals(Material.WALL_SIGN))
			{
				Sign sign = (Sign) block.getState();
				if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
						"[KarmicShare]"))
				{
					final String group = ChatColor.stripColor(sign.getLine(0))
							.toLowerCase();

					if (plugin.getPermissionHandler().checkPermission(
							event.getPlayer(), "KarmicShare.chest"))
					{
						if (karma.playerHasGroup(event.getPlayer(), event.getPlayer().getName(), group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
						{
							if (block.getRelative(BlockFace.DOWN).getType()
									.equals(Material.CHEST))
							{
								BetterChest chest = new BetterChest(
										(Chest) block.getRelative(
												BlockFace.DOWN).getState());
								final String name = event.getPlayer().getName();
								if (chest.isDoubleChest())
								{
									try
									{
										int page;
										if(plugin.getCommander().getChestPage().containsKey(name))
										{
											page = grabNextPage(plugin.getCommander().getChestPage().get(name).intValue() - 1, 54, group, false);
											plugin.getCommander().getChestPage().remove(name);
										}
										else
										{
											page = grabNextPage(
													Integer.parseInt(""
															+ sign.getLine(3)), 54,
													group, true);
										}
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
										int page;
										if(plugin.getCommander().getChestPage().containsKey(name))
										{
											page = grabNextPage(plugin.getCommander().getChestPage().get(name).intValue() - 1, 27, group, false);
											plugin.getCommander().getChestPage().remove(name);
										}
										else
										{
											page = grabNextPage(
													Integer.parseInt(""
															+ sign.getLine(3)), 27,
													group, true);
										}
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
							}
						}
						else
						{
							event.getPlayer().sendMessage(
									ChatColor.RED + KarmicShare.prefix
											+ " Not part of group "
											+ ChatColor.GRAY + group);
							event.setCancelled(true);
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
						final String group = ChatColor.stripColor(
								sign.getLine(0)).toLowerCase();
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
							if (karma.playerHasGroup(event.getPlayer(), event.getPlayer().getName(),
									group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
							{
								BetterChest chest = new BetterChest(
										(Chest) block.getState());
								if (chest.isDoubleChest())
								{
									try
									{
										int page = grabNextPage(
												Integer.parseInt(""
														+ sign.getLine(3)), 54,
												group, false);
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
														+ sign.getLine(3)), 27,
												group, false);
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
							}
							else
							{
								event.getPlayer().sendMessage(
										ChatColor.RED + KarmicShare.prefix
												+ " Not part of group "
												+ ChatColor.GRAY + group);
								event.setCancelled(true);
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
									String group = ChatColor.stripColor(
											sign.getLine(0)).toLowerCase();
									if (plugin.getPermissionHandler()
											.checkPermission(event.getPlayer(),
													"KarmicShare.chest"))
									{
										if (karma.playerHasGroup(event.getPlayer(), event.getPlayer()
												.getName(), group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
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
															54, group, false);
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
															27, group, false);
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
										}
										else
										{
											event.getPlayer()
													.sendMessage(
															ChatColor.RED
																	+ KarmicShare.prefix
																	+ " Not part of group "
																	+ ChatColor.GRAY
																	+ group);
											event.setCancelled(true);
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
			else if (block.getType().equals(Material.WALL_SIGN))
			{
				Sign sign = (Sign) block.getState();
				if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
						"[KarmicShare]"))
				{
					final String group = ChatColor.stripColor(sign.getLine(0))
							.toLowerCase();
					if (plugin.getPermissionHandler().checkPermission(
							event.getPlayer(), "KarmicShare.chest"))
					{
						if (karma.playerHasGroup(event.getPlayer(), event.getPlayer().getName(), group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
						{
							if (block.getRelative(BlockFace.DOWN).getType()
									.equals(Material.CHEST))
							{
								BetterChest chest = new BetterChest(
										(Chest) block.getRelative(
												BlockFace.DOWN).getState());
								if (chest.isDoubleChest())
								{
									try
									{
										int page = grabNextPage(
												Integer.parseInt(""
														+ sign.getLine(3)), 54,
												group, false);
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
														+ sign.getLine(3)), 27,
												group, false);
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
							}
						}
						else
						{
							event.getPlayer().sendMessage(
									ChatColor.RED + KarmicShare.prefix
											+ " Not part of group "
											+ ChatColor.GRAY + group);
							event.setCancelled(true);
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

	private int grabNextPage(int current, int limit, String group, boolean backwards)
	{
		//Calculate number of slots
		int slots = 0;
		Query all = plugin.getDatabaseHandler().select("SELECT * FROM "
						+ plugin.getPluginConfig().tablePrefix
						+ "items WHERE groups='" + group + "';");
		try
		{
			if(all.getResult().next())
			{
				do
				{
					final int amount = all.getResult().getInt("amount");
					if(!all.getResult().wasNull())
					{
						final ItemStack item = new ItemStack(all.getResult().getInt("itemid"), amount);
						int maxStack = item.getType().getMaxStackSize();
						if(maxStack <= 0)
						{
							maxStack = 1;
						}
						int stacks = amount / maxStack;
						final double rem = (double) amount % (double) maxStack;
						if(rem != 0)
						{
							stacks++;
						}
						slots += stacks;
					}
				}while(all.getResult().next());
			}
			all.closeQuery();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			plugin.getLogger().warning(
					ChatColor.RED + KarmicShare.prefix + "SQL error.");
			e.printStackTrace();
		}
		//if no slots, return 1
		if(slots <= 0)
		{
			return 1;
		}
		//Calculate pages
		int pageTotal = slots / limit;
		final double rem = (double) slots % (double) limit;
		if(rem != 0)
		{
			pageTotal++;
		}
		//Check against maximum
		if(current >= Integer.MAX_VALUE)
		{
			//Cycle back as we're at the max value for an integer
			return 1;
		}
		int page = 1;
		if(backwards)
		{
			page = current - 1;
		}
		else
		{
			page = current + 1;
		}
		if (page <= 0)
		{
			// Was negative or zero, loop back to max page
			page = (pageTotal  +1);
		}
		//Allow for empty page
		else if (page > (pageTotal +1))
		{
			// Going to page beyond the total items, cycle back to
			// first
			page = 1;
		}
		return page;
	}

	private void populateChest(Inventory inventory, int page, boolean isDouble,
			String group) {
		try
		{
			int count = 0;
			int limit = 27;
			if (isDouble)
			{
				limit = 54;
			}
			int start = (page - 1) * limit;
			Query itemList = plugin.getDatabaseHandler().select(
					"SELECT * FROM "
						+ plugin.getPluginConfig().tablePrefix
						+ "items WHERE groups='" + group + "';");
			if (itemList.getResult().next())
			{
				boolean done = false;
				do
				{
					// Generate item
					int id = itemList.getResult().getInt("itemid");
					int amount = itemList.getResult().getInt("amount");
					byte data = itemList.getResult().getByte("data");
					short dur = itemList.getResult().getShort("durability");
					ItemStack item = new ItemStack(id, amount, dur, data);
					//Generate psudo item to calculate slots taken up
					int maxStack = item.getType().getMaxStackSize();
					if(maxStack <= 0)
					{
						maxStack = 1;
					}
					int stacks = amount / maxStack;
					final double rem = (double) amount % (double) maxStack;
					if(rem != 0)
					{
						stacks++;
					}
					for(int x = 0; x < stacks; x++)
					{
						ItemStack add = item.clone();
						if(amount < maxStack)
						{
							add.setAmount(amount);
						}
						else
						{
							add.setAmount(maxStack);
							amount -= maxStack;
						}

					if (count >= start)
					{
						Item meta = new Item(id, data, dur);
						// If tool
						if (meta.isTool())
						{
								// Check for enchantments
								String enchantments = itemList.getResult()
										.getString("enchantments");
								if (!itemList.getResult().wasNull())
								{
									String[] cut = enchantments.split("i");
									for (int s = 0; s < cut.length; s++)
									{
										String[] cutter = cut[s].split("v");
										EnchantmentWrapper e = new EnchantmentWrapper(
												Integer.parseInt(cutter[0]));
										add.addUnsafeEnchantment(
												e.getEnchantment(),
												Integer.parseInt(cutter[1]));
									}
								}
								final HashMap<Integer, ItemStack> residual = inventory
										.addItem(add);
								if (!residual.isEmpty())
								{
									done = true;
								}
						}
						else if (meta.isPotion())
						{
								// Remove data for full potion compatibility
								item = new ItemStack(id, amount, dur);
								final HashMap<Integer, ItemStack> residual = inventory
										.addItem(add);
								if (!residual.isEmpty())
								{
									done = true;
								}
						}
						else
						{
							final HashMap<Integer, ItemStack> residual = inventory
									.addItem(add);
							if (!residual.isEmpty())
							{
								done = true;
							}
						}
					}
					count++;
					}
				}
				while (itemList.getResult().next() && !done);
			}
			else
			{
				// No items to add.
				inventory.clear();
			}
			// Close select
			itemList.closeQuery();
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
