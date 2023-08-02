package com.minecrafttas.tas8999.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class Util {

	public static MessageCreateData constructErrorMessage(Exception e) {
		String message = "The error has no message";
		if (e.getMessage() != null) {
			message = e.getMessage();
		}
		MessageCreateData msg = new MessageCreateBuilder().addEmbeds(
				new EmbedBuilder().setTitle("Error ._.")
				.addField(e.getClass().getSimpleName(), message, false).setColor(0xB90000).build()).build();
		return msg;
	}

}
