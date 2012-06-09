package com.mitsugaru.KarmicShare.prompts;

import org.bukkit.conversations.BooleanPrompt;
import org.bukkit.conversations.ConversationContext;

public abstract class Question extends BooleanPrompt
{

	@Override
	public String getPromptText(ConversationContext context)
	{
		return (String) context.getSessionData("question");
	}

}
