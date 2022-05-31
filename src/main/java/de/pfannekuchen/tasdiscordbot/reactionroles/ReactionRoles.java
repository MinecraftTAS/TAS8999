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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Messages with reactions that give the user the roles when reacting
 * 
 * @author Scribble
 *
 */
public class ReactionRoles {

	private final File folder = new File("reactionroles/");

	private final String fileExstension = ".rr";

	private HashMap<Long, List<ReactionRoleMessage>> allMessages = new HashMap<>();

	private int color;

	public ReactionRoles(List<Guild> list, int color) {
		// Make sure the folder exists
		folder.mkdir();
		clearUnusedFiles(list);
		try {
			initialize(list);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.color = color;
	}

	/**
	 * Adds a new reactionrole message
	 * @param guild The guild
	 * @param channel The channel to send it to
	 * @param argText The arguments for this reactionrole
	 * @throws Exception
	 */
	public void addNewMessage(Guild guild, MessageChannel channel, String argText) throws Exception {
		System.out.println(String.format("[RR] Adding new message with arguments: %s", argText));
		
		//Check if the arguments are empty
		if (argText.isEmpty()) {
			throw new IllegalArgumentException("The arguments can't be empty");
		}

		//Construct a new message
		ReactionRoleMessage newMessage = new ReactionRoleMessage(guild, channel.getIdLong(), argText, color);
		
		//Send the new message to the channel
		channel.sendMessage(newMessage.getMessage()).submit().whenComplete((msg, throwable) -> {
			
			//Add the reactions when the message is done sending
			newMessage.getReactions().forEach(emote -> {
				// Due to discord not distinguishing correctly between unicode and custom emote, you have to do this stuff 
				if (emote.isUnicode()) {
					msg.addReaction(emote.getId()).queue();
				} else {
					msg.addReaction(emote.getEmote()).queue();
				}
			});
			
			//Set the message id, since you can only get the id after a message has been sent
			newMessage.setMessageId(msg.getIdLong());
			//Add it to the list of messages
			allMessages.get(guild.getIdLong()).add(newMessage);
		});
	}

	/**
	 * Updates a given message with a new message
	 * @param guild
	 * @param channel
	 * @param messageId
	 * @param text
	 * @throws Exception
	 */
	public void editMessage(Guild guild, MessageChannel channel, long messageId, String text) throws Exception {
		System.out.println("[RR] Editing message: " + messageId);
		ReactionRoleMessage newMessage = new ReactionRoleMessage(guild, channel.getIdLong(), text, color, messageId);

		channel.retrieveMessageById(messageId).submit().whenComplete((msg, throwable) -> {

			if (!Util.isThisUserThisBot(msg.getAuthor())) {
				return;
			}
			// Remove the reactions
			for (MessageReaction reaction : msg.getReactions()) {
				reaction.removeReaction(TASDiscordBot.getBot().getJDA().getSelfUser()).queue();
			}

			// Edit the message
			msg.editMessage(newMessage.getMessage()).queue();

			// Add the reactions
			newMessage.getReactions().forEach(emote -> {
				if (emote.isUnicode()) {
					msg.addReaction(emote.getId()).queue();
				} else {
					msg.addReaction(emote.getEmote()).queue();
				}
			});

			// Update all messages
			List<ReactionRoleMessage> rrmessages = allMessages.get(guild.getIdLong());

			List<ReactionRoleMessage> copy = new ArrayList<>(rrmessages);

			// Remove the old message and add the new one
			for (Iterator<ReactionRoleMessage> iterator = copy.iterator(); iterator.hasNext();) {
				ReactionRoleMessage reactionRoleMessage = (ReactionRoleMessage) iterator.next();

				if (reactionRoleMessage.getMessageId() == messageId) {
					rrmessages.remove(reactionRoleMessage);
					rrmessages.add(newMessage);
					break;
				}

				if (!iterator.hasNext()) {
					rrmessages.add(newMessage);
				}
			}
			save(guild);
		});
	}

	/**
	 * Saves the current messages from a guild to a file
	 * @param guild
	 */
	private void save(Guild guild) {
		File outFile = new File(folder, guild.getId() + fileExstension);
		if (outFile.exists()) {
			outFile.delete();
		}
		List<ReactionRoleMessage> messages = allMessages.get(guild.getIdLong());
		try {
			FileUtils.writeLines(outFile, StandardCharsets.UTF_8.name(), messages);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes a message from the list
	 * @param messageToRemove The message to remove
	 */
	public void removeMessage(Message messageToRemove) {
		removeMessage(messageToRemove.getGuild(), messageToRemove.getIdLong());
	}

	/**
	 * Removes a message from the list
	 * @param guild The guild
	 * @param messageToRemove The message to remove
	 */
	public void removeMessage(Guild guild, long messageToRemove) {
		List<ReactionRoleMessage> messagesList = new ArrayList<ReactionRoleMessage>(allMessages.get(guild.getIdLong()));
		for (ReactionRoleMessage message : messagesList) {
			if (message.getMessageId() == messageToRemove) {
				System.out.println(String.format("[RR] Removing message: %s", messageToRemove));
				allMessages.get(guild.getIdLong()).remove(message);
			}
		}
		save(guild);
	}

	/**
	 * Retrieves the role for the given emote
	 * @param guild The guild
	 * @param messageId The message
	 * @param emoteIn The emote
	 * @return The role associated with the emote
	 */
	public String getRole(Guild guild, long messageId, String emoteIn) {
		List<ReactionRoleMessage> messages = allMessages.get(guild.getIdLong());
		for (ReactionRoleMessage message : messages) {
			if (messageId == message.getMessageId()) {
				return message.getRole(emoteIn);
			}
		}
		return "";
	}

	/**
	 * Executed when the bot is shutting down
	 * @param guilds
	 */
	public void onShutDown(List<Guild> guilds) {
		guilds.forEach(guild -> save(guild));
	}

	// =============================================Serialization
	
	/**
	 * Checks if a guild removed the bot and removed the associated reactionrole file
	 * @param list
	 */
	private void clearUnusedFiles(List<Guild> list) {
		for (File fileInFolder : folder.listFiles((dir, name) -> name.endsWith(fileExstension))) {
			for (Guild guild : list) {
				if (fileInFolder.getName().equals(guild.getId())) {
					fileInFolder.delete();
				}
			}
		}
	}

	/**
	 * Establishes a link between the messages and the bot
	 * @param list The list of guilds to initialize from
	 * @throws IOException
	 */
	private void initialize(List<Guild> list) throws IOException {
		for (Guild guild : list) {
			File file = new File(folder, guild.getId() + fileExstension);
			if (!file.exists()) {
				file.createNewFile();
				allMessages.put(guild.getIdLong(), new ArrayList<ReactionRoleMessage>());
			} else {
				List<ReactionRoleMessage> messageList = readMessages(guild, file);
				allMessages.put(guild.getIdLong(), messageList);
			}
		}
	}

	/**
	 * Deserialises the messages
	 * @param guild The guild
	 * @param file The file to read
	 * @return A list of ReactionRole messages for the guild
	 * @throws IOException
	 */
	private List<ReactionRoleMessage> readMessages(Guild guild, File file) throws IOException {
		
		List<ReactionRoleMessage> guildMessages = new ArrayList<>();
		
		List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
		
		for (String line : lines) {
			
			String[] split = line.split("\\|");

			ReactionRoleMessage newmessage;

			long messageId = Long.parseLong(split[0]);
			long channelId = Long.parseLong(split[1]);
			String argText = split[2];
			int color = Integer.parseInt(split[3]);

			try {
				guild.getChannelById(TextChannel.class, channelId).retrieveMessageById(messageId);
				newmessage = new ReactionRoleMessage(guild, channelId, argText, color, messageId);
			} catch (Exception e) {
				//If the message doesn't exist anymore or the reading of the message fails, ignore it and move to the next line
				continue;
			}

			guildMessages.add(newmessage);
		}
		return guildMessages;
	}

}
