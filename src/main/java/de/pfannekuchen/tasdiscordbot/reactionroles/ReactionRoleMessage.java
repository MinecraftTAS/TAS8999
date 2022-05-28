package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class ReactionRoleMessage {

	private long channelId;
	
	private Message msg;
	
	private long messageId;
	
	private List<Pair<EmoteWrapper, RoleWrapper>> reactionPairs=new ArrayList<>();
	
	private int color;
	
	
	public ReactionRoleMessage(Guild guild, MessageChannel channel, String argumentText, int color) throws Exception{

		constructReactionPairs(guild, argumentText);
		this.color=color;
		
		this.channelId=channel.getIdLong();
		
		msg=new MessageBuilder().setEmbeds(embed(argumentText, color, channel)).build();
	}
	
	public ReactionRoleMessage(Guild guild, long channelId, long messageId, String argumentText, int color) throws Exception {
		MessageChannel channel=(MessageChannel) guild.getGuildChannelById(channelId);
		
		//Test if message exists
		channel.retrieveMessageById(messageId).complete();
		
		this.channelId=channelId;
		this.messageId=messageId;
		constructReactionPairs(guild, argumentText);
		this.color=color;
	}
	
	private void constructReactionPairs(Guild guild, String argumentText) {
		String[] args=argumentText.split(",");
		
		for (String arg : args) {
			arg=arg.trim();
			String[] argPairs=arg.split(" ", 2);
			
			EmoteWrapper emote=new EmoteWrapper(argPairs[0]);
			
			RoleWrapper role=new RoleWrapper(guild, argPairs[1]);
			
			if(!guild.getBotRole().canInteract(guild.getRoleById(role.getId()))) {
				throw new HierarchyException("The bot doesn't have access to this role: "+role);
			}
			
			Pair<EmoteWrapper, RoleWrapper> emoteRole = Pair.of(emote, role);
			
			reactionPairs.add(emoteRole);
		}
	}

	public CompletableFuture<Message> sendMessageWithReactions(MessageChannel channel) throws IllegalArgumentException{
		
		CompletableFuture<Message> queuedMessage=channel.sendMessage(msg).submit();
		
		return queuedMessage.whenComplete((message, staging) -> {
			reactionPairs.forEach(pair -> {
				EmoteWrapper emote=pair.getLeft();
				
				if(emote.isUnicode()) {
					message.addReaction(emote.getId()).queue();
				}
				else {
					Emote customEmote=channel.getJDA().getEmoteById(emote.getId());
					if(customEmote!=null) {
						message.addReaction(customEmote).queue();
					}
				}
			});
			messageId=message.getIdLong();
		});
		
		
	}
	
	private MessageEmbed embed(String text, int color, MessageChannel channel) {
		EmbedBuilder builder=new EmbedBuilder();
		
		if(reactionPairs.size()>1) {
			builder.setTitle("React with the following emote(s) to get the role(s)");
		} 
		else {
			builder.setTitle("React with the following emote to get the role");
		}
		String roleList="";
		
		for(Pair<EmoteWrapper, RoleWrapper> pair: reactionPairs) {
			roleList=roleList.concat(String.format("%s : %s \n", pair.getLeft(), pair.getRight()));
		}
		
		builder.addField("", roleList, false);
		builder.setColor(color);
		
		return builder.build();
	}
	
	public long getMessageId() {
		return messageId;
	}
	
	public int getColor() {
		return color;
	}
	
	public boolean containsEmote(String emoteId) {
		for(Pair<EmoteWrapper, RoleWrapper> pair : reactionPairs) {
			if(pair.getLeft().getId().equals(emoteId)) {
				return true;
			}
		}
		return false;
	}
	
	public String getRole(String emoteId) {
		for(Pair<EmoteWrapper, RoleWrapper> pair : reactionPairs) {
			if(pair.getLeft().getId().equals(emoteId)) {
				return pair.getRight().getId();
			}
		}
		return "";
	}
	
	@Override
	public String toString() {
		String out="";
		
		out=out.concat(Long.toString(messageId)+"|"+Long.toString(channelId)+"|");
		
		for (Iterator<Pair<EmoteWrapper, RoleWrapper>> iterator = reactionPairs.iterator(); iterator.hasNext();) {
			Pair<EmoteWrapper, RoleWrapper> pair = iterator.next();
			String seperator=iterator.hasNext() ? "," : "|";
			out=out.concat(pair.getLeft()+" "+pair.getRight()+seperator);
		}
		
		out=out.concat(Integer.toString(color));
		
		return out;
	}
}
