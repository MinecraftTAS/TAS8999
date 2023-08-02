package com.minecrafttas.tas8999.modules;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.minecrafttas.tas8999.TAS8999;
import org.slf4j.Logger;

import com.minecrafttas.tas8999.util.Storable;
import com.minecrafttas.tas8999.util.Util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.utils.tuple.Pair;

/**
 * Messages with reactions that give the user the roles when reacting
 * 
 * @author Scribble
 * @deprecated Onboarding will replace reaction roles
 */
@Deprecated
public class ReactionRoles extends Storable {

	private String separator = ";:";

	public ReactionRoles(Logger logger) {
		super("Reaction Roles", new File("reactionsrole"), logger);
	}

	public void createNewMessage(SlashCommandInteractionEvent event) {
		LOGGER.info("{{}} Created reactionrole message", event.getGuild().getName());
		event.deferReply().queue(defermsg -> {
			MessageChannel channel = event.getChannel();
			EmbedBuilder builder = createEmbed(null);
			MessageCreateBuilder msg = new MessageCreateBuilder();
			msg.addEmbeds(builder.build());
			Util.sendMessage(channel, msg.build());
			defermsg.deleteOriginal().queue();
		}, failure -> {
			Util.sendErrorMessage(event.getMessageChannel(), failure);
			failure.printStackTrace();
		});
	}

	public void upsertOption(SlashCommandInteractionEvent event, String messageID, EmojiUnion emoji, Role role, String description) {
		LOGGER.info("{{}} Added reactionrole {}", event.getGuild().getName(), emoji.getFormatted());
		MessageChannel channel = event.getMessageChannel();
		Guild guild = event.getGuild();
		channel.retrieveMessageById(messageID).queue(msg -> {

			HashMap<EmojiUnion, Pair<Role, String>> reactions = getReactionsForMessage(guild, messageID);
			reactions.put(emoji, Pair.of(role, description));
			setReactionsForMessage(guild, messageID, reactions);

			if(!Util.isThisUserThisBot(msg.getAuthor())) {
				LOGGER.info("{{}} Added reaction role to a message not from the bot", guild.getName());
			} else {
				EmbedBuilder embed = createEmbed(reactions);
				channel.editMessageEmbedsById(messageID, embed.build()).queue();
			}

			boolean hasReacted = false;
			for (MessageReaction react : msg.getReactions()) {
				if (react.getEmoji().equals(emoji)) {
					hasReacted = true;
				}
			}
			if (!hasReacted)
				msg.addReaction(emoji).queue();

			Util.sendReply(event, "Added/updated reactionrole", true);

		});
	}

	public void removeOption(SlashCommandInteractionEvent event, String messageID, Emoji emoji) {
		LOGGER.info("{{}} Removed reactionrole {}", event.getGuild().getName(), emoji.getFormatted());
		MessageChannel channel = event.getMessageChannel();
		Guild guild = event.getGuild();
		channel.retrieveMessageById(messageID).queue(msg -> {

			HashMap<EmojiUnion, Pair<Role, String>> reactions = getReactionsForMessage(guild, messageID);
			reactions.remove(emoji);
			setReactionsForMessage(guild, messageID, reactions);

			if(!Util.isThisUserThisBot(msg.getAuthor())) {
				LOGGER.info("{{}} Removed reaction role from a message not from the bot", guild.getName());
			} else {
				EmbedBuilder embed = createEmbed(reactions);
				channel.editMessageEmbedsById(messageID, embed.build()).queue();
			}

			boolean hasReacted = false;
			for (MessageReaction react : msg.getReactions()) {
				if (react.getEmoji().equals(emoji)) {
					hasReacted = true;
				}
			}
			if (hasReacted)
				msg.removeReaction(emoji).queue();

			Util.sendReply(event, "Removed reactionrole", true);

		}, failure -> {
			Util.sendErrorMessage(channel, failure);
			failure.printStackTrace();
		});
	}

	public boolean onReactionAdd(MessageReactionAddEvent event) {
		LinkedHashMap<EmojiUnion, Pair<Role, String>> reactions = getReactionsForMessage(event.getGuild(), event.getMessageId());
		if (reactions == null || !reactions.containsKey(event.getEmoji())) {
			return false;
		}
		Pair<Role, String> pair = reactions.get(event.getEmoji());
		Role role = pair.getLeft();
		try {
			event.getGuild().addRoleToMember(event.getMember(), role).queue();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean onReactionRemove(MessageReactionRemoveEvent event) {	
		if(event.getUser()==null) {
			return false;
		}
		LinkedHashMap<EmojiUnion, Pair<Role, String>> reactions = getReactionsForMessage(event.getGuild(), event.getMessageId());
		if (reactions == null || Util.isThisUserThisBot(event.getUser()) || !reactions.containsKey(event.getEmoji())) {
			return false;
		}
		Pair<Role, String> pair = reactions.get(event.getEmoji());
		Role role = pair.getLeft();
		try {
			event.getGuild().removeRoleFromMember(event.getMember(), role).queue();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void onDelete(Guild guild, String msgId) {
		if(containsKey(guild, msgId)) {
			LOGGER.info("{{}} Removed rr message {}", guild.getName(), msgId);
			remove(guild, msgId);
		}
	}

	private LinkedHashMap<EmojiUnion, Pair<Role, String>> deserializeReactionRoles(Guild guild, String value) {
		LinkedHashMap<EmojiUnion, Pair<Role, String>> out = new LinkedHashMap<>();
		String[] split = value.split("\n");
		for (String reaction : split) {
			String[] rolesplit = reaction.split(separator, 3);
			EmojiUnion emote = Emoji.fromData(DataObject.fromJson(rolesplit[0]));
			Role role = guild.getRoleById(rolesplit[1]);
			out.put(emote, Pair.of(role, rolesplit[2]));
		}
		return out;
	}

	private String serializeReactionRoles(HashMap<EmojiUnion, Pair<Role, String>> reactions) {
		String out = "";

		Set<EmojiUnion> keySet = reactions.keySet();

		for (EmojiUnion emoji : keySet) {
			Pair<Role, String> val = reactions.get(emoji);
			out += emoji.toData().toString() + separator;
			out += val.getLeft().getId() + separator;
			out += val.getRight() + "\n";
		}

		return out;
	}

	private EmbedBuilder createEmbed(HashMap<EmojiUnion, Pair<Role, String>> reactions) {
		EmbedBuilder builder = new EmbedBuilder();

		builder.setColor(TAS8999.getBot().color);

		if (reactions == null || reactions.isEmpty()) {
			builder.setTitle("React with the following emote to get the role");
			builder.setDescription("*This reaction role message is empty. Use `/reactionrole upsert` to add options to this message*");
			return builder;
		}

		builder.setTitle(String.format("React with the following emote%s to get the role%s", reactions.size() > 1 ? "s" : "", reactions.size() > 1 ? "s" : ""));

		Set<EmojiUnion> keySet = reactions.keySet();

		for (EmojiUnion emoji : keySet) {
			Pair<Role, String> val = reactions.get(emoji);
			builder.addField("", String.format("%s->%s: %s", emoji.getFormatted(), val.getLeft().getAsMention(), val.getRight()), false);
		}

		return builder;
	}

	private LinkedHashMap<EmojiUnion, Pair<Role, String>> getReactionsForMessage(Guild guild, String messageID) {
		String value = get(guild, messageID);
		if (value == null || value.isEmpty()) {
			return new LinkedHashMap<>();
		}

		return deserializeReactionRoles(guild, value);
	}

	private void setReactionsForMessage(Guild guild, String messageID, HashMap<EmojiUnion, Pair<Role, String>> reactions) {
		put(guild, messageID, serializeReactionRoles(reactions));
	}
}
