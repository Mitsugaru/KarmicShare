package com.mitsugaru.KarmicShare;

import java.util.Enumeration;
import java.util.Vector;

public class KSQuestionsReaper implements Runnable
{
	private final Vector<KSQuestion> questions;

	public KSQuestionsReaper(Vector<KSQuestion> questions) {
		this.questions = questions;
	}

	@Override
	public void run() {
		final Enumeration<KSQuestion> enm = questions.elements();
		while (enm.hasMoreElements()) {
			final KSQuestion question = enm.nextElement();
			if (question.isExpired())
				questions.remove(question);
		}
	}
}
