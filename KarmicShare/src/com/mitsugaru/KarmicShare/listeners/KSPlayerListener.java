package com.mitsugaru.KarmicShare.listeners;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.SQLibrary.Database.Query;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.inventory.KSInventoryHolder;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.permissions.PermCheck;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;

public class KSPlayerListener implements Listener
{
	private KarmicShare plugin;
	private static final BlockFace[] nav = { BlockFace.NORTH, BlockFace.SOUTH,
			BlockFace.EAST, BlockFace.WEST };

	public KSPlayerListener(KarmicShare karmicShare)
	{
		plugin = karmicShare;
	}

	public void onPlayerQuit(final PlayerQuitEvent event)
	{
		// player quit event, since that doesn't throw an inventory close
		// event just to double check and just viewers for inventories
		try
		{
			if (event.getPlayer().getInventory().getHolder() != null)
			{
				if (event.getPlayer().getInventory().getHolder() instanceof KSInventoryHolder)
				{
					((KSInventoryHolder) event.getPlayer().getInventory()
							.getHolder()).getInfo().removeViewer();
				}
			}
		}
		catch (NullPointerException n)
		{
			// IGNORE
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getPlayer() == null || event.getClickedBlock() == null)
		{
			// Null check
			return;
		}
		// Grab player
		final Player player = event.getPlayer();
		// Grab block
		final Block block = event.getClickedBlock();
		boolean isChest = false, showInventory = false;
		int page = 1;
		// Determine if its a chest
		if (block.getType().equals(Material.CHEST))
		{
			isChest = true;
		}
		// Determine if it is ours.
		final Sign sign = grabOurSign(block);
		if (sign == null)
		{
			// Not ours, so don't care
			return;
		}
		// Check if chests are enabled
		if (!plugin.getPluginConfig().chests || !plugin.useChest())
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Chests disabled. Cannot use physical chests.");
			event.setCancelled(true);
			return;
		}
		// Check permission
		if (!PermCheck.checkPermission(event.getPlayer(), PermissionNode.CHEST))
		{
			event.getPlayer().sendMessage(
					ChatColor.RED + KarmicShare.TAG + " Lack permission: "
							+ PermissionNode.CHEST.getNode());
			event.setCancelled(true);
			return;
		}
		// Grab selected group for player
		String group = Karma.selectedGroup.get(player.getName());
		if (group == null)
		{
			Karma.selectedGroup.put(player.getName(), "global");
			group = "global";
		}
		// Handle chest page jump / grab current page number
		try
		{
			if (Karma.chestPage.containsKey(player.getName()))
			{
				page = grabNextPage(Karma.chestPage.get(player.getName())
						.intValue() - 1, Karma.chestSize, group,
						Direction.CURRENT);
				Karma.chestPage.remove(player.getName());
				sign.setLine(3, "" + page);
				sign.update();
			}
			else
			{
				// Assures that the page number on sign does not conflict with
				// players selected group's page limit
				page = grabNextPage(Integer.parseInt("" + sign.getLine(3)),
						Karma.chestSize, group, Direction.CURRENT);
			}
		}
		catch (NumberFormatException e)
		{
			event.getPlayer()
					.sendMessage(
							ChatColor.RED
									+ KarmicShare.TAG
									+ " Sign has wrong formatting. Noninteger page number. Remake sign.");
			return;
		}
		// Handle click logic
		if (player.isSneaking())
		{
			/**
			 * Group cycling / show inventory
			 */
			if (event.getAction() == Action.LEFT_CLICK_BLOCK)
			{
				// Sign or chest
				// cycle group forward
				cycleGroup(player, group, Direction.FORWARD);
			}
			else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !isChest)
			{
				// cycle group backward
				cycleGroup(player, group, Direction.BACKWARD);
			}
			else
			{
				// Right click and chest
				showInventory = true;
			}
		}
		else
		{
			/**
			 * Page cycling / show inventory
			 */
			try
			{
				if (event.getAction() == Action.LEFT_CLICK_BLOCK)
				{
					// cycle page forward

					page = grabNextPage(Integer.parseInt("" + sign.getLine(3)),
							Karma.chestSize, group, Direction.FORWARD);
					sign.setLine(3, "" + page);
					sign.update();

				}
				else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
						&& !isChest)
				{
					// cycle page backward
					page = grabNextPage(Integer.parseInt("" + sign.getLine(3)),
							Karma.chestSize, group, Direction.BACKWARD);
					sign.setLine(3, "" + page);
					sign.update();
				}
				else
				{
					// Right click and chest
					showInventory = true;
				}
			}
			catch (NumberFormatException e)
			{
				event.getPlayer()
						.sendMessage(
								ChatColor.RED
										+ KarmicShare.TAG
										+ " Sign has wrong formatting. Noninteger page number. Remake sign.");
				return;
			}
		}
		if (showInventory)
		{
			// Cancel event to stop chest inventory from interferring
			event.setCancelled(true);
			Karma.showInventory(player, group, page);
		}
	}

	private Sign grabOurSign(final Block block)
	{
		if (block.getType().equals(Material.CHEST))
		{
			if (block.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
			{
				final Sign sign = (Sign) block.getRelative(BlockFace.UP)
						.getState();
				if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
						KarmicShare.TAG))
				{
					return sign;
				}
			}
			else
			{
				// Check all 4 directions for adjacent chest
				for (BlockFace face : nav)
				{
					if (block.getRelative(face).getType()
							.equals(Material.CHEST))
					{
						final Block adjBlock = block.getRelative(face);
						if (adjBlock.getRelative(BlockFace.UP).getType()
								.equals(Material.WALL_SIGN))
						{
							final Sign sign = (Sign) adjBlock.getRelative(
									BlockFace.UP).getState();
							if (ChatColor.stripColor(sign.getLine(1))
									.equalsIgnoreCase(KarmicShare.TAG))
							{
								return sign;
							}
						}
					}
				}
			}
		}
		else if (block.getType().equals(Material.WALL_SIGN))
		{
			final Sign sign = (Sign) block.getState();
			if (ChatColor.stripColor(sign.getLine(1)).equalsIgnoreCase(
					KarmicShare.TAG))
			{
				return sign;
			}
		}
		return null;
	}

	private void cycleGroup(Player player, String current, Direction direction)
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

	private int grabNextPage(int current, int limit, String group,
			Direction direction)
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
		int pageTotal = slots / limit;
		final double rem = (double) slots % (double) limit;
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

	private enum Direction
	{
		FORWARD, BACKWARD, CURRENT;
	}
}
