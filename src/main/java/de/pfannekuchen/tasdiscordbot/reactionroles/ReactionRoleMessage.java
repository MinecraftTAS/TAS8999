package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class ReactionRoleMessage {

	private long messageId;
	
	private long guildId;
	
	private List<Pair<EmoteWrapper, RoleWrapper>> reactionPairs=new ArrayList<>();
	
	private int color;
	
	
	public ReactionRoleMessage(Guild guild, MessageChannel channel, String argumentText, int color) throws IllegalArgumentException{
		String[] args=argumentText.split(",");
		
		for (String arg : args) {
			arg=arg.trim();
			String[] argPairs=arg.split(" ", 2);
			
			EmoteWrapper emote=new EmoteWrapper(argPairs[0]);
			
			RoleWrapper role=new RoleWrapper(guild, argPairs[1]);
			
			Pair<EmoteWrapper, RoleWrapper> emoteRole = Pair.of(emote, role);
			
			reactionPairs.add(emoteRole);
		}
		
		this.color=color;
		
		this.guildId=guild.getIdLong();
		
		Message msg=new MessageBuilder().setEmbeds(embed(argumentText, color, channel)).build();
		
		sendMessageWithReactions(channel, msg);
	}
	
	private void sendMessageWithReactions(MessageChannel channel, Message message) throws IllegalArgumentException{
		
		CompletableFuture<Message> queuedMessage=channel.sendMessage(message).submit();
		
		queuedMessage.whenComplete((msg, staging) -> {
			reactionPairs.forEach(pair -> {
				EmoteWrapper emote=pair.getLeft();
				
				if(emote.isUnicode()) {
					msg.addReaction(emote.getId()).queue();
				}
				else {
					Emote customEmote=channel.getJDA().getEmoteById(emote.getId());
					if(customEmote!=null) {
						msg.addReaction(customEmote).queue();
					}
				}
			});
			messageId=msg.getIdLong();
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
	
	public long getGuildId() {
		return guildId;
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
}
