package com.mitsugaru.KarmicShare.prompts;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.tasks.RemoveGroupTask;

public class RemoveGroupQuestion extends Question
{

	@Override
	protected Prompt acceptValidatedInput(ConversationContext context,
			boolean input)
	{
		final KarmicShare plugin = (KarmicShare) context
				.getSessionData("plugin");
		final String group = (String) context.getSessionData("group");
		final int groupId = Karma.getGroupId(group);
		if (input)
		{
			if (groupId != -1)
			{
				context.getForWhom().sendRawMessage(
						ChatColor.YELLOW + KarmicShare.TAG
								+ " This could take a while...");
				int i = plugin
						.getServer()
						.getScheduler()
						.scheduleAsyncDelayedTask(
								plugin,
								new RemoveGroupTask(plugin, (Player) context
										.getForWhom(), group));
				if (i == -1)
				{
					context.getForWhom().sendRawMessage(
							ChatColor.YELLOW + KarmicShare.TAG
									+ " Could not schedule task");
				}
				plugin.getDatabaseHandler().standardQuery(
						"DELETE FROM " + Table.ITEMS.getName()
								+ " WHERE groups='" + groupId + "';");
				context.getForWhom().sendRawMessage(
						ChatColor.YELLOW + KarmicShare.TAG
								+ " Removed all items of group: "
								+ ChatColor.GOLD + group);
			}
			else
			{
				context.getForWhom().sendRawMessage(
						ChatColor.YELLOW + KarmicShare.TAG
								+ ChatColor.DARK_AQUA
								+ " Unknown group id for " + ChatColor.GOLD
								+ group);
			}
		}
		else
		{
			context.getForWhom()
					.sendRawMessage(
							ChatColor.YELLOW + KarmicShare.TAG
									+ ChatColor.DARK_AQUA
									+ " Cancelled removal of " + ChatColor.GOLD
									+ group);
		}
		return END_OF_CONVERSATION;
	}

}
