package com.mitsugaru.KarmicShare.prompts;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.logic.Karma;

public class PlayerKarmaResetQuestion extends Question
{

	@Override
	protected Prompt acceptValidatedInput(ConversationContext context,
			boolean valid)
	{
		final KarmicShare plugin = (KarmicShare) context
				.getSessionData("plugin");
		final String name = (String) context.getSessionData("name");
		if (valid)
		{
			Karma.setPlayerKarma(name,
					plugin.getPluginConfig().playerKarmaDefault);
			context.getForWhom().sendRawMessage(
					ChatColor.GREEN + KarmicShare.TAG + " " + name
							+ "'s karma reset");
		}
		else
		{
			context.getForWhom().sendRawMessage(
					ChatColor.YELLOW + KarmicShare.TAG + ChatColor.DARK_AQUA
							+ " Karma reset for " + ChatColor.GOLD + name
							+ ChatColor.DARK_AQUA + " cancelled.");
		}
		return END_OF_CONVERSATION;
	}

}
