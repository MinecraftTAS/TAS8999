package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.pfannekuchen.tasdiscordbot.TASDiscordBot;
import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

/**
 * Combines emojis and custom emotes
 * 
 * @author Scribble
 *
 */
public class EmoteWrapper {

	private boolean isUnicode;

	private String id = "";

	private static final Pattern pattern = Pattern.compile("<:.*:(\\d+)>");

	private String printName = "";

	public EmoteWrapper(String idIn) {

		if (!isEmoteAvailable(idIn)) {
			throw new IllegalArgumentException(String.format("The bot can't access this emote: %s", idIn));
		}

		if (EmojiUtils.isEmoji(idIn)) {
			isUnicode = true;
			id = idIn;
			printName = idIn;
		} else {
			id = extractId(idIn);
			printName = idIn;
		}
	}

	public boolean isUnicode() {
		return isUnicode;
	}

	public String getId() {
		if (isUnicode()) {
			return id;
		} else {
			return TASDiscordBot.getBot().getJDA().getEmoteById(id).getId();
		}
	}

	
	@Override
	public String toString() {
		return printName;
	}

	public static boolean isEmoteAvailable(String emoteId) {
		if (!EmojiUtils.isEmoji(emoteId)) {
			return TASDiscordBot.getBot().getJDA().getEmoteById(extractId(emoteId)) != null;
		} else {
			return true;
		}
	}

	private static String extractId(String idIn) {
		Matcher matcher = pattern.matcher(idIn);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "";
		}
	}
	
	public static String getReactionEmoteId(ReactionEmote emoteIn) {
		if(emoteIn.isEmoji()) {
			return emoteIn.getEmoji();
		}
		else if (emoteIn.isEmote()) {
			return emoteIn.getEmote().getId();
		}
		return "";
	}
	
	public Emote getEmote() {
		if(!isUnicode) {
			return TASDiscordBot.getBot().getJDA().getEmoteById(id);
		}
		return null;
	}
}
