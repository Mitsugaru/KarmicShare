package com.mitsugaru.KarmicShare;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PermCheck {

	public PermCheck()
	{
	}

	public boolean checkPermission(CommandSender sender, String node)
	{
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("PermissionsEx"))
		{
			//Pex only supports player check, no CommandSender objects
			if(sender instanceof Player)
			{
				final Player p = (Player) sender;
				final PermissionManager permissions = PermissionsEx.getPermissionManager();
				//Handle pex check
				if(permissions.has(p, node))
				{
					return true;
				}
			}
		}
		//If not using PEX, OR if sender is not a player
		//Attempt to use SuperPerms
		if(sender.hasPermission(node))
		{
			return true;
		}
		//Else, they don't have permission
		return false;
	}
}
