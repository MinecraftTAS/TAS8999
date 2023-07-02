package com.minecrafttas.tas8999;

import java.io.File;

import org.slf4j.Logger;

import com.minecrafttas.tas8999.util.Storable;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

/**
 * Messages with reactions that give the user the roles when reacting
 * 
 * @author Scribble
 *
 */
public class ReactionRoles extends Storable {

	private String separator;
	
	public ReactionRoles(Logger logger) {
		super("Reaction Roles", new File("reactionsroles"), logger);
	}

	public void createNewMessage(SlashCommandInteractionEvent event) {

	}

	public void addOption(SlashCommandInteractionEvent event, String messageID, Emoji emote, Role role, String description) {
		
	}

	public void editOption(SlashCommandInteractionEvent event, String messageID, Emoji emote, Role role, String description) {
		
	}
	
	/**
	 * Mark an existing bot message to be a reactionrole message
	 * @param event
	 */
	public void linkMessage(SlashCommandInteractionEvent event, String messageID) {
		
	}
	
	public void removeOption(SlashCommandInteractionEvent event, Emoji emote) {
	}
	
	public boolean onReactionAdd(MessageReactionAddEvent event) {
		return false;
	}
	
	public boolean onReactionRemove(MessageReactionRemoveEvent event) {
		return false;
	}
	
	public void onDelete(Guild guild, long l) {
		
	}
}
