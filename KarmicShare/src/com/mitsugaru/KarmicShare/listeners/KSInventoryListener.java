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
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent event)
	{
		// Valid slot numbers are not negative
		if (event.getSlot() < 0)
		{
			return;
		}
		// Verify our inventory holder
		final KSInventoryHolder holder = instanceCheck(event);
		if (holder == null)
		{
			// Not ours, we don't care
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
		String group = holder.getInfo().getGroup();
		if (!plugin.useChest())
		{
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
					if (event.getCurrentItem().getType().equals(Material.AIR))
					{
						// We don't care about air if its not inside the chest
						// inventory
						return;
					}
					else if (event.isLeftClick())
					{
						// plugin.getLogger().info("shift left click from chest");
						shiftTakeItem(event, group);
					}
					else if (event.isRightClick())
					{
						// plugin.getLogger().info("shift left click from chest");
						shiftTakeItem(event, group);
					}
				}
				else if (!event.getCurrentItem().getType().equals(Material.AIR)
						&& !event.getCursor().getType().equals(Material.AIR))
				{
					// plugin.getLogger().info("not both air");
					if (event.getCurrentItem() != null
							&& event.getCursor() != null)
					{
						final Item a = new Item(event.getCurrentItem());
						final Item b = new Item(event.getCursor());
						if (a.areSame(b) && event.isLeftClick())
						{
							// plugin.getLogger().info("left click same type");
							int cursorAmount = event.getCursor().getAmount();
							int itemAmount = event.getCurrentItem().getAmount();
							int totalAmount = cursorAmount + itemAmount;
							if (itemAmount < event.getCurrentItem()
									.getMaxStackSize())
							{
								// plugin.getLogger().info(
								// "item stack not at max stack");
								ItemStack item = handleEnchantments(event
										.getCursor());
								if (totalAmount > event.getCurrentItem()
										.getMaxStackSize())
								{
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
									event.setCancelled(true);
								}
							}
							else
							{
								// TODO fix logic to give the partial amount
								// that would be added
								event.setCancelled(true);
							}
						}
						else if (a.areSame(b) && event.isRightClick())
						{
							// plugin.getLogger().info("right click same type");
							giveSingle(event, group);
						}
						else
						{
							// plugin.getLogger().info("switch");
							switchingItems(event, group);
						}
					}
				}
				else if (!event.getCurrentItem().getType().equals(Material.AIR))
				{
					if (event.isLeftClick())
					{
						// plugin.getLogger().info("left click take");
						/*
						 * Attempting to take item
						 */
						final int amount = ItemLogic.takeItem(
								plugin.getServer().getPlayer(
										event.getWhoClicked().getName()),
								event.getCurrentItem(), group);
						final int original = event.getCurrentItem().getAmount();
						if (amount == event.getCurrentItem().getAmount())
						{
							// IGNORE
						}
						else if (amount < event.getCurrentItem().getAmount()
								&& amount > 0)
						{
							event.getCurrentItem().setAmount(amount);
							final ItemStack bak = event.getCurrentItem()
									.clone();
							bak.setAmount(original - amount);
						}
						else
						{
							event.setCancelled(true);
						}
					}
					else if (event.isRightClick())
					{
						// plugin.getLogger().info("right click half stack");
						halfStack(event, group);
					}
				}
				else if (!event.getCursor().getType().equals(Material.AIR))
				{
					if (event.isLeftClick())
					{
						// plugin.getLogger().info("left click empty slot");
						// Putting item into empty slot in chest
						if (!ItemLogic.giveItem(
								plugin.getServer().getPlayer(
										event.getWhoClicked().getName()),
								event.getCursor(), group))
						{
							//event.setResult(Event.Result.DENY);
							event.setCancelled(true);
						}
					}
					else if (event.isRightClick())
					{
						// plugin.getLogger().info("left click give one");
						// Clone
						ItemStack item = handleEnchantments(event
								.getCursor());
						// Only giving one
						item.setAmount(1);
						if (!ItemLogic.giveItem(
								plugin.getServer().getPlayer(
										event.getWhoClicked().getName()), item,
								group))
						{
							//event.setResult(Event.Result.DENY);
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
					return;
				}
				else if (!event.isShiftClick())
				{
					// Only care about shift click here
					return;
				}
				else if (event.isLeftClick())
				{
					// plugin.getLogger().info("shift left click from player");
					shiftGiveItem(event, group);
				}
				else if (event.isRightClick())
				{
					// plugin.getLogger().info("shift right click from player");
					shiftGiveItem(event, group);
				}
			}
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	private void giveSingle(InventoryClickEvent event, String group)
	{
		// plugin.getLogger().info(
		// "same item, give one");
		// Same item, so give only one from cursor
		// to item
		// Construct singular of cursor
		// item
		ItemStack item = handleEnchantments(event.getCurrentItem());
		int itemAmount = event.getCurrentItem().getAmount() + 1;
		if (itemAmount < event.getCurrentItem().getMaxStackSize())
		{
			// plugin.getLogger().info(
			// "can add 1 to stack");
			if (ItemLogic.giveItem(
					plugin.getServer().getPlayer(
							event.getWhoClicked().getName()), item, group))
			{
				item.setAmount(event.getCurrentItem().getAmount());
				event.getWhoClicked().getInventory().clear(event.getSlot());
			}
			else
			{
				event.setCancelled(true);
			}
		}
		else
		{
			event.setCancelled(true);
		}
	}

	private void halfStack(InventoryClickEvent event, String group)
	{
		// plugin.getLogger().info("take half the stack");
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
			// IGNORE
		}
		else if (amount < event.getCurrentItem().getAmount() && amount > 0)
		{
			final ItemStack bak = event.getCurrentItem().clone();
			bak.setAmount(event.getCurrentItem().getAmount() - amount);
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
			item.setAmount(amount);
			task = new Repopulate(plugin.getServer()
					.getPlayer(event.getWhoClicked().getName()).getInventory(),
					item);
			id = plugin.getServer().getScheduler()
					.scheduleSyncDelayedTask(plugin, task, 1);
			if (id == -1)
			{
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
			event.setResult(Event.Result.DENY);
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
			}
			else if (amount < event.getCurrentItem().getAmount() && amount > 0)
			{
				event.getCurrentItem().setAmount(amount);
				final ItemStack bak = event.getCurrentItem().clone();
				bak.setAmount(original - amount);
			}
			else
			{
				event.setCancelled(true);
				// TODO clear cursor as the item was at least given
			}
		}
		else
		{
			event.setCancelled(true);
		}
	}

	private void shiftGiveItem(InventoryClickEvent event, String group)
	{
		// plugin.getLogger().info("from player");
		if (event.getInventory().firstEmpty() < 0)
		{
			event.setCancelled(true);
		}
		if (ItemLogic.giveItem(
				plugin.getServer().getPlayer(event.getWhoClicked().getName()),
				event.getCurrentItem(), group))
		{
			// plugin.getLogger().info("gave item");
			ItemStack item = handleEnchantments(event.getCurrentItem());
			final Repopulate task = new Repopulate(event.getInventory(), item);
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
			event.getWhoClicked().getInventory().clear(event.getSlot());
		}
		else
		{
			event.setCancelled(true);
		}
	}

	private void shiftTakeItem(InventoryClickEvent event, String group)
	{
		if (event.getWhoClicked().getInventory().firstEmpty() < 0)
		{
			event.setCancelled(true);
		}
		final int amount = ItemLogic.takeItem(
				plugin.getServer().getPlayer(event.getWhoClicked().getName()),
				event.getCurrentItem(), group);
		final int original = event.getCurrentItem().getAmount();
		if (amount < event.getCurrentItem().getAmount() && amount > 0)
		{
			// plugin.getLogger().info("take some");
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