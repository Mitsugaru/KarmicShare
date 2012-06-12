package com.mitsugaru.KarmicShare.commands;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.Config;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.database.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.logic.Karma;

class ListCommands
{
	private static KarmicShare plugin;
	private static Config config;

	static void init(KarmicShare ks)
	{
		plugin = ks;
		config = ks.getPluginConfig();
	}

	static void valueCommand(CommandSender sender, String[] args)
	{
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
					Karma.multiPage.put(sender.getName(), pageNum - 1);
					// Show page if possible
					listMultipliers(sender, 0);
				}
				catch (NumberFormatException e)
				{
					// Maybe they did prev/next?
					if (args[1].equals("prev"))
					{
						// List, with previous page
						listMultipliers(sender, -1);
					}
					else if (args[1].equals("next"))
					{
						// List, with previous page
						listMultipliers(sender, 1);
					}
					else
					{
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Invalid extra parameter: " + args[1]);
					}
				}
			}
			else
			{
				// List with current page
				listMultipliers(sender, 0);
			}
		}
		else
		{
			// karma system disabled
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Karma disabled");
		}
	}

	static void listCommand(CommandSender sender, String[] args)
	{
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
				Karma.page.put(sender.getName(), pageNum - 1);
				// Show page if possible
				listPool(sender, 0);
			}
			catch (NumberFormatException e)
			{
				// TODO this is where I would catch the item's
				// partial name. Probably use regex and see if
				// an item's name in cache matches.
				// Will need to modify listPool to accept regex
				// Can't think of a good way to page through this
				// new list without having a hashmap per custom
				// cache, and I really don't want to do that :\
				sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
						+ " Invalid integer for page number");
			}
		}
		else
		{
			// List with current page
			listPool(sender, 0);
		}
	}

	private static void listMultipliers(CommandSender sender, int pageAdjust)
	{
		if (config.karmaDisabled)
		{
			// Karma disabled
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Karma disabled.");
			return;
		}
		else if (config.statickarma)
		{
			sender.sendMessage(ChatColor.YELLOW
					+ KarmicShare.TAG
					+ " Using static karma system, all items have karma value of "
					+ config.karmaChange);
			return;
		}
		// Add player to page hashmap, if they're not in it
		// so we know their position in the result list
		if (!Karma.multiPage.containsKey(sender.getName()))
		{
			Karma.multiPage.put(sender.getName(), 0);
		}
		else
		{
			// They already exist, so adjust if necessary
			if (pageAdjust != 0)
			{
				int adj = Karma.multiPage.get(sender.getName()).intValue()
						+ pageAdjust;
				Karma.multiPage.put(sender.getName(), adj);
			}
		}
		// Check if there is any entries in map
		if (config.karma.isEmpty())
		{
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " No karma multipliers, all items have karma value of "
					+ config.karmaChange);
			return;
		}
		// Set hashmap to array
		Object[] array = config.karma.entrySet().toArray();
		int num = array.length / config.listlimit;
		double rem = (double) array.length % (double) config.listlimit;
		boolean valid = true;
		if (rem != 0)
		{
			num++;
		}
		if (Karma.multiPage.get(sender.getName()).intValue() < 0)
		{
			// They tried to use /ks prev when they're on page 0
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Page does not exist");
			// reset their current page back to 0
			Karma.multiPage.put(sender.getName(), 0);
			valid = false;
		}
		else if ((Karma.multiPage.get(sender.getName()).intValue())
				* config.listlimit > array.length)
		{
			// They tried to use /ks next at the end of the list
			sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Page does not exist");
			// Revert to last page
			Karma.multiPage.put(sender.getName(), num - 1);
			valid = false;
		}
		if (valid)
		{
			// Header with amount of pages
			sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.GOLD
					+ "Karma Multiplier" + ChatColor.BLUE + "==="
					+ ChatColor.GREEN + "Page: "
					+ ((Karma.multiPage.get(sender.getName()).intValue()) + 1)
					+ " of " + num + ChatColor.BLUE + "===");
			// list
			for (int i = ((Karma.multiPage.get(sender.getName()).intValue()) * config.listlimit); i < ((Karma.multiPage
					.get(sender.getName()).intValue()) * config.listlimit)
					+ config.listlimit; i++)
			{
				// Don't try to pull something beyond the bounds
				if (i < array.length)
				{
					@SuppressWarnings("unchecked")
					String out = ChatColor.WHITE
							+ "Item: "
							+ ChatColor.AQUA
							// Thanks to DiddiZ for id -> material
							// name
							// using built-in class
							+ ((Map.Entry<Item, Integer>) array[i]).getKey().name
							+ ChatColor.WHITE
							+ " Karma: "
							+ ChatColor.GOLD
							+ ((Map.Entry<Item, Integer>) array[i]).getValue()
							+ ChatColor.WHITE
							+ " ID: "
							+ ChatColor.LIGHT_PURPLE
							+ ((Map.Entry<Item, Integer>) array[i]).getKey()
									.itemId()
							+ ChatColor.WHITE
							+ " Data: "
							+ ChatColor.LIGHT_PURPLE
							+ ((Map.Entry<Item, Integer>) array[i]).getKey()
									.itemData();
					sender.sendMessage(out);
				}
				else
				{
					break;
				}
			}
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
	@SuppressWarnings("unchecked")
	static void listPool(CommandSender sender, int pageAdjust)
	{
		String current = Karma.selectedGroup.get(sender.getName());
		if (current == null)
		{
			Karma.selectedGroup.put(sender.getName(), "global");
			current = "global";
		}
		final int groupId = Karma.getGroupId(current);
		if (groupId == -1)
		{
			return;
		}
		// Get list of items from database
		Query itemlist = plugin.getDatabaseHandler().select(
				"SELECT * FROM " + Table.ITEMS.getName() + " WHERE groups='"
						+ groupId + "';");
		boolean empty = false;
		try
		{
			final Map<Item, Integer> cache = new LinkedHashMap<Item, Integer>();
			if (itemlist.getResult().next())
			{
				// Add player to page hashmap, if they're not in it
				// so we know their position in the result list
				if (!Karma.page.containsKey(sender.getName()))
				{
					Karma.page.put(sender.getName(), 0);
				}
				else
				{
					// They already exist, so adjust if necessary
					if (pageAdjust != 0)
					{
						int adj = Karma.page.get(sender.getName()).intValue()
								+ pageAdjust;
						Karma.page.put(sender.getName(), adj);
					}
				}
				// Clear cache
				cache.clear();
				do
				{
					// update cache with current result set
					Item i = new Item(itemlist.getResult().getInt("itemid"),
							itemlist.getResult().getByte("data"), itemlist
									.getResult().getShort("durability"));
					if (i.isTool())
					{
						// add to current amount
						int itemAmount = itemlist.getResult().getInt("amount");
						if (cache.containsKey(i))
						{
							itemAmount += cache.get(i).intValue();
						}
						cache.put(i, itemAmount);
					}
					else
					{
						cache.put(i, itemlist.getResult().getInt("amount"));
					}
				} while (itemlist.getResult().next());
			}
			else
			{
				// No items in pool
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " No items in pool.");
				// Clear hashmap (for memory reasons?)
				// considering no items, therefore no pages,
				// and thus no need to know what page a player is on
				Karma.page.clear();
				empty = true;
			}
			// Close query
			itemlist.closeQuery();
			if (!empty)
			{
				// Set hashmap to array
				Object[] array = cache.entrySet().toArray();
				boolean valid = true;
				// Caluclate amount of pages
				int num = array.length / config.listlimit;
				double rem = (double) array.length % (double) config.listlimit;
				if (rem != 0)
				{
					num++;
				}
				if (Karma.page.get(sender.getName()).intValue() < 0)
				{
					// They tried to use /ks prev when they're on page 0
					sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
							+ " Page does not exist");
					// reset their current page back to 0
					Karma.page.put(sender.getName(), 0);
					valid = false;
				}
				else if ((Karma.page.get(sender.getName()).intValue())
						* config.listlimit > array.length)
				{
					// They tried to use /ks next at the end of the list
					sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
							+ " Page does not exist");
					// Revert to last page
					Karma.page.put(sender.getName(), num - 1);
					valid = false;
				}
				if (valid)
				{
					// Header with amount of pages
					sender.sendMessage(ChatColor.BLUE
							+ "==="
							+ ChatColor.GOLD
							+ current
							+ ChatColor.BLUE
							+ "==="
							+ ChatColor.GREEN
							+ "Page: "
							+ ((Karma.page.get(sender.getName()).intValue()) + 1)
							+ " of " + num + ChatColor.BLUE + "===");
					// list
					for (int i = ((Karma.page.get(sender.getName()).intValue()) * config.listlimit); i < ((Karma.page
							.get(sender.getName()).intValue()) * config.listlimit)
							+ config.listlimit; i++)
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
							// sb.append(ChatColor.WHITE
							// + " ID: "
							// + ChatColor.LIGHT_PURPLE
							// + ((Map.Entry<Item, Integer>) array[i])
							// .getKey().itemId());
							// sb.append(ChatColor.WHITE + " Data: "
							// + ChatColor.LIGHT_PURPLE);
							// if (((Map.Entry<Item, Integer>)
							// array[i]).getKey()
							// .isPotion())
							// {
							// sb.append(((Map.Entry<Item, Integer>) array[i])
							// .getKey().itemDurability());
							// }
							// else
							// {
							// sb.append(((Map.Entry<Item, Integer>) array[i])
							// .getKey().itemData());
							// }
							sender.sendMessage(sb.toString());
						}
						else
						{
							break;
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + "SQL error.");
			e.printStackTrace();
		}
		catch (NullPointerException n)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Error getting item list.");
			n.printStackTrace();
		}
	}
}
