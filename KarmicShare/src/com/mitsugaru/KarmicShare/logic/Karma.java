package com.mitsugaru.KarmicShare.logic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.inventory.GroupPageInfo;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.inventory.KSInventoryHolder;
import com.mitsugaru.KarmicShare.tasks.ShowKSInventoryTask;

public class Karma
{
	private static KarmicShare plugin;
	public static final int chestSize = 54;
	public static final String GROUP_NAME_REGEX = "[\\p{Alnum}_[\\-]]*";
	public static final Map<String, String> selectedGroup = new HashMap<String, String>();
	public static final Map<GroupPageInfo, KSInventoryHolder> inventories = new HashMap<GroupPageInfo, KSInventoryHolder>();
	public static final Map<Item, Integer> cache = new HashMap<Item, Integer>();
	public static final Map<String, Integer> chestPage = new HashMap<String, Integer>();
	public static final Map<String, Integer> page = new HashMap<String, Integer>(),
			multiPage = new HashMap<String, Integer>();

	public static void init(KarmicShare ks)
	{
		plugin = ks;
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
	public static void updatePlayerKarma(String name, int k)
			throws SQLException
	{
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
				query = "UPDATE " + Table.PLAYERS.getName() + " SET karma='"
						+ plugin.getPluginConfig().lower
						+ "' WHERE playername='" + name + "';";
			}
			else if (karma >= plugin.getPluginConfig().upper)
			{
				// Updated karma value is beyond upper limit, so set to max
				query = "UPDATE " + Table.PLAYERS.getName() + " SET karma='"
						+ plugin.getPluginConfig().upper
						+ "' WHERE playername='" + name + "';";
			}
			else
			{
				// Updated karma value is within acceptable range
				query = "UPDATE " + Table.PLAYERS.getName() + " SET karma='"
						+ karma + "' WHERE playername='" + name + "';";
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
	public static int getPlayerKarma(String name) throws SQLException
	{
		String query = "SELECT * FROM " + Table.PLAYERS.getName()
				+ " WHERE playername='" + name + "';";
		final Query rs = plugin.getDatabaseHandler().select(query);
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
				} while (rs.getResult().next());
			}
			rs.closeQuery();
			if (!has)
			{
				// Player not in database, therefore add them
				query = "INSERT INTO " + Table.PLAYERS.getName()
						+ " (playername,karma) VALUES ('" + name + "','"
						+ karma + "');";
				plugin.getDatabaseHandler().standardQuery(query);
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		return karma;
	}

	public static boolean validGroup(CommandSender sender, String group)
	{
		if (group.equals("global"))
		{
			return true;
		}
		else if (group.equals("self_" + sender.getName().toLowerCase()))
		{
			return true;
		}
		boolean valid = false;
		try
		{
			final Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.GROUPS.getName()
							+ " WHERE groupname='" + group + "';");
			if (rs.getResult().next())
			{
				valid = true;
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
		return valid;
	}

	public static boolean playerHasGroup(CommandSender sender, String name,
			String group)
	{
		if (group.equals("global"))
		{
			return true;
		}
		else if (group.equals("self_" + sender.getName().toLowerCase()))
		{
			return true;
		}
		boolean has = false;
		try
		{
			// Insures that the player is added to the database
			getPlayerKarma(name);
			String groups = "";
			final Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName()
							+ " WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				if (!rs.getResult().wasNull())
				{
					if (groups.contains("&"))
					{
						// they have multiple groups
						for (String s : groups.split("&"))
						{
							try
							{
								// grab id of given group and compare against
								// ids
								final String groupName = getGroupName(Integer
										.parseInt(s));
								if (groupName.equalsIgnoreCase(group))
								{
									has = true;
								}
							}
							catch (NumberFormatException n)
							{
								// bad group id
							}
						}
					}
					else
					{
						try
						{
							final String groupName = getGroupName(Integer
									.parseInt(groups));
							// they only have one group
							if (groupName.equalsIgnoreCase(group))
							{
								has = true;
							}
						}
						catch (NumberFormatException n)
						{
							// bad group id
						}
					}
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
		return has;
	}

	public static List<String> getPlayerGroups(CommandSender sender, String name)
	{
		List<String> list = new ArrayList<String>();
		boolean wasNull = false;
		try
		{
			String groups = "";
			Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName()
							+ " WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				wasNull = rs.getResult().wasNull();
			}
			rs.closeQuery();
			if (wasNull)
			{
				// No groups, add in the global and self
				plugin.getDatabaseHandler().standardQuery(
						"INSERT INTO " + Table.GROUPS.getName()
								+ " (groupname) VALUES ('self_"
								+ name.toLowerCase() + "');");
				groups = getGroupId("global") + "&"
						+ getGroupId("self_" + name.toLowerCase());
				// Set groups for future reference
				plugin.getDatabaseHandler()
						.standardQuery(
								"UPDATE " + Table.PLAYERS.getName()
										+ " SET groups='" + groups
										+ "' WHERE playername='" + name + "';");
			}
			String[] split = groups.split("&");
			for (String s : split)
			{
				try
				{
					list.add(getGroupName(Integer.parseInt(s)));
				}
				catch (NumberFormatException n)
				{
					plugin.getLogger().severe("Bad group id '" + s + "'");
					n.printStackTrace();
				}
			}
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
		return list;
	}

	public static int getGroupId(String group)
	{
		int id = -1;
		try
		{
			final Query query = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.GROUPS.getName()
							+ " WHERE groupname='" + group + "';");
			if (query.getResult().next())
			{
				id = query.getResult().getInt("id");
				if (query.getResult().wasNull())
				{
					id = -1;
				}
			}
			query.closeQuery();
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					"SQL Exception on getting group '" + group + "' id");
			e.printStackTrace();
		}
		return id;
	}

	public static String getGroupName(int id)
	{
		String group = "NONE";
		try
		{
			final Query query = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.GROUPS.getName() + " WHERE id='"
							+ id + "';");
			if (query.getResult().next())
			{
				group = query.getResult().getString("groupname");
				if (query.getResult().wasNull())
				{
					group = "NONE";
				}
			}
			query.closeQuery();
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					"SQL Exception on getting group name for  id '" + id + "'");
			e.printStackTrace();
		}
		return group;
	}

	/**
	 * Quietly updates the local cache of the item pool
	 */
	public static void updateCache(CommandSender sender)
	{
		// Get list of items from database
		Query itemlist = plugin.getDatabaseHandler().select(
				"SELECT * FROM " + Table.ITEMS.getName()
						+ " WHERE groups='global';");
		try
		{
			if (itemlist.getResult().next())
			{
				// Loop that updates the hashmap cache
				// This way I won't be querying the database
				// every time list is called
				do
				{
					// update cache with current result set
					Item i = new Item(itemlist.getResult().getInt("itemid"),
							itemlist.getResult().getByte("data"), itemlist
									.getResult().getShort("durability"));
					Karma.cache.put(i, itemlist.getResult().getInt("amount"));
				} while (itemlist.getResult().next());
			}
			itemlist.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " SQL error.");
			e.printStackTrace();
		}
	}

	public static void removePlayerFromGroup(CommandSender sender, String name,
			String group)
	{
		try
		{
			String groups = "";
			Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName()
							+ " WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				if (!rs.getResult().wasNull())
				{
					if (groups.contains("&"))
					{
						// Multigroup
						StringBuilder sb = new StringBuilder();
						for (String s : groups.split("&"))
						{
							plugin.getLogger().info(s);
							// Add back all groups excluding specified group
							if (!s.equals(group))
							{
								sb.append(s + "&");
							}
						}
						// Remove trailing ampersand
						sb.deleteCharAt(sb.length() - 1);
						groups = sb.toString();
					}
					else
					{
						groups = "";
					}
				}
			}
			rs.closeQuery();
			// Update their groups
			plugin.getDatabaseHandler().standardQuery(
					"UPDATE " + Table.PLAYERS.getName() + " SET groups='"
							+ groups + "' WHERE playername='" + name + "';");
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public static void addPlayerToGroup(CommandSender sender, String name,
			String group)
	{
		try
		{
			// Insures that the player is added to the database
			Karma.getPlayerKarma(name);
			String groups = "";
			Query rs = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.PLAYERS.getName()
							+ " WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				groups = rs.getResult().getString("groups");
				if (!rs.getResult().wasNull())
				{
					groups += "&" + group;
				}
				else
				{
					groups = group;
				}
			}
			rs.closeQuery();
			// Update their groups
			plugin.getDatabaseHandler().standardQuery(
					"UPDATE " + Table.PLAYERS.getName() + " SET groups='"
							+ groups + "' WHERE playername='" + name + "';");
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " SQL Exception");
			e.printStackTrace();
		}
	}

	public static void showInventory(Player player, String group, int page)
	{
		// Show inventory
		final GroupPageInfo info = new GroupPageInfo(group, page);
		Inventory inventory = null;
		if (Karma.inventories.containsKey(info))
		{
			// Grab already open inventory
			inventory = Karma.inventories.get(info).getInventory();
			// plugin.getLogger().info("inventory already open");
		}
		else
		{
			final KSInventoryHolder holder = new KSInventoryHolder(info);
			inventory = plugin.getServer().createInventory(holder, chestSize,
					group + " : " + page);
			populateInventory(inventory, page, group);
			holder.setInventory(inventory);
			Karma.inventories.put(info, holder);
			// plugin.getLogger().info("inventory first open");
		}
		// Set task
		final int id = plugin
				.getServer()
				.getScheduler()
				.scheduleSyncDelayedTask(plugin,
						new ShowKSInventoryTask(plugin, player, inventory), 3);
		if (id == -1)
		{
			plugin.getLogger()
					.warning(
							"Could not schedule open inventory for "
									+ player.getName());
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Could not schedule open inventory!");
		}
	}

	private static void populateInventory(Inventory inventory, int page,
			String group)
	{
		try
		{
			int count = 0;
			int start = (page - 1) * chestSize;
			int groupId = Karma.getGroupId(group);
			if (groupId == -1)
			{
				return;
			}
			Query itemList = plugin.getDatabaseHandler().select(
					"SELECT * FROM " + Table.ITEMS.getName()
							+ " WHERE groups='" + groupId + "';");
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
					ItemStack item = null;
					if (Item.isTool(id))
					{
						item = new ItemStack(id, amount, dur);
					}
					else
					{
						item = new ItemStack(id, amount, dur, data);
					}
					// Generate psudo item to calculate slots taken up
					int maxStack = item.getType().getMaxStackSize();
					if (maxStack <= 0)
					{
						maxStack = 1;
					}
					int stacks = amount / maxStack;
					final double rem = (double) amount % (double) maxStack;
					if (rem != 0)
					{
						stacks++;
					}
					for (int x = 0; x < stacks; x++)
					{
						ItemStack add = item.clone();
						if (amount < maxStack)
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
							try
							{
								// If tool
								if (meta.isTool())
								{
									// Check for enchantments
									String enchantments = itemList.getResult()
											.getString("enchantments");
									if (!itemList.getResult().wasNull())
									{
										if (!enchantments.equals(""))
										{
											String[] cut = enchantments
													.split("i");
											for (int s = 0; s < cut.length; s++)
											{
												String[] cutter = cut[s]
														.split("v");
												EnchantmentWrapper e = new EnchantmentWrapper(
														Integer.parseInt(cutter[0]));
												add.addUnsafeEnchantment(
														e.getEnchantment(),
														Integer.parseInt(cutter[1]));

											}
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
							catch (NumberFormatException e)
							{
								// Ignore faulty item
							}
						}
						count++;
					}
				} while (itemList.getResult().next() && !done);
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
			plugin.getLogger().warning(
					ChatColor.RED + KarmicShare.TAG + "SQL error.");
			e.printStackTrace();
		}
	}

	public static void cycleGroup(Player player, String current,
			Direction direction)
	{
		String nextGroup = current;
		final List<String> list = Karma.getPlayerGroups(player,
				player.getName());
		int index = list.indexOf(current);
		switch (direction)
		{
			case FORWARD:
			{
				if (index + 1 >= list.size())
				{
					nextGroup = list.get(0);
				}
				else
				{
					nextGroup = list.get(index + 1);
				}
				break;
			}
			case BACKWARD:
			{
				if (index - 1 < 0)
				{
					nextGroup = list.get(list.size() - 1);
				}
				else
				{
					nextGroup = list.get(index - 1);
				}
				break;
			}
		}
		Karma.selectedGroup.put(player.getName(), nextGroup);
		player.sendMessage(ChatColor.GREEN + KarmicShare.TAG
				+ " Changed group to '" + ChatColor.GOLD + nextGroup
				+ ChatColor.GREEN + "'");
	}

	public static int grabPage(int current, String group, Direction direction)
	{
		// Calculate number of slots
		int slots = 0;
		int groupId = Karma.getGroupId(group);
		if (groupId == -1)
		{
			return 1;
		}
		final Query all = plugin.getDatabaseHandler().select(
				"SELECT * FROM " + Table.ITEMS.getName() + " WHERE groups='"
						+ groupId + "';");
		try
		{
			if (all.getResult().next())
			{
				do
				{
					final int amount = all.getResult().getInt("amount");
					if (!all.getResult().wasNull())
					{
						final ItemStack item = new ItemStack(all.getResult()
								.getInt("itemid"), amount);
						int maxStack = item.getType().getMaxStackSize();
						if (maxStack <= 0)
						{
							maxStack = 1;
						}
						int stacks = amount / maxStack;
						final double rem = (double) amount % (double) maxStack;
						if (rem != 0)
						{
							stacks++;
						}
						slots += stacks;
					}
				} while (all.getResult().next());
			}
			all.closeQuery();
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					ChatColor.RED + KarmicShare.TAG + "SQL error.");
			e.printStackTrace();
		}
		// if no slots, return 1
		if (slots <= 0)
		{
			return 1;
		}
		// Calculate pages
		int pageTotal = slots / chestSize;
		final double rem = (double) slots % (double) chestSize;
		if (rem != 0)
		{
			pageTotal++;
		}
		// Check against maximum
		if (current >= Integer.MAX_VALUE)
		{
			// Cycle back as we're at the max value for an integer
			return 1;
		}
		int page = current;
		switch (direction)
		{
			case FORWARD:
			{
				page++;
				break;
			}
			case BACKWARD:
			{
				page--;
				break;
			}
			default:
			{
				break;
			}
		}
		if (page <= 0)
		{
			// Was negative or zero, loop back to max page
			page = (pageTotal + 1);
		}
		// Allow for empty page
		else if (page > (pageTotal + 1))
		{
			// Going to page beyond the total items, cycle back to
			// first
			page = 1;
		}
		return page;
	}

	public enum Direction
	{
		FORWARD, BACKWARD, CURRENT;
	}
}