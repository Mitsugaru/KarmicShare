package com.mitsugaru.KarmicShare.prompts;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.logic.Karma;

public class DrainQuestion extends Question
{

	@Override
	protected Prompt acceptValidatedInput(ConversationContext context,
			boolean valid)
	{
		final KarmicShare plugin = (KarmicShare) context
				.getSessionData("plugin");
		final String group = (String) context.getSessionData("group");
		if (valid)
		{
			// Wipe table
			final int id = Karma.getGroupId(group);
			if (id != -1)
			{

				final String query = "DELETE FROM " + Table.ITEMS.getName()
						+ " WHERE groups='" + id + "';";
				plugin.getDatabaseHandler().standardQuery(query);
				plugin.getLogger().info(
						"'" + group + "'" + " items table cleared");
				context.getForWhom().sendRawMessage(
						ChatColor.GREEN + KarmicShare.TAG + " "
								+ ChatColor.GOLD + group + ChatColor.GREEN
								+ " item pool emptied.");
			}
			else
			{
				context.getForWhom().sendRawMessage(
						ChatColor.YELLOW + KarmicShare.TAG + " "
								+ ChatColor.GOLD + group + ChatColor.YELLOW
								+ " id not found.");
			}
		}
		else
		{
			context.getForWhom().sendRawMessage(
					ChatColor.YELLOW + KarmicShare.TAG + " Drain cancelled.");
		}
		return END_OF_CONVERSATION;
	}

}
