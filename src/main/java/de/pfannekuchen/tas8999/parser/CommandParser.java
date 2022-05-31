package de.pfannekuchen.tas8999.parser;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class CommandParser {

	private final String command;
	private String defaultMessage;
	private HashMap<String, String> exceptions = new HashMap<>();
	
	protected CommandParser(String command) {
		this.command = command;
	}
	
	public MessageEmbed run(TextChannel channel, User caller) {
		/* Get the Message to print out, by going through every exception */
		String message = defaultMessage;
		for (Entry<String, String> exception : exceptions.entrySet()) 
			if (exception.getKey().equalsIgnoreCase("#" + channel.getName())) 
				message = exception.getValue();
		
		MessageEmbed msg = new EmbedBuilder().addField("/" + command + " - Results", message.split("\\[#")[0].replaceAll("%s", caller.getName()).replaceAll("%n", "#" + channel.getName()), true).build();
		return msg;
	}
	
	/**
	 * All this Code does is parse the File to get a custom message for every channel.
	 */
	public static CommandParser parseMessage(String alias, String message) {
		final CommandParser command = new CommandParser(alias);
		
		/* Parse Command Exceptions */
		final Pattern pattern = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)", Pattern.DOTALL);
		final Matcher matcher = pattern.matcher(message);
		String result;
		String channel;
		String channelmessage;
		while (matcher.find()) {
			result = matcher.group();
			channel = result.substring(1).split("\\]\\(")[0];
			channelmessage = result.split("\\]\\(")[1].split("\\)")[0];
			System.out.println("[CommandParser] Using a different message for " + channel + ": " + channelmessage.replace('\n', '\\'));
			command.addException(channel, channelmessage);
			message = message.replaceFirst(result, "");
		}
		
		command.defaultMessage = message;
		
		return command;
	}
	
	public String getMessage() {
		return defaultMessage;
	}
	
	public void addException(String channel, String message) {
		exceptions.put(channel, message);
	}
	
	public String getCommand() {
		return command;
	}
	
}
