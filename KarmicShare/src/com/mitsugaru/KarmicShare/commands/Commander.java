/**
 * Separate class to handle commands Followed example from DiddiZ's LB.
 * 
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare.commands;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.Config;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.permissions.PermCheck;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;

public class Commander implements CommandExecutor
{
	// Class variables
	private final KarmicShare plugin;
	private final static String bar = "======================";
	private final Config config;
	private long time = 0;

	/**
	 * Constructor
	 * 
	 * @param karmicShare
	 *            plugin
	 */
	public Commander(KarmicShare plugin)
	{
		// Instantiate variables
		this.plugin = plugin;
		config = plugin.getPluginConfig();
		// Initialize commands classes
		ItemCommands.init(plugin);
		ListCommands.init(plugin);
		GroupCommands.init(plugin);
		AdminCommands.init(plugin);
	}

	/**
	 * Command handler
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args)
	{
		if (config.debugTime)
		{
			time = System.nanoTime();
		}
		// See if any arguments were given
		if (args.length == 0)
		{
			// Check if they have "karma" permission
			if (PermCheck.checkPermission(sender, PermissionNode.KARMA))
			{
				if (!config.karmaDisabled)
				{
					// Show player karma
					this.showPlayerKarma(sender, args);

				}
				else
				{
					// karma system disabled
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Karma disabled");
				}
				if (sender instanceof Player)
				{
					String current = Karma.selectedGroup.get(sender.getName());
					if (current == null)
					{
						Karma.selectedGroup.put(sender.getName(), "global");
						current = "global";
					}
					final StringBuilder sb = new StringBuilder();
					for (String s : Karma.getPlayerGroups(sender,
							sender.getName()))
					{
						if (s.equalsIgnoreCase(current))
						{
							sb.append(ChatColor.BOLD + ""
									+ ChatColor.LIGHT_PURPLE + s
									+ ChatColor.RESET + ""
									+ ChatColor.DARK_AQUA + "-");
						}
						else
						{
							sb.append(ChatColor.GRAY + s + ChatColor.DARK_AQUA
									+ "-");
						}
					}
					// Remove trailing characters
					try
					{
						sb.deleteCharAt(sb.length() - 1);
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Groups: " + sb.toString());
					}
					catch (StringIndexOutOfBoundsException e)
					{
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " No groups");
					}

				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.KARMA.getNode());
			}
		}
		else
		{
			final String com = args[0].toLowerCase();
			if (com.equals("version") || com.equals("ver"))
			{
				// Version and author
				this.showVersion(sender, args);
			}
			else if (com.equals("?") || com.equals("help"))
			{
				this.displayHelp(sender);
			}
			else if (com.equals("info"))
			{
				// Info command
				ItemCommands.inspectItem(sender, args);
			}
			// Player is giving item to pool
			else if (com.equals("give"))
			{
				ItemCommands.giveItem(sender, args);
			}
			// Player requested an item
			else if (com.equals("take"))
			{
				ItemCommands.takeItem(sender, args);
			}
			// Previous page of item pool
			else if (com.equals("prev"))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_LIST))
				{
					// List, with previous page
					ListCommands.listPool(sender, -1);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Lack permission: "
							+ PermissionNode.COMMANDS_LIST.getNode());
				}
			}
			// Next page of item pool
			else if (com.equals("next"))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_LIST))
				{
					// List with next page
					ListCommands.listPool(sender, 1);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Lack permission: "
							+ PermissionNode.COMMANDS_LIST.getNode());
				}
			}
			// List items in pool
			else if (com.equals("list"))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_LIST))
				{
					ListCommands.listCommand(sender, args);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Lack permission: "
							+ PermissionNode.COMMANDS_LIST.getNode());
				}
			}
			// Ask for karma multipliers / page through muliplier list
			else if (com.equals("value"))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_VALUE))
				{
					ListCommands.valueCommand(sender, args);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Lack permission: "
							+ PermissionNode.COMMANDS_VALUE.getNode());
				}
			}
			else if (com.equals("page"))
			{
				if (plugin.useChest())
				{
					if (PermCheck.checkPermission(sender, PermissionNode.CHEST))
					{
						if (args.length > 1)
						{
							try
							{
								Integer page = Integer.parseInt(args[1]);
								Karma.chestPage.put(sender.getName(), page);
								sender.sendMessage(ChatColor.GREEN
										+ KarmicShare.TAG
										+ " Right click on sign to jump to page "
										+ ChatColor.GOLD + page.intValue());
							}
							catch (NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED
										+ KarmicShare.TAG + " Invalid number: "
										+ ChatColor.GOLD + args[1]);
							}
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Lack permission: "
								+ PermissionNode.CHEST.getNode());
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Chests are disabled.");
				}
			}
			// Admin command
			else if (com.equals("group"))
			{
				GroupCommands.parseCommand(sender, args);
			}
			// Admin command
			else if (com.equals("admin"))
			{
				AdminCommands.parseCommand(sender, args);
			}
			// Other player karma lookup
			else if (com.equals("player"))
			{
				this.otherPlayerKarma(sender, args);
			}
			else
			{
				// Bad command entered
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Wrong syntax. Try /ks ? for help.");
			}
		}
		if (config.debugTime)
		{
			time = System.nanoTime() - time;
			sender.sendMessage("[Debug]" + KarmicShare.TAG + "Process time: "
					+ time);
		}
		return true;
	}

	private void otherPlayerKarma(CommandSender sender, String[] args)
	{
		// Check if karma is enabled
		if (!config.karmaDisabled)
		{
			// Check if name was given
			if (args.length > 1)
			{
				// Check if they have the permission node
				if (PermCheck.checkPermission(sender, "KarmicShare.admin")
						|| PermCheck.checkPermission(sender,
								"KarmicShare.karma.other"))
				{
					// attempt to parse name
					String name = plugin.expandName(args[1]);
					if (name == null)
					{
						name = args[1];
					}
					try
					{
						// Colorize karma
						sender.sendMessage(this.colorizeKarma(Karma
								.getPlayerKarma(name)));
					}
					catch (SQLException e)
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Could not get " + name + "'s karma");
						e.printStackTrace();
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED
							+ " Lack permission: KarmicShare.karma.other");
				}
			}
			else
			{
				// did not give a player name, therefore error
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " No player name given.");
			}
		}
		else
		{
			// karma system disabled
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ " Karma disabled");
		}
	}

	private void showVersion(CommandSender sender, String[] args)
	{
		sender.sendMessage(ChatColor.BLUE + bar + "=====");
		sender.sendMessage(ChatColor.GREEN + "KarmicShare v"
				+ plugin.getDescription().getVersion());
		sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru");
		sender.sendMessage(ChatColor.BLUE + "===========" + ChatColor.GRAY
				+ "Config" + ChatColor.BLUE + "===========");
		sender.sendMessage(ChatColor.GRAY + "Effects: " + config.effects);
		sender.sendMessage(ChatColor.GRAY + "Chests: " + config.chests);
		sender.sendMessage(ChatColor.GRAY + "Karma enabled: "
				+ !config.karmaDisabled);
		sender.sendMessage(ChatColor.GRAY + "Static karma: "
				+ config.statickarma);
		sender.sendMessage(ChatColor.GRAY + "Karma lower-upper limit: "
				+ config.lower + " :: " + config.upper);
		sender.sendMessage(ChatColor.GRAY + "Karma lower/upper %: "
				+ config.lowerPercent * 100 + "% / " + config.upperPercent
				* 100 + "%");
		sender.sendMessage(ChatColor.GRAY + "Default karma: "
				+ config.playerKarmaDefault);
		sender.sendMessage(ChatColor.GRAY + "Default karma rate: "
				+ config.karmaChange);
	}

	private void showPlayerKarma(CommandSender sender, String[] args)
	{
		// Check if player sent command
		if (!(sender instanceof Player))
		{
			return;
		}
		Player player = (Player) sender;
		try
		{
			// Retrieve karma from database and colorize
			sender.sendMessage(this.colorizeKarma(Karma.getPlayerKarma(player
					.getName())));
		}
		catch (SQLException e)
		{
			player.sendMessage(ChatColor.RED + KarmicShare.TAG
					+ "Could not obtain player karma!");
			e.printStackTrace();
		}
	}

	/**
	 * Show the help menu, with commands and description
	 * 
	 * @param sender
	 *            to display to
	 */
	private void displayHelp(CommandSender sender)
	{
		sender.sendMessage(ChatColor.BLUE + "=====" + ChatColor.RED
				+ "KarmicShare" + ChatColor.BLUE + "=====");
		sender.sendMessage(ChatColor.GREEN + "/ks" + ChatColor.YELLOW
				+ " : Show karma");
		if (PermCheck.checkPermission(sender, PermissionNode.GIVE))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks give" + ChatColor.YELLOW
					+ " : Give item stack in current hand");
		}
		if (PermCheck.checkPermission(sender, PermissionNode.TAKE))
		{
			sender.sendMessage(ChatColor.GREEN
					+ "/ks take <item>[:data] [amount]" + ChatColor.YELLOW
					+ " : Take item(s) from pool");
			sender.sendMessage(ChatColor.GREEN
					+ "/ks take <item name> [amount]" + ChatColor.YELLOW
					+ " : Take item(s) from pool");
		}
		sender.sendMessage(ChatColor.GREEN + "/ks list [page]"
				+ ChatColor.YELLOW + " : List items in pool");
		sender.sendMessage(ChatColor.GREEN + "/ks <prev | next>"
				+ ChatColor.YELLOW + " : Show previous/next page of list");
		sender.sendMessage(ChatColor.GREEN + "/ks value [prev|next|page#]"
				+ ChatColor.YELLOW
				+ " : List karma multiplier values, and page through list");
		if (plugin.useChest())
		{
			sender.sendMessage(ChatColor.GREEN + "/ks page <num>"
					+ ChatColor.YELLOW + " : Jump page numbers for chests");
			sender.sendMessage(ChatColor.GREEN + "/ks group" + ChatColor.YELLOW
					+ " : List group commands");
		}
		sender.sendMessage(ChatColor.GREEN + "/ks help" + ChatColor.YELLOW
				+ " : Show help menu");
		if (PermCheck.checkPermission(sender, PermissionNode.INFO))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks info" + ChatColor.YELLOW
					+ " : Inspect currently held item");
		}
		if (PermCheck.checkPermission(sender, PermissionNode.KARMA_OTHER))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks player <name>"
					+ ChatColor.YELLOW + " : Show karma for given player name");
		}
		if (PermCheck.checkPermission(sender, PermissionNode.ADMIN_ADD)
				|| PermCheck
						.checkPermission(sender, PermissionNode.ADMIN_RESET)
				|| PermCheck.checkPermission(sender, PermissionNode.ADMIN_SET)
				|| PermCheck
						.checkPermission(sender, PermissionNode.ADMIN_DRAIN)
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_RELOAD)
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_GROUP_ADD)
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_GROUP_CREATE)
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_GROUP_DELETE)
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_GROUP_REMOVE))
		{
			sender.sendMessage(ChatColor.GREEN + "/ks admin" + ChatColor.YELLOW
					+ " : List admin commands");
		}
		sender.sendMessage(ChatColor.GREEN + "/ks version" + ChatColor.YELLOW
				+ " : Show version and config");
	}

	/**
	 * Colorizes the karma based on percentages in the config file
	 * 
	 * @param player
	 *            karma
	 * @return Appropriate string with color codes
	 */
	private String colorizeKarma(int karma)
	{
		// Colorize based on how high/low karma is
		if (Math.abs(karma + config.lower) <= Math.abs(karma + config.upper))
		{
			// Positive karma
			if (((double) karma + Math.abs(config.lower))
					/ ((double) Math.abs(config.upper) + Math.abs(config.lower)) >= config.upperPercent)
			{
				return (ChatColor.YELLOW + KarmicShare.TAG + ChatColor.GREEN
						+ " Karma: " + karma);
			}
			else
			{
				// Not in upper percentage
				return (ChatColor.YELLOW + KarmicShare.TAG + " Karma: " + karma);
			}
		}
		else
		{
			// Negative karma
			if (((double) karma + Math.abs(config.lower))
					/ ((double) Math.abs(config.upper) + Math.abs(config.lower)) <= config.lowerPercent)
			{
				return (ChatColor.YELLOW + KarmicShare.TAG + ChatColor.RED
						+ " Karma: " + karma);
			}
			else
			{
				// Not in lower percentage
				return (ChatColor.YELLOW + KarmicShare.TAG + " Karma: " + karma);
			}
		}
	}
}