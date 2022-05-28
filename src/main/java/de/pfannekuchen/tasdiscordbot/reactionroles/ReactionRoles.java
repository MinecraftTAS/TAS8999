package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

/**
 * Reaction roles
 * 
 * @author Scribble
 *
 */
public class ReactionRoles {
	
	private List<ReactionRoleMessage> messages=new ArrayList<>();
	
	private int color;
	
	public ReactionRoles(int color) {
		this.color=color;
	}
	
	public void addNewMessage(Guild guild, MessageChannel channel, String text) throws IllegalArgumentException {
		ReactionRoleMessage newMessage=new ReactionRoleMessage(guild, channel, text, color);
		messages.add(newMessage);
	}
	
	public boolean isReactionMessage(long messageId, String emoteIn) {
		
		for(ReactionRoleMessage message : messages) {
			if(messageId==message.getMessageId()) {
				return message.containsEmote(emoteIn);
			}
		}
		return false;
	}
	
	public String getRole(long messageId, String emoteIn) {
		for(ReactionRoleMessage message : messages) {
			if(messageId==message.getMessageId()) {
				return message.getRole(emoteIn);
			}
		}
		return "";
	}
}
