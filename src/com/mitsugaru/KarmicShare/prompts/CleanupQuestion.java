package com.mitsugaru.KarmicShare.prompts;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;

public class CleanupQuestion extends Question
{

	@Override
	protected Prompt acceptValidatedInput(ConversationContext context,
			boolean valid)
	{
		final KarmicShare plugin = (KarmicShare) context
				.getSessionData("plugin");
		if(valid)
		{
			plugin.getDatabaseHandler().standardQuery(
					"DELETE FROM " + Table.ITEMS.getName()
							+ " WHERE amount<='0';");
			plugin.getLogger()
					.info("Cleanup task executed.");
			context.getForWhom().sendRawMessage(ChatColor.GREEN + KarmicShare.TAG
					+ " Cleanup task executed.");
		}
		else
		{
			context.getForWhom().sendRawMessage(ChatColor.YELLOW + KarmicShare.TAG
					+ " Cleanup task cancelled.");
		}
		return null;
	}

}
