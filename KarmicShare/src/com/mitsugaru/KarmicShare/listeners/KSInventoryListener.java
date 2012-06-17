package com.mitsugaru.KarmicShare.listeners;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.RootConfig;
import com.mitsugaru.KarmicShare.config.ConfigNode;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.inventory.KSInventoryHolder;
import com.mitsugaru.KarmicShare.logic.ItemLogic;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.tasks.Repopulate;

public class KSInventoryListener implements Listener
{
	private KarmicShare plugin;

	public KSInventoryListener(KarmicShare karmicShare)
	{
		plugin = karmicShare;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryOpen(InventoryOpenEvent event)
	{
		if (!event.isCancelled())
		{
			final KSInventoryHolder holder = instanceCheck(event);
			if (holder != null)
			{
				holder.getInfo().addViewer();
				if (RootConfig.getBoolean(ConfigNode.DEBUG_ITEM))
				{
					plugin.getLogger().info(
							"added viewer to " + holder.getInfo().getGroup()
									+ ":" + holder.getInfo().getPage());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryClose(InventoryCloseEvent event)
	{
		final KSInventoryHolder holder = instanceCheck(event);
		if (holder != null)
		{
			holder.getInfo().removeViewer();
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						"removed viewer from " + holder.getInfo().getGroup()
								+ ":" + holder.getInfo().getPage());
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent event)
	{
		// Valid slot numbers are not negative
		if (event.getSlot() < 0)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						"Inventory slot negative, ignore action");
			}
			return;
		}
		// Verify our inventory holder
		final KSInventoryHolder holder = instanceCheck(event);
		if (holder == null)
		{
			// Not ours, we don't care
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("Not a KS inventory, ignore action");
			}
			return;
		}
		boolean fromChest = false;
		// Differentiate between chest inventory and player
		// inventory click
		// plugin.getLogger().info("slot: " + event.getRawSlot());
		if (event.getRawSlot() < Karma.chestSize)
		{
			fromChest = true;
		}
		if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
		{
			plugin.getLogger().info(
					event.getWhoClicked().getName() + " action from chest: "
							+ fromChest);
		}
		String group = holder.getInfo().getGroup();
		if (!plugin.useChest())
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("Plugin chests are disabled");
			}
			event.setCancelled(true);
			return;
		}
		// plugin.getLogger().info("our chest");
		try
		{
			if (fromChest)
			{
				if (event.isShiftClick())
				{
					if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
					{
						plugin.getLogger().info(
								event.getWhoClicked().getName()
										+ ": Shift click");
					}
					if (event.getCurrentItem().getType().equals(Material.AIR))
					{
						// We don't care about air if its not inside the chest
						// inventory
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": AIR, ignore");
						}
						return;
					}
					else if (event.isLeftClick())
					{
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": left click");
						}
						shiftTakeItem(event, group);
					}
					else if (event.isRightClick())
					{
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": Right click");
						}
						shiftTakeItem(event, group);
					}
				}
				else if (!event.getCurrentItem().getType().equals(Material.AIR)
						&& !event.getCursor().getType().equals(Material.AIR))
				{
					if (event.getCurrentItem() != null
							&& event.getCursor() != null)
					{
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": Not both air");
						}
						final Item a = new Item(event.getCurrentItem());
						final Item b = new Item(event.getCursor());
						if (a.areSame(b) && event.isLeftClick())
						{
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger().info(
										event.getWhoClicked().getName()
												+ ": Left click, same type");
							}
							int cursorAmount = event.getCursor().getAmount();
							int itemAmount = event.getCurrentItem().getAmount();
							int totalAmount = cursorAmount + itemAmount;
							if (itemAmount < event.getCurrentItem()
									.getMaxStackSize())
							{
								if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
								{
									plugin.getLogger()
											.info(event.getWhoClicked()
													.getName()
													+ ": Item not at max stack");
								}
								ItemStack item = handleEnchantments(event
										.getCursor());
								if (totalAmount > event.getCurrentItem()
										.getMaxStackSize())
								{
									if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
									{
										plugin.getLogger()
												.info(event.getWhoClicked()
														.getName()
														+ ": Adjusting amount to not exceed max stack");
									}
									item.setAmount(event.getCurrentItem()
											.getMaxStackSize() - itemAmount);
								}
								/*
								 * Of the same time, so add to current stack
								 */
								if (!ItemLogic.giveItem(
										plugin.getServer()
												.getPlayer(
														event.getWhoClicked()
																.getName()),
										item, group))
								{
									if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
									{
										plugin.getLogger()
												.info(event.getWhoClicked()
														.getName()
														+ ": Did not give item, deny");
									}
									event.setCancelled(true);
								}
							}
							else
							{
								if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
								{
									plugin.getLogger().info(
											event.getWhoClicked().getName()
													+ ": Denied");
								}
								event.setCancelled(true);
							}
						}
						else if (a.areSame(b) && event.isRightClick())
						{
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger()
										.info(event.getWhoClicked().getName()
												+ ": Right click, same type, give single");
							}
							giveSingle(event, group);
						}
						else
						{
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger().info(
										event.getWhoClicked().getName()
												+ ": Not same type, switch");
							}
							switchingItems(event, group);
						}
					}
				}
				else if (!event.getCurrentItem().getType().equals(Material.AIR))
				{
					if (event.isLeftClick())
					{
						/*
						 * Attempting to take item
						 */
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": left click, take");
						}
						final int amount = ItemLogic.takeItem(
								plugin.getServer().getPlayer(
										event.getWhoClicked().getName()),
								event.getCurrentItem(), group);
						final int original = event.getCurrentItem().getAmount();
						if (amount == event.getCurrentItem().getAmount())
						{
							// IGNORE
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger().info(
										event.getWhoClicked().getName()
												+ ": success, take all");
							}
						}
						else if (amount < event.getCurrentItem().getAmount()
								&& amount > 0)
						{
							event.getCurrentItem().setAmount(amount);
							final ItemStack bak = event.getCurrentItem()
									.clone();
							bak.setAmount(original - amount);
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger().info(
										event.getWhoClicked().getName()
												+ ": success, take some");
							}
						}
						else
						{
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger().info(
										event.getWhoClicked().getName()
												+ ": denied");
							}
							event.setCancelled(true);
						}
					}
					else if (event.isRightClick())
					{
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": right click, half stack");
						}
						halfStack(event, group);
					}
				}
				else if (!event.getCursor().getType().equals(Material.AIR))
				{
					if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
					{
						plugin.getLogger().info("Cursor not AIR");
					}
					if (event.isLeftClick())
					{
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger().info(
									event.getWhoClicked().getName()
											+ ": left click, empty slot");
						}
						// Putting item into empty slot in chest
						if (!ItemLogic.giveItem(
								plugin.getServer().getPlayer(
										event.getWhoClicked().getName()),
								event.getCursor(), group))
						{
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger().info(
										event.getWhoClicked().getName()
												+ ": Did not give item, deny");
							}
							event.setCancelled(true);
						}
					}
					else if (event.isRightClick())
					{
						if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
						{
							plugin.getLogger()
									.info(event.getWhoClicked().getName()
											+ ": right click, empty slot, give one");
						}
						// Clone
						ItemStack item = handleEnchantments(event.getCursor());
						// Only giving one
						item.setAmount(1);
						if (!ItemLogic.giveItem(
								plugin.getServer().getPlayer(
										event.getWhoClicked().getName()), item,
								group))
						{
							if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
							{
								plugin.getLogger()
										.info(event.getWhoClicked().getName()
												+ ": Did not give one of item, deny");
							}
							event.setCancelled(true);
						}
					}
				}
			}
			else
			{
				if (event.getCurrentItem().getType().equals(Material.AIR))
				{
					// We don't care about air if its not inside the chest
					// inventory
					if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
					{
						plugin.getLogger().info(
								event.getWhoClicked().getName()
										+ ": AIR, not inside chest");
					}
					return;
				}
				else if (!event.isShiftClick())
				{
					// Only care about shift click here
					if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
					{
						plugin.getLogger().info(
								event.getWhoClicked().getName()
										+ ": Not shift click, ignore");
					}
					return;
				}
				else if (event.isLeftClick())
				{
					if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
					{
						plugin.getLogger().info(
								event.getWhoClicked().getName()
										+ ": Shift left click from player");
					}
					shiftGiveItem(event, group);
				}
				else if (event.isRightClick())
				{
					if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
					{
						plugin.getLogger().info(
								event.getWhoClicked().getName()
										+ ": Shift right click from player");
					}
					shiftGiveItem(event, group);
				}
			}
		}
		catch (NullPointerException e)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						event.getWhoClicked().getName() + ": NPE exception");
			}
			e.printStackTrace();
		}
	}

	private void giveSingle(InventoryClickEvent event, String group)
	{
		if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
		{
			plugin.getLogger().info(
					event.getWhoClicked().getName() + ": giveSingle(event,"
							+ group + ")");
		}
		// Same item, so give only one from cursor
		// to item
		// Construct singular of cursor
		// item
		ItemStack item = handleEnchantments(event.getCurrentItem());
		if ((item.getAmount() + 1) <= event.getCurrentItem().getMaxStackSize())
		{
			// plugin.getLogger().info(
			// "can add 1 to stack");
			item.setAmount(1);
			if (!ItemLogic.giveItem(
					plugin.getServer().getPlayer(
							event.getWhoClicked().getName()), item, group))
			{
				if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
				{
					plugin.getLogger().info(
							event.getWhoClicked().getName()
									+ ": did not give item, deny");
				}
				event.setCancelled(true);
			}
		}
		else
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						event.getWhoClicked().getName()
								+ ": would have gone over max stack");
			}
			event.setCancelled(true);
		}
	}

	private void halfStack(InventoryClickEvent event, String group)
	{
		if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
		{
			plugin.getLogger().info(
					event.getWhoClicked().getName() + ": take half stack");
		}
		// If cursor is air and item is not air they are
		// taking half of the stack, with the larger half
		// given to cursor
		// Calculate "half"
		int half = event.getCurrentItem().getAmount() / 2;
		final double rem = (double) event.getCurrentItem().getAmount() % 2.0;
		if (rem != 0)
		{
			half++;
		}
		// Clone
		ItemStack item = handleEnchantments(event.getCurrentItem());
		item.setAmount(half);
		// Send to database
		final int amount = ItemLogic.takeItem(
				plugin.getServer().getPlayer(event.getWhoClicked().getName()),
				item, group);
		if (amount == half)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						event.getWhoClicked().getName()
								+ ": success, took half");
			}
			// IGNORE
		}
		else if (amount < event.getCurrentItem().getAmount() && amount > 0)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						event.getWhoClicked().getName()
								+ ": success, took some");
			}
			final ItemStack bak = event.getCurrentItem().clone();
			bak.setAmount(event.getCurrentItem().getAmount() - amount);
			Repopulate task = new Repopulate(event.getInventory(), bak,
					event.getSlot(), false);
			int id = plugin.getServer().getScheduler()
					.scheduleSyncDelayedTask(plugin, task, 1);
			if (id == -1)
			{
				if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
				{
					plugin.getLogger().warning(
							event.getWhoClicked().getName()
									+ ": could not repopulate slot");
				}
				plugin.getServer()
						.getPlayer(event.getWhoClicked().getName())
						.sendMessage(
								ChatColor.YELLOW + KarmicShare.TAG
										+ " Could not repopulate slot.");
			}
			item.setAmount(amount);
			task = new Repopulate(plugin.getServer()
					.getPlayer(event.getWhoClicked().getName()).getInventory(),
					item);
			id = plugin.getServer().getScheduler()
					.scheduleSyncDelayedTask(plugin, task, 1);
			if (id == -1)
			{
				if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
				{
					plugin.getLogger().warning(
							event.getWhoClicked().getName()
									+ ": could not repopulate slot");
				}
				plugin.getServer()
						.getPlayer(event.getWhoClicked().getName())
						.sendMessage(
								ChatColor.YELLOW + KarmicShare.TAG
										+ "Could not give item.");
			}
			event.setResult(Event.Result.DENY);
		}
		else
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						event.getWhoClicked().getName() + ": deny");
			}
			event.setCancelled(true);
		}
	}

	private ItemStack handleEnchantments(ItemStack item)
	{
		ItemStack send;
		if (item.getEnchantments().isEmpty())
		{
			return item.clone();
		}
		send = new ItemStack(item.getTypeId(), item.getAmount(),
				item.getDurability());
		for (Map.Entry<Enchantment, Integer> enchantment : item
				.getEnchantments().entrySet())
		{
			send.addUnsafeEnchantment(enchantment.getKey(), enchantment
					.getValue().intValue());
		}
		return send;
	}

	/**
	 * Switching items from chest to cursor When switching, put item first, then
	 * attempt to take item
	 */
	private void switchingItems(InventoryClickEvent event, String group)
	{
		if (ItemLogic.giveItem(
				plugin.getServer().getPlayer(event.getWhoClicked().getName()),
				event.getCursor(), group))
		{
			final int amount = ItemLogic.takeItem(
					plugin.getServer().getPlayer(
							event.getWhoClicked().getName()),
					event.getCurrentItem(), group);
			final int original = event.getCurrentItem().getAmount();
			if (amount == event.getCurrentItem().getAmount())
			{
				// IGNORE
				if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
				{
					plugin.getLogger().info(
							event.getWhoClicked().getName()
									+ ": success, take all");
				}
			}
			else if (amount < event.getCurrentItem().getAmount() && amount > 0)
			{
				if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
				{
					plugin.getLogger().info(
							event.getWhoClicked().getName()
									+ ": success, take some");
				}
				event.getCurrentItem().setAmount(amount);
				final ItemStack bak = event.getCurrentItem().clone();
				bak.setAmount(original - amount);
			}
			else
			{
				if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
				{
					plugin.getLogger().info(
							event.getWhoClicked().getName() + ": deny take");
				}
				event.setCancelled(true);
				// TODO clear cursor as the item was at least given
			}
		}
		else
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info(
						event.getWhoClicked().getName() + ": deny give");
			}
			event.setCancelled(true);
		}
	}

	private void shiftGiveItem(InventoryClickEvent event, String group)
	{
		if (event.getInventory().firstEmpty() < 0)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("No empty slot available");
			}
			event.setCancelled(true);
			return;
		}
		if (ItemLogic.giveItem(
				plugin.getServer().getPlayer(event.getWhoClicked().getName()),
				event.getCurrentItem(), group))
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("gave item");
			}
		}
		else
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("Did not give item");
			}
			event.setCancelled(true);
		}
	}

	private void shiftTakeItem(InventoryClickEvent event, String group)
	{
		if (event.getWhoClicked().getInventory().firstEmpty() < 0)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("No empty slot available");
			}
			event.setCancelled(true);
			return;
		}
		final int amount = ItemLogic.takeItem(
				plugin.getServer().getPlayer(event.getWhoClicked().getName()),
				event.getCurrentItem(), group);
		final int original = event.getCurrentItem().getAmount();
		if (amount < event.getCurrentItem().getAmount() && amount > 0)
		{
			if (RootConfig.getBoolean(ConfigNode.DEBUG_INVENTORY))
			{
				plugin.getLogger().info("take some");
			}
			final ItemStack bak = event.getCurrentItem().clone();
			bak.setAmount(original - amount);
			Repopulate task = new Repopulate(event.getInventory(), bak,
					event.getSlot(), false);
			int id = plugin.getServer().getScheduler()
					.scheduleSyncDelayedTask(plugin, task, 1);
			if (id == -1)
			{
				plugin.getServer()
						.getPlayer(event.getWhoClicked().getName())
						.sendMessage(
								ChatColor.YELLOW + KarmicShare.TAG
										+ " Could not repopulate slot.");
			}
			ItemStack item = handleEnchantments(event.getCurrentItem());
			item.setAmount(amount);
			task = new Repopulate(event.getWhoClicked().getInventory(), item);
			id = plugin.getServer().getScheduler()
					.scheduleSyncDelayedTask(plugin, task, 1);
			if (id == -1)
			{
				plugin.getServer()
						.getPlayer(event.getWhoClicked().getName())
						.sendMessage(
								ChatColor.YELLOW + KarmicShare.TAG
										+ " Could not give item.");
			}
			event.setCancelled(true);
		}
	}

	private KSInventoryHolder instanceCheck(InventoryEvent event)
	{
		KSInventoryHolder holder = null;
		try
		{
			if (event.getInventory().getHolder() != null)
			{
				if (event.getInventory().getHolder() instanceof KSInventoryHolder)
				{
					holder = (KSInventoryHolder) event.getInventory()
							.getHolder();
				}
			}
		}
		catch (NullPointerException n)
		{
			// IGNORE
		}
		return holder;
	}
}