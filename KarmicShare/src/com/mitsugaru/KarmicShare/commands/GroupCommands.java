package com.mitsugaru.KarmicShare.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.permissions.PermCheck;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;

public class GroupCommands
{
	private static KarmicShare plugin;

	public static void init(KarmicShare ks)
	{
		plugin = ks;
	}
	
	public static void parseCommand(CommandSender sender, String[] args)
	{
			if (args.length > 1)
			{
				// They have a parameter, thus
				// parse in adminCommand method
				if (!groupCommand(sender, args))
				{
					// Bad command
					sender.sendMessage(ChatColor.RED
							+ KarmicShare.TAG
							+ " Syntax error. Use /ks group for list of commands");
				}
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
				sender.sendMessage(ChatColor.GREEN
						+ "/ks group set <group>" + ChatColor.YELLOW
						+ " : Set current group");
			}
	}

	// TODO need to also check against if they are creator/manager of group
	// TODO group settings
	//TODO list groups, highlight public versus private groups
	public static boolean groupCommand(CommandSender sender, String[] args)
	{
		// Show group commands help menu
		final String com = args[1];
		// Add generated items to pool
		if (com.equals("create"))
		{
			if (!PermCheck.checkPermission(sender, PermissionNode.GROUP_CREATE))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: "
						+ PermissionNode.GROUP_CREATE.getNode());
				return true;
			}
			try
			{
				// force group names to lower case
				final String group = args[2].toLowerCase();
				if (!group.matches(Karma.GROUP_NAME_REGEX))
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
				else if (!Karma.validGroup(sender, group))
				{
					// Create group
					plugin.getDatabaseHandler().standardQuery(
							"INSERT INTO " + Table.GROUPS.getName()
									+ " (groupname) VALUES ('" + group + "');");
					sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
							+ " Group " + ChatColor.GRAY + group
							+ ChatColor.GREEN + " created");
					if (sender instanceof Player)
					{
						// add player to group
						Karma.addPlayerToGroup(sender,
								((Player) sender).getName(), group);
						sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
								+ " Added " + ChatColor.GOLD
								+ ((Player) sender).getName() + ChatColor.GREEN
								+ " to " + ChatColor.GRAY + group);
					}
					else
					{
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
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
			catch (IndexOutOfBoundsException e)
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Group name not given");
				return false;
			}
			return true;
		}
		else if (com.equals("add"))
		{
			if (!PermCheck.checkPermission(sender, PermissionNode.GROUP_ADD))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: "
						+ PermissionNode.GROUP_ADD.getNode());
				return true;
			}
			// Grab group name
			String group = "";
			if (args.length > 2)
			{
				// force group names to lower case
				group = args[2].toLowerCase();
				if (group.startsWith("self_"))
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Cannot remove players from the self group.");
					return true;
				}
				else if (group.equalsIgnoreCase("global"))
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Cannot add players from global group.");
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
			if (!group.matches(Karma.GROUP_NAME_REGEX))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Group name must be alphanumeric");
				return true;
			}
			if (args.length <= 3)
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Must specify player");
				return false;
			}
			// Grab all names
			for (int i = 3; i < args.length; i++)
			{
				String name = plugin.expandName(args[i]);
				if (name == null)
				{
					name = args[i];
				}
				else if (Karma.playerHasGroup(sender, name, group))
				{
					sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG + " "
							+ ChatColor.AQUA + name + ChatColor.YELLOW
							+ " is already in " + ChatColor.GRAY + group);
					return true;
				}
				else
				{
					if (Karma.validGroup(sender, group))
					{
						// Grab player on server
						Player other = plugin.getServer().getPlayer(name);
						if (other != null)
						{
							// add other player to group
							Karma.addPlayerToGroup(sender, other.getName(),
									group);
							sender.sendMessage(ChatColor.GREEN
									+ KarmicShare.TAG + " Added "
									+ ChatColor.GOLD + name + ChatColor.GREEN
									+ " to " + ChatColor.GRAY + group);
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
			return true;
		}
		else if (com.equals("remove"))
		{
			if (!PermCheck.checkPermission(sender, PermissionNode.GROUP_REMOVE))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Lack permission: "
						+ PermissionNode.GROUP_REMOVE.getNode());
				return true;
			}
			String group = "";
			if (args.length <= 2)
			{
				// Group name was not given
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Must specify group");
				return false;
			}
			// Grab group name if given
			// force group names to lower case
			group = args[2].toLowerCase();
			if (group.startsWith("self_"))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Cannot remove players from the self group.");
				return true;
			}
			else if (group.equalsIgnoreCase("global"))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Cannot remove players from global group.");
				return true;
			}
			if (sender instanceof Player)
			{
				if (!Karma.playerHasGroup(sender, ((Player) sender).getName(),
						group))
				{
					sender.sendMessage(ChatColor.RED
							+ KarmicShare.TAG
							+ " Cannot remove players from groups you're not in.");
					return true;
				}
			}
			if (!group.matches(Karma.GROUP_NAME_REGEX))
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Group name must be alphanumeric");
				return true;
			}
			if (args.length > 3)
			{
				// Player name was not given
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Must specify player");
				return false;
			}
			for (int i = 3; i < args.length; i++)
			{
				String name = plugin.expandName(args[i]);
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
						Karma.removePlayerFromGroup(sender, name, group);
						sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
								+ " Removed " + ChatColor.GOLD + name
								+ ChatColor.GREEN + " from " + ChatColor.GRAY
								+ group);
						final Player p = plugin.getServer().getPlayer(name);
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
			return true;
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
					if (!group.matches(Karma.GROUP_NAME_REGEX))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Group name must be alphanumeric");
						return true;
					}
					else if (group.equalsIgnoreCase("global"))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Cannot leave the global group.");
						return true;
					}
					else if (group.startsWith("self_"))
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Cannot leave the self group.");
						return true;
					}
					else if (Karma.validGroup(sender, group))
					{
						// remove other player to group
						Karma.removePlayerFromGroup(sender, sender.getName(),
								group);
						sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
								+ " Removed " + ChatColor.GOLD
								+ sender.getName() + ChatColor.GREEN + " from "
								+ ChatColor.GRAY + group);
					}
					else
					{
						sender.sendMessage(ChatColor.RED + KarmicShare.TAG
								+ " Group '" + ChatColor.GRAY + group
								+ ChatColor.RED + "' does not exist");
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
		else if (com.equals("set"))
		{
			if (args.length > 2)
			{
				String group = args[2].toLowerCase();
				if (Karma.validGroup(sender, group))
				{
					boolean valid = false;
					if (Karma.playerHasGroup(sender, sender.getName(), group))
					{
						valid = true;
					}
					else if (PermCheck.checkPermission(sender,
							PermissionNode.IGNORE_GROUP))
					{
						valid = true;
					}
					if (valid)
					{
						Karma.selectedGroup.put(sender.getName(), group);
						sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
								+ " Set group to " + ChatColor.GRAY + group);
					}
					else
					{
						sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
								+ ChatColor.AQUA + sender.getName()
								+ ChatColor.YELLOW + " not in "
								+ ChatColor.GRAY + group);
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + KarmicShare.TAG
							+ " Group '" + ChatColor.GRAY + group
							+ ChatColor.RED + "' does not exist");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + KarmicShare.TAG
						+ " Must specify a group");
			}
			return true;
		}
		return false;
	}
}
