package de.diddiz.LogBlockQuestioner;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Question
{
	private String answer = null;
	private final HashMap<Integer, String> answers;
	private final String questionMessage;
	private final Player respondent;
	private final int respondentHash;
	private final long start;

	public Question(Player respondent, String questionMessage, String[] answers) {
		start = System.currentTimeMillis();
		this.respondent = respondent;
		respondentHash = respondent.getName().hashCode();
		this.questionMessage = questionMessage;
		this.answers = new HashMap<Integer, String>(answers.length);
		for (final String ans : answers)
			this.answers.put(ChatColor.stripColor(ans).toLowerCase().hashCode(), ans);
	}

	public synchronized String ask() {
		final StringBuilder options = new StringBuilder();
		for (final String ans : answers.values())
			options.append(ChatColor.WHITE +"/" + ans + ChatColor.WHITE + ", ");
		options.delete(options.length() - 2, options.length());
		respondent.sendMessage(questionMessage);
		respondent.sendMessage("- " + options + "?");
		try {
			this.wait();
		} catch (final InterruptedException ex) {
			answer = "interrupted";
		}
		return answer;
	}

	synchronized boolean isExpired() {
		if (System.currentTimeMillis() - start > 300000) {
			answer = "timed out";
			notify();
			return true;
		}
		return false;
	}

	boolean isPlayerQuestioned(int playerNameHash) {
		return playerNameHash == respondentHash;
	}

	boolean isRightAnswer(int answerHash) {
		return answers.containsKey(answerHash);
	}

	synchronized void returnAnswer(int answerHash) {
		answer = ChatColor.stripColor(answers.get(answerHash));
		notify();
	}
}
