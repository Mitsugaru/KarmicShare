package com.mitsugaru.KarmicShare;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
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
	private Karma karma;

	public KSInventoryListener(KarmicShare karmicShare) {
		plugin = karmicShare;
		karma = karmicShare.getKarma();
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
						String group = "global";
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
									group = ChatColor.stripColor(
											sign.getLine(0)).toLowerCase();
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
										group = ChatColor.stripColor(
												sign.getLine(0)).toLowerCase();
									}
								}
							}
						}
						if (kschest && plugin.hasSpout)
						{
							try
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
												final int amount = karma.takeItem(
														event.getPlayer(),
														event.getItem(), group);
												final int original = event
														.getItem().getAmount();
												if (amount == event.getItem()
														.getAmount())
												{
													event.setResult(Event.Result.ALLOW);
													event.setResult(Event.Result.ALLOW);
													ItemStack item;
													if (event.getItem()
															.getEnchantments()
															.isEmpty())
													{
														item = event.getItem()
																.clone();
													}
													else
													{
														// Handle enchantments
														item = new ItemStack(
																event.getItem()
																		.getTypeId(),
																event.getItem()
																		.getAmount(),
																event.getItem()
																		.getDurability(),
																event.getItem()
																		.getData()
																		.getData());
														for (Map.Entry<Enchantment, Integer> enchantment : event
																.getItem()
																.getEnchantments()
																.entrySet())
														{
															item.addUnsafeEnchantment(
																	enchantment
																			.getKey(),
																	enchantment
																			.getValue()
																			.intValue());
														}
													}
													final Repopulate task = new Repopulate(
															event.getPlayer()
																	.getInventory(),
															item);
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
													event.setItem(null);
												}
												else if (amount < event
														.getItem().getAmount()
														&& amount > 0)
												{
													event.setResult(Event.Result.DENY);
													final ItemStack bak = event
															.getItem().clone();
													bak.setAmount(original
															- amount);
													Repopulate task = new Repopulate(
															event.getInventory(),
															bak, event
																	.getSlot(), false);
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
													final ItemStack give = event
															.getItem().clone();
													give.setAmount(amount);
													task = new Repopulate(event
															.getPlayer()
															.getInventory(),
															give);
													id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not give item.");
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
												if (karma.giveItem(event.getPlayer(),
														event.getItem(), group))
												{
													event.setResult(Event.Result.ALLOW);
													ItemStack item;
													if (event.getItem()
															.getEnchantments()
															.isEmpty())
													{
														item = event.getItem()
																.clone();
													}
													else
													{
														// Handle enchantments
														item = new ItemStack(
																event.getItem()
																		.getTypeId(),
																event.getItem()
																		.getAmount(),
																event.getItem()
																		.getDurability(),
																event.getItem()
																		.getData()
																		.getData());
														for (Map.Entry<Enchantment, Integer> enchantment : event
																.getItem()
																.getEnchantments()
																.entrySet())
														{
															item.addUnsafeEnchantment(
																	enchantment
																			.getKey(),
																	enchantment
																			.getValue()
																			.intValue());
														}
													}
													final Repopulate task = new Repopulate(
															chest.getInventory(),
															item);
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
													event.setItem(null);
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
												if (karma.giveItem(event.getPlayer(),
														event.getCursor(),
														group))
												{
													event.setResult(Event.Result.ALLOW);
													ItemStack item;
													if (event.getCursor()
															.getEnchantments()
															.isEmpty())
													{
														item = event
																.getCursor()
																.clone();
													}
													else
													{
														// Handle enchantments
														item = new ItemStack(
																event.getCursor()
																		.getTypeId(),
																event.getCursor()
																		.getAmount(),
																event.getCursor()
																		.getDurability(),
																event.getCursor()
																		.getData()
																		.getData());
														for (Map.Entry<Enchantment, Integer> enchantment : event
																.getCursor()
																.getEnchantments()
																.entrySet())
														{
															item.addUnsafeEnchantment(
																	enchantment
																			.getKey(),
																	enchantment
																			.getValue()
																			.intValue());
														}
													}
													repopulateTask(event.getPlayer(), chest.getInventory(), item);
													event.setItem(null);
												}
												else
												{
													event.setResult(Event.Result.DENY);
													event.setCancelled(true);
												}
											}
											else
											{
												// When switching, put item
												// first,
												// then
												// attempt to take item
												if (karma.giveItem(event.getPlayer(),
														event.getCursor(),
														group))
												{
													final int amount = karma.takeItem(
															event.getPlayer(),
															event.getItem(),
															group);
													final int original = event
															.getItem()
															.getAmount();
													if (amount == event
															.getItem()
															.getAmount())
													{
														event.setResult(Event.Result.ALLOW);
													}
													else if (amount < event
															.getItem()
															.getAmount()
															&& amount > 0)
													{
														event.setResult(Event.Result.ALLOW);
														event.getItem()
																.setAmount(
																		amount);
														final ItemStack bak = event
																.getItem()
																.clone();
														bak.setAmount(original
																- amount);
														final Repopulate task = new Repopulate(
																event.getInventory(),
																bak,
																event.getSlot(), false);
														int id = plugin
																.getServer()
																.getScheduler()
																.scheduleSyncDelayedTask(
																		plugin,
																		task, 1);
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
													event.setResult(Event.Result.DENY);
													event.setCancelled(true);
												}
											}
										}
										else if (event.getItem() != null)
										{
											final int amount = karma.takeItem(
													event.getPlayer(),
													event.getItem(), group);
											final int original = event
													.getItem().getAmount();
											if (amount == event.getItem()
													.getAmount())
											{
												event.setResult(Event.Result.ALLOW);
											}
											else if (amount < event.getItem()
													.getAmount() && amount > 0)
											{
												event.setResult(Event.Result.ALLOW);
												event.getItem().setAmount(
														amount);
												final ItemStack bak = event
														.getItem().clone();
												bak.setAmount(original - amount);
												final Repopulate task = new Repopulate(
														event.getInventory(),
														bak, event.getSlot(), false);
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task, 1);
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
										else if (event.getCursor() != null)
										{

											// they clicked on an item in chest
											if (karma.giveItem(event.getPlayer(),
													event.getCursor(), group))
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
									//If item is not null, they are taking whole stack
									// Handle right shift click
									if (event.isShiftClick())
									{
										if (event.getItem() != null)
										{
											if (fromChest)
											{
												final int amount = karma.takeItem(
														event.getPlayer(),
														event.getItem(), group);
												final int original = event
														.getItem().getAmount();
												if (amount == event.getItem()
														.getAmount())
												{
													event.setResult(Event.Result.DENY);
													ItemStack item;
													if (event.getItem()
															.getEnchantments()
															.isEmpty())
													{
														item = event.getItem()
																.clone();
													}
													else
													{
														// Handle enchantments
														item = new ItemStack(
																event.getItem()
																		.getTypeId(),
																event.getItem()
																		.getAmount(),
																event.getItem()
																		.getDurability(),
																event.getItem()
																		.getData()
																		.getData());
														for (Map.Entry<Enchantment, Integer> enchantment : event
																.getItem()
																.getEnchantments()
																.entrySet())
														{
															item.addUnsafeEnchantment(
																	enchantment
																			.getKey(),
																	enchantment
																			.getValue()
																			.intValue());
														}
													}
													final Repopulate task = new Repopulate(
															event.getPlayer()
																	.getInventory(),
															item);
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
													final Repopulate clear = new Repopulate(
															event.getInventory(), item, event.getSlot(), true);
													id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	clear, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
												}
												else if (amount < event
														.getItem().getAmount()
														&& amount > 0)
												{
													event.setResult(Event.Result.DENY);
													final ItemStack bak = event
															.getItem().clone();
													bak.setAmount(original
															- amount);
													Repopulate task = new Repopulate(
															event.getInventory(),
															bak, event
																	.getSlot(), false);
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
													final ItemStack give = event
															.getItem().clone();
													give.setAmount(amount);
													task = new Repopulate(event
															.getPlayer()
															.getInventory(),
															give);
													id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not give item.");
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
												if (karma.giveItem(event.getPlayer(),
														event.getItem(), group))
												{
													event.setResult(Event.Result.ALLOW);
													ItemStack item;
													if (event.getItem()
															.getEnchantments()
															.isEmpty())
													{
														item = event.getItem()
																.clone();
													}
													else
													{
														// Handle enchantments
														item = new ItemStack(
																event.getItem()
																		.getTypeId(),
																event.getItem()
																		.getAmount(),
																event.getItem()
																		.getDurability(),
																event.getItem()
																		.getData()
																		.getData());
														for (Map.Entry<Enchantment, Integer> enchantment : event
																.getItem()
																.getEnchantments()
																.entrySet())
														{
															item.addUnsafeEnchantment(
																	enchantment
																			.getKey(),
																	enchantment
																			.getValue()
																			.intValue());
														}
													}
													final Repopulate task = new Repopulate(
															chest.getInventory(),
															item);
													int id = plugin
															.getServer()
															.getScheduler()
															.scheduleSyncDelayedTask(
																	plugin,
																	task, 1);
													if (id == -1)
													{
														event.getPlayer()
																.sendMessage(
																		ChatColor.YELLOW
																				+ KarmicShare.prefix
																				+ "Could not repopulate slot.");
													}
													event.setItem(null);
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
											//If they're both the same item, giving 1 from cursor to item
											if (event
													.getItem()
													.getType()
													.equals(event.getCursor()
															.getType()))
											{
												//Construct singular of cursor item
												ItemStack item;
												if (event.getCursor()
														.getEnchantments()
														.isEmpty())
												{
													item = event
															.getCursor()
															.clone();
													item.setAmount(1);
												}
												else
												{
													// Handle enchantments
													item = new ItemStack(
															event.getCursor()
																	.getTypeId(),
															1,
															event.getCursor()
																	.getDurability(),
															event.getCursor()
																	.getData()
																	.getData());
													for (Map.Entry<Enchantment, Integer> enchantment : event
															.getCursor()
															.getEnchantments()
															.entrySet())
													{
														item.addUnsafeEnchantment(
																enchantment
																		.getKey(),
																enchantment
																		.getValue()
																		.intValue());
													}
												}
												if (karma.giveItem(event.getPlayer(),
														item,
														group))
												{
													event.setResult(Event.Result.ALLOW);
													item.setAmount(event.getItem().getAmount());
													repopulateTask(event.getPlayer(), chest.getInventory(), item);
													event.setItem(null);
												}
												else
												{
													event.setResult(Event.Result.DENY);
													event.setCancelled(true);
												}
											}
											else
											{
												// When switching, put item
												// first,
												// then
												// attempt to take item
												if (karma.giveItem(event.getPlayer(),
														event.getCursor(),
														group))
												{
													final int amount = karma.takeItem(
															event.getPlayer(),
															event.getItem(),
															group);
													final int original = event
															.getItem()
															.getAmount();
													if (amount == event
															.getItem()
															.getAmount())
													{
														event.setResult(Event.Result.ALLOW);
													}
													else if (amount < event
															.getItem()
															.getAmount()
															&& amount > 0)
													{
														event.setResult(Event.Result.ALLOW);
														event.getItem()
																.setAmount(
																		amount);
														final ItemStack bak = event
																.getItem()
																.clone();
														bak.setAmount(original
																- amount);
														final Repopulate task = new Repopulate(
																event.getInventory(),
																bak,
																event.getSlot(), false);
														int id = plugin
																.getServer()
																.getScheduler()
																.scheduleSyncDelayedTask(
																		plugin,
																		task, 1);
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
													event.setResult(Event.Result.DENY);
													event.setCancelled(true);
												}
											}
										}
										// If cursor is null and item is not null, they are taking half of the stack, with the larger half on cursor
										else if (event.getItem() != null)
										{
											//Calculate "half"
											int half = event.getItem().getAmount() / 2;
											final double rem = (double) event.getItem().getAmount() % 2.0;
											if(rem != 0)
											{
												half++;
											}
											//Clone
											ItemStack item;
											if (event.getItem()
													.getEnchantments()
													.isEmpty())
											{
												item = event.getItem()
														.clone();
											}
											else
											{
												// Handle enchantments
												item = new ItemStack(
														event.getItem()
																.getTypeId(),
														event.getItem()
																.getAmount(),
														event.getItem()
																.getDurability(),
														event.getItem()
																.getData()
																.getData());
												for (Map.Entry<Enchantment, Integer> enchantment : event
														.getItem()
														.getEnchantments()
														.entrySet())
												{
													item.addUnsafeEnchantment(
															enchantment
																	.getKey(),
															enchantment
																	.getValue()
																	.intValue());
												}
											}
											item.setAmount(half);
											//Send to database
											final int amount = karma.takeItem(
													event.getPlayer(),
													item, group);
											if (amount == half)
											{
												event.setResult(Event.Result.ALLOW);
											}
											else if (amount < event
													.getItem().getAmount()
													&& amount > 0)
											{
												event.setResult(Event.Result.DENY);
												final ItemStack bak = event
														.getItem().clone();
												bak.setAmount(event.getItem().getAmount()
														- amount);
												Repopulate task = new Repopulate(
														event.getInventory(),
														bak, event
																.getSlot(), false);
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin,
																task, 1);
												if (id == -1)
												{
													event.getPlayer()
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ "Could not repopulate slot.");
												}
												item.setAmount(amount);
												task = new Repopulate(event
														.getPlayer()
														.getInventory(),
														item);
												id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin,
																task, 1);
												if (id == -1)
												{
													event.getPlayer()
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ "Could not give item.");
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
											//Clone
											ItemStack item;
											if (event.getCursor()
													.getEnchantments()
													.isEmpty())
											{
												item = event.getCursor()
														.clone();
											}
											else
											{
												// Handle enchantments
												item = new ItemStack(
														event.getCursor()
																.getTypeId(),
														event.getCursor()
																.getAmount(),
														event.getCursor()
																.getDurability(),
														event.getCursor()
																.getData()
																.getData());
												for (Map.Entry<Enchantment, Integer> enchantment : event
														.getCursor()
														.getEnchantments()
														.entrySet())
												{
													item.addUnsafeEnchantment(
															enchantment
																	.getKey(),
															enchantment
																	.getValue()
																	.intValue());
												}
											}
											//Only giving one
											item.setAmount(1);
											if(!karma.giveItem(event.getPlayer(), item, group))
											{
												event.setResult(Event.Result.DENY);
												event.setCancelled(true);
											}
										}
									}
								}
							}
							catch (NullPointerException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	private void repopulateTask(Player player, Inventory inv, ItemStack item)
	{
		final Repopulate task = new Repopulate(inv,item);
		int id = plugin
				.getServer()
				.getScheduler()
				.scheduleSyncDelayedTask(
						plugin,
						task, 1);
		if (id == -1)
		{
			player.sendMessage(
							ChatColor.YELLOW
									+ KarmicShare.prefix
									+ "Could not repopulate slot.");
		}
	}

	static class Repopulate implements Runnable {
		int slot;
		ItemStack item;
		Inventory inventory;
		boolean clear;

		public Repopulate(Inventory inv, ItemStack i) {
			slot = -999;
			inventory = inv;
			item = i;
		}

		public Repopulate(Inventory inv, ItemStack i, int s, boolean c) {
			inventory = inv;
			item = i;
			slot = s;
			clear = c;
		}

		@Override
		public void run() {
			if (slot >= 0)
			{
				if(clear)
				{
					inventory.clear(slot);
				}
				else
				{
					inventory.setItem(slot, item);
				}
			}
			else
			{
				inventory.addItem(item);
			}
		}

	}
}
