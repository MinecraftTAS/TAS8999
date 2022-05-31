package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.pfannekuchen.tasdiscordbot.TASDiscordBot;
import de.pfannekuchen.tasdiscordbot.util.Util;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;

/**
 * Reaction roles
 * 
 * @author Scribble
 *
 */
public class ReactionRoles {
	
	private final File folder=new File("reactionroles/");
	
	private final String fileExstension=".rr";
	
	private HashMap<Long,List<ReactionRoleMessage>> allMessages=new HashMap<>();
	
	private int color;
	
	public ReactionRoles(List<Guild> list, int color) {
		folder.mkdir();
		clearUnusedFiles(list);
		try {
			initialize(list);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.color=color;
	}
	
	private void initialize(List<Guild> list) throws IOException {
		for(Guild guild : list) {
			File file=new File(folder, guild.getId()+fileExstension);
			if(!file.exists()) {
				file.createNewFile();
				allMessages.put(guild.getIdLong(), new ArrayList<ReactionRoleMessage>());
			}else {
				List<ReactionRoleMessage> messageList = readMessages(guild, file);
				allMessages.put(guild.getIdLong(), messageList);
			}
		}
	}

	private List<ReactionRoleMessage> readMessages(Guild guild, File file) throws IOException {
			List<ReactionRoleMessage> guildMessages=new ArrayList<>();
			List<String> lines=FileUtils.readLines(file, StandardCharsets.UTF_8);
			for (String line : lines) {
				String[] split=line.split("\\|");
				
				ReactionRoleMessage newmessage;
				
				long messageId=Long.parseLong(split[0]);
				
				long channelId=Long.parseLong(split[1]);
				
				String argText=split[2];
				
				int color = Integer.parseInt(split[3]);
				
				try {
					guild.getChannelById(TextChannel.class, channelId).retrieveMessageById(messageId);
					newmessage=new ReactionRoleMessage(guild, channelId, argText, color, messageId);
				}catch (Exception e) {
					continue;
				}
				
				guildMessages.add(newmessage);
			}
		return guildMessages;
	}

	private void clearUnusedFiles(List<Guild> list) {
		for(File fileInFolder : folder.listFiles((dir, name) -> name.endsWith(fileExstension))) {
			for(Guild guild : list) {
				if(fileInFolder.getName().equals(guild.getId())) {
					fileInFolder.delete();
				}
			}
		}
	}

	public void addNewMessage(Guild guild, MessageChannel channel, String text) throws Exception {
		System.out.println(String.format("[RR] Adding new message with arguments: %s", text));
		if(text.isEmpty()) {
			throw new IllegalArgumentException("The arguments can't be empty");
		}
		
		ReactionRoleMessage newMessage=new ReactionRoleMessage(guild, channel.getIdLong(), text, color);
		channel.sendMessage(newMessage.getMessage()).submit().whenComplete((msg, throwable) ->{
			newMessage.getReactions().forEach(emote -> {
				if(emote.isUnicode()) {
					msg.addReaction(emote.getId()).queue();
				}else {
					msg.addReaction(emote.getEmote()).queue();
				}
			});
			newMessage.setMessageId(msg.getIdLong());
			allMessages.get(guild.getIdLong()).add(newMessage);
		});
	}
	
	public void editMessage(Guild guild, MessageChannel channel, long messageId, String text) throws Exception {
		System.out.println("[RR] Editing message: "+messageId);
		ReactionRoleMessage newMessage = new ReactionRoleMessage(guild, channel.getIdLong(), text, color, messageId);
		
		channel.retrieveMessageById(messageId).submit().whenComplete((msg, throwable) ->{
			
			if(!Util.isThisUserThisBot(msg.getAuthor())) {
				return;
			}
			//Remove the reactions
			for (MessageReaction reaction : msg.getReactions()) {
				reaction.removeReaction(TASDiscordBot.getBot().getJDA().getSelfUser()).queue();
			}
			
			//Edit the message
			msg.editMessage(newMessage.getMessage()).queue();
			
			//Add the reactions
			newMessage.getReactions().forEach(emote -> {
				if(emote.isUnicode()) {
					msg.addReaction(emote.getId()).queue();
				}else {
					msg.addReaction(emote.getEmote()).queue();
				}
			});
			
			//Update all messages
			List<ReactionRoleMessage> rrmessages = allMessages.get(guild.getIdLong());
			
			List<ReactionRoleMessage> copy = new ArrayList<>(rrmessages);
			
			for (Iterator<ReactionRoleMessage> iterator = copy.iterator(); iterator.hasNext();) {
				ReactionRoleMessage reactionRoleMessage = (ReactionRoleMessage) iterator.next();
				
				if(reactionRoleMessage.getMessageId()==messageId) {
					rrmessages.remove(reactionRoleMessage);
					rrmessages.add(newMessage);
					break;
				}
				
				if(!iterator.hasNext()) {
					rrmessages.add(newMessage);
				}
			}
			
		});
	}
	
	private void save(Guild guild) {
		File outFile=new File(folder, guild.getId()+fileExstension);
		if(outFile.exists()) {
			outFile.delete();
		}
		List<ReactionRoleMessage> messages=allMessages.get(guild.getIdLong());
		try {
			FileUtils.writeLines(outFile, StandardCharsets.UTF_8.name(), messages);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void removeMessage(Message messageToRemove) {
		removeMessage(messageToRemove.getGuild(), messageToRemove.getIdLong());
	}
	
	public void removeMessage(Guild guild, long messageToRemove) {
		List<ReactionRoleMessage> messagesList = new ArrayList<ReactionRoleMessage>(allMessages.get(guild.getIdLong()));
		for(ReactionRoleMessage message : messagesList) {
			if(message.getMessageId()==messageToRemove) {
				System.out.println(String.format("[RR] Removing message: %s", messageToRemove));
				allMessages.get(guild.getIdLong()).remove(message);
			}
		}
		save(guild);
	}
	
	public String getRole(Guild guild, long messageId, String emoteIn) {
		List<ReactionRoleMessage> messages = allMessages.get(guild.getIdLong());
		for(ReactionRoleMessage message : messages) {
			if(messageId==message.getMessageId()) {
				return message.getRole(emoteIn);
			}
		}
		return "";
	}

	public void onShutDown(List<Guild> guilds) {
		guilds.forEach(guild-> save(guild));
	}

}
