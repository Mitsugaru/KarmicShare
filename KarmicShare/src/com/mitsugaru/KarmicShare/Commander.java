/**
 * Separate class to handle commands Followed example from DiddiZ's LB.
 * 
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.permissions.PermCheck;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;
import com.mitsugaru.KarmicShare.questioner.tasks.ConfirmCleanup;
import com.mitsugaru.KarmicShare.questioner.tasks.ConfirmDrain;
import com.mitsugaru.KarmicShare.questioner.tasks.ConfirmPlayerKarmaReset;
import com.mitsugaru.KarmicShare.questioner.tasks.ConfirmRemoveGroup;
import com.mitsugaru.KarmicShare.questioner.tasks.RemoveGroupTask;

public class Commander implements CommandExecutor
{
	// Class variables
	private final KarmicShare ks;
	private final static String bar = "======================";
	public static final String GROUP_NAME_REGEX = "[\\p{Alnum}_[\\-]]*";
	private final Config config;
	private final Map<String, Integer> page = new HashMap<String, Integer>();
	private final Map<String, Integer> multiPage = new HashMap<String, Integer>();
	public static final Map<Item, Integer> cache = new HashMap<Item, Integer>();
	public static final Map<String, Integer> chestPage = new HashMap<String, Integer>();
	private int limit;
	private long time;

	/**
	 * Constructor
	 * 
	 * @param karmicShare
	 *            plugin
	 */
	public Commander(KarmicShare karmicShare)
	{
		// Instantiate variables
		ks = karmicShare;
		config = ks.getPluginConfig();
		limit = config.listlimit;
		time = 0;
	}

	// TODO refactor parsing the input string for when adding/taking items
	// and player has specified the item. Throw it into a method for
	// ks take and ks admin add to utilize, as well as chest access
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
					final StringBuilder sb = new StringBuilder();
					for (String s : Karma.getPlayerGroups(sender, sender.getName()))
					{
						//TODO show currently selected group as different
						sb.append(ChatColor.GRAY + s + ChatColor.DARK_AQUA
								+ "-");
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
				this.inspectItem(sender, args);
			}
			// Player is giving item to pool
			else if (com.equals("give"))
			{
				this.giveItem(sender, args);
			}
			// Player requested an item
			else if (com.equals("take"))
			{
				if (this.takeItem(sender, args))
				{
					if (config.debugTime)
					{
						debugTime(sender, time);
					}
					return true;
				}
			}
			// Previous page of item pool
			else if (com.equals("prev"))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_LIST))
				{
					// List, with previous page
					this.listPool(sender, -1);
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
					this.listPool(sender, 1);
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
					this.listCommand(sender, args);
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
					this.valueCommand(sender, args);
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
				if (ks.useChest())
				{
					if (PermCheck.checkPermission(sender, PermissionNode.CHEST))
					{
						if (args.length > 1)
						{
							try
							{
								Integer page = Integer.parseInt(args[1]);
								chestPage.put(sender.getName(), page);
								sender.sendMessage(ChatColor.GREEN
										+ KarmicShare.TAG
										+ " Right click on sign to jump to page "
										+ ChatColor.GOLD + page.intValue());
							}
							catch (NumberFormatException e)
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Invalid number: " + ChatColor.GOLD
										+ args[1]);
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
				if (PermCheck.checkPermission(sender, PermissionNode.GROUP))
				{
					if (args.length > 1)
					{
						// They have a parameter, thus
						// parse in adminCommand method
						if (!this.groupCommand(sender, args))
						{
							// Bad command
							sender.sendMessage(ChatColor.RED
									+ KarmicShare.TAG
									+ " Syntax error. Use /ks group for list of commands");
						}
						if (config.debugTime)
						{
							debugTime(sender, time);
						}
						return true;
					}
					else
					{

						// Show group commands help menu
						sender.sendMessage(ChatColor.BLUE + "==="
								+ ChatColor.LIGHT_PURPLE + "KarmicShare Group"
								+ ChatColor.BLUE + "===");
						if (PermCheck.checkPermission(sender,
								PermissionNode.GROUP_CREATE))
						{
							sender.sendMessage(ChatColor.GREEN
									+ "/ks group create <name>"
									+ ChatColor.YELLOW
									+ " : Creates a new group");
						}
						if (PermCheck.checkPermission(sender,
								PermissionNode.GROUP_ADD))
						{
							sender.sendMessage(ChatColor.GREEN
									+ "/ks group add <group> <player> [player2] ..."
									+ ChatColor.YELLOW
									+ " : Adds a player to the group");
						}
						if (PermCheck.checkPermission(sender,
								PermissionNode.GROUP_REMOVE))
						{
							sender.sendMessage(ChatColor.GREEN
									+ "/ks group remove <group> <player> [player2] ..."
									+ ChatColor.YELLOW
									+ " : Removes player from the group");
						}
						if (PermCheck.checkPermission(sender,
								PermissionNode.GROUP_LEAVE))
						{
							sender.sendMessage(ChatColor.GREEN
									+ "/ks group leave <group> [group2] ..."
									+ ChatColor.YELLOW + " : Leave group");
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + " Lack permission: "
							+ PermissionNode.GROUP.getNode());
				}
			}
			// Admin command
			else if (com.equals("admin"))
			{
				if (args.length > 1)
				{
					// They have a parameter, thus
					// parse in adminCommand method
					if (!this.adminCommand(sender, args))
					{
						// Bad command
						sender.sendMessage(ChatColor.RED
								+ KarmicShare.TAG
								+ " Syntax error. Use /ks admin for list of commands");
					}
					if (config.debugTime)
					{
						debugTime(sender, time);
					}
					return true;
				}
				else
				{
					// Show admin commands help menu
					sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.RED
							+ "KarmicShare Admin" + ChatColor.BLUE + "===");
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_ADD))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin add <item>[:data] [amount]"
								+ ChatColor.YELLOW + " : Add item(s) to pool");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_RESET))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin reset <player>" + ChatColor.YELLOW
								+ " : Resets player's karma");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_SET))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin set <player> <karma>"
								+ ChatColor.YELLOW
								+ " : Sets player's karma to value");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_DRAIN))
					{
						sender.sendMessage(ChatColor.GREEN + "/ks admin drain"
								+ ChatColor.YELLOW + " : Empty item pool");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_CREATE))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin group create <group>"
								+ ChatColor.YELLOW
								+ " : Create group in database");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_DELETE))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin group delete <group>"
								+ ChatColor.YELLOW
								+ " : Remove group from database");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_ADD))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin group add  <group> <player> [player2] ..."
								+ ChatColor.YELLOW
								+ " : Force add player to group");
					}
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_REMOVE))
					{
						sender.sendMessage(ChatColor.GREEN
								+ "/ks admin group remove  <group> <player> [player2] ..."
								+ ChatColor.YELLOW
								+ " : Force remove player to group");
					}
					if (PermCheck.checkPermission(sender, "KarmicShare.admin.reload"))
					{
						sender.sendMessage(ChatColor.GREEN + "/ks admin reload"
								+ ChatColor.YELLOW + " : Reload configuration");
					}
				}
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
			debugTime(sender, time);
		}
		return true;
	}

	// TODO need permission nodes for groups
	// TODO need to also check against if they are creator/manager of group
	// TODO group settings
	private boolean groupCommand(CommandSender sender, String[] args)
	{
		// Show group commands help menu
		final String com = args[1];
		// Add generated items to pool
		if (com.equals("create"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.GROUP_CREATE))
			{
				try
				{
					// force group names to lower case
					final String group = args[2].toLowerCase();
					if (!group.matches(GROUP_NAME_REGEX))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Group name must be alphanumeric");
						return true;
					}
					else if (group.length() > 15)
					{
						// Restrict length to sign character limit
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Group name must be 15 characters or less.");
						return true;
					}
					else
					{
						if (!Karma.validGroup(sender, group))
						{
							// Create group
							ks.getDatabaseHandler().standardQuery(
									"INSERT INTO " + Table.GROUPS.getName()
											+ " (groupname) VALUES ('" + group
											+ "');");
							sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
									+ " Group " + ChatColor.GRAY + group
									+ ChatColor.GREEN + " created");
							if (sender instanceof Player)
							{
								// add player to group
								addPlayerToGroup(sender,
										((Player) sender).getName(), group);
								sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
										+ " Added " + ChatColor.GOLD
										+ ((Player) sender).getName()
										+ ChatColor.GREEN + " to "
										+ ChatColor.GRAY + group);
							}
							else
							{
								sender.sendMessage(ChatColor.YELLOW
										+ KarmicShare.TAG
										+ " Cannot add NPCs to groups. Group is empty.");
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Group " + ChatColor.GRAY + group
									+ ChatColor.RED + " already exists");
						}
					}
				}
				catch (IndexOutOfBoundsException e)
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Group name not given");
					return false;
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: "
						+ PermissionNode.GROUP_CREATE.getNode());
				return true;
			}
		}
		else if (com.equals("add"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.GROUP_ADD))
			{
				// Grab group name
				String group = "";
				if (args.length > 2)
				{
					// force group names to lower case
					group = args[2].toLowerCase();
					if (group.equals("global"))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Cannot modify global group.");
						return true;
					}
					if (sender instanceof Player)
					{
						if (!Karma.playerHasGroup(sender,
								((Player) sender).getName(), group))
						{
							sender.sendMessage(ChatColor.RED
									+ KarmicShare.TAG
									+ " Cannot add players to groups you're not in.");
							return true;
						}
					}
				}
				else
				{
					// Group name was not given
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Must specify group");
					return false;
				}
				if (!group.matches(GROUP_NAME_REGEX))
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Group name must be alphanumeric");
					return true;
				}
				if (args.length > 3)
				{
					// Grab all names
					for (int i = 3; i < args.length; i++)
					{
						String name = expandName(args[i]);
						if (name == null)
						{
							name = args[i];
						}
						else if (Karma.playerHasGroup(sender, name, group))
						{
							sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG + " "
									+ ChatColor.AQUA + name + ChatColor.YELLOW
									+ " is already in " + ChatColor.GRAY
									+ group);
							return true;
						}
						else
						{
							if (Karma.validGroup(sender, group))
							{
								// Grab player on server
								Player other = ks.getServer().getPlayer(name);
								if (other != null)
								{
									// add other player to group
									addPlayerToGroup(sender, other.getName(),
											group);
									sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
											+ " Added " + ChatColor.GOLD + name
											+ ChatColor.GREEN + " to "
											+ ChatColor.GRAY + group);
									other.sendMessage(ChatColor.GREEN + KarmicShare.TAG
											+ " You have been added to "
											+ ChatColor.GRAY + group);
								}
								else
								{
									sender.sendMessage(ChatColor.YELLOW
											+ KarmicShare.TAG
											+ " Can only add players if they're online.");
								}
							}
							else
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Group " + ChatColor.GRAY + group
										+ ChatColor.RED + " does not exist");
							}
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Must specify player");
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.GROUP_ADD.getNode());
				return true;
			}
		}
		else if (com.equals("remove"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.GROUP_REMOVE))
			{
				String group = "";
				if (args.length > 2)
				{
					// Grab group name if given
					// force group names to lower case
					group = args[2].toLowerCase();
					if (group.equals("global"))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Cannot remove players from global.");
						return true;
					}
					if (sender instanceof Player)
					{
						if (!Karma.playerHasGroup(sender,
								((Player) sender).getName(), group))
						{
							sender.sendMessage(ChatColor.RED
									+ KarmicShare.TAG
									+ " Cannot remove players from groups you're not in.");
							return true;
						}
					}
					if (!group.matches(GROUP_NAME_REGEX))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Group name must be alphanumeric");
						return true;
					}
				}
				else
				{
					// Group name was not given
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Must specify group");
					return false;
				}
				if (args.length > 3)
				{
					for (int i = 3; i < args.length; i++)
					{
						String name = expandName(args[i]);
						if (name == null)
						{
							name = args[i];
						}
						if (!Karma.playerHasGroup(sender, name, group))
						{
							sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
									+ ChatColor.AQUA + name + ChatColor.YELLOW
									+ " not in " + ChatColor.GRAY + group);
							return true;
						}
						else
						{
							if (Karma.validGroup(sender, group))
							{
								// remove other player to group
								removePlayerFromGroup(sender, name, group);
								sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
										+ " Removed " + ChatColor.GOLD + name
										+ ChatColor.GREEN + " from "
										+ ChatColor.GRAY + group);
								final Player p = ks.getServer().getPlayer(name);
								if (p != null)
								{
									p.sendMessage(ChatColor.GREEN + KarmicShare.TAG
											+ " You have been removed from "
											+ ChatColor.GRAY + group);
								}
							}
							else
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Group " + ChatColor.GRAY + group
										+ ChatColor.RED + " does not exist");
							}
						}
					}

				}
				else
				{
					// Player name was not given
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Must specify player");
					return false;
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: "
						+ PermissionNode.GROUP_REMOVE.getNode());
				return true;
			}
		}
		else if (com.equals("leave"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.GROUP_LEAVE))
			{
				if (args.length > 2)
				{
					String group = "";
					for (int i = 2; i < args.length; i++)
					{
						group = args[i].toLowerCase();
					}
					if (!Karma.playerHasGroup(sender, sender.getName(), group))
					{
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ ChatColor.AQUA + sender.getName()
								+ ChatColor.YELLOW + " not in "
								+ ChatColor.GRAY + group);
						return true;
					}
					if (!group.matches(GROUP_NAME_REGEX))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Group name must be alphanumeric");
						return true;
					}
					else
					{
						if (Karma.validGroup(sender, group))
						{
							// remove other player to group
							removePlayerFromGroup(sender, sender.getName(),
									group);
							sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
									+ " Removed " + ChatColor.GOLD
									+ sender.getName() + ChatColor.GREEN
									+ " from " + ChatColor.GRAY + group);
						}
						else
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Group " + ChatColor.GRAY + group
									+ ChatColor.RED + " does not exist");
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Must specify a group");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: "
						+ PermissionNode.GROUP_LEAVE.getNode());
				return true;
			}
		}
		return false;
	}

	private void removePlayerFromGroup(CommandSender sender, String name,
			String group)
	{
		try
		{
			String groups = "";
			Query rs = ks.getDatabaseHandler().select(
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
							ks.getLogger().info(s);
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
			ks.getDatabaseHandler().standardQuery(
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

	private void addPlayerToGroup(CommandSender sender, String name,
			String group)
	{
		try
		{
			// Insures that the player is added to the database
			Karma.getPlayerKarma(name);
			String groups = "";
			Query rs = ks.getDatabaseHandler().select(
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
			ks.getDatabaseHandler().standardQuery(
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
					String name = expandName(args[1]);
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
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Karma disabled");
		}
	}

	private void valueCommand(CommandSender sender, String[] args)
	{
		if (!config.karmaDisabled)
		{
			if (args.length > 1)
			{
				// If they provided a page number

				try
				{
					// Attempt to parse argument for page number
					int pageNum = Integer.parseInt(args[1]);
					// Set current page to given number
					multiPage.put(sender.getName(), pageNum - 1);
					// Show page if possible
					this.listMultipliers(sender, 0);
				}
				catch (NumberFormatException e)
				{
					// Maybe they did prev/next?
					if (args[1].equals("prev"))
					{
						// List, with previous page
						this.listMultipliers(sender, -1);
					}
					else if (args[1].equals("next"))
					{
						// List, with previous page
						this.listMultipliers(sender, 1);
					}
					else
					{
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Invalid extra parameter: " + args[1]);
					}
				}
			}
			else
			{
				// List with current page
				this.listMultipliers(sender, 0);
			}
		}
		else
		{
			// karma system disabled
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Karma disabled");
		}
	}

	private void listCommand(CommandSender sender, String[] args)
	{
		// TODO allow people to "find" items
		// i.e. limit list entries to what they want
		if (args.length > 1)
		{
			// If they provided a page number
			try
			{
				// Attempt to parse argument for page number
				int pageNum = Integer.parseInt(args[1]);
				// Set current page to given number
				page.put(sender.getName(), pageNum - 1);
				// Show page if possible
				this.listPool(sender, 0);
			}
			catch (NumberFormatException e)
			{
				// TODO this is where I would catch the item's
				// partial name. Probably use regex and see if
				// an item's name in cache matches.
				// Will need to modify listPool to accept regex
				// Can't think of a good way to page through this
				// new list without having a hashmap per custom
				// cache, and I really don't want to do that :\
				sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
						+ " Invalid integer for page number");
			}
		}
		else
		{
			// List with current page
			this.listPool(sender, 0);
		}
	}

	private boolean takeItem(CommandSender sender, String[] args)
	{
		// Take item from pool
		// Check if player sent command
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			// Check if they have "take" permission
			if (PermCheck.checkPermission(sender, PermissionNode.TAKE))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_TAKE))
				{
					// Check that they gave an item name/id
					if (args.length > 1)
					{
						// Player will always request at least 1 item
						int itemid = 0;
						int data = 0;
						int amount = 1;
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
									if (config.debugTime)
									{
										debugTime(sender, time);
									}
									return true;
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
								this.updateCache(sender);
								// Check if item exists in cache through
								// reverse lookup: name -> id:data
								Item[] array = cache.keySet().toArray(
										new Item[0]);
								for (int i = 0; i < array.length; i++)
								{
									String cacheName = array[i].name
											.toLowerCase();
									if (temp.equals(cacheName))
									{
										// Item is in cache, so get item id
										// and data values
										itemid = array[i].itemId();
										data = array[i].getData();
										dur = array[i].itemDurability();
										has = true;
										break;
									}
								}
								if (!has)
								{
									// Item not in cache, therefore
									// potential error on player part
									player.sendMessage(ChatColor.RED + KarmicShare.TAG
											+ " Item not in pool...");
									if (config.debugTime)
									{
										debugTime(sender, time);
									}
									return true;
								}
							}
						}
						// Create temp item
						final Item temp = new Item(itemid, Byte.valueOf(""
								+ data), dur);
						ItemStack item = new ItemStack(1);
						int finalAmount = 0;
						if (temp.isTool())
						{
							// Grab all entries of the same tool id
							String toolQuery = "SELECT * FROM "
									+ Table.ITEMS.getName() + " WHERE itemid='"
									+ itemid + "' AND groups='global';";
							Query toolRS = ks.getDatabaseHandler().select(
									toolQuery);
							try
							{
								ArrayList<ItemStack> itemList = new ArrayList<ItemStack>();
								if (toolRS.getResult().next())
								{
									do
									{
										// Generate item
										ItemStack toolItem = new ItemStack(
												itemid, toolRS.getResult()
														.getInt("amount"),
												toolRS.getResult().getShort(
														"data"));
										String enchant = toolRS.getResult()
												.getString("enchantments");
										if (!toolRS.getResult().wasNull())
										{
											// It had enchantments
											String[] cut = enchant.split("i");
											for (int i = 0; i < cut.length; i++)
											{
												String[] cutter = cut[i]
														.split("v");
												EnchantmentWrapper e = new EnchantmentWrapper(
														Integer.parseInt(cutter[0]));
												toolItem.addUnsafeEnchantment(
														e.getEnchantment(),
														Integer.parseInt(cutter[1]));
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
										int a = Karma.takeItem(player, i,
												"gloabl");
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
															.getPlayerKarma(player
																	.getName());
													Karma.giveItem(player, i,
															"global");
													Karma.updatePlayerKarma(
															player.getName(),
															currentKarma);
												}
												catch (SQLException e)
												{
													// INFO Auto-generated catch
													// block
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
							item = new ItemStack(itemid, amount,
									Short.valueOf("" + data));
							finalAmount = Karma
									.takeItem(player, item, "global");
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
										player.sendMessage(ChatColor.YELLOW
												+ KarmicShare.TAG
												+ " Your inventory is completely full...");
									}
									item.setAmount(residual.size());
									try
									{
										int currentKarma = Karma
												.getPlayerKarma(player
														.getName());
										Karma.giveItem(player, item, "global");
										Karma.updatePlayerKarma(
												player.getName(), currentKarma);
									}
									catch (SQLException e)
									{
										sender.sendMessage(ChatColor.RED
												+ KarmicShare.TAG
												+ " SQL Exception");
										e.printStackTrace();
									}
								}
							}
						}
						else
						{
							item = new ItemStack(itemid, amount,
									Byte.valueOf("" + data));
							finalAmount = Karma
									.takeItem(player, item, "global");
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
										player.sendMessage(ChatColor.YELLOW
												+ KarmicShare.TAG
												+ " Your inventory is completely full...");
									}
									item.setAmount(residual.size());
									try
									{
										int currentKarma = Karma
												.getPlayerKarma(player
														.getName());
										Karma.giveItem(player, item, "global");
										Karma.updatePlayerKarma(
												player.getName(), currentKarma);
									}
									catch (SQLException e)
									{
										sender.sendMessage(ChatColor.RED
												+ KarmicShare.TAG
												+ " SQL Exception");
										e.printStackTrace();
									}
								}
							}
						}
						if (finalAmount > 0)
						{
							player.sendMessage(ChatColor.GREEN + KarmicShare.TAG
									+ " Given " + ChatColor.GOLD + finalAmount
									+ ChatColor.GREEN + " of " + ChatColor.AQUA
									+ temp.name);
						}
					}
					else
					{
						player.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Need an item name or id");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Lack permission: "
							+ PermissionNode.COMMANDS_TAKE.getNode());
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.TAKE.getNode());
			}
		}
		else
		{
			sender.sendMessage(KarmicShare.TAG + " Cannot use this command as console.");
		}
		if (config.debugTime)
		{
			debugTime(sender, time);
		}
		return true;
	}

	private void giveItem(CommandSender sender, String[] args)
	{
		// TODO allow for player to specify item and amount
		// parse more parameters to allow spaces for item names
		// Check if player sent command
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			// Check if they have "give" permission
			if (PermCheck.checkPermission(sender, PermissionNode.GIVE))
			{
				if (PermCheck.checkPermission(sender,
						PermissionNode.COMMANDS_GIVE))
				{
					// Grab item in player's hand.
					final ItemStack items = player.getItemInHand();
					int itemid = items.getTypeId();
					// Check if there is an item in their hand
					if (itemid != 0)
					{
						// TODO un-hardcode global?
						Karma.giveItem(player, items, "global");
						// Remove item from player inventory
						// Thanks to @nisovin for the following line
						final Item i = new Item(items.getTypeId(), items
								.getData().getData(), items.getDurability());
						player.setItemInHand(null);
						player.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " Added "
								+ ChatColor.GOLD + items.getAmount()
								+ ChatColor.GREEN + " of " + ChatColor.AQUA
								+ i.name + ChatColor.GREEN + " to pool.");
					}
					else
					{
						// If there is no item, stop
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " No item in hand. Nothing to give.");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Lack permission: "
							+ PermissionNode.COMMANDS_GIVE.getNode());
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.GIVE.getNode());
			}
		}
		else
		{
			sender.sendMessage(KarmicShare.TAG + " Cannot use this command as console.");
		}
	}

	private void inspectItem(CommandSender sender, String[] args)
	{
		// Inspect item in hand
		if (sender instanceof Player)
		{
			// Permission check
			if (PermCheck.checkPermission(sender, PermissionNode.INFO))
			{
				Player player = (Player) sender;
				// Grab item in player's hand.
				ItemStack items = player.getItemInHand();
				int itemid = items.getTypeId();
				// Check if there is an item in their hand
				if (itemid != 0)
				{
					int quantity = items.getAmount();
					Item item = new Item(itemid, items.getData().getData(),
							items.getDurability());
					StringBuffer buf = new StringBuffer();
					buf.append("Info: Name: " + ChatColor.AQUA + item.name
							+ ChatColor.GREEN + " ID: "
							+ ChatColor.LIGHT_PURPLE + itemid + ChatColor.GREEN
							+ " Amount:" + ChatColor.GOLD + quantity
							+ ChatColor.GREEN + " Data: "
							+ ChatColor.LIGHT_PURPLE + item.getData()
							+ ChatColor.GREEN + " Damage: "
							+ ChatColor.LIGHT_PURPLE + items.getDurability()
							+ ChatColor.GREEN + " Tool: " + ChatColor.GRAY
							+ item.isTool() + ChatColor.GREEN + " Potion: "
							+ ChatColor.GRAY + item.isPotion());
					if (!config.karmaDisabled)
					{
						if (config.statickarma)
						{
							buf.append(ChatColor.GREEN + " Multiplier: "
									+ ChatColor.YELLOW + config.karmaChange);
							buf.append(ChatColor.GREEN + " Total Karma: "
									+ ChatColor.YELLOW + ""
									+ (config.karmaChange * quantity));
						}
						else
						{
							// Check if given item has a multiplier
							Item[] karmaList = config.karma.keySet().toArray(
									new Item[0]);
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
									buf.append(ChatColor.GREEN
											+ " Multiplier: "
											+ ChatColor.YELLOW
											+ config.karma.get(item));
									buf.append(ChatColor.GREEN
											+ " Total Karma: "
											+ ChatColor.YELLOW
											+ ""
											+ (config.karma.get(item) * quantity));
								}
								catch (NullPointerException n)
								{
									// Found item, but there is no
									// config for specific data value
									// thus adjust using regular means
									buf.append(ChatColor.GREEN
											+ " Multiplier: "
											+ ChatColor.YELLOW
											+ config.karmaChange);
									buf.append(ChatColor.GREEN
											+ " Total Karma: "
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
					Map<Enchantment, Integer> enchantments = items
							.getEnchantments();
					if (enchantments.isEmpty())
					{
						buf.append(ChatColor.GREEN + " Enchantments: "
								+ ChatColor.WHITE + "NONE");
					}
					else
					{
						buf.append(ChatColor.GREEN + " Enchantments: ");

						for (Map.Entry<Enchantment, Integer> e : enchantments
								.entrySet())
						{
							buf.append(ChatColor.WHITE + e.getKey().getName()
									+ ChatColor.YELLOW + " v"
									+ e.getValue().intValue() + ", ");
						}
					}
					player.sendMessage(ChatColor.GREEN + KarmicShare.TAG
							+ buf.toString());
				}
				else
				{
					// If there is no item, stop
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " No item in hand. Nothing to lookup.");
				}
			}
			else
			{
				// Lack permission
				sender.sendMessage(KarmicShare.TAG + " Lack permission: "
						+ PermissionNode.INFO.getNode());
			}

		}
		else
		{
			// Console cannot inspect items
			sender.sendMessage(KarmicShare.TAG + " Cannot use this command as console.");
		}
	}

	private void showVersion(CommandSender sender, String[] args)
	{
		sender.sendMessage(ChatColor.BLUE + bar + "=====");
		sender.sendMessage(ChatColor.GREEN + "KarmicShare v"
				+ ks.getDescription().getVersion());
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
		if (sender instanceof Player)
		{
			Player player = (Player) sender;

			try
			{
				// Retrieve karma from database and colorize
				sender.sendMessage(this.colorizeKarma(Karma
						.getPlayerKarma(player.getName())));
			}
			catch (SQLException e)
			{
				player.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ "Could not obtain player karma!");
				e.printStackTrace();
			}
		}
	}

	private void debugTime(CommandSender sender, long time)
	{
		time = System.nanoTime() - time;
		sender.sendMessage("[Debug]" + KarmicShare.TAG + "Process time: " + time);
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
		if (ks.useChest())
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
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_RESET)
				|| PermCheck.checkPermission(sender, PermissionNode.ADMIN_SET)
				|| PermCheck.checkPermission(sender,
						PermissionNode.ADMIN_DRAIN)
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

	private boolean adminCommand(CommandSender sender, String[] args)
	{
		final String com = args[1];
		// Add generated items to pool
		if (com.equals("add"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.ADMIN_ADD))
			{
				if (args.length > 2)
				{
					int itemid = 0;
					int data = 0;
					short dur = 0;
					// Player will always request at least 1 item
					// TODO make default take amount configurable in YAML
					int amount = 1;
					try
					{
						itemid = Integer.parseInt(args[2]);
					}
					catch (NumberFormatException e)
					{
						if (args[2].contains(":"))
						{
							// Attempt to parse as itemid:data
							String[] cut = args[2].split(":");
							try
							{
								itemid = Integer.parseInt(cut[0]);
								data = Integer.parseInt(cut[1]);
								dur = Short.parseShort(cut[1]);
							}
							catch (NumberFormatException r)
							{
								// Not a number given
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Invalid item id / data value");
								return false;
							}
						}
						else
						{
							// They gave a non-integer
							// Try and parse the string as material
							final Material mat = Material
									.matchMaterial(args[2]);
							if (mat == null)
							{
								// Not a known material
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Item name/id is incorrect.");
								return false;
							}
							else
							{
								itemid = mat.getId();
							}
						}
					}

					// They specified an amount
					if (args.length > 3)
					{
						// Ignore rest of arguments and parse the
						// immediate after
						try
						{
							amount = Integer.parseInt(args[3]);
						}
						catch (NumberFormatException n)
						{
							// Not a number given
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Invalid item amount");
							return false;
						}
					}
					// Create item object
					final Item item = new Item(itemid, Byte.valueOf("" + data),
							dur);
					if (itemid != 0)
					{
						if (item.isPotion())
						{
							data = 0;
							// Create SQL query to see if item is already in
							// database
							String query = "SELECT * FROM "
									+ Table.ITEMS.getName() + " WHERE itemid='"
									+ itemid + "' AND durability='" + dur
									+ "' AND groups='global';";
							Query rs = ks.getDatabaseHandler().select(query);
							// Send Item to database
							try
							{
								if (rs.getResult().next())
								{
									// here you know that there is at least
									// one record
									do
									{
										int total = amount
												+ rs.getResult().getInt(
														"amount");
										query = "UPDATE "
												+ Table.ITEMS.getName()
												+ " SET amount='" + total
												+ "' WHERE itemid='" + itemid
												+ "' AND durability='" + dur
												+ "' AND groups='global';";
									} while (rs.getResult().next());
								}
								else
								{
									// Item not in database, therefore add
									// it
									query = "INSERT INTO "
											+ Table.ITEMS.getName()
											+ " (itemid,amount,data, durability,groups) VALUES ("
											+ itemid + "," + amount + ","
											+ data + "," + dur + ",'global');";
								}
								rs.getResult().close();
								ks.getDatabaseHandler().standardQuery(query);
								sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
										+ " Added " + ChatColor.GOLD + amount
										+ ChatColor.GREEN + " of "
										+ ChatColor.AQUA + item.name
										+ ChatColor.GREEN + " to pool.");
							}
							catch (SQLException q)
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ "Could not add item to pool!");
								q.printStackTrace();
							}
						}
						else
						{
							// Create SQL query to see if item is already in
							// database
							String query = "SELECT * FROM "
									+ Table.ITEMS.getName() + " WHERE itemid='"
									+ itemid + "' AND data='" + data
									+ "' AND groups='global';";
							Query rs = ks.getDatabaseHandler().select(query);
							// Send Item to database
							try
							{
								if (rs.getResult().next())
								{
									// here you know that there is at least
									// one record
									do
									{
										int total = amount
												+ rs.getResult().getInt(
														"amount");
										query = "UPDATE "
												+ Table.ITEMS.getName()
												+ " SET amount='" + total
												+ "' WHERE itemid='" + itemid
												+ "' AND data='" + data
												+ "' AND groups='global';";
									} while (rs.getResult().next());
								}
								else
								{
									// Item not in database, therefore add
									// it
									query = "INSERT INTO "
											+ Table.ITEMS.getName()
											+ " (itemid,amount,data, durability,groups) VALUES ("
											+ itemid + "," + amount + ","
											+ data + "," + dur + ",'global');";
								}
								rs.closeQuery();
								ks.getDatabaseHandler().standardQuery(query);
								sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
										+ " Added " + ChatColor.GOLD + amount
										+ ChatColor.GREEN + " of "
										+ ChatColor.AQUA + item.name
										+ ChatColor.GREEN + " to pool.");
							}
							catch (SQLException q)
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Could not add item to pool!");
								q.printStackTrace();
							}
						}
					}
					else
					{
						// If there is no item, stop
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Cannot add air to pool.");
					}
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.ADMIN_ADD.getNode());
				return true;
			}
		}
		else if (com.equals("drain"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.ADMIN_DRAIN))
			{
				String group = "global";
				// Check if group name was given
				if (args.length > 2)
				{
					group = args[2];
					if (!Karma.validGroup(sender, args[2]))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Group "
								+ ChatColor.GRAY + group + ChatColor.RED
								+ " does not exist");
						return true;
					}
				}
				if (sender instanceof Player)
				{
					int id = ks
							.getServer()
							.getScheduler()
							.scheduleAsyncDelayedTask(ks,
									new ConfirmDrain(ks, (Player) sender, group));
					if (id == -1)
					{
						sender.sendMessage(ChatColor.YELLOW
								+ KarmicShare.TAG
								+ " Could not schedule confirmation.");
					}
				}
				else
				{
					// Sent from console
					// Wipe table
					final String query = "DELETE FROM " + Table.ITEMS.getName()
							+ " WHERE groups='" + group + "';";
					ks.getDatabaseHandler().standardQuery(query);
					ks.getLogger().info(
							KarmicShare.TAG + " Items for group '" + group + "' cleared");
					cache.clear();
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.ADMIN_DRAIN.getNode());
				return true;
			}
		}
		else if(com.equals("cleanup"))
		{
			if(PermCheck.checkPermission(sender, PermissionNode.ADMIN_CLEANUP))
			{
				if(sender instanceof Player)
				{
					int id = ks
							.getServer()
							.getScheduler()
							.scheduleAsyncDelayedTask(ks,
									new ConfirmCleanup(ks, (Player) sender));
					if (id == -1)
					{
						sender.sendMessage(ChatColor.YELLOW
								+ KarmicShare.TAG
								+ " Could not schedule confirmation.");
					}
				}
				else
				{
					// Sent from console
					ks.getDatabaseHandler().standardQuery("DELETE FROM " + Table.ITEMS.getName()
							+ " WHERE amount<='0';");
					ks.getLogger().info(KarmicShare.TAG + " Cleanup query executed");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.ADMIN_CLEANUP.getNode());
				return true;
			}
		}
		else if (com.equals("reload"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.ADMIN_RELOAD))
			{
				config.reloadConfig();
				sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
						+ " Config reloaded");
				multiPage.clear();
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.ADMIN_RELOAD.getNode());
				return true;
			}
		}
		else if (com.equals("reset"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.ADMIN_RESET))
			{
				if (!config.karmaDisabled)
				{
					// Check if name was given
					if (args.length > 2)
					{
						// attempt to parse name
						String name = expandName(args[2]);
						if (name == null)
						{
							name = args[2];
						}
						// SQL query to get player count for specified name
						String query = "SELECT COUNT(*) FROM "
								+ Table.PLAYERS.getName()
								+ " WHERE playername='" + name + "';";
						Query rs = ks.getDatabaseHandler().select(query);
						// Check ResultSet
						boolean has = false;
						try
						{
							if (rs.getResult().next())
							{
								// Check if only received 1 entry
								if (rs.getResult().getInt(1) == 1)
								{
									// we have a single name
									has = true;
								}
								else if (rs.getResult().getInt(1) > 1)
								{
									sender.sendMessage(ChatColor.RED
											+ KarmicShare.TAG
											+ " Got more than one result. Possibly incomplete name?");
								}
								else
								{
									// Player not in database, therefore error
									// on player part
									sender.sendMessage(ChatColor.RED + KarmicShare.TAG
											+ " Player " + ChatColor.WHITE
											+ name + ChatColor.RED
											+ " not in database.");
									sender.sendMessage(ChatColor.RED
											+ KarmicShare.TAG
											+ " Player names are case sensitive.");
								}
							}
							else
							{
								// Error in query...
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " SQL query error");
							}
							rs.getResult().close();
						}
						catch (SQLException e)
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ "Could not reset " + name + "'s karma");
							e.printStackTrace();
						}
						if (has)
						{
							if (sender instanceof Player)
							{
								int i = ks
										.getServer()
										.getScheduler()
										.scheduleAsyncDelayedTask(
												ks,
												new ConfirmPlayerKarmaReset(ks, 
														(Player) sender, name));
								if (i == -1)
								{
									sender.sendMessage(ChatColor.YELLOW
											+ KarmicShare.TAG
											+ " Could not schedule task.");
								}
							}
							else
							{
								// Sent via console
								int playerKarma = config.playerKarmaDefault;
								try
								{
									// Set to zero
									playerKarma = Karma.getPlayerKarma(name)
											* -1;
									Karma.updatePlayerKarma(name, playerKarma);
									if (config.playerKarmaDefault != 0)
									{
										// Default was non-zero, so re-update to
										// config's default
										Karma.updatePlayerKarma(name,
												config.playerKarmaDefault);
									}
									sender.sendMessage(ChatColor.YELLOW
											+ KarmicShare.TAG + " " + name
											+ "'s karma reset");
								}
								catch (SQLException e)
								{
									sender.sendMessage(ChatColor.RED + KarmicShare.TAG
											+ "Could not reset " + name
											+ "'s karma");
									e.printStackTrace();
								}
							}
						}
					}
					else
					{
						// did not give a player name, therefore error
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " No player name given.");
						return false;
					}
				}
				else
				{
					// Karma system disabled
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Karma disabled.");
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.ADMIN_RESET.getNode());
				return true;
			}
		}
		else if (com.equals("set"))
		{
			if (PermCheck.checkPermission(sender, PermissionNode.ADMIN_SET))
			{
				if (!config.karmaDisabled)
				{
					// Check if name was given
					if (args.length > 2)
					{
						// attempt to parse name
						String name = expandName(args[2]);
						if (name == null)
						{
							name = args[2];
						}
						// Check if amount was given
						if (args.length > 3)
						{
							// Attempt to parse amount
							int amount = 0;
							try
							{
								amount = Integer.parseInt(args[3]);
							}
							catch (NumberFormatException e)
							{
								// Invalid integer given for amount
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ args[2] + " is not a valid integer");
								return false;
							}
							// SQL query to get player count for specified name
							String query = "SELECT COUNT(*) FROM "
									+ Table.PLAYERS.getName()
									+ " WHERE playername='" + name + "';";
							Query rs = ks.getDatabaseHandler().select(query);
							// Check ResultSet
							boolean has = false;
							try
							{
								if (rs.getResult().next())
								{
									// Check if only received 1 entry
									if (rs.getResult().getInt(1) == 1)
									{
										// we have a single name
										has = true;
									}
									else if (rs.getResult().getInt(1) > 1)
									{
										sender.sendMessage(ChatColor.RED
												+ KarmicShare.TAG
												+ " Got more than one result. Possibly incomplete name?");
									}
									else
									{
										// Player not in database, therefore
										// error
										// on player part
										sender.sendMessage(ChatColor.RED
												+ KarmicShare.TAG + " Player "
												+ ChatColor.WHITE + name
												+ ChatColor.RED
												+ " not in database.");
										sender.sendMessage(ChatColor.RED
												+ KarmicShare.TAG
												+ " Player names are case sensitive.");
									}
								}
								else
								{
									// Error in query...
									sender.sendMessage(ChatColor.RED + KarmicShare.TAG
											+ " SQL query error");
								}
								rs.closeQuery();
							}
							catch (SQLException e)
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Could not set " + name + "'s karma");
								e.printStackTrace();
							}
							if (has)
							{
								int playerKarma = config.playerKarmaDefault;
								try
								{
									// Set to given amount by using the
									// difference
									// between the two
									playerKarma = amount
											- Karma.getPlayerKarma(name);
									Karma.updatePlayerKarma(name, playerKarma);
									if (config.playerKarmaDefault != 0)
									{
										// Default was non-zero, so re-update to
										// config's default
										Karma.updatePlayerKarma(name,
												config.playerKarmaDefault);
									}
									sender.sendMessage(ChatColor.YELLOW
											+ KarmicShare.TAG + " " + name
											+ "'s karma set");
								}
								catch (SQLException e)
								{
									sender.sendMessage(ChatColor.RED + KarmicShare.TAG
											+ "Could not set " + name
											+ "'s karma");
									e.printStackTrace();
								}

							}
						}
						else
						{
							// did not give a karma value, therefore error
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " No karma amount given.");
							return false;
						}
					}
					else
					{
						// did not give a player name, therefore error
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " No player name given.");
						return false;
					}
				}
				else
				{
					// Karma disabled
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Karma disabled.");
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: " + PermissionNode.ADMIN_SET.getNode());
				return true;
			}
		}
		// Admin command for groups
		else if (com.equals("group"))
		{
			// Check for second com
			if (args.length > 2)
			{
				final String groupCom = args[2];
				if (groupCom.equals("delete"))
				{
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_DELETE))
					{
						if (args.length > 3)
						{
							final String group = args[3].toLowerCase();
							if (group.equals("global"))
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Cannot remove the global group.");
							}
							if (Karma.validGroup(sender, group))
							{
								if (sender instanceof Player)
								{
									int i = ks
											.getServer()
											.getScheduler()
											.scheduleAsyncDelayedTask(
													ks,
													new ConfirmRemoveGroup(ks,
															(Player) sender,
															group));
									if (i == -1)
									{
										sender.sendMessage(ChatColor.YELLOW
												+ KarmicShare.TAG
												+ " Could not schedule task.");
									}
								}
								else
								{
									// Sent via console
									int i = ks
											.getServer()
											.getScheduler()
											.scheduleAsyncDelayedTask(
													ks,
													new RemoveGroupTask(ks, sender,
															group));
									if (i == -1)
									{
										sender.sendMessage(ChatColor.YELLOW
												+ KarmicShare.TAG
												+ " Could not schedule task.");
									}
									ks.getDatabaseHandler().standardQuery(
											"DELETE FROM "
													+ Table.ITEMS.getName()
													+ " WHERE groups='" + group
													+ "';");
									sender.sendMessage(KarmicShare.TAG
											+ " Removed all items of group: "
											+ group);
								}
							}
							else
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Group " + ChatColor.GRAY + group
										+ ChatColor.RED + " does not exist");
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Missing group name.");
							return false;
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED
								+ KarmicShare.TAG
								+ " Lack permission: " + PermissionNode.ADMIN_GROUP_DELETE.getNode());
						return true;
					}
				}
				else if (groupCom.equals("create"))
				{
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_CREATE))
					{
						if (args.length > 3)
						{
							final String group = args[3].toLowerCase();
							if (!Karma.validGroup(sender, group))
							{
								// Create group
								ks.getDatabaseHandler().standardQuery(
										"INSERT INTO " + Table.GROUPS.getName()
												+ " (groupname) VALUES ('"
												+ group + "');");
								sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
										+ " Group " + ChatColor.GRAY + group
										+ ChatColor.GREEN + " created");
							}
							else
							{
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Group " + ChatColor.GRAY + group
										+ ChatColor.RED + " already exists");
							}
						}
						else
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Missing group name.");
							return false;
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED
								+ KarmicShare.TAG
								+ " Lack permission: " + PermissionNode.ADMIN_GROUP_CREATE.getNode());
						return true;
					}
				}
				else if (groupCom.equals("add"))
				{
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_ADD))
					{
						try
						{
							String group = "";
							if (args.length > 3)
							{
								// Grab group name if given
								// force group names to lower case
								group = args[3].toLowerCase();
								if (!group.matches(GROUP_NAME_REGEX))
								{
									sender.sendMessage(ChatColor.RED
											+ KarmicShare.TAG
											+ " Group name must be alphanumeric");
									return true;
								}
							}
							else
							{
								// Group name was not given
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Admin command must specify group");
								return false;
							}
							if (args.length > 4)
							{
								for (int i = 4; i < args.length; i++)
								{
									String name = expandName(args[i]);
									if (name == null)
									{
										name = args[i];
									}
									if (Karma.playerHasGroup(sender, name,
											group))
									{
										sender.sendMessage(ChatColor.YELLOW
												+ KarmicShare.TAG + " " + ChatColor.AQUA
												+ name + ChatColor.YELLOW
												+ " is already in "
												+ ChatColor.GRAY + group);
										return true;
									}
									else
									{
										if (Karma.validGroup(sender, group))
										{
											// add player to group
											addPlayerToGroup(sender, name,
													group);
											sender.sendMessage(ChatColor.GREEN
													+ KarmicShare.TAG + " Added "
													+ ChatColor.GOLD + name
													+ ChatColor.GREEN + " to "
													+ ChatColor.GRAY + group);
											final Player p = ks.getServer()
													.getPlayer("name");
											if (p != null)
											{
												p.sendMessage(ChatColor.GREEN
														+ KarmicShare.TAG
														+ " You have been added to "
														+ ChatColor.GRAY
														+ group);
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED
													+ KarmicShare.TAG + " Group "
													+ ChatColor.GRAY + group
													+ ChatColor.RED
													+ " does not exist");
										}
									}
								}
							}
							else
							{
								// Player name was not given
								sender.sendMessage(ChatColor.RED
										+ KarmicShare.TAG
										+ " Admin command must specify player(s)");
								return false;
							}

						}
						catch (IndexOutOfBoundsException e)
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Player name not given");
							return false;
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED
								+ KarmicShare.TAG
								+ " Lack permission: " + PermissionNode.ADMIN_GROUP_CREATE.getNode());
					}
					return true;
				}
				else if (com.equals("remove"))
				{
					if (PermCheck.checkPermission(sender,
							PermissionNode.ADMIN_GROUP_REMOVE))
					{
						try
						{
							// Grab group name if given
							String group = "";
							if (args.length > 3)
							{
								// force group names to lower case
								group = args[3].toLowerCase();
								if (!group.matches(GROUP_NAME_REGEX))
								{
									sender.sendMessage(ChatColor.RED
											+ KarmicShare.TAG
											+ " Group name must be alphanumeric");
									return true;
								}
							}
							else
							{
								// Group name was not given
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Admin must specify group");
								return false;
							}
							if (args.length > 4)
							{
								for (int i = 4; i < args.length; i++)
								{
									String name = expandName(args[i]);
									if (name == null)
									{
										name = args[i];
									}
									if (!Karma.playerHasGroup(sender, name,
											group))
									{
										sender.sendMessage(ChatColor.YELLOW
												+ KarmicShare.TAG + ChatColor.AQUA
												+ name + ChatColor.YELLOW
												+ " not in " + ChatColor.GRAY
												+ group);
										return true;
									}
									else
									{
										if (Karma.validGroup(sender, group))
										{
											// remove other player to group
											removePlayerFromGroup(sender, name,
													group);
											sender.sendMessage(ChatColor.GREEN
													+ KarmicShare.TAG + " Removed "
													+ ChatColor.GOLD + name
													+ ChatColor.GREEN
													+ " from " + ChatColor.GRAY
													+ group);
											final Player p = ks.getServer()
													.getPlayer("name");
											if (p != null)
											{
												p.sendMessage(ChatColor.GREEN
														+ KarmicShare.TAG
														+ " You have been removed from "
														+ ChatColor.GRAY
														+ group);
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED
													+ KarmicShare.TAG + " Group "
													+ ChatColor.GRAY + group
													+ ChatColor.RED
													+ " does not exist");
										}
									}
								}
							}
							else
							{
								// Player name was not given
								sender.sendMessage(ChatColor.RED + KarmicShare.TAG
										+ " Admin must specify player(s)");
								return false;
							}

						}
						catch (IndexOutOfBoundsException e)
						{
							sender.sendMessage(ChatColor.RED + KarmicShare.TAG
									+ " Player name not given");
							return false;
						}
						return true;
					}
					else
					{
						sender.sendMessage(ChatColor.RED
								+ KarmicShare.TAG
								+ " Lack permission: " + PermissionNode.ADMIN_GROUP_REMOVE.getNode());
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Quietly updates the local cache of the item pool
	 */
	private void updateCache(CommandSender sender)
	{
		// Get list of items from database
		Query itemlist = ks.getDatabaseHandler().select(
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
					cache.put(i, itemlist.getResult().getInt("amount"));
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

	private void listMultipliers(CommandSender sender, int pageAdjust)
	{
		if (!config.karmaDisabled)
		{
			if (!config.statickarma)
			{
				// Add player to page hashmap, if they're not in it
				// so we know their position in the result list
				if (!multiPage.containsKey(sender.getName()))
				{
					multiPage.put(sender.getName(), 0);
				}
				else
				{
					// They already exist, so adjust if necessary
					if (pageAdjust != 0)
					{
						int adj = multiPage.get(sender.getName()).intValue()
								+ pageAdjust;
						multiPage.put(sender.getName(), adj);
					}
				}
				// Check if there is any entries in map
				if (!config.karma.isEmpty())
				{
					// Set hashmap to array
					Object[] array = config.karma.entrySet().toArray();
					int num = array.length / limit;
					double rem = (double) array.length % (double) limit;
					boolean valid = true;
					if (rem != 0)
					{
						num++;
					}
					if (multiPage.get(sender.getName()).intValue() < 0)
					{
						// They tried to use /ks prev when they're on page 0
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Page does not exist");
						// reset their current page back to 0
						multiPage.put(sender.getName(), 0);
						valid = false;
					}
					else if ((multiPage.get(sender.getName()).intValue())
							* limit > array.length)
					{
						// They tried to use /ks next at the end of the list
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ " Page does not exist");
						// Revert to last page
						multiPage.put(sender.getName(), num - 1);
						valid = false;
					}
					if (valid)
					{
						// Header with amount of pages
						sender.sendMessage(ChatColor.BLUE
								+ "==="
								+ ChatColor.GOLD
								+ "Karma Multiplier"
								+ ChatColor.BLUE
								+ "==="
								+ ChatColor.GREEN
								+ "Page: "
								+ ((multiPage.get(sender.getName()).intValue()) + 1)
								+ " of " + num + ChatColor.BLUE + "===");
						// list
						for (int i = ((multiPage.get(sender.getName())
								.intValue()) * limit); i < ((multiPage
								.get(sender.getName()).intValue()) * limit)
								+ limit; i++)
						{
							// Don't try to pull something beyond the bounds
							if (i < array.length)
							{
								@SuppressWarnings("unchecked")
								String out = ChatColor.WHITE
										+ "Item: "
										+ ChatColor.AQUA
										// Thanks to DiddiZ for id -> material
										// name
										// using built-in class
										+ ((Map.Entry<Item, Integer>) array[i])
												.getKey().name
										+ ChatColor.WHITE
										+ " Karma: "
										+ ChatColor.GOLD
										+ ((Map.Entry<Item, Integer>) array[i])
												.getValue()
										+ ChatColor.WHITE
										+ " ID: "
										+ ChatColor.LIGHT_PURPLE
										+ ((Map.Entry<Item, Integer>) array[i])
												.getKey().itemId()
										+ ChatColor.WHITE
										+ " Data: "
										+ ChatColor.LIGHT_PURPLE
										+ ((Map.Entry<Item, Integer>) array[i])
												.getKey().itemData();
								sender.sendMessage(out);
							}
							else
							{
								break;
							}
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.YELLOW
							+ KarmicShare.TAG
							+ " No karma multipliers, all items have karma value of "
							+ config.karmaChange);
				}
			}
			else
			{
				sender.sendMessage(ChatColor.YELLOW
						+ KarmicShare.TAG
						+ " Using static karma system, all items have karma value of "
						+ config.karmaChange);
			}
		}
		else
		{
			// Karma disabled
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Karma disabled.");
		}
	}

	/**
	 * Lists the items in the pool. Allows for pagination of the cache of items
	 * in pool.
	 * 
	 * @param CommandSender
	 *            of the "list" command so we know who we're outputting to
	 * @param Integer
	 *            of the page to change to, if needed. Zero shows current page.
	 */
	@SuppressWarnings("unchecked")
	private void listPool(CommandSender sender, int pageAdjust)
	{
		// Get list of items from database
		Query itemlist = ks.getDatabaseHandler().select(
				"SELECT * FROM " + Table.ITEMS.getName()
						+ " WHERE groups='global';");
		try
		{
			if (itemlist.getResult().next())
			{
				// Add player to page hashmap, if they're not in it
				// so we know their position in the result list
				if (!page.containsKey(sender.getName()))
				{
					page.put(sender.getName(), 0);
				}
				else
				{
					// They already exist, so adjust if necessary
					if (pageAdjust != 0)
					{
						int adj = page.get(sender.getName()).intValue()
								+ pageAdjust;
						page.put(sender.getName(), adj);
					}
				}
				// Clear all tool entry amounts to refresh properly
				Item[] toolClear = cache.keySet().toArray(new Item[0]);
				for (int i = 0; i < toolClear.length; i++)
				{
					if (toolClear[i].isTool())
					{
						cache.remove(toolClear[i]);
					}
				}
				// Loop that updates the hashmap cache
				// This way I won't be querying the database
				// every time list is called
				do
				{
					// update cache with current result set
					Item i = new Item(itemlist.getResult().getInt("itemid"),
							itemlist.getResult().getByte("data"), itemlist
									.getResult().getShort("durability"));
					if (i.isTool())
					{
						// add to current amount
						int itemAmount = itemlist.getResult().getInt("amount");
						if (cache.containsKey(i))
						{
							itemAmount += cache.get(i).intValue();
						}
						cache.put(i, itemAmount);
					}
					else
					{
						cache.put(i, itemlist.getResult().getInt("amount"));
					}
				} while (itemlist.getResult().next());

				// Set hashmap to array
				Object[] array = cache.entrySet().toArray();
				boolean valid = true;
				// Caluclate amount of pages
				int num = array.length / limit;
				double rem = (double) array.length % (double) limit;
				if (rem != 0)
				{
					num++;
				}
				if (page.get(sender.getName()).intValue() < 0)
				{
					// They tried to use /ks prev when they're on page 0
					sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
							+ " Page does not exist");
					// reset their current page back to 0
					page.put(sender.getName(), 0);
					valid = false;
				}
				else if ((page.get(sender.getName()).intValue()) * limit > array.length)
				{
					// They tried to use /ks next at the end of the list
					sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
							+ " Page does not exist");
					// Revert to last page
					page.put(sender.getName(), num - 1);
					valid = false;
				}
				if (valid)
				{
					// Header with amount of pages
					sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.GOLD
							+ "Item Pool" + ChatColor.BLUE + "==="
							+ ChatColor.GREEN + "Page: "
							+ ((page.get(sender.getName()).intValue()) + 1)
							+ " of " + num + ChatColor.BLUE + "===");
					// list
					for (int i = ((page.get(sender.getName()).intValue()) * limit); i < ((page
							.get(sender.getName()).intValue()) * limit) + limit; i++)
					{
						// Don't try to pull something beyond the bounds
						if (i < array.length)
						{
							StringBuilder sb = new StringBuilder();
							sb.append(ChatColor.WHITE + "Item: "
									+ ChatColor.AQUA
									// Thanks to DiddiZ for id -> material name
									// using built-in class
									+ ((Map.Entry<Item, Integer>) array[i])
											.getKey().name);
							sb.append(ChatColor.WHITE
									+ " Amount: "
									+ ChatColor.GOLD
									+ ((Map.Entry<Item, Integer>) array[i])
											.getValue());
							sb.append(ChatColor.WHITE
									+ " ID: "
									+ ChatColor.LIGHT_PURPLE
									+ ((Map.Entry<Item, Integer>) array[i])
											.getKey().itemId());
							sb.append(ChatColor.WHITE + " Data: "
									+ ChatColor.LIGHT_PURPLE);
							if (((Map.Entry<Item, Integer>) array[i]).getKey()
									.isPotion())
							{
								sb.append(((Map.Entry<Item, Integer>) array[i])
										.getKey().itemDurability());
							}
							else
							{
								sb.append(((Map.Entry<Item, Integer>) array[i])
										.getKey().itemData());
							}
							sender.sendMessage(sb.toString());
						}
						else
						{
							break;
						}
					}
				}
			}
			else
			{
				// No items in pool
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " No items in pool.");
				// Clear hashmap (for memory reasons?)
				// considering no items, therefore no pages,
				// and thus no need to know what page a player is on
				page.clear();
			}
			itemlist.closeQuery();
		}
		catch (SQLException e)
		{
			sender.sendMessage(ChatColor.RED + KarmicShare.TAG + "SQL error.");
			e.printStackTrace();
		}
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
				return (ChatColor.YELLOW + KarmicShare.TAG + ChatColor.RED + " Karma: " + karma);
			}
			else
			{
				// Not in lower percentage
				return (ChatColor.YELLOW + KarmicShare.TAG + " Karma: " + karma);
			}
		}
	}

	

	/**
	 * Attempts to look up full name based on who's on the server Given a
	 * partial name
	 * 
	 * @author Frigid, edited by Raphfrk and petteyg359
	 */
	private String expandName(String Name)
	{
		int m = 0;
		String Result = "";
		for (int n = 0; n < ks.getServer().getOnlinePlayers().length; n++)
		{
			String str = ks.getServer().getOnlinePlayers()[n].getName();
			if (str.matches("(?i).*" + Name + ".*"))
			{
				m++;
				Result = str;
				if (m == 2)
				{
					return null;
				}
			}
			if (str.equalsIgnoreCase(Name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1)
		{
			return null;
		}
		return Name;
	}
}