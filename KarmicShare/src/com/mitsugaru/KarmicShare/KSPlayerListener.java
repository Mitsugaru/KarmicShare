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
import org.bukkit.command.CommandSender;
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
						final String group = ChatColor.stripColor(
								sign.getLine(0)).toLowerCase();
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
							if (playerHasGroup(event.getPlayer(), event.getPlayer().getName(),
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
								if (plugin.getPluginConfig().chests && plugin.hasSpout)
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
										if (playerHasGroup(event.getPlayer(), event.getPlayer()
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
						if (playerHasGroup(event.getPlayer(), event.getPlayer().getName(), group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
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
												group, true);
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
												group, true);
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
							if (playerHasGroup(event.getPlayer(), event.getPlayer().getName(),
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
										if (playerHasGroup(event.getPlayer(), event.getPlayer()
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
						if (playerHasGroup(event.getPlayer(), event.getPlayer().getName(), group) || plugin.getPermissionHandler().checkPermission(event.getPlayer(), "KarmicShare.ignore.group"))
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
		ResultSet all = plugin.getDatabaseHandler().select("SELECT * FROM '"
						+ plugin.getPluginConfig().tablePrefix
						+ "items' WHERE groups='" + group + "';");
		try
		{
			if(all.next())
			{
				do
				{
					final int amount = all.getInt("amount");
					if(!all.wasNull())
					{
						final ItemStack item = new ItemStack(all.getInt("itemid"), amount);
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
				}while(all.next());
			}
			all.close();
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
			ResultSet itemList = plugin.getDatabaseHandler().select(
					"SELECT * FROM '"
						+ plugin.getPluginConfig().tablePrefix
						+ "items' WHERE groups='" + group + "';");
			if (itemList.next())
			{
				boolean done = false;
				do
				{
					// Generate item
					int id = itemList.getInt("itemid");
					int amount = itemList.getInt("amount");
					byte data = itemList.getByte("data");
					short dur = itemList.getShort("durability");
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
								String enchantments = itemList
										.getString("enchantments");
								if (!itemList.wasNull())
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

	private boolean playerHasGroup(CommandSender sender, String name, String group)
	{
		if(group.equals("global"))
		{
			return true;
		}
		boolean has = false;
		try
		{
			//Insures that the player is added to the database
			getPlayerKarma(name);
			String groups = "";
			ResultSet rs = plugin.getDatabaseHandler().select("SELECT * FROM '"
						+ plugin.getPluginConfig().tablePrefix
						+ "players' WHERE playername='" + name + "';");
			if(rs.next())
			{
				groups = rs.getString("groups");
				if(!rs.wasNull())
				{
					if(groups.contains("&"))
					{
						//they have multiple groups
						for(String s : groups.split("&"))
						{
							if(s.equals(group))
							{
								has = true;
							}
						}
					}
					else
					{
						//they only have one group
						if(groups.equals(group))
						{
							has = true;
						}
					}
				}
			}
			rs.close();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			sender.sendMessage(ChatColor.RED + KarmicShare.prefix
					+ " SQL Exception");
			e.printStackTrace();
		}
		return has;
	}

	/**
	 * Retrieves karma value of a player from the database. Forces player to be
	 * added to database if they don't exist
	 *
	 * @param Player
	 *            name
	 * @return karma value associated with name
	 */
	private int getPlayerKarma(String name) throws SQLException {
		String query = "SELECT * FROM '"
						+ plugin.getPluginConfig().tablePrefix
						+ "players' WHERE playername='" + name + "';";
		ResultSet rs = plugin.getDatabaseHandler().select(query);
		int karma = plugin.getPluginConfig().playerKarmaDefault;
		boolean has = false;
		// Retrieve karma from database
		try
		{
			if (rs.next())
			{
				do
				{
					// Grab player karma value
					karma = rs.getInt("karma");
					has = true;
				}
				while (rs.next());
			}
			rs.close();
			if (!has)
			{
				// Player not in database, therefore add them
				query = "INSERT INTO '"
						+ plugin.getPluginConfig().tablePrefix
						+ "players' (playername,karma) VALUES ('"
						+ name + "','" + karma + "');";
				plugin.getDatabaseHandler().standardQuery(query);
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		return karma;
	}
}
