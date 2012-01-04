package com.mitsugaru.KarmicShare;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.event.inventory.InventoryClickEvent;
import org.getspout.spoutapi.event.inventory.InventoryListener;

import com.splatbang.betterchest.BetterChest;

public class KSInventoryListener extends InventoryListener {
	private KarmicShare plugin;

	public KSInventoryListener(KarmicShare karmicShare) {
		plugin = karmicShare;
	}

	@Override
	public void onInventoryClick(InventoryClickEvent event) {
		// This will verify that it was a block
		if (event.getLocation() != null)
		{
			// Valid slot numbers are not negative
			if (event.getSlot() >= 0)
			{
				// Verify that it is a chest
				final Block block = event.getLocation().getBlock();
				if (block.getType().equals(Material.CHEST))
				{
					// Don't know if getInventory null check is necessary
					if (event.getInventory() != null
							&& plugin.getPluginConfig().chests)
					{
						boolean kschest = false;
						boolean fromChest = false;
						final BetterChest chest = new BetterChest(
								(Chest) block.getState());
						if (!event.getInventory().equals(
								event.getPlayer().getInventory()))
						{
							fromChest = true;
						}
						if (fromChest || event.isShiftClick())
						{
							// Player is working on inventory that is not theirs
							// Verify that it is one of our chests
							if (block.getRelative(BlockFace.UP).getType()
									.equals(Material.WALL_SIGN))
							{
								final Sign sign = (Sign) block.getRelative(
										BlockFace.UP).getState();
								if (ChatColor.stripColor(sign.getLine(1))
										.equalsIgnoreCase("[KarmicShare]"))
								{
									kschest = true;
								}
							}
							else if (chest.isDoubleChest())
							{
								if (chest.attachedBlock()
										.getRelative(BlockFace.UP).getType()
										.equals(Material.WALL_SIGN))
								{
									final Sign sign = (Sign) chest
											.attachedBlock()
											.getRelative(BlockFace.UP)
											.getState();
									if (ChatColor.stripColor(sign.getLine(1))
											.equalsIgnoreCase("[KarmicShare]"))
									{
										kschest = true;
									}
								}
							}
						}
						if (kschest)
						{
							if (event.isLeftClick())
							{
								if (event.isShiftClick())
								{
									// We don't care about the cursor as it
									// doesn't
									// get changed on a shift click
									if (event.getItem() != null)
									{
										if (fromChest)
										{
											if (takeItem(event.getPlayer(),
													event.getItem()))
											{
												event.setResult(Event.Result.ALLOW);
												if (hasItem(event.getPlayer(),
														event.getItem()))
												{
													final ItemStack temp = event
															.getItem();
													temp.setAmount(1);
													final Repopulate task = new Repopulate(
															event.getInventory(),
															temp, event
																	.getSlot());
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 10);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
												}
											}
											else
											{
												event.setResult(Event.Result.DENY);
												event.setCancelled(true);
											}
										}
										else
										{

											if (giveItem(event.getPlayer(),
													event.getItem()))
											{
												if (chest
														.getInventory()
														.contains(
																event.getItem()
																		.getTypeId()))
												{
													//Chest already contains it, do not stack
													event.setResult(Event.Result.ALLOW);
													event.setItem(null);
												}
												else if (chest.getInventory()
														.firstEmpty() == -1)
												{
													event.setResult(Event.Result.ALLOW);
													// Handle if inventory is
													// full
													event.setItem(null);
												}
											}
											else
											{
												event.setResult(Event.Result.DENY);
												event.setCancelled(true);
											}
										}
									}
								}
								else
								{
									if (event.getItem() != null
											&& event.getCursor() != null)
									{

										if (event
												.getItem()
												.getType()
												.equals(event.getCursor()
														.getType()))
										{
											if (giveItem(event.getPlayer(),
													event.getCursor()))
											{
												event.setResult(Event.Result.ALLOW);
												final ItemStack temp = event
														.getItem();
												temp.setAmount(1);
												final Repopulate task = new Repopulate(
														event.getInventory(),
														temp, event.getSlot());
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task,
																10);
												if (id == -1)
												{
													event.getPlayer()
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ "Could not repopulate slot.");
												}
											}
											else
											{
												event.setResult(Event.Result.DENY);
												event.setCancelled(true);
											}
										}
										else
										{
											// When switching, put item first,
											// then
											// attempt to take item
											if (giveItem(event.getPlayer(),
													event.getCursor()))
											{
												if (takeItem(event.getPlayer(),
														event.getItem()))
												{
													event.setResult(Event.Result.ALLOW);
													if (hasItem(
															event.getPlayer(),
															event.getItem()))
													{
														final ItemStack temp = event
																.getItem();
														temp.setAmount(1);
														final Repopulate task = new Repopulate(
																event.getInventory(),
																temp,
																event.getSlot());
														int id = plugin
																.getServer()
																.getScheduler()
																.scheduleSyncDelayedTask(
																		plugin,
																		task,
																		10);
														if (id == -1)
														{
															event.getPlayer()
																	.sendMessage(
																			ChatColor.YELLOW
																					+ KarmicShare.prefix
																					+ "Could not repopulate slot.");
														}
													}
												}
												else
												{
													event.setResult(Event.Result.DENY);
													event.setCancelled(true);
												}
											}
											else
											{
												event.setResult(Event.Result.DENY);
												event.setCancelled(true);
											}
										}
									}
									else if (event.getItem() != null)
									{
										if (takeItem(event.getPlayer(),
												event.getItem()))
										{
											event.setResult(Event.Result.ALLOW);
											if (hasItem(event.getPlayer(),
													event.getItem()))
											{
												final ItemStack temp = event
														.getItem();
												temp.setAmount(1);
												final Repopulate task = new Repopulate(
														event.getInventory(),
														temp, event.getSlot());
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task,
																10);
												if (id == -1)
												{
													event.getPlayer()
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ "Could not repopulate slot.");
												}
											}
										}
										else
										{
											event.setResult(Event.Result.DENY);
											event.setCancelled(true);
										}
									}
									else if (event.getCursor() != null)
									{

										// they clicked on an item in chest
										if (giveItem(event.getPlayer(),
												event.getCursor()))
										{
											event.setResult(Event.Result.ALLOW);
										}
										else
										{
											event.setResult(Event.Result.DENY);
											event.setCancelled(true);
										}
									}
								}
							}
							else
							{
								event.getPlayer()
										.sendMessage(
												ChatColor.RED
														+ KarmicShare.prefix
														+ " Not allowed to right-click in chest");
								event.setResult(Event.Result.DENY);
								event.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}

	public boolean hasItem(Player player, ItemStack item) {
		// Check if pool contains item requested + amount
		boolean has = false;
		// SQL query to see if item is in pool
		// Create temp item to check if its a tool
		final Item temp = new Item(item.getTypeId(), item.getData().getData(),
				item.getDurability());
		String query = "";
		int poolAmount = 0;
		if (temp.isTool())
		{
			// Handle tools
			// Grab all entries of the same tool id
			String toolQuery = "SELECT * FROM items WHERE itemid='"
					+ item.getTypeId() + "';";
			ResultSet toolRS = plugin.getLiteDB().select(toolQuery);
			try
			{
				if (toolRS.next())
				{
					do
					{
						poolAmount += toolRS.getInt("amount");
					}
					while (toolRS.next());
					if (poolAmount >= item.getAmount())
					{
						// We have enough in pool that
						// was requested
						has = true;
					}
				}
				else
				{
					has = false;
				}
				toolRS.close();
			}
			catch (SQLException e)
			{
				// INFO Auto-generated catch block
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return false;
			}
		}

		else if (temp.isPotion())
		{
			// Separate check to see if its a potion and handle it
			// via the durability info
			query = "SELECT * FROM items WHERE itemid='" + item.getTypeId()
					+ "' AND durability='" + item.getDurability() + "';";
			ResultSet rs = plugin.getLiteDB().select(query);

			// Check ResultSet
			try
			{
				if (rs.next())
				{
					// Item already in pool, check
					// amount
					poolAmount = rs.getInt("amount");
					if (poolAmount >= item.getAmount())
					{
						// We have enough in pool that
						// was requested
						has = true;
					}
				}
				else
				{
					// Item not in database, therefore error
					// on player part
					rs.close();
					player.sendMessage(ChatColor.RED + KarmicShare.prefix
							+ " Item not in pool...");
					return false;
				}
				rs.close();
			}
			catch (SQLException e)
			{
				// INFO Auto-generated catch block
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return false;
			}
		}
		else
		{
			// Not a tool or potion
			query = "SELECT * FROM items WHERE itemid='" + item.getTypeId()
					+ "' AND data='" + item.getData().getData() + "';";
			ResultSet rs = plugin.getLiteDB().select(query);

			// Check ResultSet
			try
			{
				if (rs.next())
				{
					// Item already in pool, check
					// amount
					poolAmount = rs.getInt("amount");
					if (poolAmount >= item.getAmount())
					{
						// We have enough in pool that
						// was requested
						has = true;
					}
				}
				else
				{
					// Item not in database, therefore error
					// on player part
					rs.close();
					return false;
				}
				rs.close();
			}
			catch (SQLException e)
			{
				// INFO Auto-generated catch block
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return false;
			}
		}
		return has;
	}

	public boolean takeItem(Player player, ItemStack item) {
		// Check if they have "take" permission
		if (plugin.getPermissionHandler().checkPermission(player,
				"KarmicShare.take"))
		{
			int karma = 0;
			if (!plugin.getPluginConfig().karmaDisabled)
			{
				// Check karma before anything
				karma = plugin.getPluginConfig().playerKarmaDefault;
				try
				{
					karma = getPlayerKarma(player.getName());
					if (karma <= plugin.getPluginConfig().lower)
					{
						// They are at the limit, or somehow lower for
						// whatever reason
						player.sendMessage(ChatColor.RED + KarmicShare.prefix
								+ "Your karma is at the limit!");
						return false;
					}
				}
				catch (SQLException e1)
				{
					// INFO Auto-generated catch block
					player.sendMessage(ChatColor.RED + KarmicShare.prefix
							+ " Could not retrieve player karma");
					e1.printStackTrace();
					return false;
				}
			}
			int amount = item.getAmount();
			String query = "";
			boolean has = this.hasItem(player, item);
			Item temp = new Item(item.getTypeId(), item.getData().getData(),
					item.getDurability());
			try
			{
				if (has)
				{
					boolean hasKarma = false;
					if (!plugin.getPluginConfig().karmaDisabled)
					{
						// Check karma again, before giving item, to
						// adjust amount
						// based on karma and karma multipliers
						int karmaAdj = 0;
						if (plugin.getPluginConfig().statickarma)
						{
							// Using static karma, everything goes
							// by
							// the config's default karma change
							// value
							if (karmaAdj < plugin.getPluginConfig().lower)
							{
								karmaAdj = karma
										+ (plugin.getPluginConfig().karmaChange
												* amount * -1);
								// They went beyond the lower limit
								// adjust amount given based on
								// karma now
								amount = Math
										.abs(plugin.getPluginConfig().lower)
										- Math.abs(karma);
								amount = amount
										/ plugin.getPluginConfig().karmaChange;
								if (amount == 0)
								{
									// Cannot give any items as
									// they'd go beyond
									// karma limit
									player.sendMessage(ChatColor.RED
											+ KarmicShare.prefix
											+ " Not enough karma to take item");
									return false;
								}
								else
								{
									player.sendMessage(ChatColor.YELLOW
											+ KarmicShare.prefix
											+ " Near/Hit karma limit!");
								}
							}
						}
						else
						{
							// Using per-item karma
							Item[] karmaList = plugin.getPluginConfig().karma
									.keySet().toArray(new Item[0]);
							// Check if requested item is in the
							// karma list
							for (Item k : karmaList)
							{
								if (k.areSame(temp))
								{
									// Item karma needs to be
									// adjusted
									hasKarma = true;
								}
							}
							if (hasKarma)
							{
								try
								{
									karmaAdj = karma
											+ (plugin.getPluginConfig().karma
													.get(temp) * amount * -1);
									if (karmaAdj < plugin.getPluginConfig().lower)
									{
										// They went beyond the
										// lower limit
										// adjust amount given based
										// on karma now
										int tempKarma = Math.abs(karmaAdj)
												- Math.abs(plugin
														.getPluginConfig().lower);
										int div = tempKarma
												/ plugin.getPluginConfig().karma
														.get(temp);
										int rem = tempKarma
												% plugin.getPluginConfig().karma
														.get(temp);
										if (rem != 0)
										{
											div++;
										}
										amount -= div;
										if (amount <= 0)
										{
											// Cannot give any items
											// as they'd go beyond
											// karma limit
											player.sendMessage(ChatColor.RED
													+ KarmicShare.prefix
													+ " Not enough karma to take item");
											return false;
										}
										else
										{
											player.sendMessage(ChatColor.YELLOW
													+ KarmicShare.prefix
													+ " Near/Hit karma limit!");
										}
									}
								}
								catch (NullPointerException n)
								{
									// Found item, but there is no
									// config for specific data
									// value
									// thus adjust using regular
									// means
									karmaAdj = karma
											+ (plugin.getPluginConfig().karmaChange
													* amount * -1);
									if (karmaAdj < plugin.getPluginConfig().lower)
									{
										// They went beyond the
										// lower limit
										// adjust amount given based
										// on karma now
										int tempKarma = Math.abs(karmaAdj)
												- Math.abs(plugin
														.getPluginConfig().lower);
										int div = tempKarma
												/ plugin.getPluginConfig().karmaChange;
										int rem = tempKarma
												% plugin.getPluginConfig().karmaChange;
										if (rem != 0)
										{
											div++;
										}
										amount -= div;
										if (amount <= 0)
										{
											player.sendMessage(ChatColor.RED
													+ KarmicShare.prefix
													+ " Not enough karma to take item");
											return false;
										}
										else
										{
											player.sendMessage(ChatColor.YELLOW
													+ KarmicShare.prefix
													+ " Near/Hit karma limit!");
										}
									}
									// Reset so later we use default
									// karma change
									hasKarma = false;
								}
							}
							else
							{
								// Item does not have a multiplier,
								// so use default
								karmaAdj = karma
										+ (plugin.getPluginConfig().karmaChange
												* amount * -1);
								if (karmaAdj < plugin.getPluginConfig().lower)
								{
									// They went beyond the lower
									// limit
									// adjust amount given based on
									// karma now
									int tempKarma = Math.abs(karmaAdj)
											- Math.abs(plugin.getPluginConfig().lower);
									int div = tempKarma
											/ plugin.getPluginConfig().karmaChange;
									int rem = tempKarma
											% plugin.getPluginConfig().karmaChange;
									if (rem != 0)
									{
										div++;
									}
									amount -= div;
									amount = amount
											/ plugin.getPluginConfig().karmaChange;
									if (amount <= 0)
									{
										// Cannot give any items as
										// they'd go beyond
										// karma limit
										player.sendMessage(ChatColor.RED
												+ KarmicShare.prefix
												+ " Not enough karma to take item");
										return false;
									}
									else
									{
										player.sendMessage(ChatColor.YELLOW
												+ KarmicShare.prefix
												+ " Near/Hit karma limit!");
									}
								}
							}
						}
					}
					if (temp.isTool())
					{
						// Handle tools
						try
						{
							String toolQuery = "";
							if (!item.getEnchantments().isEmpty())
							{
								// Enchanted
								StringBuilder sb = new StringBuilder();
								for (Map.Entry<Enchantment, Integer> e : item
										.getEnchantments().entrySet())
								{
									sb.append(e.getKey().getId() + "v"
											+ e.getValue().intValue() + "i");
								}
								// Remove trailing comma
								sb.deleteCharAt(sb.length() - 1);
								toolQuery = "SELECT * FROM items WHERE itemid='"
										+ item.getTypeId()
										+ "' AND data='"
										+ item.getData().getData()
										+ "' AND enchantments='"
										+ sb.toString() + "';";
								ResultSet toolRS = plugin.getLiteDB().select(
										toolQuery);
								if (toolRS.next())
								{
									if (amount == toolRS.getInt("amount"))
									{
										// DROP
										toolQuery = "DELETE FROM items WHERE itemid='"
												+ item.getTypeId()
												+ "' AND amount='"
												+ amount
												+ "' AND data='"
												+ item.getData().getData()
												+ "' AND enchantments='"
												+ sb.toString() + "';";
									}
									else
									{
										// UPDATE
										toolQuery = "UPDATE items SET amount='"
												+ (toolRS.getInt("amount") - amount)
												+ "' WHERE itemid='"
												+ item.getTypeId()
												+ "' AND data='"
												+ item.getData().getData()
												+ "' AND enchantments='"
												+ sb.toString() + "';";
									}
								}
								toolRS.close();
							}
							else
							{
								// Non-enchanted tool
								toolQuery = "SELECT * FROM items WHERE itemid='"
										+ item.getTypeId()
										+ "' AND data='"
										+ item.getData().getData() + "';";
								ResultSet toolRS = plugin.getLiteDB().select(
										toolQuery);
								if (toolRS.next())
								{
									if (amount == toolRS.getInt("amount"))
									{
										// DROP
										toolQuery = "DELETE FROM items WHERE itemid='"
												+ item.getTypeId()
												+ "' AND amount='"
												+ amount
												+ "' AND data='"
												+ item.getData().getData()
												+ "';";
									}
									else
									{
										// UPDATE
										toolQuery = "UPDATE items SET amount='"
												+ (toolRS.getInt("amount") - amount)
												+ "' WHERE itemid='"
												+ item.getTypeId()
												+ "' AND data='"
												+ item.getData().getData()
												+ "';";
									}
								}
								toolRS.close();
							}
							plugin.getLiteDB().standardQuery(toolQuery);
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
							return false;
						}
					}
					else if (temp.isPotion())
					{
						query = "SELECT * FROM items WHERE itemid='"
								+ item.getTypeId() + "' AND durability='"
								+ item.getDurability() + "';";
						ResultSet rs = plugin.getLiteDB().select(query);
						try
						{
							if (rs.next())
							{
								if (amount == rs.getInt("amount"))
								{
									// Drop record as there are none left
									query = "DELETE FROM items WHERE itemid='"
											+ item.getTypeId()
											+ "' AND durability='"
											+ item.getDurability() + "';";
								}
								else
								{
									query = "UPDATE items SET amount='"
											+ (rs.getInt("amount") - amount)
											+ "' WHERE itemid='"
											+ item.getTypeId()
											+ "' AND durability='"
											+ item.getDurability() + "';";
								}
							}
							rs.close();
							plugin.getLiteDB().standardQuery(query);
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
							return false;
						}
					}
					else
					{
						query = "SELECT * FROM items WHERE itemid='"
								+ item.getTypeId() + "' AND data='"
								+ item.getData().getData() + "';";
						ResultSet rs = plugin.getLiteDB().select(query);
						try
						{
							if (rs.next())
							{
								if (amount == rs.getInt("amount"))
								{
									// Drop record as there are none left
									query = "DELETE FROM items WHERE itemid='"
											+ item.getTypeId() + "' AND data='"
											+ item.getData().getData() + "';";
								}
								else
								{
									query = "UPDATE items SET amount='"
											+ (rs.getInt("amount") - amount)
											+ "' WHERE itemid='"
											+ item.getTypeId() + "' AND data='"
											+ item.getData().getData() + "';";
								}
							}
							rs.close();
							plugin.getLiteDB().standardQuery(query);
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
							return false;
						}
					}
					// Smoke effect
					this.smokePlayer(player);
					// Update karma
					if (!plugin.getPluginConfig().karmaDisabled)
					{
						if (hasKarma)
						{
							this.updatePlayerKarma(player.getName(), amount
									* plugin.getPluginConfig().karma.get(temp)
									* -1);
						}
						else
						{
							this.updatePlayerKarma(player.getName(), amount
									* plugin.getPluginConfig().karmaChange * -1);
						}
					}
					// Update cache
					if (!plugin.getCommander().getCache().isEmpty())
					{
						if (plugin.getCommander().getCache().containsKey(temp))
						{
							int cacheAmount = plugin.getCommander().getCache()
									.get(temp).intValue();
							if ((cacheAmount - amount) == 0)
							{
								plugin.getCommander().getCache().remove(temp);
							}
							else
							{
								plugin.getCommander().getCache()
										.put(temp, (cacheAmount - amount));
							}
						}
					}
				}
				else
				{
					player.sendMessage(ChatColor.RED + KarmicShare.prefix
							+ " Item is no longer available.");
					return false;
				}
			}
			catch (SQLException e)
			{
				// INFO Auto-generated catch block
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return false;
			}
			return true;
		}
		else
		{
			player.sendMessage(ChatColor.RED + KarmicShare.prefix
					+ " Lack permission: KarmicShare.take");
		}
		return false;
	}

	public boolean giveItem(Player player, ItemStack item) {
		if (plugin.getPermissionHandler().checkPermission(player,
				"KarmicShare.give"))
		{
			final Item i = new Item(item.getTypeId(), item.getData().getData(),
					item.getDurability());
			// Check if its a tool
			boolean hasEnchantments = false;
			String query = "";
			if (i.isTool())
			{
				// Check if enchanted
				Map<Enchantment, Integer> enchantments = item.getEnchantments();
				if (!enchantments.isEmpty())
				{
					// Tool has enchantments
					hasEnchantments = true;
					StringBuilder sb = new StringBuilder();
					for (Map.Entry<Enchantment, Integer> e : enchantments
							.entrySet())
					{
						sb.append(e.getKey().getId() + "v"
								+ e.getValue().intValue() + "i");
					}
					// Remove trailing comma
					sb.deleteCharAt(sb.length() - 1);
					// Add new instance of item to database
					query = "INSERT INTO items (itemid,amount,data,durability,enchantments) VALUES ('"
							+ item.getTypeId()
							+ "','"
							+ item.getAmount()
							+ "','"
							+ item.getData().getData()
							+ "','"
							+ item.getDurability()
							+ "','"
							+ sb.toString()
							+ "');";
					plugin.getLiteDB().standardQuery(query);
				}
			}
			else if (i.isPotion())
			{
				// Handle potion case
				// Potion item
				// Create SQL query to see if item is already in
				// database
				query = "SELECT * FROM items WHERE itemid='" + item.getTypeId()
						+ "' AND durability='" + item.getDurability() + "';";
				ResultSet rs = plugin.getLiteDB().select(query);

				// Send Item to database
				try
				{
					if (rs.next())
					{
						do
						{
							// For potions, look up for similar
							// durability. Add amount that way
							// if it exists
							int total = item.getAmount() + rs.getInt("amount");
							query = "UPDATE items SET amount='" + total
									+ "' WHERE itemid='" + item.getTypeId()
									+ "' AND durability='"
									+ item.getDurability() + "';";
						}
						while (rs.next());
					}
					else
					{
						// Item not in database, therefore add it
						query = "INSERT INTO items (itemid,amount,data,durability) VALUES ("
								+ item.getTypeId()
								+ ","
								+ item.getAmount()
								+ ",0," + item.getDurability() + ");";
					}
					rs.close();
					plugin.getLiteDB().standardQuery(query);
				}
				catch (SQLException e)
				{
					// INFO Auto-generated catch block
					player.sendMessage(ChatColor.RED + KarmicShare.prefix
							+ "Could not query item pool!");
					e.printStackTrace();
					return false;
				}
			}
			else if (!i.isPotion() && !hasEnchantments)
			{
				// Normal item
				// Create SQL query to see if item is already in
				// database
				query = "SELECT * FROM items WHERE itemid='" + item.getTypeId()
						+ "' AND data='" + item.getData().getData() + "';";
				ResultSet rs = plugin.getLiteDB().select(query);

				// Send Item to database
				try
				{
					if (rs.next())
					{
						do
						{
							// For tools, look up for similar
							// durability. Add amount that way
							// if it exists
							int total = item.getAmount() + rs.getInt("amount");
							query = "UPDATE items SET amount='" + total
									+ "' WHERE itemid='" + item.getTypeId()
									+ "' AND data='" + item.getData().getData()
									+ "';";
						}
						while (rs.next());
					}
					else
					{
						// Item not in database, therefore add it
						query = "INSERT INTO items (itemid,amount,data,durability) VALUES ("
								+ item.getTypeId()
								+ ","
								+ item.getAmount()
								+ ","
								+ item.getData().getData()
								+ ","
								+ item.getDurability() + ");";
					}
					rs.close();
					plugin.getLiteDB().standardQuery(query);
				}
				catch (SQLException e)
				{
					// INFO Auto-generated catch block
					player.sendMessage(ChatColor.RED + KarmicShare.prefix
							+ "Could not query item pool!");
					e.printStackTrace();
					return false;
				}
			}
			try
			{
				// Update karma
				if (!plugin.getPluginConfig().karmaDisabled)
				{
					if (plugin.getPluginConfig().statickarma)
					{
						this.updatePlayerKarma(player.getName(),
								item.getAmount()
										* plugin.getPluginConfig().karmaChange);
					}
					else
					{
						// Check if given item has a multiplier
						Item[] karmaList = plugin.getPluginConfig().karma
								.keySet().toArray(new Item[0]);
						boolean hasKarma = false;
						for (Item k : karmaList)
						{
							if (k.areSame(i))
							{
								// Item karma needs to be adjusted
								hasKarma = true;
							}
						}
						if (hasKarma)
						{
							try
							{
								this.updatePlayerKarma(
										player.getName(),
										item.getAmount()
												* plugin.getPluginConfig().karma
														.get(i));
							}
							catch (NullPointerException n)
							{
								// Found item, but there is no
								// config for specific data value
								// thus adjust using regular means
								this.updatePlayerKarma(
										player.getName(),
										item.getAmount()
												* plugin.getPluginConfig().karmaChange);
							}
						}
						else
						{
							this.updatePlayerKarma(
									player.getName(),
									item.getAmount()
											* plugin.getPluginConfig().karmaChange);
						}
					}
				}
			}
			catch (SQLException e)
			{
				// INFO Auto-generated catch block
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not adjust karma to pool!");
				e.printStackTrace();
				return false;
			}
			// Smoke effect
			this.smokePlayer(player);
			// Update cache
			if (!plugin.getCommander().getCache().isEmpty())
			{
				if (plugin.getCommander().getCache().containsKey(i))
				{
					int cacheAmount = plugin.getCommander().getCache().get(i)
							.intValue();
					plugin.getCommander().getCache()
							.put(i, (cacheAmount + item.getAmount()));
				}
				else
				{
					plugin.getCommander().getCache().put(i, item.getAmount());
				}
			}
			return true;
		}
		else
		{
			player.sendMessage(ChatColor.RED + KarmicShare.prefix
					+ " Lack permission: KarmicShare.give");
		}
		return false;
	}

	/**
	 * Updates the player's karma
	 *
	 * @param Name
	 *            of player
	 * @param Amount
	 *            of karma to add
	 * @param Boolean
	 *            if to add to current karma, or to set it as given value
	 * @throws SQLException
	 */
	private void updatePlayerKarma(String name, int k) throws SQLException {
		try
		{
			// Retrieve karma from database
			int karma = getPlayerKarma(name);
			// Add to existing value
			karma += k;
			String query;
			// Check updated karma value to limits in config
			if (karma <= plugin.getPluginConfig().lower)
			{
				// Updated karma value is beyond lower limit, so set to min
				query = "UPDATE players SET karma='"
						+ plugin.getPluginConfig().lower
						+ "' WHERE playername='" + name + "';";
			}
			else if (karma >= plugin.getPluginConfig().upper)
			{
				// Updated karma value is beyond upper limit, so set to max
				query = "UPDATE players SET karma='"
						+ plugin.getPluginConfig().upper
						+ "' WHERE playername='" + name + "';";
			}
			else
			{
				// Updated karma value is within acceptable range
				query = "UPDATE players SET karma='" + karma
						+ "' WHERE playername='" + name + "';";
			}
			plugin.getLiteDB().standardQuery(query);
		}
		catch (SQLException e)
		{
			throw e;
		}
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
		String query = "SELECT * FROM players WHERE playername='" + name + "';";
		ResultSet rs = plugin.getLiteDB().select(query);
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
				query = "INSERT INTO players VALUES ('" + name + "','" + karma
						+ "');";
				plugin.getLiteDB().standardQuery(query);
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		return karma;
	}

	/**
	 * Provides a smoke effect for the player.
	 *
	 * http://forums.bukkit.org/threads/smoke-effect-yes-i-know-others-have-
	 * asked.29492/
	 *
	 * @param Player
	 *            that should get the effect
	 * @author Adamki11s
	 */
	private void smokePlayer(Player player) {
		if (plugin.getPluginConfig().effects)
		{
			Location loc = player.getLocation();
			World w = loc.getWorld();
			int repeat = 0;
			while (repeat < 1)
			{
				for (double x = (loc.getX() - 3); x <= (loc.getX() + 3); x++)
				{
					for (double y = (loc.getY() - 3); y <= (loc.getY() + 3); y++)
					{
						for (double z = (loc.getZ() - 3); z <= (loc.getZ() + 3); z++)
						{
							w.playEffect(new Location(w, x, y, z),
									Effect.SMOKE, 1);
						}
					}
				}
				repeat++;
			}
		}
	}

	class Repopulate implements Runnable {
		int slot;
		ItemStack item;
		Inventory inventory;

		public Repopulate(Inventory inv, ItemStack i, int s) {
			inventory = inv;
			item = i;
			slot = s;
		}

		@Override
		public void run() {
			inventory.setItem(slot, item);
		}

	}
}
