package de.pfannekuchen.tasdiscordbot.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.pfannekuchen.tasdiscordbot.TASDiscordBot;
import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class Util {
	
	public static void sendMessage(MessageChannel channel, String message) {
		channel.sendMessage(message).queue();
	}
	
	public static void sendMessage(MessageChannel channel, Message message) {
		channel.sendMessage(message).queue();
	}
	
	public static void sendDeletableMessage(MessageChannel channel, String message) {
		channel.sendMessage(message).queue(msg-> msg.addReaction(EmojiUtils.emojify(":x:")).queue());
	}
	
	public static void sendDeletableMessage(MessageChannel channel, Message message) {
		channel.sendMessage(message).queue(msg-> msg.addReaction(EmojiUtils.emojify(":x:")).queue());
	}
	
	public static void deleteMessage(Message msg) {
		msg.delete().queue();
	}
	
	public static void deleteMessage(Message msg, String reason) {
		msg.delete().reason(reason).queue();
	}
	
	public static boolean hasRole(Member member, String... roleNames) {
		List<Role> roles=member.getRoles();
		
		for(String rolename : roleNames) {
			for(Role role : roles) {
				if(role.getName().equalsIgnoreCase(rolename)) {
					return true;
				}
			}
		}
	
		return false;
	}
	
	public static boolean isThisUserThisBot(User user) {
		return user.getJDA().getSelfUser().getIdLong() == user.getIdLong();
	}
	
	public static boolean hasEditPerms(Member member) {
		return member.hasPermission(Permission.MESSAGE_MANAGE);
	}
	
	public static boolean hasBotReactedWith(Message msg, String emote) {
		for(MessageReaction reaction : msg.getReactions()) {
			ReactionEmote rEmote=reaction.getReactionEmote();
			if(rEmote.isEmoji()){
				if(rEmote.getEmoji().equals(emote)) {
					return reaction.isSelf();
				}
			} else if(rEmote.isEmote()) {
				if(rEmote.getEmote().getName().equals(emote)) {
					return reaction.isSelf();
				}
			}
		}
		return false;
	}

	public static void sendErrorMessage(MessageChannel channel, Exception e) {
		Message msg=new MessageBuilder(new EmbedBuilder().setTitle("Error ._.").addField(e.getClass().getSimpleName(), e.getMessage(), false).setColor(0xB90000)).build();
		sendDeletableMessage(channel, msg);
	}
	
}
