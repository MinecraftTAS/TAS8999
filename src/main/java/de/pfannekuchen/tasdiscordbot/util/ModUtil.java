package de.pfannekuchen.tasdiscordbot.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public class ModUtil {
	
	public static void sendMessage(MessageChannel channel, String message) {
		channel.sendMessage(message).queue();
	}
	
	public static void deleteMessage(Message msg, String reason) {
		msg.delete().reason(reason).queue();
	}
	
}
