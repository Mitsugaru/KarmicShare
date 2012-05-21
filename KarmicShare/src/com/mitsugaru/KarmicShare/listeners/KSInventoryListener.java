package com.mitsugaru.KarmicShare.listeners;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.Item;
import com.mitsugaru.KarmicShare.Karma;
import com.mitsugaru.KarmicShare.KarmicShare;
import com.splatbang.betterchest.BetterChest;

public class KSInventoryListener implements Listener
{
	private KarmicShare plugin;
	private Karma karma;

	public KSInventoryListener(KarmicShare karmicShare)
	{
		plugin = karmicShare;
		karma = karmicShare.getKarma();
	}

	// TODO check for our custom inventory holder class.
	// Handle logic if it is ours.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent event)
	{
		// Valid slot numbers are not negative
		if (event.getSlot() >= 0)
		{
			// Verify that it is a chest
			if (event.getInventory().getType().equals(InventoryType.CHEST))
			{
				Block block = null;
				boolean doubleChest = false, kschest = false, fromChest = false, isChest = false;
				String group = "global";
				BetterChest chest = null;
				try
				{
					if (event.getInventory() instanceof DoubleChestInventory)
					{
						// Double chest
						try
						{
							block = ((Chest) ((DoubleChestInventory) event
									.getInventory()).getLeftSide().getHolder())
									.getBlock();
							doubleChest = true;
							isChest = true;
						}
						catch (NullPointerException e)
						{
							/*
							 * if(event.getWhoClicked() instanceof Player) {
							 * ((Player
							 * )event.getWhoClicked()).sendMessage(ChatColor.RED
							 * + KarmicShare.prefix +
							 * " Something went wrong! D:"); }
							 * //plugin.getLogger().warning(
							 * "Error with getting the block for double chest."
							 * ); //plugin.getLogger().warning(e.getMessage());
							 * event.setCancelled(true); return;
							 */
							// Ignore as non-chest inventory
						}
					}
					else
					{
						// single chest
						try
						{
							block = ((Chest) event.getInventory().getHolder())
									.getBlock();
							isChest = true;
						}
						catch (NullPointerException e)
						{
							/*
							 * if(event.getWhoClicked() instanceof Player) {
							 * ((Player
							 * )event.getWhoClicked()).sendMessage(ChatColor.RED
							 * + KarmicShare.prefix +
							 * " Something went wrong! D:"); }
							 * //plugin.getLogger()
							 * .warning("Error with getting the block for chest."
							 * ); //plugin.getLogger().warning(e.getMessage());
							 * event.setCancelled(true); return;
							 */
							// Ignore as non-chest inventory
						}
					}
				}
				catch (ClassCastException c)
				{
					// Custom inventory, ignore.
				}
				if (plugin.getPluginConfig().chests && isChest)
				{

					chest = new BetterChest((Chest) block.getState());
					// Differentiate between chest inventory and player
					// inventory click
					if (doubleChest && (event.getRawSlot() < 54))
					{
						fromChest = true;
					}
					else if (!doubleChest && (event.getRawSlot() < 27))
					{
						fromChest = true;
					}
					// Determine if it is one of our chests
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
								group = ChatColor.stripColor(sign.getLine(0))
										.toLowerCase();
							}
						}
						else if (chest.isDoubleChest())
						{
							if (chest.attachedBlock().getRelative(BlockFace.UP)
									.getType().equals(Material.WALL_SIGN))
							{
								final Sign sign = (Sign) chest.attachedBlock()
										.getRelative(BlockFace.UP).getState();
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
				}
				if (kschest && plugin.useChest())
				{
					// plugin.getLogger().info("our chest");
					try
					{
						if (event.isLeftClick())
						{
							// plugin.getLogger().info("left click");
							if (event.isShiftClick())
							{
								// plugin.getLogger().info("shift click");
								/*
								 * Shift Left click We don't care about the
								 * cursor as it doesn't get changed on a shift
								 * click
								 */
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									// plugin.getLogger().info("not air");
									if (fromChest)
									{
										// plugin.getLogger().info(
										// "from chest");
										if (event.getWhoClicked()
												.getInventory().firstEmpty() >= 0)
										{
											final int amount = karma
													.takeItem(
															plugin.getServer()
																	.getPlayer(
																			event.getWhoClicked()
																					.getName()),
															event.getCurrentItem(),
															group);
											final int original = event
													.getCurrentItem()
													.getAmount();
											if (amount == event
													.getCurrentItem()
													.getAmount())
											{
												// plugin.getLogger().info(
												// "same amount");
												// event.setResult(Event.Result.ALLOW);
												ItemStack item;
												if (event.getCurrentItem()
														.getEnchantments()
														.isEmpty())
												{
													item = event
															.getCurrentItem()
															.clone();
												}
												else
												{
													// Handle enchantments
													item = new ItemStack(
															event.getCurrentItem()
																	.getTypeId(),
															event.getCurrentItem()
																	.getAmount(),
															event.getCurrentItem()
																	.getDurability(),
															event.getCurrentItem()
																	.getData()
																	.getData());
													for (Map.Entry<Enchantment, Integer> enchantment : event
															.getCurrentItem()
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
														event.getWhoClicked()
																.getInventory(),
														item);
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task, 1);
												if (id == -1)
												{
													plugin.getServer()
															.getPlayer(
																	event.getWhoClicked()
																			.getName())
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ " Could not repopulate slot.");
												}
												event.getInventory().clear(
														event.getRawSlot());
											}
											else if (amount < event
													.getCurrentItem()
													.getAmount()
													&& amount > 0)
											{
												// plugin.getLogger()
												// .info("amount less than current item amount");
												final ItemStack bak = event
														.getCurrentItem()
														.clone();
												bak.setAmount(original - amount);
												Repopulate task = new Repopulate(
														event.getInventory(),
														bak, event.getSlot(),
														false);
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task, 1);
												if (id == -1)
												{
													plugin.getServer()
															.getPlayer(
																	event.getWhoClicked()
																			.getName())
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ " Could not repopulate slot.");
												}
												final ItemStack give = event
														.getCurrentItem()
														.clone();
												give.setAmount(amount);
												task = new Repopulate(
														plugin.getServer()
																.getPlayer(
																		event.getWhoClicked()
																				.getName())
																.getInventory(),
														give);
												id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task, 1);
												if (id == -1)
												{
													plugin.getServer()
															.getPlayer(
																	event.getWhoClicked()
																			.getName())
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ " Could not give item.");
												}
												// event.setResult(Event.Result.DENY);
											}
											else
											{
												event.setCancelled(true);
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
										// plugin.getLogger().info(
										// "from player");
										if (event.getInventory().firstEmpty() >= 0)
										{
											if (karma
													.giveItem(
															plugin.getServer()
																	.getPlayer(
																			event.getWhoClicked()
																					.getName()),
															event.getCurrentItem(),
															group))
											{
												// plugin.getLogger().info(
												// "gave item");
												// event.setResult(Event.Result.ALLOW);
												ItemStack item;
												if (event.getCurrentItem()
														.getEnchantments()
														.isEmpty())
												{
													item = event
															.getCurrentItem()
															.clone();
												}
												else
												{
													// Handle enchantments
													item = new ItemStack(
															event.getCurrentItem()
																	.getTypeId(),
															event.getCurrentItem()
																	.getAmount(),
															event.getCurrentItem()
																	.getDurability(),
															event.getCurrentItem()
																	.getData()
																	.getData());
													for (Map.Entry<Enchantment, Integer> enchantment : event
															.getCurrentItem()
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
																plugin, task, 1);
												if (id == -1)
												{
													plugin.getServer()
															.getPlayer(
																	event.getWhoClicked()
																			.getName())
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ " Could not repopulate slot.");
												}
												event.getWhoClicked()
														.getInventory()
														.clear(event.getSlot());
											}
											else
											{
												event.setCancelled(true);
											}
										}
										else
										{
											// event.setResult(Event.Result.DENY);
											event.setCancelled(true);
										}
									}
								}
							}
							else
							{
								// plugin.getLogger().info("not shift");
								/*
								 * Regular left click
								 */
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR)
										&& !event.getCursor().getType()
												.equals(Material.AIR))
								{
									// plugin.getLogger().info("not both air");
									if (event.getCurrentItem() != null
											&& event.getCursor() != null)
									{
										final Item a = new Item(
												event.getCurrentItem());
										final Item b = new Item(
												event.getCursor());
										if (a.areSame(b))
										{
											// plugin.getLogger().info(
											// "same type");
											int cursorAmount = event
													.getCursor().getAmount();
											int itemAmount = event
													.getCurrentItem()
													.getAmount();
											int totalAmount = cursorAmount
													+ itemAmount;
											if (itemAmount < event
													.getCurrentItem()
													.getMaxStackSize())
											{
												// plugin.getLogger()
												// .info("item stack not at max stack");
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
													item = new ItemStack(event
															.getCursor()
															.getTypeId(), event
															.getCursor()
															.getAmount(), event
															.getCursor()
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
												if (totalAmount > event
														.getCurrentItem()
														.getMaxStackSize())
												{
													item.setAmount(event
															.getCurrentItem()
															.getMaxStackSize()
															- itemAmount);
												}
												/*
												 * Of the same time, so add to
												 * current stack
												 */
												if (karma
														.giveItem(
																plugin.getServer()
																		.getPlayer(
																				event.getWhoClicked()
																						.getName()),
																item, group))
												{
													// event.setResult(Event.Result.ALLOW);

													/*
													 * repopulateTask(
													 * plugin.getServer()
													 * .getPlayer(
													 * event.getWhoClicked()
													 * .getName()),
													 * chest.getInventory(),
													 * item);
													 * event.getWhoClicked()
													 * .getInventory()
													 * .clear(event .getSlot());
													 */
												}
												else
												{
													event.setCancelled(true);
												}
											}
											else
											{
												// event.setResult(Event.Result.DENY);
												event.setCancelled(true);
											}
										}
										else
										{
											// plugin.getLogger().info(
											// "switching items");
											/*
											 * Switching items from chest to
											 * cursor When switching, put item
											 * first, then attempt to take item
											 */
											if (karma
													.giveItem(
															plugin.getServer()
																	.getPlayer(
																			event.getWhoClicked()
																					.getName()),
															event.getCursor(),
															group))
											{
												final int amount = karma
														.takeItem(
																plugin.getServer()
																		.getPlayer(
																				event.getWhoClicked()
																						.getName()),
																event.getCurrentItem(),
																group);
												final int original = event
														.getCurrentItem()
														.getAmount();
												if (amount == event
														.getCurrentItem()
														.getAmount())
												{
													// event.setResult(Event.Result.ALLOW);
												}
												else if (amount < event
														.getCurrentItem()
														.getAmount()
														&& amount > 0)
												{
													// event.setResult(Event.Result.ALLOW);
													event.getCurrentItem()
															.setAmount(amount);
													final ItemStack bak = event
															.getCurrentItem()
															.clone();
													bak.setAmount(original
															- amount);
													/*
													 * final Repopulate task =
													 * new Repopulate(
													 * event.getInventory(),
													 * bak, event .getSlot(),
													 * false); int id = plugin
													 * .getServer()
													 * .getScheduler()
													 * .scheduleSyncDelayedTask
													 * ( plugin, task, 1); if
													 * (id == -1) {
													 * plugin.getServer()
													 * .getPlayer(
													 * event.getWhoClicked()
													 * .getName()) .sendMessage(
													 * ChatColor.YELLOW +
													 * KarmicShare.prefix +
													 * " Could not repopulate slot."
													 * ); }
													 */
												}
												else
												{
													// event.setResult(Event.Result.DENY);
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
								}
								else if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									// plugin.getLogger().info("take item");
									/*
									 * Attempting to take item
									 */
									final int amount = karma.takeItem(
											plugin.getServer().getPlayer(
													event.getWhoClicked()
															.getName()), event
													.getCurrentItem(), group);
									final int original = event.getCurrentItem()
											.getAmount();
									if (amount == event.getCurrentItem()
											.getAmount())
									{
										// event.setResult(Event.Result.ALLOW);
									}
									else if (amount < event.getCurrentItem()
											.getAmount() && amount > 0)
									{
										// event.setResult(Event.Result.ALLOW);
										event.getCurrentItem()
												.setAmount(amount);
										final ItemStack bak = event
												.getCurrentItem().clone();
										bak.setAmount(original - amount);
										/*
										 * final Repopulate task = new
										 * Repopulate( event.getInventory(),
										 * bak, event.getSlot(), false); int id
										 * = plugin .getServer() .getScheduler()
										 * .scheduleSyncDelayedTask( plugin,
										 * task, 1); if (id == -1) {
										 * plugin.getServer() .getPlayer(
										 * event.getWhoClicked() .getName())
										 * .sendMessage( ChatColor.YELLOW +
										 * KarmicShare.prefix +
										 * " Could not repopulate slot."); }
										 */
									}
									else
									{
										event.setResult(Event.Result.DENY);
										event.setCancelled(true);
									}
								}
								else if (!event.getCursor().getType()
										.equals(Material.AIR))
								{
									// plugin.getLogger().info(
									// "putting item into empty slot");
									/*
									 * Putting item into empty slot in chest
									 */
									if (karma.giveItem(
											plugin.getServer().getPlayer(
													event.getWhoClicked()
															.getName()), event
													.getCursor(), group))
									{
										// event.setResult(Event.Result.ALLOW);
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
							// plugin.getLogger().info("right click");
							if (event.isShiftClick())
							{
								// plugin.getLogger().info("shift click");
								/*
								 * Shift right click
								 */
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									// plugin.getLogger().info("not air");
									if (fromChest)
									{
										// plugin.getLogger().info(
										// "from chest, take item");
										if (event.getWhoClicked()
												.getInventory().firstEmpty() >= 0)
										{
											final int amount = karma
													.takeItem(
															plugin.getServer()
																	.getPlayer(
																			event.getWhoClicked()
																					.getName()),
															event.getCurrentItem(),
															group);
											final int original = event
													.getCurrentItem()
													.getAmount();
											if (amount == event
													.getCurrentItem()
													.getAmount())
											{
												// plugin.getLogger().info(
												// "take all");
												// event.setResult(Event.Result.DENY);
												ItemStack item;
												if (event.getCurrentItem()
														.getEnchantments()
														.isEmpty())
												{
													item = event
															.getCurrentItem()
															.clone();
												}
												else
												{
													// Handle enchantments
													item = new ItemStack(
															event.getCurrentItem()
																	.getTypeId(),
															event.getCurrentItem()
																	.getAmount(),
															event.getCurrentItem()
																	.getDurability(),
															event.getCurrentItem()
																	.getData()
																	.getData());
													for (Map.Entry<Enchantment, Integer> enchantment : event
															.getCurrentItem()
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
												/*
												 * final Repopulate task = new
												 * Repopulate(
												 * event.getWhoClicked()
												 * .getInventory(), item); int
												 * id = plugin .getServer()
												 * .getScheduler()
												 * .scheduleSyncDelayedTask(
												 * plugin, task, 1); if (id ==
												 * -1) { plugin.getServer()
												 * .getPlayer(
												 * event.getWhoClicked()
												 * .getName()) .sendMessage(
												 * ChatColor.YELLOW +
												 * KarmicShare.prefix +
												 * " Could not repopulate slot."
												 * ); } final Repopulate clear =
												 * new Repopulate(
												 * event.getInventory(), item,
												 * event.getSlot(), true); id =
												 * plugin .getServer()
												 * .getScheduler()
												 * .scheduleSyncDelayedTask(
												 * plugin, clear, 1); if (id ==
												 * -1) { plugin.getServer()
												 * .getPlayer(
												 * event.getWhoClicked()
												 * .getName()) .sendMessage(
												 * ChatColor.YELLOW +
												 * KarmicShare.prefix +
												 * " Could not repopulate slot."
												 * ); }
												 */
											}
											else if (amount < event
													.getCurrentItem()
													.getAmount()
													&& amount > 0)
											{
												// plugin.getLogger().info(
												// "take some");
												final ItemStack bak = event
														.getCurrentItem()
														.clone();
												bak.setAmount(original - amount);
												Repopulate task = new Repopulate(
														event.getInventory(),
														bak, event.getSlot(),
														false);
												int id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task, 1);
												if (id == -1)
												{
													plugin.getServer()
															.getPlayer(
																	event.getWhoClicked()
																			.getName())
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ " Could not repopulate slot.");
												}
												final ItemStack give = event
														.getCurrentItem()
														.clone();
												give.setAmount(amount);
												task = new Repopulate(event
														.getWhoClicked()
														.getInventory(), give);
												id = plugin
														.getServer()
														.getScheduler()
														.scheduleSyncDelayedTask(
																plugin, task, 1);
												if (id == -1)
												{
													plugin.getServer()
															.getPlayer(
																	event.getWhoClicked()
																			.getName())
															.sendMessage(
																	ChatColor.YELLOW
																			+ KarmicShare.prefix
																			+ " Could not give item.");
												}
												event.setResult(Event.Result.DENY);
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
										// plugin.getLogger().info(
										// "player inventory");
										if (karma.giveItem(
												plugin.getServer().getPlayer(
														event.getWhoClicked()
																.getName()),
												event.getCurrentItem(), group))
										{
											// plugin.getLogger().info(
											// "gave item");
											// event.setResult(Event.Result.ALLOW);
											ItemStack item;
											if (event.getCurrentItem()
													.getEnchantments()
													.isEmpty())
											{
												item = event.getCurrentItem()
														.clone();
											}
											else
											{
												// Handle enchantments
												item = new ItemStack(event
														.getCurrentItem()
														.getTypeId(), event
														.getCurrentItem()
														.getAmount(), event
														.getCurrentItem()
														.getDurability(), event
														.getCurrentItem()
														.getData().getData());
												for (Map.Entry<Enchantment, Integer> enchantment : event
														.getCurrentItem()
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
													chest.getInventory(), item);
											int id = plugin
													.getServer()
													.getScheduler()
													.scheduleSyncDelayedTask(
															plugin, task, 1);
											if (id == -1)
											{
												plugin.getServer()
														.getPlayer(
																event.getWhoClicked()
																		.getName())
														.sendMessage(
																ChatColor.YELLOW
																		+ KarmicShare.prefix
																		+ " Could not repopulate slot.");
											}
											event.getWhoClicked()
													.getInventory()
													.clear(event.getSlot());
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
								// plugin.getLogger().info("not shift");
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR)
										&& !event.getCursor().getType()
												.equals(Material.AIR))
								{
									// plugin.getLogger().info("not both air");
									if (event.getCurrentItem() != null
											&& event.getCursor() != null)
									{
										final Item a = new Item(
												event.getCurrentItem());
										final Item b = new Item(
												event.getCursor());
										if (a.areSame(b))
										{
											// plugin.getLogger().info(
											// "same item, give one");
											/*
											 * Same item, so give only one from
											 * cursor to item
											 */
											// Construct singular of cursor
											// item
											ItemStack item;
											if (event.getCursor()
													.getEnchantments()
													.isEmpty())
											{
												item = event.getCursor()
														.clone();
												item.setAmount(1);
											}
											else
											{
												// Handle enchantments
												item = new ItemStack(event
														.getCursor()
														.getTypeId(), 1, event
														.getCursor()
														.getDurability(), event
														.getCursor().getData()
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
											int itemAmount = event
													.getCurrentItem()
													.getAmount() + 1;
											if (itemAmount < event
													.getCurrentItem()
													.getMaxStackSize())
											{
												// plugin.getLogger()
												// .info("can add 1 to stack");
												if (karma
														.giveItem(
																plugin.getServer()
																		.getPlayer(
																				event.getWhoClicked()
																						.getName()),
																item, group))
												{
													// event.setResult(Event.Result.ALLOW);
													item.setAmount(event
															.getCurrentItem()
															.getAmount());
													/*
													 * repopulateTask(
													 * plugin.getServer()
													 * .getPlayer(
													 * event.getWhoClicked()
													 * .getName()),
													 * chest.getInventory(),
													 * item);
													 */
													event.getWhoClicked()
															.getInventory()
															.clear(event
																	.getSlot());
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
										else
										{
											// plugin.getLogger().info(
											// "switching item");
											/*
											 * Switching Put item first, then
											 * attempt to take item
											 */
											if (karma
													.giveItem(
															plugin.getServer()
																	.getPlayer(
																			event.getWhoClicked()
																					.getName()),
															event.getCursor(),
															group))
											{
												final int amount = karma
														.takeItem(
																plugin.getServer()
																		.getPlayer(
																				event.getWhoClicked()
																						.getName()),
																event.getCurrentItem(),
																group);
												final int original = event
														.getCurrentItem()
														.getAmount();
												if (amount == event
														.getCurrentItem()
														.getAmount())
												{
													// event.setResult(Event.Result.ALLOW);
												}
												else if (amount < event
														.getCurrentItem()
														.getAmount()
														&& amount > 0)
												{
													// event.setResult(Event.Result.ALLOW);
													event.getCurrentItem()
															.setAmount(amount);
													final ItemStack bak = event
															.getCurrentItem()
															.clone();
													bak.setAmount(original
															- amount);
													/*
													 * final Repopulate task =
													 * new Repopulate(
													 * event.getInventory(),
													 * bak, event .getSlot(),
													 * false); int id = plugin
													 * .getServer()
													 * .getScheduler()
													 * .scheduleSyncDelayedTask
													 * ( plugin, task, 1); if
													 * (id == -1) {
													 * plugin.getServer()
													 * .getPlayer(
													 * event.getWhoClicked()
													 * .getName()) .sendMessage(
													 * ChatColor.YELLOW +
													 * KarmicShare.prefix +
													 * " Could not repopulate slot."
													 * ); }
													 */
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
								}
								// If cursor is null and item is not null,
								// they are taking half of the stack, with
								// the larger half on cursor
								else if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									// plugin.getLogger().info(
									// "take half the stack");
									/*
									 * If cursor is air and item is not air they
									 * are taking half of the stack, with the
									 * larger half given to cursor
									 */
									// Calculate "half"
									int half = event.getCurrentItem()
											.getAmount() / 2;
									final double rem = (double) event
											.getCurrentItem().getAmount() % 2.0;
									if (rem != 0)
									{
										half++;
									}
									// Clone
									ItemStack item;
									if (event.getCurrentItem()
											.getEnchantments().isEmpty())
									{
										item = event.getCurrentItem().clone();
									}
									else
									{
										// Handle enchantments
										item = new ItemStack(event
												.getCurrentItem().getTypeId(),
												event.getCurrentItem()
														.getAmount(), event
														.getCurrentItem()
														.getDurability(), event
														.getCurrentItem()
														.getData().getData());
										for (Map.Entry<Enchantment, Integer> enchantment : event
												.getCurrentItem()
												.getEnchantments().entrySet())
										{
											item.addUnsafeEnchantment(
													enchantment.getKey(),
													enchantment.getValue()
															.intValue());
										}
									}
									item.setAmount(half);
									// Send to database
									final int amount = karma.takeItem(
											plugin.getServer().getPlayer(
													event.getWhoClicked()
															.getName()), item,
											group);
									if (amount == half)
									{
										// event.setResult(Event.Result.ALLOW);
									}
									else if (amount < event.getCurrentItem()
											.getAmount() && amount > 0)
									{
										final ItemStack bak = event
												.getCurrentItem().clone();
										bak.setAmount(event.getCurrentItem()
												.getAmount() - amount);
										Repopulate task = new Repopulate(
												event.getInventory(), bak,
												event.getSlot(), false);
										int id = plugin
												.getServer()
												.getScheduler()
												.scheduleSyncDelayedTask(
														plugin, task, 1);
										if (id == -1)
										{
											plugin.getServer()
													.getPlayer(
															event.getWhoClicked()
																	.getName())
													.sendMessage(
															ChatColor.YELLOW
																	+ KarmicShare.prefix
																	+ " Could not repopulate slot.");
										}
										item.setAmount(amount);
										task = new Repopulate(plugin
												.getServer()
												.getPlayer(
														event.getWhoClicked()
																.getName())
												.getInventory(), item);
										id = plugin
												.getServer()
												.getScheduler()
												.scheduleSyncDelayedTask(
														plugin, task, 1);
										if (id == -1)
										{
											plugin.getServer()
													.getPlayer(
															event.getWhoClicked()
																	.getName())
													.sendMessage(
															ChatColor.YELLOW
																	+ KarmicShare.prefix
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
								else if (!event.getCursor().getType()
										.equals(Material.AIR))
								{
									// plugin.getLogger()
									// .info("only give one");
									// Clone
									ItemStack item;
									if (event.getCursor().getEnchantments()
											.isEmpty())
									{
										item = event.getCursor().clone();
									}
									else
									{
										// Handle enchantments
										item = new ItemStack(event.getCursor()
												.getTypeId(), event.getCursor()
												.getAmount(), event.getCursor()
												.getDurability(), event
												.getCursor().getData()
												.getData());
										for (Map.Entry<Enchantment, Integer> enchantment : event
												.getCursor().getEnchantments()
												.entrySet())
										{
											item.addUnsafeEnchantment(
													enchantment.getKey(),
													enchantment.getValue()
															.intValue());
										}
									}
									// Only giving one
									item.setAmount(1);
									if (!karma.giveItem(
											plugin.getServer().getPlayer(
													event.getWhoClicked()
															.getName()), item,
											group))
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

	@SuppressWarnings("unused")
	private void repopulateTask(Player player, Inventory inv, ItemStack item)
	{
		final Repopulate task = new Repopulate(inv, item);
		int id = plugin.getServer().getScheduler()
				.scheduleSyncDelayedTask(plugin, task, 1);
		if (id == -1)
		{
			player.sendMessage(ChatColor.YELLOW + KarmicShare.prefix
					+ "Could not repopulate slot.");
		}
	}

	static class Repopulate implements Runnable
	{
		int slot;
		ItemStack item;
		Inventory inventory;
		boolean clear;

		public Repopulate(Inventory inv, ItemStack i)
		{
			slot = -999;
			inventory = inv;
			item = i;
		}

		public Repopulate(Inventory inv, ItemStack i, int s, boolean c)
		{
			inventory = inv;
			item = i;
			slot = s;
			clear = c;
		}

		@Override
		public void run()
		{
			if (slot >= 0)
			{
				if (clear)
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
