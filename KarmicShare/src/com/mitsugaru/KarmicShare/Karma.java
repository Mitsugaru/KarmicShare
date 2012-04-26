package com.mitsugaru.KarmicShare;

import java.sql.SQLException;
import java.util.Map;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.database.Table;


public class Karma {
	KarmicShare plugin;

	public Karma(KarmicShare ks)
	{
		plugin = ks;
	}

	public boolean hasItem(Player player, ItemStack item, String group) {
		// Check if pool contains item requested + amount
		boolean has = false;
		// SQL query to see if item is in pool
		// Create temp item to check if its a tool
		final Item temp = new Item(item);
		String query = "";
		int poolAmount = 0;
		if (temp.isTool())
		{
			// Handle tools
			// Grab all entries of the same tool id
			String toolQuery = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
					+ item.getTypeId() + "' AND groups='" + group + "';";
			Query toolRS = plugin.getDatabaseHandler().select(toolQuery);
			try
			{
				if (toolRS.getResult().next())
				{
					do
					{
						poolAmount += toolRS.getResult().getInt("amount");
					}
					while (toolRS.getResult().next());
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
				toolRS.closeQuery();
			}
			catch (SQLException e)
			{
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
			query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='" + item.getTypeId()
					+ "' AND durability='" + item.getDurability()
					+ "' AND groups='" + group + "';";
			Query rs = plugin.getDatabaseHandler().select(query);

			// Check ResultSet
			try
			{
				if (rs.getResult().next())
				{
					// Item already in pool, check
					// amount
					poolAmount = rs.getResult().getInt("amount");
					if (poolAmount >= item.getAmount())
					{
						// We have enough in pool that
						// was requested
						has = true;
					}
				}
				else
				{
					// Item not in database
					has = false;
				}
				rs.closeQuery();
			}
			catch (SQLException e)
			{
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return false;
			}
		}
		else
		{
			// Not a tool or potion
			query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='" + item.getTypeId()
					+ "' AND data='" + item.getData().getData()
					+ "' AND groups='" + group + "';";
			Query rs = plugin.getDatabaseHandler().select(query);

			// Check ResultSet
			try
			{
				if (rs.getResult().next())
				{
					// Item already in pool, check
					// amount
					poolAmount = rs.getResult().getInt("amount");
					if (poolAmount >= item.getAmount())
					{
						// We have enough in pool that
						// was requested
						has = true;
					}
				}
				else
				{
					// Item not in database
					has = false;
				}
				rs.closeQuery();
			}
			catch (SQLException e)
			{
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return false;
			}
		}
		return has;
	}

	public int takeItem(Player player, ItemStack item, String group) {
		// Check if they have "take" permission
		if (plugin.getPermissionHandler().checkPermission(player,
				"KarmicShare.take"))
		{
			int karma = 0;
			if (!plugin.getPluginConfig().karmaDisabled)
			{
				if (!plugin.getPermissionHandler().checkPermission(player,
						"KarmicShare.ignore.karma"))
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
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Your karma is at the limit!");
							return -1;
						}
					}
					catch (SQLException e1)
					{
						player.sendMessage(ChatColor.RED + KarmicShare.prefix
								+ " Could not retrieve player karma");
						e1.printStackTrace();
						return -1;
					}
				}
			}
			int amount = item.getAmount();
			String query = "";
			boolean has = this.hasItem(player, item, group);
			Item temp = new Item(item);
			try
			{
				if (has)
				{
					boolean hasKarma = false;
					if (!plugin.getPluginConfig().karmaDisabled)
					{
						if (!plugin.getPermissionHandler().checkPermission(
								player, "KarmicShare.ignore.karma"))
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
									if (amount <= 0)
									{
										// Cannot get any items as
										// they'd go beyond
										// karma limit
										player.sendMessage(ChatColor.RED
												+ KarmicShare.prefix
												+ " Not enough karma to take item");
										return -1;
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
												return -1;
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
												return -1;
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
											return -1;
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
								toolQuery = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
										+ item.getTypeId()
										+ "' AND data='"
										+ item.getData().getData()
										+ "' AND enchantments='"
										+ sb.toString()
										+ "' AND groups='"
										+ group + "';";
								Query toolRS = plugin.getDatabaseHandler().select(
										toolQuery);
								if (toolRS.getResult().next())
								{
									if ((toolRS.getResult().getInt("amount") - amount) <= 0)
									{
										// DROP
										toolQuery = "DELETE FROM "
						+ Table.ITEMS.getName()
						+ " WHERE id='"
												+ toolRS.getResult().getInt("id") + "';";
									}
									else
									{
										// UPDATE
										toolQuery = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='"
												+ (toolRS.getResult().getInt("amount") - amount)
												+ "' WHERE id='"
												+ toolRS.getResult().getInt("id") + "';";
									}
								}
								toolRS.closeQuery();
							}
							else
							{
								// Non-enchanted tool
								toolQuery = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
										+ item.getTypeId()
										+ "' AND data='"
										+ item.getData().getData()
										+ "' AND groups='" + group + "';";
								Query toolRS = plugin.getDatabaseHandler().select(
										toolQuery);
								if (toolRS.getResult().next())
								{
									if ((toolRS.getResult().getInt("amount") - amount) <= 0)
									{
										// DROP
										toolQuery = "DELETE FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
												+ item.getTypeId()
												+ "' AND amount='"
												+ amount
												+ "' AND data='"
												+ item.getData().getData()
												+ "' AND groups='"
												+ group
												+ "';";
									}
									else
									{
										// UPDATE
										toolQuery = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='"
												+ (toolRS.getResult().getInt("amount") - amount)
												+ "' WHERE itemid='"
												+ item.getTypeId()
												+ "' AND data='"
												+ item.getData().getData()
												+ "' AND groups='" + group
												+ "';";
									}
								}
								toolRS.closeQuery();
							}
							plugin.getDatabaseHandler().standardQuery(toolQuery);
						}
						catch (SQLException e)
						{
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
							return -1;
						}
					}
					else if (temp.isPotion())
					{
						query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
								+ item.getTypeId() + "' AND durability='"
								+ item.getDurability() + "' AND groups='"
								+ group + "';";
						Query rs = plugin.getDatabaseHandler().select(query);
						try
						{
							if (rs.getResult().next())
							{
								if ((rs.getResult().getInt("amount") - amount) <= 0)
								{
									// Drop record as there are none left
									query = "DELETE FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
											+ item.getTypeId()
											+ "' AND durability='"
											+ item.getDurability()
											+ "' AND groups='" + group + "';";
								}
								else
								{
									query = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='"
											+ (rs.getResult().getInt("amount") - amount)
											+ "' WHERE itemid='"
											+ item.getTypeId()
											+ "' AND durability='"
											+ item.getDurability()
											+ "' AND groups='" + group + "';";
								}
							}
							rs.closeQuery();
							plugin.getDatabaseHandler().standardQuery(query);
						}
						catch (SQLException e)
						{
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
							return -1;
						}
					}
					else
					{
						query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
								+ item.getTypeId() + "' AND data='"
								+ item.getData().getData() + "' AND groups='"
								+ group + "';";
						Query rs = plugin.getDatabaseHandler().select(query);
						try
						{
							if (rs.getResult().next())
							{
								if ((rs.getResult().getInt("amount") - amount) <= 0)
								{
									// Drop record as there are none left
									query = "DELETE FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
											+ item.getTypeId() + "' AND data='"
											+ item.getData().getData()
											+ "' AND groups='" + group + "';";
								}
								else
								{
									query = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='"
											+ (rs.getResult().getInt("amount") - amount)
											+ "' WHERE itemid='"
											+ item.getTypeId() + "' AND data='"
											+ item.getData().getData()
											+ "' AND groups='" + group + "';";
								}
							}
							rs.closeQuery();
							plugin.getDatabaseHandler().standardQuery(query);
						}
						catch (SQLException e)
						{
							player.sendMessage(ChatColor.RED
									+ KarmicShare.prefix
									+ "Could not retrieve item in pool!");
							e.printStackTrace();
							return -1;
						}
					}
					// Smoke effect
					this.smokePlayer(player);
					// Update karma
					if (!plugin.getPluginConfig().karmaDisabled)
					{
						if (!plugin.getPermissionHandler().checkPermission(
								player, "KarmicShare.ignore.karma"))
						{
							if (hasKarma)
							{
								this.updatePlayerKarma(
										player.getName(),
										amount
												* plugin.getPluginConfig().karma
														.get(temp) * -1);
							}
							else
							{
								this.updatePlayerKarma(player.getName(), amount
										* plugin.getPluginConfig().karmaChange
										* -1);
							}
						}
					}
					// Update cache
					if (!plugin.getCommander().getCache().isEmpty())
					{
						if (plugin.getCommander().getCache().containsKey(temp))
						{
							int cacheAmount = plugin.getCommander().getCache()
									.get(temp).intValue();
							if ((cacheAmount - amount) <= 0)
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
					return -1;
				}
			}
			catch (SQLException e)
			{
				player.sendMessage(ChatColor.RED + KarmicShare.prefix
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
				return -1;
			}
			return amount;
		}
		else
		{
			player.sendMessage(ChatColor.RED + KarmicShare.prefix
					+ " Lack permission: KarmicShare.take");
		}
		return -1;
	}

	public boolean giveItem(Player player, ItemStack item, String group) {
		if (plugin.getPermissionHandler().checkPermission(player,
				"KarmicShare.give"))
		{
			final Item i = new Item(item);
			// Check if its a tool
			String query = "";
			if (i.isTool())
			{
				// Check if enchanted
				Map<Enchantment, Integer> enchantments = item.getEnchantments();
				if (!enchantments.isEmpty())
				{
					// Tool has enchantments
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
					query = "INSERT INTO "
						+ Table.ITEMS.getName()
						+ " (itemid,amount,data,durability,enchantments,groups) VALUES ('"
							+ item.getTypeId()
							+ "','"
							+ item.getAmount()
							+ "','"
							+ item.getData().getData()
							+ "','"
							+ item.getDurability()
							+ "','"
							+ sb.toString()
							+ "','" + group + "');";
					plugin.getDatabaseHandler().standardQuery(query);
				}
				else
				{
					// Normal tool
					// Create SQL query to see if item is already in
					// database
					query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='"
							+ item.getTypeId() + "' AND data='"
							+ item.getData().getData() + "' AND groups='"
							+ group + "';";
					Query rs = plugin.getDatabaseHandler().select(query);

					// Send Item to database
					try
					{
						if (rs.getResult().next())
						{
							do
							{
								// For tools, look up for similar
								// durability. Add amount that way
								// if it exists
								int total = item.getAmount()
										+ rs.getResult().getInt("amount");
								query = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='" + total
										+ "' WHERE itemid='" + item.getTypeId()
										+ "' AND data='"
										+ item.getData().getData()
										+ "' AND groups='" + group + "';";
							}
							while (rs.getResult().next());
						}
						else
						{
							// Item not in database, therefore add it
							query = "INSERT INTO "
						+ Table.ITEMS.getName()
						+ " (itemid,amount,data,durability,groups) VALUES ('"
									+ item.getTypeId()
									+ "','"
									+ item.getAmount()
									+ "','"
									+ item.getData().getData()
									+ "','"
									+ item.getDurability()
									+ "','"
									+ group
									+ "');";
						}
						rs.closeQuery();
						plugin.getDatabaseHandler().standardQuery(query);
					}
					catch (SQLException e)
					{
						player.sendMessage(ChatColor.RED + KarmicShare.prefix
								+ "Could not query item pool!");
						e.printStackTrace();
						return false;
					}
				}
			}
			else if (i.isPotion())
			{
				// Handle potion case
				// Potion item
				// Create SQL query to see if item is already in
				// database
				query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='" + item.getTypeId()
						+ "' AND durability='" + item.getDurability()
						+ "' AND groups='" + group + "';";
				Query rs = plugin.getDatabaseHandler().select(query);

				// Send Item to database
				try
				{
					if (rs.getResult().next())
					{
						// For potions, look up for similar
						// durability. Add amount that way
						// if it exists
						int total = item.getAmount() + rs.getResult().getInt("amount");
						query = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='" + total
								+ "' WHERE itemid='" + item.getTypeId()
								+ "' AND durability='" + item.getDurability()
								+ "' AND groups='" + group + "';";
					}
					else
					{
						// Item not in database, therefore add it
						query = "INSERT INTO "
						+ Table.ITEMS.getName()
						+ " (itemid,amount,data,durability,groups) VALUES ('"
								+ item.getTypeId()
								+ "','"
								+ item.getAmount()
								+ "','0','"
								+ item.getDurability()
								+ "','"
								+ group + "');";
					}
					rs.closeQuery();
					plugin.getDatabaseHandler().standardQuery(query);
				}
				catch (SQLException e)
				{
					player.sendMessage(ChatColor.RED + KarmicShare.prefix
							+ "Could not query item pool!");
					e.printStackTrace();
					return false;
				}
			}
			else
			{
				// Normal item
				// Create SQL query to see if item is already in
				// database
				query = "SELECT * FROM "
						+ Table.ITEMS.getName()
						+ " WHERE itemid='" + item.getTypeId()
						+ "' AND data='" + item.getData().getData()
						+ "' AND groups='" + group + "';";
				Query rs = plugin.getDatabaseHandler().select(query);

				// Send Item to database
				try
				{
					if (rs.getResult().next())
					{
						do
						{
							// For tools, look up for similar
							// durability. Add amount that way
							// if it exists
							int total = item.getAmount() + rs.getResult().getInt("amount");
							query = "UPDATE "
						+ Table.ITEMS.getName()
						+ " SET amount='" + total
									+ "' WHERE itemid='" + item.getTypeId()
									+ "' AND data='" + item.getData().getData()
									+ "' AND groups='" + group + "';";
						}
						while (rs.getResult().next());
					}
					else
					{
						// Item not in database, therefore add it
						query = "INSERT INTO "
						+ Table.ITEMS.getName()
						+ " (itemid,amount,data,durability,groups) VALUES ('"
								+ item.getTypeId()
								+ "','"
								+ item.getAmount()
								+ "','"
								+ item.getData().getData()
								+ "','"
								+ item.getDurability() + "','" + group + "');";
					}
					rs.closeQuery();
					plugin.getDatabaseHandler().standardQuery(query);
				}
				catch (SQLException e)
				{
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
					if (!plugin.getPermissionHandler().checkPermission(player,
							"KarmicShare.ignore.karma"))
					{
						if (plugin.getPluginConfig().statickarma)
						{
							this.updatePlayerKarma(
									player.getName(),
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
			}
			catch (SQLException e)
			{
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
	public void updatePlayerKarma(String name, int k) throws SQLException {
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
				query = "UPDATE "
						+ Table.PLAYERS.getName()
						+ " SET karma='"
						+ plugin.getPluginConfig().lower
						+ "' WHERE playername='" + name + "';";
			}
			else if (karma >= plugin.getPluginConfig().upper)
			{
				// Updated karma value is beyond upper limit, so set to max
				query = "UPDATE "
						+ Table.PLAYERS.getName()
						+ " SET karma='"
						+ plugin.getPluginConfig().upper
						+ "' WHERE playername='" + name + "';";
			}
			else
			{
				// Updated karma value is within acceptable range
				query = "UPDATE "
						+ Table.PLAYERS.getName()
						+ " SET karma='" + karma
						+ "' WHERE playername='" + name + "';";
			}
			plugin.getDatabaseHandler().standardQuery(query);
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
	public int getPlayerKarma(String name) throws SQLException {
		String query = "SELECT * FROM "
						+ Table.PLAYERS.getName()
						+ " WHERE playername='" + name + "';";
		Query rs = plugin.getDatabaseHandler().select(query);
		int karma = plugin.getPluginConfig().playerKarmaDefault;
		boolean has = false;
		// Retrieve karma from database
		try
		{
			if (rs.getResult().next())
			{
				do
				{
					// Grab player karma value
					karma = rs.getResult().getInt("karma");
					has = true;
				}
				while (rs.getResult().next());
			}
			rs.closeQuery();
			if (!has)
			{
				// Player not in database, therefore add them
				query = "INSERT INTO "
						+ Table.PLAYERS.getName()
						+ " (playername,karma) VALUES ('"
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
	public void smokePlayer(Player player) {
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

	public boolean validGroup(CommandSender sender, String group)
	{
		if(group.equals("global"))
		{
			return true;
		}
		boolean valid = false;
		try
		{
			Query rs = plugin.getDatabaseHandler().select("SELECT * FROM "
						+ Table.GROUPS.getName()
						+ " WHERE groupname='" + group + "';");
			if(rs.getResult().next())
			{
				valid = true;
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.prefix
					+ " SQL Exception");
			e.printStackTrace();
		}
		return valid;
	}

	public boolean playerHasGroup(CommandSender sender, String name, String group)
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
			Query rs = plugin.getDatabaseHandler().select("SELECT * FROM "
						+ Table.PLAYERS.getName()
						+ " WHERE playername='" + name + "';");
			if(rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				if(!rs.getResult().wasNull())
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
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.prefix
					+ " SQL Exception");
			e.printStackTrace();
		}
		return has;
	}
}
