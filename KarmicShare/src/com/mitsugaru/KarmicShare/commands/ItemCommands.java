package com.mitsugaru.KarmicShare.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.config.Config;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.logic.ItemLogic;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.permissions.PermCheck;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;

public class ItemCommands
{
	private static KarmicShare plugin;
	private static Config config;

	public static void init(KarmicShare ks)
	{
		plugin = ks;
		config = ks.getPluginConfig();
	}

	public static void takeItem(CommandSender sender, String[] args)
	{
		// Take item from pool
		// Check if player sent command
		if (!(sender instanceof Player))
		{
			sender.sendMessage(KarmicShare.TAG
					+ " Cannot use this command as console.");
			return;
		}
		Player player = (Player) sender;
		// Check if they have "take" permission
		if (!PermCheck.checkPermission(sender, PermissionNode.TAKE))
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Lack permission: " + PermissionNode.TAKE.getNode());
			return;
		}
		if (!PermCheck.checkPermission(sender, PermissionNode.COMMANDS_TAKE))
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Lack permission: "
					+ PermissionNode.COMMANDS_TAKE.getNode());
			return;
		}
		// Check that they gave an item name/id
		if (args.length <= 1)
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Need an item name or id");
			return;
		}
		// Grab group
		String group = Karma.selectedGroup.get(sender.getName());
		if (group == null)
		{
			Karma.selectedGroup.put(sender.getName(), "global");
			group = "global";
		}
		final int groupId = Karma.getGroupId(group);
		if (groupId == -1)
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Unknown group '" + ChatColor.GRAY + group
					+ ChatColor.RED + "'");
			return;
		}
		// Player will always request at least 1 item
		int itemid = 0, data = 0, amount = 1;
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
					dur = Short.parseShort(cut[1]);
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
					player.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Invalid item id / data value");
					return;
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
				// Karma.updateCache(sender);
				// FIXME Check if item exists in cache through
				// reverse lookup: name -> id:data
				// Item[] array = Karma.cache.keySet().toArray(new Item[0]);
				// for (int i = 0; i < array.length; i++)
				// {
				// String cacheName = array[i].name.toLowerCase();
				// if (temp.equals(cacheName))
				// {
				// // Item is in cache, so get item id
				// // and data values
				// itemid = array[i].itemId();
				// data = array[i].getData();
				// dur = array[i].itemDurability();
				// has = true;
				// break;
				// }
				// }
				if (!has)
				{
					// Item not in cache, therefore
					// potential error on player part
					player.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Item not in pool...");
					return;
				}
			}
		}
		// Create temp item
		final Item temp = new Item(itemid, Byte.valueOf("" + data), dur);
		ItemStack item = new ItemStack(1);
		int finalAmount = 0;
		if (temp.isTool())
		{
			// Grab all entries of the same tool id
			String toolQuery = "SELECT * FROM " + Table.ITEMS.getName()
					+ " WHERE itemid='" + itemid + "' AND groups='" + groupId
					+ "';";
			Query toolRS = plugin.getDatabaseHandler().select(toolQuery);
			try
			{
				ArrayList<ItemStack> itemList = new ArrayList<ItemStack>();
				if (toolRS.getResult().next())
				{
					do
					{
						// Generate item
						ItemStack toolItem = new ItemStack(itemid, toolRS
								.getResult().getInt("amount"), toolRS
								.getResult().getShort("data"));
						String enchant = toolRS.getResult().getString(
								"enchantments");
						if (!toolRS.getResult().wasNull())
						{
							// It had enchantments
							String[] cut = enchant.split("i");
							for (int i = 0; i < cut.length; i++)
							{
								try
								{
									String[] cutter = cut[i].split("v");
									EnchantmentWrapper e = new EnchantmentWrapper(
											Integer.parseInt(cutter[0]));
									toolItem.addUnsafeEnchantment(
											e.getEnchantment(),
											Integer.parseInt(cutter[1]));
								}
								catch (NumberFormatException n)
								{
									// INGORE
								}
							}
						}
					} while (toolRS.getResult().next());
				}
				// Close ResultSet
				toolRS.closeQuery();
				boolean done = false;
				for (ItemStack i : itemList)
				{
					if (!done)
					{
						int a = ItemLogic.takeItem(player, i, group);
						if (a <= 0)
						{
							done = true;
						}
						else
						{
							i.setAmount(a);
							final HashMap<Integer, ItemStack> residual = player
									.getInventory().addItem(i);
							if (residual.size() != 0)
							{
								// Add back extra
								finalAmount -= residual.size();
								if (finalAmount <= 0)
								{
									// Did not give any items
									player.sendMessage(ChatColor.YELLOW
											+ KarmicShare.TAG
											+ " Your inventory is completely full...");
								}
								i.setAmount(residual.size());
								try
								{
									int currentKarma = Karma
											.getPlayerKarma(player.getName());
									ItemLogic.giveItem(player, i, group);
									Karma.updatePlayerKarma(player.getName(),
											currentKarma);
								}
								catch (SQLException e)
								{
									sender.sendMessage(ChatColor.RED
											+ KarmicShare.TAG
											+ " SQL Exception");
									e.printStackTrace();
								}
								done = true;
							}
							else
							{
								finalAmount += a;
							}
						}

					}
					else
					{
						break;
					}
				}
			}
			catch (SQLException e)
			{
				player.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ "Could not retrieve item in pool!");
				e.printStackTrace();
			}
		}
		else if (temp.isPotion())
		{
			item = new ItemStack(itemid, amount, Short.valueOf("" + data));
			finalAmount = ItemLogic.takeItem(player, item, group);
			if (finalAmount > 0)
			{
				item.setAmount(finalAmount);
				final HashMap<Integer, ItemStack> residual = player
						.getInventory().addItem(item);
				if (residual.size() != 0)
				{
					// Add back extra
					finalAmount -= residual.size();
					if (finalAmount <= 0)
					{
						// Did not give any items
						player.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Your inventory is completely full...");
					}
					item.setAmount(residual.size());
					try
					{
						int currentKarma = Karma.getPlayerKarma(player
								.getName());
						ItemLogic.giveItem(player, item, group);
						Karma.updatePlayerKarma(player.getName(), currentKarma);
					}
					catch (SQLException e)
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " SQL Exception");
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			item = new ItemStack(itemid, amount, Byte.valueOf("" + data));
			finalAmount = ItemLogic.takeItem(player, item, group);
			if (finalAmount > 0)
			{
				item.setAmount(finalAmount);
				final HashMap<Integer, ItemStack> residual = player
						.getInventory().addItem(item);
				if (residual.size() != 0)
				{
					// Add back extra
					finalAmount -= residual.size();
					if (finalAmount <= 0)
					{
						// Did not give any items
						player.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Your inventory is completely full...");
					}
					item.setAmount(residual.size());
					try
					{
						int currentKarma = Karma.getPlayerKarma(player
								.getName());
						ItemLogic.giveItem(player, item, group);
						Karma.updatePlayerKarma(player.getName(), currentKarma);
					}
					catch (SQLException e)
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " SQL Exception");
						e.printStackTrace();
					}
				}
			}
		}
		if (finalAmount > 0)
		{
			player.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " Given "
					+ ChatColor.GOLD + finalAmount + ChatColor.GREEN + " of "
					+ ChatColor.AQUA + temp.name);
		}
	}

	public static void giveItem(CommandSender sender, String[] args)
	{
		// TODO allow for player to specify item and amount
		// parse more parameters to allow spaces for item names
		// Check if player sent command
		if (!(sender instanceof Player))
		{
			sender.sendMessage(KarmicShare.TAG
					+ " Cannot use this command as console.");
			return;
		}
		Player player = (Player) sender;
		// Check if they have "give" permission
		if (!PermCheck.checkPermission(sender, PermissionNode.GIVE))
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Lack permission: " + PermissionNode.GIVE.getNode());
			return;
		}
		if (!PermCheck.checkPermission(sender, PermissionNode.COMMANDS_GIVE))
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Lack permission: "
					+ PermissionNode.COMMANDS_GIVE.getNode());
			return;
		}
		// Grab group
		String group = Karma.selectedGroup.get(sender.getName());
		if (group == null)
		{
			Karma.selectedGroup.put(sender.getName(), "global");
			group = "global";
		}
		final int groupId = Karma.getGroupId(group);
		if (groupId == -1)
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Unknown group '" + ChatColor.GRAY + group
					+ ChatColor.RED + "'");
			return;
		}
		// Grab item in player's hand.
		final ItemStack items = player.getItemInHand();
		// Check if there is an item in their hand
		if (items.getTypeId() == 0)
		{
			// If there is no item, stop
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " No item in hand. Nothing to give.");
			return;
		}
		ItemLogic.giveItem(player, items, group);
		// Remove item from player inventory
		// Thanks to @nisovin for the following line
		final Item i = new Item(items.getTypeId(), items.getData().getData(),
				items.getDurability());
		player.setItemInHand(null);
		player.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " Added "
				+ ChatColor.GOLD + items.getAmount() + ChatColor.GREEN + " of "
				+ ChatColor.AQUA + i.name + ChatColor.GREEN + " to pool.");
	}

	public static void inspectItem(CommandSender sender, String[] args)
	{
		// Inspect item in hand
		if (!(sender instanceof Player))
		{
			// Console cannot inspect items
			sender.sendMessage(KarmicShare.TAG
					+ " Cannot use this command as console.");
			return;
		}
		// Permission check
		if (!PermCheck.checkPermission(sender, PermissionNode.INFO))
		{
			// Lack permission
			sender.sendMessage(KarmicShare.TAG + " Lack permission: "
					+ PermissionNode.INFO.getNode());
			return;
		}
		Player player = (Player) sender;
		// Grab item in player's hand.
		ItemStack items = player.getItemInHand();
		int itemid = items.getTypeId();
		// Check if there is an item in their hand
		if (itemid != 0)
		{
			// If there is no item, stop
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " No item in hand. Nothing to lookup.");
			return;
		}
		int quantity = items.getAmount();
		Item item = new Item(itemid, items.getData().getData(),
				items.getDurability());
		StringBuffer buf = new StringBuffer();
		buf.append("Info: Name: " + ChatColor.AQUA + item.name
				+ ChatColor.GREEN + " ID: " + ChatColor.LIGHT_PURPLE + itemid
				+ ChatColor.GREEN + " Amount:" + ChatColor.GOLD + quantity
				+ ChatColor.GREEN + " Data: " + ChatColor.LIGHT_PURPLE
				+ item.getData() + ChatColor.GREEN + " Damage: "
				+ ChatColor.LIGHT_PURPLE + items.getDurability()
				+ ChatColor.GREEN + " Tool: " + ChatColor.GRAY + item.isTool()
				+ ChatColor.GREEN + " Potion: " + ChatColor.GRAY
				+ item.isPotion());
		if (!config.karmaDisabled)
		{
			if (config.statickarma)
			{
				buf.append(ChatColor.GREEN + " Multiplier: " + ChatColor.YELLOW
						+ config.karmaChange);
				buf.append(ChatColor.GREEN + " Total Karma: "
						+ ChatColor.YELLOW + ""
						+ (config.karmaChange * quantity));
			}
			else
			{
				// Check if given item has a multiplier
				Item[] karmaList = config.karma.keySet().toArray(new Item[0]);
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
						buf.append(ChatColor.GREEN + " Multiplier: "
								+ ChatColor.YELLOW + config.karma.get(item));
						buf.append(ChatColor.GREEN + " Total Karma: "
								+ ChatColor.YELLOW + ""
								+ (config.karma.get(item) * quantity));
					}
					catch (NullPointerException n)
					{
						// Found item, but there is no
						// config for specific data value
						// thus adjust using regular means
						buf.append(ChatColor.GREEN + " Multiplier: "
								+ ChatColor.YELLOW + config.karmaChange);
						buf.append(ChatColor.GREEN + " Total Karma: "
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
		Map<Enchantment, Integer> enchantments = items.getEnchantments();
		if (enchantments.isEmpty())
		{
			buf.append(ChatColor.GREEN + " Enchantments: " + ChatColor.WHITE
					+ "NONE");
		}
		else
		{
			buf.append(ChatColor.GREEN + " Enchantments: ");

			for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet())
			{
				buf.append(ChatColor.WHITE + e.getKey().getName()
						+ ChatColor.YELLOW + " v" + e.getValue().intValue()
						+ ", ");
			}
		}
		player.sendMessage(ChatColor.GREEN + KarmicShare.TAG + buf.toString());
	}
}
