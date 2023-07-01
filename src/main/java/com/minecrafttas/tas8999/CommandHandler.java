package com.minecrafttas.tas8999;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;

import com.minecrafttas.tas8999.util.MD2Embed;
import com.minecrafttas.tas8999.util.Storable;
import com.minecrafttas.tas8999.util.Util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.interactions.command.CommandImpl;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class CommandHandler extends Storable {
	
	/**
	 * Separator for the command description and the command body
	 */
	private String separator = ";:";
	
	public CommandHandler(Logger logger) {
		super("Commands", new File("commands"), logger);
	}
	
	@Override
	public void loadForGuild(Guild guild) {
		super.loadForGuild(guild);
		Properties prop = getGuildProperty(guild);
		prop.forEach((key, value) -> {
			String strKey = (String) key;
			LOGGER.info("{{}} Loading custom command: {}", guild.getName(), strKey);
			String strValue = (String) value;
			String[] split = strValue.split(separator, 3);
			upsertCommand(guild, strKey, split[1], split[2]);
		});
	}

	public void upsertCommand(SlashCommandInteractionEvent event, String name, String description, String markdown) {
		
		Guild guild = event.getGuild();
		
		if(!containsKey(guild, name)) {
			guild.retrieveCommands().queue(commands -> {
				for(Command command : commands) {
					LOGGER.info("Checking {}", command.getName());
					if(command.getName().equals(name)) {
						Util.sendErrorReply(event, "Error","Command name is already in use", false);
						return;
					}
					addCommand(event, name, description, markdown);
				}
			});
		} else {
			addCommand(event, name, description, markdown);
		}
	}
	
	public void upsertCommand(Guild guild, String name, String description, String markdown) {
		if(!containsKey(guild, name)) {
			guild.retrieveCommands().queue(commands -> {
				for(Command command : commands) {
					if(command.getName() == name) {
						LOGGER.error("Command name is already in use");
						return;
					}
					addCommand(guild, name, description, markdown);
				}
			});
		} else {
			addCommand(guild, name, description, markdown);
		}
	}
	
	private void addCommand(Guild guild, String name, String description, String markdown) {
		guild.upsertCommand(name, description).queue(command -> {
			put(guild, name, command.getId()+separator+description+separator+markdown);
		});
	}
	
	private void addCommand(SlashCommandInteractionEvent event, String name, String description, String markdown) {
		
		Guild guild = event.getGuild();
		
		guild.upsertCommand(name, description).queue(command -> {
			put(guild, name, command.getId()+separator+description+separator+markdown);
			
			MessageCreateBuilder message = new MessageCreateBuilder();
			EmbedBuilder builder = null;
			try {
				builder = MD2Embed.parseEmbed(markdown, TAS8999.getBot().color);
			} catch (Exception e) {
				Util.sendErrorReply(event, e, false);
				return;
			}
			message.addEmbeds(builder.build());
			message.setContent("Added new command: /"+name);
			
			Util.sendSelfDestructingReply(event, message.build(), 10);
		}, fail->{
			Util.sendErrorReply(event, "Error", fail.getMessage(), true);
		});
	}
	
	public boolean executeCommand(SlashCommandInteractionEvent event, String name) throws Exception {
		Guild guild = event.getGuild();
		if(!containsKey(guild, name)) {
			return false;
		}
		
		String markdown = getMarkdownByName(guild, name);
		
		if(markdown.isEmpty()) {
			Util.sendErrorReply(event, "Error", "The command body is empty", false);
			return true;
		}
		
		MessageCreateBuilder builder = new MessageCreateBuilder();
		EmbedBuilder embed = MD2Embed.parseEmbed(markdown, TAS8999.getBot().color);
		builder.addEmbeds(embed.build());
		
		Util.sendReply(event, builder.build(), false);
		
		return true;
	}
	
	public void removeCommand(SlashCommandInteractionEvent event, String name) {
		Guild guild = event.getGuild();
		String commandID = getIdByName(guild, name);
		guild.deleteCommandById(commandID).queue(command -> {
			remove(guild, name);
			Util.sendReply(event, "Deleted custom command `"+name+"`", false);
		});
	}
	
	public String getIdByName(Guild guild, String name) {
		String value = get(guild, name);
		String[] split = value.split(separator, 3);
		return split[0];
	}
	
	public String getDescriptionByName(Guild guild, String name) {
		String value = get(guild, name);
		String[] split = value.split(separator, 3);
		return split[1];
	}
	
	public String getMarkdownByName(Guild guild, String name) {
		String value = get(guild, name);
		String[] split = value.split(separator, 3);
		return split[2];
	}

	public void renameCommand(SlashCommandInteractionEvent event, String name, String newName) {
		Guild guild = event.getGuild();
		guild.editCommandById(getIdByName(guild, name)).setName(newName).queue();
		Properties prop = getGuildProperty(guild);
		String value = prop.getProperty(name);
		prop.remove(name);
		prop.put(newName, value);
		putGuildProperty(guild, prop);
		Util.sendReply(event, "Renamed command `" + name + "` to `"+newName+"`", false);
	}
}
