/**
 * Separate class to handle commands
 * Followed example from DiddiZ's LB.
 *
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Commander implements CommandExecutor {
	// Class variables
	private final KarmicShare ks;
	private final PermCheck perm;
	private final static String bar = "======================";
	private final String prefix;
	private final Config config;
	private final Map<String, Integer> page = new HashMap<String, Integer>();
	private final Map<String, Integer> multiPage = new HashMap<String, Integer>();
	private final Map<Item, Integer> cache = new HashMap<Item, Integer>();
	private int limit;
	private long time;

	/**
	 * Constructor
	 *
	 * @param karmicShare
	 *            plugin
	 */
	public Commander(KarmicShare karmicShare) {
		// Instantiate variables
		ks = karmicShare;
		prefix = ks.getPluginPrefix();
		config = ks.getPluginConfig();
		perm = ks.getPermissionHandler();
		limit = config.listlimit;
		time = 0;
	}

	/**
	 * Command handler
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		if (config.debugTime)
		{
			time = System.nanoTime();
		}
		// See if any arguments were given
		if (args.length == 0)
		{
			if (!config.karmaDisabled)
			{
				// Show player karma
				this.showPlayerKarma(sender, args);
			}
			else
			{
				// karma system disabled
				sender.sendMessage(ChatColor.RED + prefix + " Karma disabled");
			}
		}
		else
		{
			final String com = args[0].toLowerCase();
			if (com.equals("version") || com.equals("ver"))
			{
				// Version and author
				this.showVersion(sender, args);
			}
			else if (com.equals("?") || com.equals("help"))
			{
				this.displayHelp(sender);
			}
			else if (com.equals("info"))
			{
				// Info command
				this.inspectItem(sender, args);
			}
			// Player is giving item to pool
			else if (com.equals("give"))
			{
				this.giveItem(sender, args);
			}
			// Player requested an item
			else if (com.equals("take"))
			{
				if (this.takeItem(sender, args))
				{
					if (config.debugTime)
					{
						debugTime(sender, time);
					}
					return true;
				}
			}
			// Previous page of item pool
			else if (com.equals("prev"))
			{
				// List, with previous page
			}
			// Next page of item pool
			else if (com.equals("next"))
			{
				// List with next page
				this.listPool(sender, 1);
			}
			// List items in pool
			else if (com.equals("list"))
			{
				this.listCommand(sender, args);
			}
			// Ask for karma multipliers / page through muliplier list
			else if (com.equals("value"))
			{
				this.valueCommand(sender, args);
			}
			// Admin command
			else if (com.equals("admin"))
			{
				if (perm.checkPermission(sender, "KarmicShare.admin"))
				{
					if (args.length > 1)
					{
						// They have a parameter, thus
						// parse in adminCommand method
						if (this.adminCommand(sender, args))
						{
							if (config.debugTime)
							{
								debugTime(sender, time);
							}
							return true;
						}
					}
					else
					{
						// Show admin commands help menu
						sender.sendMessage(ChatColor.BLUE + "==="
								+ ChatColor.RED + "KarmicShare Admin"
								+ ChatColor.BLUE + "===");
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin add <item>[:data] [amount]"
								+ ChatColor.YELLOW + " : Add item(s) to pool");
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin reset <player>" + ChatColor.YELLOW
								+ " : Resets player's karma");
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin set <player> <karma>"
								+ ChatColor.YELLOW
								+ " : Sets player's karma to value");
						sender.sendMessage(ChatColor.GREEN + "/ks admin drain"
								+ ChatColor.YELLOW + " : Empty item pool");
						sender.sendMessage(ChatColor.GREEN + "/ks admin reload"
								+ ChatColor.YELLOW + " : Reload configuration");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED
							+ " Lack permission: KarmicShare.admin");
				}
			}
			// Other player karma lookup
			else if (com.equals("player"))
			{
				this.otherPlayerKarma(sender, args);
			}
			else
			{
				// Bad command entered
				sender.sendMessage(ChatColor.RED + prefix
						+ " Wrong syntax. Try /ks ? for help.");
			}
		}
		if (config.debugTime)
		{
			debugTime(sender, time);
		}
		return true;
	}

	private void otherPlayerKarma(CommandSender sender, String[] args) {
		// Check if karma is enabled
		if (!config.karmaDisabled)
		{
			// Check if name was given
			if (args.length > 1)
			{
				// Check if they have the permission node
				if (perm.checkPermission(sender, "KarmicShare.admin")
						|| perm.checkPermission(sender,
								"KarmicShare.karma.other"))
				{
					// attempt to parse name
					String name = args[1];
					// SQL query to get player count for specified name
					String query = "SELECT COUNT(*) FROM players WHERE playername='"
							+ name + "'";
					ResultSet rs = ks.getLiteDB().select(query);
					// Check ResultSet
					boolean has = false;
					try
					{
						if (rs.next())
						{
							// Check if only received 1 entry
							if (rs.getInt(1) == 1)
							{
								// we have a single name
								has = true;
							}
							else if (rs.getInt(1) > 1)
							{
								sender.sendMessage(ChatColor.RED
										+ prefix
										+ " Got more than one result. Possibly incomplete name?");
							}
							else
							{
								// Player not in database, therefore error
								// on player part
								sender.sendMessage(ChatColor.RED + prefix
										+ " Player " + ChatColor.WHITE + name
										+ ChatColor.RED + " not in database.");
								sender.sendMessage(ChatColor.RED + prefix
										+ " Player names are case sensitive.");
							}
						}
						else
						{
							// Error in query...
							sender.sendMessage(ChatColor.RED + prefix
									+ " SQL query error");
						}
						rs.close();
					}
					catch (SQLException e)
					{
						// INFO Auto-generated catch block
						sender.sendMessage(ChatColor.RED + prefix
								+ " Could not get " + name + "'s karma");
						e.printStackTrace();
					}
					if (has)
					{
						// Grab default
						int karma = config.playerKarmaDefault;
						try
						{
							query = "SELECT * FROM players WHERE playername='"
									+ name + "';";
							rs = ks.getLiteDB().select(query);
							if (rs.next())
							{
								do
								{
									// Grab player karma value
									karma = rs.getInt("karma");
								}
								while (rs.next());
							}
							rs.close();
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							sender.sendMessage(ChatColor.RED + prefix
									+ " Could not get " + name + "'s karma");
							e.printStackTrace();
						}
						// Colorize karma
						sender.sendMessage(this.colorizeKarma(karma));
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED
							+ " Lack permission: KarmicShare.karma.other");
				}
			}
			else
			{
				// did not give a player name, therefore error
				sender.sendMessage(ChatColor.RED + prefix
						+ " No player name given.");
			}
		}
		else
		{
			// karma system disabled
			sender.sendMessage(ChatColor.RED + prefix + " Karma disabled");
		}
	}

	private void valueCommand(CommandSender sender, String[] args) {
		if (!config.karmaDisabled)
		{
			if (args.length > 1)
			{
				// If they provided a page number

				try
				{
					// Attempt to parse argument for page number
					int pageNum = Integer.parseInt(args[1]);
					// Set current page to given number
					multiPage.put(sender.getName(), pageNum - 1);
					// Show page if possible
					this.listMultipliers(sender, 0);
				}
				catch (NumberFormatException e)
				{
					// Maybe they did prev/next?
					if (args[1].equals("prev"))
					{
						// List, with previous page
						this.listMultipliers(sender, -1);
					}
					else if (args[1].equals("next"))
					{
						// List, with previous page
						this.listMultipliers(sender, 1);
					}
					else
					{
						sender.sendMessage(ChatColor.YELLOW + prefix
								+ " Invalid extra parameter: " + args[1]);
					}
				}
			}
			else
			{
				// List with current page
				this.listMultipliers(sender, 0);
			}
		}
		else
		{
			// karma system disabled
			sender.sendMessage(ChatColor.RED + prefix + " Karma disabled");
		}
	}

	private void listCommand(CommandSender sender, String[] args) {
		// TODO allow people to "find" items
		// i.e. limit list entries to what they want
		if (args.length > 1)
		{
			// If they provided a page number
			try
			{
				// Attempt to parse argument for page number
				int pageNum = Integer.parseInt(args[1]);
				// Set current page to given number
				page.put(sender.getName(), pageNum - 1);
				// Show page if possible
				this.listPool(sender, 0);
			}
			catch (NumberFormatException e)
			{
				sender.sendMessage(ChatColor.YELLOW + prefix
						+ " Invalid integer for page number");
			}
		}
		else
		{
			// List with current page
			this.listPool(sender, 0);
		}
	}

	private boolean takeItem(CommandSender sender, String[] args) {
		// Take item from pool
		// Check if player sent command
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			// Check if they have "take" permission
			if (perm.checkPermission(sender, "KarmicShare.take"))
			{
				int karma = 0;
				if (!config.karmaDisabled)
				{
					// Check karma before anything
					karma = config.playerKarmaDefault;
					try
					{
						karma = getPlayerKarma(player.getName());
						if (karma <= config.lower)
						{
							// They are at the limit, or somehow lower for
							// whatever reason
							player.sendMessage(ChatColor.RED + prefix
									+ "Your karma is at the limit!");
							if (config.debugTime)
							{
								debugTime(sender, time);
							}
							return true;
						}
					}
					catch (SQLException e1)
					{
						// INFO Auto-generated catch block
						player.sendMessage(ChatColor.RED + prefix
								+ " Could not retrieve player karma");
						e1.printStackTrace();
					}
				}
				// Current karma is not at limit, so continue
				// Check that they gave an item name/id
				if (args.length > 1)
				{
					// Player will always request at least 1 item
					int itemid = 0;
					int data = 0;
					int amount = 1;
					short dur = 0;
					boolean has = false;
					try
					{
						// Attempt to grab simple, singular itemid
						itemid = Integer.parseInt(args[1]);
					}
					catch (NumberFormatException e)
					{
						// They gave a string
						if (args[1].contains(":"))
						{
							// Attempt to parse as itemid:data
							// TODO parse as strings as well? Be extra
							// work
							String[] cut = args[1].split(":");
							try
							{
								itemid = Integer.parseInt(cut[0]);
								data = Integer.parseInt(cut[1]);
								if (args.length > 2)
								{
									// Grab amount as well if they gave
									// it
									amount = Integer.parseInt(args[2]);
								}
							}
							catch (NumberFormatException r)
							{
								// Not a number given
								player.sendMessage(ChatColor.RED + prefix
										+ " Invalid item id / data value");
								if (config.debugTime)
								{
									debugTime(sender, time);
								}
								return true;
							}
						}
						else
						{
							// Did not follow the id:data format
							// Try and parse the rest of the args[] as
							// material name
							StringBuffer sb = new StringBuffer();
							for (int i = 1; i < args.length; i++)
							{
								try
								{
									// If they specified an amount,
									// catch it
									amount = Integer.parseInt(args[i]);
									// Ignore the rest once we have an
									// amount
									break;
								}
								catch (NumberFormatException num)
								{
									sb.append(args[i] + " ");
								}
							}
							String temp = sb.toString();
							temp = temp.replaceAll("\\s+$", "");
							temp = temp.toLowerCase();
							// Update cache
							this.updateCache(sender);
							// Check if item exists in cache through
							// reverse lookup: name -> id:data
							Item[] array = cache.keySet().toArray(new Item[0]);
							for (int i = 0; i < array.length; i++)
							{
								String cacheName = array[i].name.toLowerCase();
								if (temp.equals(cacheName))
								{
									// Item is in cache, so get item id
									// and data values
									itemid = array[i].itemId();
									data = array[i].getData();
									dur = array[i].itemDurability();
									has = true;
									break;
								}
							}
							if (!has)
							{
								// Item not in cache, therefore
								// potential error on player part
								player.sendMessage(ChatColor.RED + prefix
										+ " Item not in pool...");
								if (config.debugTime)
								{
									debugTime(sender, time);
								}
								return true;
							}
						}
					}
					// Check if pool contains item requested + amount
					int total = 0;
					// SQL query to see if item is in pool
					// Create temp item to check if its a tool
					Item temp = new Item(itemid, Byte.valueOf("" + data), dur);
					String query = "";
					int poolAmount = 0;
					boolean toolCheck = false;
					if (temp.isTool())
					{
						// Handle tools
						toolCheck = true;
						// Grab all entries of the same tool id
						String toolQuery = "SELECT * FROM items WHERE itemid='"
								+ itemid + "';";
						ResultSet toolRS = ks.getLiteDB().select(toolQuery);
						try
						{
							if (toolRS.next())
							{
								do
								{
									poolAmount += toolRS.getInt("amount");
								}
								while (toolRS.next());
								if (poolAmount >= amount)
								{
									// We have enough in pool that
									// was requested
									has = true;
									total = poolAmount;
								}
								else if (poolAmount < amount)
								{
									// They requested too much, adjust
									// amount to max
									has = true;
									amount = poolAmount;
									total = poolAmount;
									sender.sendMessage(ChatColor.YELLOW
											+ prefix
											+ " Requested too much. Adjusting to max.");
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
							player.sendMessage(ChatColor.RED + prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
						}
					}
					// TODO separate check to see if its a potion and handle it
					// via the durability info
					else
					{
						// Not a tool
						query = "SELECT * FROM items WHERE itemid='" + itemid
								+ "' AND data='" + data + "';";
						ResultSet rs = ks.getLiteDB().select(query);

						// Check ResultSet
						try
						{
							if (rs.next())
							{
								// Item already in pool, check
								// amount
								poolAmount = rs.getInt("amount");
								if (poolAmount >= amount)
								{
									// We have enough in pool that
									// was requested
									has = true;
									total = poolAmount;
								}
								else if (poolAmount < amount)
								{
									// They requested too much, adjust
									// amount to max
									has = true;
									amount = poolAmount;
									total = poolAmount;
									sender.sendMessage(ChatColor.YELLOW
											+ prefix
											+ " Requested too much. Adjusting to max.");
								}
							}
							else
							{
								// Item not in database, therefore error
								// on player part
								rs.close();
								player.sendMessage(ChatColor.RED + prefix
										+ " Item not in pool...");
								if (config.debugTime)
								{
									debugTime(sender, time);
								}
								return true;
							}
							rs.close();
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							player.sendMessage(ChatColor.RED + prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
						}
					}
					try
					{
						if (has)
						{
							final Item item = new Item(itemid, Byte.valueOf(""
									+ data), (short) 0);
							boolean hasKarma = false;
							if (!config.karmaDisabled)
							{
								// Check karma again, before giving item, to
								// adjust amount
								// based on karma and karma multipliers
								int karmaAdj = 0;
								if (config.statickarma)
								{
									// Using static karma, everything goes
									// by
									// the config's default karma change
									// value
									if (karmaAdj < config.lower)
									{
										karmaAdj = karma
												+ (config.karmaChange * amount * -1);
										// They went beyond the lower limit
										// adjust amount given based on
										// karma now
										amount = Math.abs(config.lower)
												- Math.abs(karma);
										amount = amount / config.karmaChange;
										if (amount == 0)
										{
											// Cannot give any items as
											// they'd go beyond
											// karma limit
											player.sendMessage(ChatColor.RED
													+ prefix
													+ " Not enough karma to take item");
											if (config.debugTime)
											{
												debugTime(sender, time);
											}
											return true;
										}
										else
										{
											player.sendMessage(ChatColor.YELLOW
													+ prefix
													+ " Near/Hit karma limit!");
										}
									}
								}
								else
								{
									// Using per-item karma
									Item[] karmaList = config.karma.keySet()
											.toArray(new Item[0]);
									// Check if requested item is in the
									// karma list
									for (Item k : karmaList)
									{
										if (k.areSame(item))
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
													+ (config.karma.get(item)
															* amount * -1);
											if (karmaAdj < config.lower)
											{
												// They went beyond the
												// lower limit
												// adjust amount given based
												// on karma now
												int tempKarma = Math
														.abs(karmaAdj)
														- Math.abs(config.lower);
												int div = tempKarma
														/ config.karma
																.get(item);
												int rem = tempKarma
														% config.karma
																.get(item);
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
															+ prefix
															+ " Not enough karma to take item");
													if (config.debugTime)
													{
														debugTime(sender, time);
													}
													return true;
												}
												else
												{
													player.sendMessage(ChatColor.YELLOW
															+ prefix
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
													+ (config.karmaChange
															* amount * -1);
											if (karmaAdj < config.lower)
											{
												// They went beyond the
												// lower limit
												// adjust amount given based
												// on karma now
												int tempKarma = Math
														.abs(karmaAdj)
														- Math.abs(config.lower);
												int div = tempKarma
														/ config.karmaChange;
												int rem = tempKarma
														% config.karmaChange;
												if (rem != 0)
												{
													div++;
												}
												amount -= div;
												if (amount <= 0)
												{
													player.sendMessage(ChatColor.RED
															+ prefix
															+ " Not enough karma to take item");
													if (config.debugTime)
													{
														debugTime(sender, time);
													}
													return true;
												}
												else
												{
													player.sendMessage(ChatColor.YELLOW
															+ prefix
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
												+ (config.karmaChange * amount * -1);
										if (karmaAdj < config.lower)
										{
											// They went beyond the lower
											// limit
											// adjust amount given based on
											// karma now
											int tempKarma = Math.abs(karmaAdj)
													- Math.abs(config.lower);
											int div = tempKarma
													/ config.karmaChange;
											int rem = tempKarma
													% config.karmaChange;
											if (rem != 0)
											{
												div++;
											}
											amount -= div;
											amount = amount
													/ config.karmaChange;
											if (amount <= 0)
											{
												// Cannot give any items as
												// they'd go beyond
												// karma limit
												player.sendMessage(ChatColor.RED
														+ prefix
														+ " Not enough karma to take item");
												if (config.debugTime)
												{
													debugTime(sender, time);
												}
												return true;
											}
											else
											{
												player.sendMessage(ChatColor.YELLOW
														+ prefix
														+ " Near/Hit karma limit!");
											}
										}
									}
								}
							}
							// Handle tool generation
							if (toolCheck)
							{
								// Grab all entries of the same tool id
								String toolQuery = "SELECT * FROM items WHERE itemid='"
										+ itemid + "';";
								ResultSet toolRS = ks.getLiteDB().select(
										toolQuery);
								try
								{
									int tempAmount = amount;
									boolean done = false;
									boolean extra = false;
									boolean en = false;
									String residualToolQuery = "";
									ArrayList<String> dropList = new ArrayList<String>();
									if (toolRS.next())
									{
										do
										{
											// Generate item
											ItemStack toolItem = new ItemStack(
													itemid,
													toolRS.getInt("amount"),
													toolRS.getShort("data"));
											String enchant = toolRS
													.getString("enchantments");
											if (!toolRS.wasNull())
											{
												// It had enchantments
												en = true;
												String[] cut = enchant
														.split("i");
												for (int i = 0; i < cut.length; i++)
												{
													String[] cutter = cut[i]
															.split("v");
													EnchantmentWrapper e = new EnchantmentWrapper(
															Integer.parseInt(cutter[0]));
													toolItem.addUnsafeEnchantment(
															e.getEnchantment(),
															Integer.parseInt(cutter[1]));
												}
											}
											// Give item to player
											HashMap<Integer, ItemStack> toolResidual = player
													.getInventory().addItem(
															toolItem);
											if (toolResidual.size() != 0)
											{
												// Add back extra
												extra = true;
												tempAmount += toolResidual
														.size();
												amount -= tempAmount;
												// Create appropriate query
												if (en)
												{
													residualToolQuery = "UPDATE items SET amount='"
															+ toolResidual
																	.size()
															+ "' WHERE itemid='"
															+ itemid
															+ "' AND data='"
															+ data
															+ "' AND enchantments='"
															+ enchant + ";";
												}
												else
												{
													residualToolQuery = "UPDATE items SET amount='"
															+ toolResidual
																	.size()
															+ "' WHERE itemid='"
															+ itemid
															+ "' AND data='"
															+ data + "';";
												}
												// Done
												done = true;
											}
											else
											{
												// Update amount
												tempAmount -= toolRS
														.getInt("amount");
												// Add entry to drop list
												if (en)
												{
													dropList.add("DELETE FROM items WHERE itemid='"
															+ itemid
															+ "' AND amount='"
															+ toolRS.getInt("amount")
															+ "' AND data='"
															+ toolRS.getShort("data")
															+ "' AND enchantments='"
															+ enchant + "';");
												}
												else
												{
													dropList.add("DELETE FROM items WHERE itemid='"
															+ itemid
															+ "' AND amount='"
															+ toolRS.getInt("amount")
															+ "' AND data='"
															+ toolRS.getShort("data")
															+ "';");
												}
											}
											if (tempAmount == 0)
											{
												// Done
												done = true;
											}
											en = false;
											if (!toolRS.next())
											{
												done = true;
											}
										}
										while (!done);
									}
									// Close ResultSet
									toolRS.close();
									// Add back extra
									if (extra)
									{
										ks.getLiteDB().standardQuery(
												residualToolQuery);
										player.sendMessage(ChatColor.YELLOW
												+ prefix
												+ " Your inventory is completely full...");
									}
									// Drop entries
									for (String s : dropList)
									{
										ks.getLiteDB().standardQuery(s);
									}
								}
								catch (SQLException e)
								{
									// INFO Auto-generated catch block
									player.sendMessage(ChatColor.RED
											+ prefix
											+ "Could not retrieve item in pool!");
									e.printStackTrace();
								}
							}
							else
							{
								// Handle non-tools
								// Generate item
								final ItemStack give = item.toItemStack(amount);
								HashMap<Integer, ItemStack> residual = player
										.getInventory().addItem(give);
								if (residual.size() != 0)
								{
									// Add back extra that could
									// not be added to player inventory
									// Calculate new amount removed from
									// pool
									amount -= residual.size();
									if (amount == 0)
									{
										// Didn't actually give any items
										// due
										// to completely full inventory,
										// therefore
										// Notify player of their mistake
										player.sendMessage(ChatColor.YELLOW
												+ prefix
												+ " Your inventory is completely full...");
										if (config.debugTime)
										{
											debugTime(sender, time);
										}
										return true;
									}
								}
								// Calculate new total
								total -= amount;

								if (total == 0)
								{
									// Drop record as there are none left
									query = "DELETE FROM items WHERE amount='"
											+ amount + "' AND itemid='"
											+ itemid + "' AND data='" + data
											+ "';";
									ks.getLiteDB().standardQuery(query);
									// Remove from cache list
									cache.remove(item);
								}
								// Update amount to new total if there
								// are items remaining
								else
								{
									query = "UPDATE items SET amount='" + total
											+ "' WHERE itemid='" + itemid
											+ "' AND data='" + data + "';";
									ks.getLiteDB().standardQuery(query);
								}
							}
							player.sendMessage(ChatColor.GREEN + prefix
									+ " Given " + ChatColor.GOLD + amount
									+ ChatColor.GREEN + " of " + ChatColor.AQUA
									+ item.name);
							// Smoke effect
							this.smokePlayer(player);
							// Update karma
							if (!config.karmaDisabled)
							{
								if (hasKarma)
								{
									this.updatePlayerKarma(player.getName(),
											amount * config.karma.get(item)
													* -1);
								}
								else
								{
									this.updatePlayerKarma(player.getName(),
											amount * config.karmaChange * -1);
								}
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + prefix
									+ " Item is no longer available.");
						}
					}
					catch (SQLException e)
					{
						// INFO Auto-generated catch block
						player.sendMessage(ChatColor.RED + prefix
								+ "Could not retrieve item in pool!");
						e.printStackTrace();
					}
				}
				else
				{
					player.sendMessage(ChatColor.RED + prefix
							+ " Need an item name or id");
				}
				// TODO remove highest durability tool first
			}
			else
			{
				sender.sendMessage(ChatColor.RED
						+ " Lack permission: KarmicShare.take");
			}
		}
		else
		{
			sender.sendMessage(prefix + " Cannot use this command as console.");
		}
		return true;
	}

	private void giveItem(CommandSender sender, String[] args) {
		// TODO allow for player to specify item and amount
		// parse more parameters to allow spaces for item names
		// Check if player sent command
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			// Check if they have "give" permission
			if (perm.checkPermission(sender, "KarmicShare.give"))
			{
				// Grab item in player's hand.
				ItemStack items = player.getItemInHand();
				int itemid = items.getTypeId();
				// Check if there is an item in their hand
				if (itemid != 0)
				{
					int quantity = items.getAmount();
					int data = items.getData().getData();
					short durability = items.getDurability();
					final Item item = new Item(itemid, Byte.valueOf("" + data),
							durability);
					boolean hasEnchantments = false;
					String query = "";
					if (item.isTool())
					{
						// Is a tool, check for enchantments
						Map<Enchantment, Integer> enchantments = items
								.getEnchantments();
						if (!items.getEnchantments().isEmpty())
						{
							// Tool has enchantments
							hasEnchantments = true;
							// Remove item from player inventory
							// Thanks to @nisovin for the following line
							player.setItemInHand(null);
							// Create string for enchantments
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
									+ itemid
									+ "','"
									+ quantity
									+ "','"
									+ data
									+ "','"
									+ durability
									+ "','"
									+ sb.toString() + "');";
						}
					}
					// Remove item from player inventory
					// Thanks to @nisovin for the following line
					player.setItemInHand(null);
					// Not a tool or doesn't have enchantments, so treat as
					// normal
					if (!hasEnchantments && !item.isPotion())
					{
						// Create SQL query to see if item is already in
						// database
						query = "SELECT * FROM items WHERE itemid='" + itemid
								+ "' AND data='" + data + "';";
						ResultSet rs = ks.getLiteDB().select(query);

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
									int total = quantity + rs.getInt("amount");
									query = "UPDATE items SET amount='" + total
											+ "' WHERE itemid='" + itemid
											+ "' AND data='" + data + "';";
								}
								while (rs.next());
							}
							else
							{
								// Item not in database, therefore add it
								query = "INSERT INTO items (itemid,amount,data,durability) VALUES ("
										+ itemid
										+ ","
										+ quantity
										+ ","
										+ data
										+ "," + durability + ");";
							}
							rs.close();
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							player.sendMessage(ChatColor.RED + prefix
									+ "Could not query item pool!");
							e.printStackTrace();
						}
					}
					else if (item.isPotion())
					{
						// Potion item
						// Create SQL query to see if item is already in
						// database
						query = "SELECT * FROM items WHERE itemid='" + itemid
								+ "' AND durability='" + durability + "';";
						ResultSet rs = ks.getLiteDB().select(query);

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
									int total = quantity + rs.getInt("amount");
									query = "UPDATE items SET amount='" + total
											+ "' WHERE itemid='" + itemid
											+ "' AND durability='" + durability
											+ "';";
								}
								while (rs.next());
							}
							else
							{
								// Item not in database, therefore add it
								query = "INSERT INTO items (itemid,amount,data,durability) VALUES ("
										+ itemid
										+ ","
										+ quantity
										+ ","
										+ data
										+ "," + durability + ");";
							}
							rs.close();
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							player.sendMessage(ChatColor.RED + prefix
									+ "Could not query item pool!");
							e.printStackTrace();
						}
					}
					try
					{
						ks.getLiteDB().standardQuery(query);
						player.sendMessage(ChatColor.GREEN + prefix + " Added "
								+ ChatColor.GOLD + quantity + ChatColor.GREEN
								+ " of " + ChatColor.AQUA + item.name
								+ ChatColor.GREEN + " to pool.");
						// Smoke effect
						this.smokePlayer(player);
						// Update karma
						if (!config.karmaDisabled)
						{
							if (config.statickarma)
							{
								this.updatePlayerKarma(player.getName(),
										quantity * config.karmaChange * -1);
							}
							else
							{
								// Check if given item has a multiplier
								Item[] karmaList = config.karma.keySet()
										.toArray(new Item[0]);
								boolean hasKarma = false;
								for (Item k : karmaList)
								{
									if (k.areSame(item))
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
												quantity
														* config.karma
																.get(item));
									}
									catch (NullPointerException n)
									{
										// Found item, but there is no
										// config for specific data value
										// thus adjust using regular means
										this.updatePlayerKarma(
												player.getName(), quantity
														* config.karmaChange);
									}
								}
								else
								{
									this.updatePlayerKarma(player.getName(),
											quantity * config.karmaChange);
								}
							}
						}
					}
					catch (SQLException e)
					{
						// INFO Auto-generated catch block
						player.sendMessage(ChatColor.RED + prefix
								+ "Could not adjust karma or add item to pool!");
						e.printStackTrace();
					}
				}
				else
				{
					// If there is no item, stop
					sender.sendMessage(ChatColor.RED + prefix
							+ " No item in hand. Nothing to give.");
				}

			}
			else
			{
				sender.sendMessage(ChatColor.RED
						+ " Lack permission: KarmicShare.give");
			}
		}
		else
		{
			sender.sendMessage(prefix + " Cannot use this command as console.");
		}
	}

	private void inspectItem(CommandSender sender, String[] args) {
		// Permission check
		if (perm.checkPermission(sender, "KarmicShare.info"))
		{
			// Inspect item in hand
			if (sender instanceof Player)
			{
				Player player = (Player) sender;
				// Grab item in player's hand.
				ItemStack items = player.getItemInHand();
				int itemid = items.getTypeId();
				// Check if there is an item in their hand
				if (itemid != 0)
				{
					int quantity = items.getAmount();
					Item item = new Item(itemid, items.getData().getData(),
							items.getDurability());
					StringBuffer buf = new StringBuffer();
					buf.append("Info: Name: " + ChatColor.AQUA + item.name
							+ ChatColor.GREEN + " ID: "
							+ ChatColor.LIGHT_PURPLE + itemid + ChatColor.GREEN
							+ " Amount:" + ChatColor.GOLD + quantity
							+ ChatColor.GREEN + " Data: "
							+ ChatColor.LIGHT_PURPLE + item.getData()
							+ ChatColor.GREEN + " Damage: "
							+ items.getDurability() + ChatColor.GREEN
							+ " Tool: " + ChatColor.GRAY + item.isTool());
					if (!config.karmaDisabled)
					{
						if (config.statickarma)
						{
							buf.append(ChatColor.GREEN + " Multiplier: "
									+ ChatColor.YELLOW + config.karmaChange);
							buf.append(ChatColor.GREEN + " Total Karma: "
									+ ChatColor.YELLOW + ""
									+ (config.karmaChange * quantity));
						}
						else
						{
							// Check if given item has a multiplier
							Item[] karmaList = config.karma.keySet().toArray(
									new Item[0]);
							boolean hasKarma = false;
							for (Item k : karmaList)
							{
								if (k.areSame(item))
								{
									// Item karma needs to be adjusted
									hasKarma = true;
								}
							}
							if (hasKarma)
							{
								try
								{
									buf.append(ChatColor.GREEN
											+ " Multiplier: "
											+ ChatColor.YELLOW
											+ config.karma.get(item));
									buf.append(ChatColor.GREEN
											+ " Total Karma: "
											+ ChatColor.YELLOW
											+ ""
											+ (config.karma.get(item) * quantity));
								}
								catch (NullPointerException n)
								{
									// Found item, but there is no
									// config for specific data value
									// thus adjust using regular means
									buf.append(ChatColor.GREEN
											+ " Multiplier: "
											+ ChatColor.YELLOW
											+ config.karmaChange);
									buf.append(ChatColor.GREEN
											+ " Total Karma: "
											+ ChatColor.YELLOW + ""
											+ (config.karmaChange * quantity));
								}
							}
							else
							{
								buf.append(ChatColor.GREEN + " Multiplier: "
										+ ChatColor.YELLOW + config.karmaChange);
								buf.append(ChatColor.GREEN + " Total Karma: "
										+ ChatColor.YELLOW + ""
										+ (config.karmaChange * quantity));
							}
						}
					}
					Map<Enchantment, Integer> enchantments = items
							.getEnchantments();
					if (enchantments.isEmpty())
					{
						buf.append(ChatColor.GREEN + " Enchantments: "
								+ ChatColor.WHITE + "NONE");
					}
					else
					{
						buf.append(ChatColor.GREEN + " Enchantments: ");

						for (Map.Entry<Enchantment, Integer> e : enchantments
								.entrySet())
						{
							buf.append(ChatColor.WHITE + e.getKey().getName()
									+ ChatColor.YELLOW + " v"
									+ e.getValue().intValue() + ", ");
						}
					}
					player.sendMessage(ChatColor.GREEN + prefix
							+ buf.toString());
				}
				else
				{
					// If there is no item, stop
					sender.sendMessage(ChatColor.RED + prefix
							+ " No item in hand. Nothing to lookup.");
				}
			}
			else
			{
				// Console cannot inspect items
				sender.sendMessage(prefix
						+ " Cannot use this command as console.");
			}
		}
		else
		{
			// Lack permission
			sender.sendMessage(prefix + " Lack permission: KarmicShare.info");
		}
	}

	private void showVersion(CommandSender sender, String[] args) {
		sender.sendMessage(ChatColor.BLUE + bar + "=====");
		sender.sendMessage(ChatColor.GREEN + "KarmicShare v"
				+ ks.getDescription().getVersion());
		sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru");
		sender.sendMessage(ChatColor.BLUE + "===========" + ChatColor.GRAY
				+ "Config" + ChatColor.BLUE + "===========");
		sender.sendMessage(ChatColor.GRAY + "Effects: " + config.effects);
		sender.sendMessage(ChatColor.GRAY + "Karma enabled: "
				+ !config.karmaDisabled);
		sender.sendMessage(ChatColor.GRAY + "Static karma: "
				+ config.statickarma);
		sender.sendMessage(ChatColor.GRAY + "Karma lower-upper limit: "
				+ config.lower + ":" + config.upper);
		sender.sendMessage(ChatColor.GRAY + "Karma lower/upper %: "
				+ config.lowerPercent * 100 + "% /" + config.upperPercent * 100
				+ "%");
		sender.sendMessage(ChatColor.GRAY + "Default karma: "
				+ config.playerKarmaDefault);
		sender.sendMessage(ChatColor.GRAY + "Default karma rate: "
				+ config.karmaChange);
	}

	private void showPlayerKarma(CommandSender sender, String[] args) {
		// Check if player sent command
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			// Check if they have "karma" permission
			if (perm.checkPermission(sender, "KarmicShare.karma"))
			{
				try
				{
					// Retrieve karma from database and colorize
					sender.sendMessage(this.colorizeKarma(getPlayerKarma(player
							.getName())));
				}
				catch (SQLException e)
				{
					// INFO Auto-generated catch block
					player.sendMessage(ChatColor.RED + prefix
							+ "Could not obtain player karma!");
					e.printStackTrace();
				}
			}
		}
	}

	private void debugTime(CommandSender sender, long time) {
		time = System.nanoTime() - time;
		sender.sendMessage("[Debug]" + prefix + "Process time: " + time);
	}

	/**
	 * Show the help menu, with commands and description
	 *
	 * @param sender
	 *            to display to
	 */
	// TODO Expand on commands as necessary
	private void displayHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.BLUE + "=====" + ChatColor.RED
				+ "KarmicShare" + ChatColor.BLUE + "=====");
		sender.sendMessage(ChatColor.GREEN + "/ks" + ChatColor.YELLOW
				+ " : Show karma");
		if (perm.checkPermission(sender, "KarmicShare.give"))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks give" + ChatColor.YELLOW
					+ " : Give item stack in current hand");
		}
		if (perm.checkPermission(sender, "KarmicShare.take"))
		{
			sender.sendMessage(ChatColor.GREEN
					+ "/ks take <item>[:data] [amount]" + ChatColor.YELLOW
					+ " : Take item(s) from pool");
			sender.sendMessage(ChatColor.GREEN
					+ "/ks take <item name> [amount]" + ChatColor.YELLOW
					+ " : Take item(s) from pool");
		}
		sender.sendMessage(ChatColor.GREEN + "/ks list [page]"
				+ ChatColor.YELLOW + " : List items in pool");
		sender.sendMessage(ChatColor.GREEN + "/ks <prev | next>"
				+ ChatColor.YELLOW + " : Show previous/next page of list");
		sender.sendMessage(ChatColor.GREEN + "/ks value [prev|next|page#]"
				+ ChatColor.YELLOW
				+ " : List karma multiplier values, and page through list");
		sender.sendMessage(ChatColor.GREEN + "/ks help" + ChatColor.YELLOW
				+ " : Show help menu");
		if (perm.checkPermission(sender, "KarmicShare.info"))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks info" + ChatColor.YELLOW
					+ " : Inspect currently held item");
		}
		if (perm.checkPermission(sender, "KarmicShare.karma.other"))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks player <name>"
					+ ChatColor.YELLOW + " : Show karma for given player name");
		}
		if (perm.checkPermission(sender, "KarmicShare.admin"))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks admin" + ChatColor.YELLOW
					+ " : List admin commands");
		}
		sender.sendMessage(ChatColor.GREEN + "/ks version" + ChatColor.YELLOW
				+ " : Show version and config");
	}

	private boolean adminCommand(CommandSender sender, String[] args) {
		String com = args[1];
		// Add generated items to pool
		if (com.equals("add"))
		{
			if (args.length > 2)
			{
				// TODO allow for players to use item name
				// Might not do, simply because I don't have
				// a map of all names, only the cache
				int itemid = 0;
				int data = 0;
				short dur = 0;
				// Player will always request at least 1 item
				// TODO make default take amount configurable in YAML
				int amount = 1;
				try
				{
					itemid = Integer.parseInt(args[2]);
				}
				catch (NumberFormatException e)
				{
					if (args[2].contains(":"))
					{
						// Attempt to parse as itemid:data
						// TODO parse as strings as well?
						// Be extra work
						String[] cut = args[2].split(":");
						try
						{
							itemid = Integer.parseInt(cut[0]);
							data = Integer.parseInt(cut[1]);
							dur = Short.parseShort(cut[1]);
						}
						catch (NumberFormatException r)
						{
							// Not a number given
							sender.sendMessage(ChatColor.RED + prefix
									+ " Invalid item id / data value");
							return true;
						}
					}
					else
					{
						// They gave a non-integer
						// Try and parse the string as material
						// TODO parse names with more than one word
						// ought to parse until hit parse int success as
						// amount
						final Material mat = Material.matchMaterial(args[2]);
						if (mat == null)
						{
							// Not a known material
							sender.sendMessage(ChatColor.RED + prefix
									+ " Item name/id is incorrect.");
							return true;
						}
						else
						{
							itemid = mat.getId();
						}
					}
				}

				// They specified an amount
				if (args.length > 3)
				{
					// Ignore rest of arguments and parse the
					// immediate after
					try
					{
						amount = Integer.parseInt(args[3]);
					}
					catch (NumberFormatException n)
					{
						// Not a number given
						sender.sendMessage(ChatColor.RED + prefix
								+ " Invalid item amount");
						return true;
					}
				}
				if (itemid != 0)
				{
					// Create SQL query to see if item is already in
					// database
					String query = "SELECT * FROM items WHERE itemid='"
							+ itemid + "' AND data='" + data + "';";
					ResultSet rs = ks.getLiteDB().select(query);
					// Send Item to database
					try
					{
						if (rs.next())
						{
							// here you know that there is at least
							// one record
							do
							{
								// TODO format initial or other query to include
								// enchantments
								int total = amount + rs.getInt("amount");
								query = "UPDATE items SET amount='" + total
										+ "' WHERE itemid='" + itemid
										+ "' AND data='" + data + "';";
							}
							while (rs.next());
						}
						else
						{
							// Item not in database, therefore add
							// it
							query = "INSERT INTO items (itemid,amount,data, durability) VALUES ("
									+ itemid
									+ ","
									+ amount
									+ ","
									+ data
									+ ","
									+ dur + ");";
						}
						// Needs to be outside of loop for
						// whatever reason
						// so that it doesn't hang. Need to have
						// an array of queries or
						// Something if I'm going to do more
						// than one query.
						rs.close();
						ks.getLiteDB().standardQuery(query);
						Item item = new Item(itemid, Byte.valueOf("" + data),
								dur);
						sender.sendMessage(ChatColor.GREEN + prefix + " Added "
								+ ChatColor.GOLD + amount + ChatColor.GREEN
								+ " of " + ChatColor.AQUA + item.name
								+ ChatColor.GREEN + " to pool.");
					}
					catch (SQLException q)
					{
						// INFO Auto-generated catch block
						sender.sendMessage(ChatColor.RED + prefix
								+ "Could not add item to pool!");
						q.printStackTrace();
					}
				}
				else
				{
					// If there is no item, stop
					sender.sendMessage(ChatColor.RED + prefix
							+ " Cannot add air to pool.");
				}
			}
			return true;
		}
		else if (com.equals("drain"))
		{
			// Wipe table
			final String query = "DELETE FROM items";
			ks.getLiteDB().standardQuery(query);
			ks.getLogger().info(prefix + "Items table cleared");
			sender.sendMessage(ChatColor.GREEN + prefix + " Item pool emptied.");
			cache.clear();
			return true;
		}
		else if (com.equals("reload"))
		{
			config.reloadConfig();
			sender.sendMessage(ChatColor.YELLOW + prefix + " Config reloaded");
			multiPage.clear();
			return true;
		}
		else if (com.equals("reset"))
		{
			if (!config.karmaDisabled)
			{
				// Check if name was given
				if (args.length > 2)
				{
					// attempt to parse name
					String name = args[2];
					// SQL query to get player count for specified name
					String query = "SELECT COUNT(*) FROM players WHERE playername='"
							+ name + "';";
					ResultSet rs = ks.getLiteDB().select(query);
					// Check ResultSet
					boolean has = false;
					try
					{
						if (rs.next())
						{
							// Check if only received 1 entry
							if (rs.getInt(1) == 1)
							{
								// we have a single name
								has = true;
							}
							else if (rs.getInt(1) > 1)
							{
								sender.sendMessage(ChatColor.RED
										+ prefix
										+ " Got more than one result. Possibly incomplete name?");
							}
							else
							{
								// Player not in database, therefore error
								// on player part
								sender.sendMessage(ChatColor.RED + prefix
										+ " Player " + ChatColor.WHITE + name
										+ ChatColor.RED + " not in database.");
								sender.sendMessage(ChatColor.RED + prefix
										+ " Player names are case sensitive.");
							}
						}
						else
						{
							// Error in query...
							sender.sendMessage(ChatColor.RED + prefix
									+ " SQL query error");
						}
						rs.close();
					}
					catch (SQLException e)
					{
						// INFO Auto-generated catch block
						sender.sendMessage(ChatColor.RED + prefix
								+ "Could not reset " + name + "'s karma");
						e.printStackTrace();
					}
					if (has)
					{
						int karma;
						try
						{
							// Set to zero
							karma = this.getPlayerKarma(name) * -1;
							this.updatePlayerKarma(name, karma);
							if (config.playerKarmaDefault != 0)
							{
								// Default was non-zero, so re-update to
								// config's default
								this.updatePlayerKarma(name,
										config.playerKarmaDefault);
							}
							sender.sendMessage(ChatColor.YELLOW + prefix + " "
									+ name + "'s karma reset");
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							sender.sendMessage(ChatColor.RED + prefix
									+ "Could not reset " + name + "'s karma");
							e.printStackTrace();
						}

					}
				}
				else
				{
					// did not give a player name, therefore error
					sender.sendMessage(ChatColor.RED + prefix
							+ " No player name given.");
				}
			}
			else
			{
				// Karma system disabled
				sender.sendMessage(ChatColor.RED + prefix + " Karma disabled.");
			}
			return true;
		}
		else if (com.equals("set"))
		{
			if (!config.karmaDisabled)
			{
				// Check if name was given
				if (args.length > 2)
				{
					// attempt to parse name
					String name = args[2];
					// Check if amount was given
					if (args.length > 3)
					{
						// Attempt to parse amount
						int amount = 0;
						try
						{
							amount = Integer.parseInt(args[3]);
						}
						catch (NumberFormatException e)
						{
							// Invalid integer given for amount
							sender.sendMessage(ChatColor.RED + prefix + args[2]
									+ " is not a valid integer");
						}
						// SQL query to get player count for specified name
						String query = "SELECT COUNT(*) FROM players WHERE playername='"
								+ name + "';";
						ResultSet rs = ks.getLiteDB().select(query);
						// Check ResultSet
						boolean has = false;
						try
						{
							if (rs.next())
							{
								// Check if only received 1 entry
								if (rs.getInt(1) == 1)
								{
									// we have a single name
									has = true;
								}
								else if (rs.getInt(1) > 1)
								{
									sender.sendMessage(ChatColor.RED
											+ prefix
											+ " Got more than one result. Possibly incomplete name?");
								}
								else
								{
									// Player not in database, therefore error
									// on player part
									sender.sendMessage(ChatColor.RED + prefix
											+ " Player " + ChatColor.WHITE
											+ name + ChatColor.RED
											+ " not in database.");
									sender.sendMessage(ChatColor.RED
											+ prefix
											+ " Player names are case sensitive.");
								}
							}
							else
							{
								// Error in query...
								sender.sendMessage(ChatColor.RED + prefix
										+ " SQL query error");
							}
							rs.close();
						}
						catch (SQLException e)
						{
							// INFO Auto-generated catch block
							sender.sendMessage(ChatColor.RED + prefix
									+ "Could not set " + name + "'s karma");
							e.printStackTrace();
						}
						if (has)
						{
							int karma;
							try
							{
								// Set to given amount by using the difference
								// between the two
								karma = amount - this.getPlayerKarma(name);
								this.updatePlayerKarma(name, karma);
								if (config.playerKarmaDefault != 0)
								{
									// Default was non-zero, so re-update to
									// config's default
									this.updatePlayerKarma(name,
											config.playerKarmaDefault);
								}
								sender.sendMessage(ChatColor.YELLOW + prefix
										+ " " + name + "'s karma set");
							}
							catch (SQLException e)
							{
								// INFO Auto-generated catch block
								sender.sendMessage(ChatColor.RED + prefix
										+ "Could not set " + name + "'s karma");
								e.printStackTrace();
							}

						}
					}
					else
					{
						// did not give a karma value, therefore error
						sender.sendMessage(ChatColor.RED + prefix
								+ " No karma amount given.");
					}
				}
				else
				{
					// did not give a player name, therefore error
					sender.sendMessage(ChatColor.RED + prefix
							+ " No player name given.");
				}
			}
			else
			{
				// Karma disabled
				sender.sendMessage(ChatColor.RED + prefix + " Karma disabled.");
			}
			return true;
		}
		else
		{
			// Bad command
			sender.sendMessage(ChatColor.RED + prefix
					+ " Syntax error. Use /ks admin for list of commands");
			return false;
		}
	}

	/**
	 * Quietly updates the local cache of the item pool
	 */
	private void updateCache(CommandSender sender) {
		// Get list of items from database
		ResultSet itemlist = ks.getLiteDB().select("SELECT * FROM items;");
		try
		{
			if (itemlist.next())
			{
				// Loop that updates the hashmap cache
				// This way I won't be querying the database
				// every time list is called
				do
				{
					// update cache with current result set
					Item i = new Item(itemlist.getInt("itemid"),
							itemlist.getByte("data"),
							itemlist.getShort("durability"));
					cache.put(i, itemlist.getInt("amount"));
				}
				while (itemlist.next());
			}
			itemlist.close();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			sender.sendMessage(ChatColor.RED + prefix + " SQL error.");
			e.printStackTrace();
		}
	}

	private void listMultipliers(CommandSender sender, int pageAdjust) {
		if (!config.karmaDisabled)
		{
			if (!config.statickarma)
			{
				// Add player to page hashmap, if they're not in it
				// so we know their position in the result list
				if (!multiPage.containsKey(sender.getName()))
				{
					multiPage.put(sender.getName(), 0);
				}
				else
				{
					// They already exist, so adjust if necessary
					if (pageAdjust != 0)
					{
						int adj = multiPage.get(sender.getName()).intValue()
								+ pageAdjust;
						multiPage.put(sender.getName(), adj);
					}
				}
				// Check if there is any entries in map
				if (!config.karma.isEmpty())
				{
					// Set hashmap to array
					Object[] array = config.karma.entrySet().toArray();
					int num = array.length / limit;
					double rem = (double) array.length % (double) limit;
					boolean valid = true;
					if (rem != 0)
					{
						num++;
					}
					if (multiPage.get(sender.getName()).intValue() < 0)
					{
						// They tried to use /ks prev when they're on page 0
						sender.sendMessage(ChatColor.YELLOW + prefix
								+ " Page does not exist");
						// reset their current page back to 0
						multiPage.put(sender.getName(), 0);
						valid = false;
					}
					else if ((multiPage.get(sender.getName()).intValue())
							* limit > array.length)
					{
						// They tried to use /ks next at the end of the list
						sender.sendMessage(ChatColor.YELLOW + prefix
								+ " Page does not exist");
						// Revert to last page
						multiPage.put(sender.getName(), num - 1);
						valid = false;
					}
					if (valid)
					{
						// Header with amount of pages
						sender.sendMessage(ChatColor.BLUE
								+ "==="
								+ ChatColor.GOLD
								+ "Karma Multiplier"
								+ ChatColor.BLUE
								+ "==="
								+ ChatColor.GREEN
								+ "Page: "
								+ ((multiPage.get(sender.getName()).intValue()) + 1)
								+ " of " + num + ChatColor.BLUE + "===");
						// list
						for (int i = ((multiPage.get(sender.getName())
								.intValue()) * limit); i < ((multiPage
								.get(sender.getName()).intValue()) * limit)
								+ limit; i++)
						{
							// Don't try to pull something beyond the bounds
							if (i < array.length)
							{
								@SuppressWarnings ("unchecked")
								String out = ChatColor.WHITE
										+ "Item: "
										+ ChatColor.AQUA
										// Thanks to DiddiZ for id -> material
										// name
										// using built-in class
										+ ((Map.Entry<Item, Integer>) array[i])
												.getKey().name
										+ ChatColor.WHITE
										+ " Karma: "
										+ ChatColor.GOLD
										+ ((Map.Entry<Item, Integer>) array[i])
												.getValue()
										+ ChatColor.WHITE
										+ " ID: "
										+ ChatColor.LIGHT_PURPLE
										+ ((Map.Entry<Item, Integer>) array[i])
												.getKey().itemId()
										+ ChatColor.WHITE
										+ " Data: "
										+ ChatColor.LIGHT_PURPLE
										+ ((Map.Entry<Item, Integer>) array[i])
												.getKey().itemData();
								sender.sendMessage(out);
							}
							else
							{
								break;
							}
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.YELLOW
							+ prefix
							+ " No karma multipliers, all items have karma value of "
							+ config.karmaChange);
				}
			}
			else
			{
				sender.sendMessage(ChatColor.YELLOW
						+ prefix
						+ " Using static karma system, all items have karma value of "
						+ config.karmaChange);
			}
		}
		else
		{
			// Karma disabled
			sender.sendMessage(ChatColor.RED + prefix + " Karma disabled.");
		}
	}

	/**
	 * Lists the items in the pool. Allows for pagination of the cache of items
	 * in pool.
	 *
	 * @param CommandSender
	 *            of the "list" command so we know who we're outputting to
	 * @param Integer
	 *            of the page to change to, if needed. Zero shows current page.
	 */
	@SuppressWarnings ("unchecked")
	private void listPool(CommandSender sender, int pageAdjust) {
		// Get list of items from database
		ResultSet itemlist = ks.getLiteDB().select("SELECT * FROM items;");
		try
		{
			if (itemlist.next())
			{
				// Add player to page hashmap, if they're not in it
				// so we know their position in the result list
				if (!page.containsKey(sender.getName()))
				{
					page.put(sender.getName(), 0);
				}
				else
				{
					// They already exist, so adjust if necessary
					if (pageAdjust != 0)
					{
						int adj = page.get(sender.getName()).intValue()
								+ pageAdjust;
						page.put(sender.getName(), adj);
					}
				}
				// Clear all tool entry amounts to refresh properly
				Item[] toolClear = cache.keySet().toArray(new Item[0]);
				for (int i = 0; i < toolClear.length; i++)
				{
					if (toolClear[i].isTool())
					{
						cache.remove(toolClear[i]);
					}
				}
				// Loop that updates the hashmap cache
				// This way I won't be querying the database
				// every time list is called
				do
				{
					// update cache with current result set
					Item i = new Item(itemlist.getInt("itemid"),
							itemlist.getByte("data"),
							itemlist.getShort("durability"));
					if (i.isTool())
					{
						// add to current amount
						int itemAmount = itemlist.getInt("amount");
						if (cache.containsKey(i))
						{
							itemAmount += cache.get(i).intValue();
						}
						cache.put(i, itemAmount);
					}
					else
					{
						cache.put(i, itemlist.getInt("amount"));
					}
				}
				while (itemlist.next());

				// Set hashmap to array
				Object[] array = cache.entrySet().toArray();
				boolean valid = true;
				// Caluclate amount of pages
				int num = array.length / limit;
				double rem = (double) array.length % (double) limit;
				if (rem != 0)
				{
					num++;
				}
				if (page.get(sender.getName()).intValue() < 0)
				{
					// They tried to use /ks prev when they're on page 0
					sender.sendMessage(ChatColor.YELLOW + prefix
							+ " Page does not exist");
					// reset their current page back to 0
					page.put(sender.getName(), 0);
					valid = false;
				}
				else if ((page.get(sender.getName()).intValue()) * limit > array.length)
				{
					// They tried to use /ks next at the end of the list
					sender.sendMessage(ChatColor.YELLOW + prefix
							+ " Page does not exist");
					// Revert to last page
					page.put(sender.getName(), num - 1);
					valid = false;
				}
				if (valid)
				{
					// Header with amount of pages
					sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.GOLD
							+ "Item Pool" + ChatColor.BLUE + "==="
							+ ChatColor.GREEN + "Page: "
							+ ((page.get(sender.getName()).intValue()) + 1)
							+ " of " + num + ChatColor.BLUE + "===");
					// list
					for (int i = ((page.get(sender.getName()).intValue()) * limit); i < ((page
							.get(sender.getName()).intValue()) * limit) + limit; i++)
					{
						// Don't try to pull something beyond the bounds
						if (i < array.length)
						{
							StringBuilder sb = new StringBuilder();
							sb.append(ChatColor.WHITE + "Item: "
									+ ChatColor.AQUA
									// Thanks to DiddiZ for id -> material name
									// using built-in class
									+ ((Map.Entry<Item, Integer>) array[i])
											.getKey().name);
							sb.append(ChatColor.WHITE
									+ " Amount: "
									+ ChatColor.GOLD
									+ ((Map.Entry<Item, Integer>) array[i])
											.getValue());
							sb.append(ChatColor.WHITE
									+ " ID: "
									+ ChatColor.LIGHT_PURPLE
									+ ((Map.Entry<Item, Integer>) array[i])
											.getKey().itemId());
							sb.append(ChatColor.WHITE + " Data: "
									+ ChatColor.LIGHT_PURPLE);
							if (((Map.Entry<Item, Integer>) array[i]).getKey()
									.isPotion())
							{
								sb.append(((Map.Entry<Item, Integer>) array[i])
										.getKey().itemDurability());
							}
							else
							{
								sb.append(((Map.Entry<Item, Integer>) array[i])
										.getKey().itemData());
							}
							sender.sendMessage(sb.toString());
						}
						else
						{
							break;
						}
					}
				}
			}
			else
			{
				// No items in pool
				sender.sendMessage(ChatColor.RED + prefix
						+ " No items in pool.");
				// Clear hashmap (for memory reasons?)
				// considering no items, therefore no pages,
				// and thus no need to know what page a player is on
				page.clear();
			}
			itemlist.close();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			sender.sendMessage(ChatColor.RED + prefix + "SQL error.");
			e.printStackTrace();
		}
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
		if (config.effects)
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
		ResultSet rs = ks.getLiteDB().select(query);
		int karma = config.playerKarmaDefault;
		// Retrieve karma from database
		try
		{
			if (rs.next())
			{
				do
				{
					// TODO Catch multiple names via count(*) query
					// Grab player karma value
					karma = rs.getInt("karma");
				}
				while (rs.next());
			}
			else
			{
				// Player not in database, therefore add them
				query = "INSERT INTO players VALUES ('" + name + "','" + karma
						+ "');";
				ks.getLiteDB().standardQuery(query);
			}
			rs.close();
		}
		catch (SQLException e)
		{
			throw e;
		}
		return karma;
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
			if (karma <= config.lower)
			{
				// Updated karma value is beyond lower limit, so set to min
				query = "UPDATE players SET karma='" + config.lower
						+ "' WHERE playername='" + name + "';";
			}
			else if (karma >= config.upper)
			{
				// Updated karma value is beyond upper limit, so set to max
				query = "UPDATE players SET karma='" + config.upper
						+ "' WHERE playername='" + name + "';";
			}
			else
			{
				// Updated karma value is within acceptable range
				query = "UPDATE players SET karma='" + karma
						+ "' WHERE playername='" + name + "';";
			}
			ks.getLiteDB().standardQuery(query);
		}
		catch (SQLException e)
		{
			throw e;
		}
	}

	/**
	 * Colorizes the karma based on percentages in the config file
	 *
	 * @param player
	 *            karma
	 * @return Appropriate string with color codes
	 */
	private String colorizeKarma(int karma) {
		// Colorize based on how high/low karma is
		if (Math.abs(karma + config.lower) <= Math.abs(karma + config.upper))
		{
			// Positive karma
			if (((double) karma + Math.abs(config.lower))
					/ ((double) Math.abs(config.upper) + Math.abs(config.lower)) >= config.upperPercent)
			{
				return (ChatColor.YELLOW + prefix + ChatColor.GREEN
						+ " Karma: " + karma);
			}
			else
			{
				// Not in upper percentage
				return (ChatColor.YELLOW + prefix + " Karma: " + karma);
			}
		}
		else
		{
			// Negative karma
			if (((double) karma + Math.abs(config.lower))
					/ ((double) Math.abs(config.upper) + Math.abs(config.lower)) <= config.lowerPercent)
			{
				return (ChatColor.YELLOW + prefix + ChatColor.RED + " Karma: " + karma);
			}
			else
			{
				// Not in lower percentage
				return (ChatColor.YELLOW + prefix + " Karma: " + karma);
			}
		}
	}
}