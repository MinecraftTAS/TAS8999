package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

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
				guildMessages.add(new ReactionRoleMessage(guild, Long.parseLong(split[1]), Long.parseLong(split[0]), split[2], Integer.parseInt(split[3])));
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
		ReactionRoleMessage newMessage=new ReactionRoleMessage(guild, channel, text, color);
		allMessages.get(guild.getIdLong()).add(newMessage);
		
		save(guild);
	}
	
	private void save(Guild guild) {
		File outFile=new File(folder, guild.getId()+fileExstension);
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
