package com.mitsugaru.KarmicShare;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.splatbang.betterchest.BetterChest;

public class KSPlayerListener extends PlayerListener {
	private KarmicShare plugin;
	private static final BlockFace[] nav = {BlockFace.NORTH,BlockFace.SOUTH,BlockFace.EAST,BlockFace.WEST};

	public KSPlayerListener(KarmicShare karmicShare) {
		plugin = karmicShare;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			final Block block = event.getClickedBlock();
			//TODO update?
			if(block.getType().equals(Material.CHEST))
			{
				if(block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
				{
					Sign sign = (Sign) block.getRelative(BlockFace.UP).getState();
					if(ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[KarmicShare]"))
					{
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
							BetterChest chest = new BetterChest((Chest) block.getState());
							if(chest.isDoubleChest())
							{
								BetterChest adj = new BetterChest((Chest) chest.attached());
								chest = adj;
							}
							chest.getInventory().clear();
							chest.update();
							if(plugin.getPluginConfig().chests)
							{
								populateChest(chest.getInventory());
								chest.update();
							}
						}
						else
						{
							event.getPlayer()
									.sendMessage(
											ChatColor.RED
													+ KarmicShare.prefix
													+ " Lack permission: KarmicShare.chest");
							event.setCancelled(true);
						}
					}
				}
				else
				{
					//Check all 4 directions for adjacent chest
					for(BlockFace face : nav)
					{
						if(block.getRelative(face).getType().equals(Material.CHEST))
						{
							final Block adjBlock = block.getRelative(face);
							if(adjBlock.getRelative(BlockFace.UP).getType().equals(Material.WALL_SIGN))
							{
								Sign sign = (Sign) adjBlock.getRelative(BlockFace.UP).getState();
								if(ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[KarmicShare]"))
								{
									if (plugin.getPermissionHandler().checkPermission(
											event.getPlayer(), "KarmicShare.chest"))
									{
									//populate chests
									BetterChest chest = new BetterChest((Chest) block.getState());
									chest.getInventory().clear();
									chest.update();
									if(plugin.getPluginConfig().chests)
									{
										populateChest(chest.getInventory());
										chest.update();
									}
									}
									else
									{
										event.getPlayer()
												.sendMessage(
														ChatColor.RED
																+ KarmicShare.prefix
																+ " Lack permission: KarmicShare.chest");
										event.setCancelled(true);
									}
								}
							}
						}
					}
				}

			}
		}
		else if (event.getAction() == Action.LEFT_CLICK_BLOCK)
		{
			final Block block = event.getClickedBlock();
			if(block.getType().equals(Material.CHEST))
			{
				plugin.getLogger().info("Left Click Chest");

				if(block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
				{
					Sign sign = (Sign) block.getRelative(BlockFace.UP).getState();
					if(ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[KarmicShare]"))
					{
						if (plugin.getPermissionHandler().checkPermission(
								event.getPlayer(), "KarmicShare.chest"))
						{
						//TODO cycle
						//TODO update sign to proper page
						}
						else
						{
							event.getPlayer()
									.sendMessage(
											ChatColor.RED
													+ KarmicShare.prefix
													+ " Lack permission: KarmicShare.chest");
							event.setCancelled(true);
						}
					}
				}
				else
				{
					//Check all 4 directions for adjacent chest
					for(BlockFace face : nav)
					{
						if(block.getRelative(face).getType().equals(Material.CHEST))
						{
							final Block adjBlock = block.getRelative(face);
							if(adjBlock.getRelative(BlockFace.UP).getType().equals(Material.WALL_SIGN))
							{
								Sign sign = (Sign) adjBlock.getRelative(BlockFace.UP).getState();
								if(ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase("[KarmicShare]"))
								{
									if (plugin.getPermissionHandler().checkPermission(
											event.getPlayer(), "KarmicShare.chest"))
									{
									//TODO cycle
									//TODO update sign to proper page
									}
									else
									{
										event.getPlayer()
												.sendMessage(
														ChatColor.RED
																+ KarmicShare.prefix
																+ " Lack permission: KarmicShare.chest");
										event.setCancelled(true);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void populateChest(Inventory inventory) {
		try
		{
			ResultSet itemList = plugin.getLiteDB().select("SELECT * FROM items;");
			if(itemList.next())
			{
				boolean done = false;
				do
				{
					//Generate item
					int id = itemList.getInt("itemid");
					byte data = itemList.getByte("data");
					short dur = itemList.getShort("durability");
					ItemStack item = new ItemStack(id, 1, dur, data);
					Item meta = new Item(id, data, dur);
					//If tool
					if(meta.isTool())
					{
						//Check for enchantments
						String enchantments = itemList.getString("enchantments");
						if(!itemList.wasNull())
						{
							String[] cut = enchantments.split("i");
							for (int i = 0; i < cut.length; i++)
							{
								String[] cutter = cut[i]
										.split("v");
								EnchantmentWrapper e = new EnchantmentWrapper(
										Integer.parseInt(cutter[0]));
								item.addUnsafeEnchantment(
										e.getEnchantment(),
										Integer.parseInt(cutter[1]));
							}
						}
					}
					if(meta.isPotion())
					{
						//Remove data for full potion compatibility
						item = new ItemStack(id, 1, dur);
					}
					HashMap<Integer, ItemStack> residual = inventory.addItem(item);
					if(!residual.isEmpty())
					{
						done = true;
					}
				} while(itemList.next() && !done);
			}
			else
			{
				//No items to add.
				inventory.clear();
			}
			itemList.close();
		}
		catch (SQLException e)
		{
			// INFO Auto-generated catch block
			plugin.getLogger().warning(ChatColor.RED + KarmicShare.prefix + "SQL error.");
			e.printStackTrace();
		}
	}
}
