package com.mitsugaru.KarmicShare;

import java.util.EnumMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;

import com.mitsugaru.KarmicShare.config.LocalizeConfig;

public enum LocalString
{
	PERMISSION_DENY(LocalizeConfig.permissionDeny), LACK_MESSAGE(
			LocalizeConfig.lackMessage), ECONOMY_FAILURE(
			LocalizeConfig.econFailure), UNKNOWN_COMMAND(
			LocalizeConfig.unknownCommand), RELOAD_CONFIG(
			LocalizeConfig.reloadConfig), HELP_HELP(LocalizeConfig.helpHelp), HELP_VERSION(
			LocalizeConfig.helpVersion), HELP_ADMIN_RELOAD(
			LocalizeConfig.helpAdminReload), LOCAL_MESSAGE(LocalizeConfig.localMessage);

	private String string;

	private LocalString(String s)
	{
		this.string = s;
	}

	public String parseString(EnumMap<Flag, String> replace)
	{
		/**
		 * Thanks to @Njol for the following
		 * http://forums.bukkit.org/threads/multiple-classes-config-colours.79719/#post-1154761
		 */
		String out = ChatColor.translateAlternateColorCodes('&', string);
		if (replace != null)
		{
			for (Entry<Flag, String> entry : replace.entrySet())
			{
				out = out.replaceAll(entry.getKey().getFlag(), entry.getValue());
			}
		}
		return out;
	}

	public enum Flag
	{
		NAME("%name"), EVENT("%event"), REASON("%reason"), EXTRA("%extra"), TAG(
				"%tag"), AMOUNT("%amount");

		private String flag;

		private Flag(String flag)
		{
			this.flag = flag;
		}

		public String getFlag()
		{
			return flag;
		}
	}
}
