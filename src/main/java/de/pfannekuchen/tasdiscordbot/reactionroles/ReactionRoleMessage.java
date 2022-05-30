package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.pfannekuchen.tasdiscordbot.util.Triple;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class ReactionRoleMessage {

	private long channelId;
	
	private long messageId;
	
	private List<Triple<EmoteWrapper, String, RoleWrapper>> reactionPairs=new ArrayList<>();
	
	private int color;
	
	
	public ReactionRoleMessage(Guild guild, long channelId, String argumentText, int color) throws Exception{
		constructReactionPairs(guild, argumentText);
		this.color=color;
		
		this.channelId=channelId;
	}
	
	public ReactionRoleMessage(Guild guild, long channelId, String argumentText, int color, long messageId) throws Exception {
		MessageChannel channel=(MessageChannel) guild.getGuildChannelById(channelId);

		// Test if message exists
		try {
			channel.retrieveMessageById(messageId).complete();
		} catch (Exception e) {
			throw new IllegalArgumentException("The specified message does not exist");
		}
		
		this.channelId=channelId;
		this.messageId=messageId;
		constructReactionPairs(guild, argumentText);
		this.color=color;
	}
	
	private void constructReactionPairs(Guild guild, String argumentText) {
		String[] args=argumentText.split(",");
		
		for (String arg : args) {
			arg=arg.trim();
			String[] argTriples=arg.split(" ", 3);
			
			EmoteWrapper emote=new EmoteWrapper(argTriples[0]);
			
			RoleWrapper role=new RoleWrapper(guild, argTriples[1]);
			
			String description=argTriples[2];
			
			if(!guild.getBotRole().canInteract(guild.getRoleById(role.getId()))) {
				throw new HierarchyException("The bot doesn't have access to this role: "+role);
			}
			
			Triple<EmoteWrapper, String, RoleWrapper> emoteRole = Triple.of(emote, description, role);
	
			reactionPairs.add(emoteRole);
		}
	}

	public Message getMessage() throws IllegalArgumentException{
		return new MessageBuilder(embed()).build();
	}
	
	public List<EmoteWrapper> getReactions(){
		List<EmoteWrapper> out=new ArrayList<>();
		reactionPairs.forEach(triple -> {
			out.add(triple.getLeft());
		});
		return out;
	}
	
	public void setMessageId(long messageId) {
		this.messageId=messageId;
	}
	
	private EmbedBuilder embed() {
		EmbedBuilder builder=new EmbedBuilder();
		
		if(reactionPairs.size()>1) {
			builder.setTitle("React with the following emote(s) to get the role(s)");
		} 
		else {
			builder.setTitle("React with the following emote to get the role");
		}
		String roleList="";
		
		for(Triple<EmoteWrapper, String, RoleWrapper> triple: reactionPairs) {
			roleList=roleList.concat(String.format("%s -> %s: %s\n", triple.getLeft(), triple.getRight(), triple.getMiddle()));
		}
		
		builder.addField("", roleList, false);
		builder.setColor(color);
		
		return builder;
	}
	
	public long getMessageId() {
		return messageId;
	}
	
	public int getColor() {
		return color;
	}
	
	public boolean containsEmote(String emoteId) {
		for(Triple<EmoteWrapper, String, RoleWrapper> triple : reactionPairs) {
			if(triple.getLeft().getId().equals(emoteId)) {
				return true;
			}
		}
		return false;
	}
	
	public String getRole(String emoteId) {
		for(Triple<EmoteWrapper, String, RoleWrapper> triple : reactionPairs) {
			if(triple.getLeft().getId().equals(emoteId)) {
				return triple.getRight().getId();
			}
		}
		return "";
	}
	
	@Override
	public String toString() {
		String out="";
		
		out=out.concat(Long.toString(messageId)+"|"+Long.toString(channelId)+"|");
		
		for (Iterator<Triple<EmoteWrapper, String, RoleWrapper>> iterator = reactionPairs.iterator(); iterator.hasNext();) {
			Triple<EmoteWrapper, String, RoleWrapper> triple = iterator.next();
			String seperator=iterator.hasNext() ? "," : "|";
			out=out.concat(triple.getLeft()+" "+triple.getRight()+" "+triple.getMiddle()+seperator);
		}
		
		out=out.concat(Integer.toString(color));
		
		return out;
	}
}
