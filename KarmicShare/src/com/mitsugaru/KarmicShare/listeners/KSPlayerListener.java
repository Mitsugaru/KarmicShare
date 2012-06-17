package com.mitsugaru.KarmicShare.listeners;

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

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.RootConfig;
import com.mitsugaru.KarmicShare.config.ConfigNode;
import com.mitsugaru.KarmicShare.inventory.KSInventoryHolder;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.logic.Karma.Direction;
import com.mitsugaru.KarmicShare.permissions.PermissionHandler;
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
		if (!RootConfig.getBoolean(ConfigNode.CHESTS) || !plugin.useChest())
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Chests disabled. Cannot use physical chests.");
			event.setCancelled(true);
			return;
		}
		// Check permission
		if (!PermissionHandler.has(event.getPlayer(), PermissionNode.CHEST))
		{
			event.getPlayer().sendMessage(
					ChatColor.RED + KarmicShare.TAG + " Lack permission: "
							+ PermissionNode.CHEST.getNode());
			event.setCancelled(true);
			return;
		}
		//Check world
		final String world = block.getWorld().getName();
		if(RootConfig.getStringList(ConfigNode.DISABLED_WORLDS).contains(world))
		{
			event.getPlayer().sendMessage(
					ChatColor.RED + KarmicShare.TAG + " KarmicShare access disabled for this world.");
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
				page = Karma.grabPage(Karma.chestPage.get(player.getName())
						.intValue() - 1, group, Direction.CURRENT);
				Karma.chestPage.remove(player.getName());
				sign.setLine(3, "" + page);
				sign.update();
			}
			else
			{
				// Assures that the page number on sign does not conflict with
				// players selected group's page limit
				page = Karma.grabPage(Integer.parseInt("" + sign.getLine(3)),
						group, Direction.CURRENT);
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
				Karma.cycleGroup(player, group, Direction.FORWARD);
			}
			else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !isChest)
			{
				// cycle group backward
				Karma.cycleGroup(player, group, Direction.BACKWARD);
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

					page = Karma.grabPage(
							Integer.parseInt("" + sign.getLine(3)), group,
							Direction.FORWARD);
					sign.setLine(3, "" + page);
					sign.update();

				}
				else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
						&& !isChest)
				{
					// cycle page backward
					page = Karma.grabPage(
							Integer.parseInt("" + sign.getLine(3)), group,
							Direction.BACKWARD);
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
			Karma.showInventory(player, group, page);
		}
		event.setCancelled(true);
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
}
