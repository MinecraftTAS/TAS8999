package com.minecrafttas.tas8999.modules;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.minecrafttas.tas8999.TAS8999.LOGGER;

/**
 * Discord spam/scam protection
 * @author Scribble
 */
public class SpamProtection extends TimerTask {

	private final List<UserData> suspiciousUsers;

	/**
	 * Initialize spam protection
	 */
	public SpamProtection() {
		this.suspiciousUsers = new ArrayList<>();

		var timer = new Timer("Spam Protection", true);
		timer.scheduleAtFixedRate(this, 0L,  10L);
	}

	/**
	 * Update spam protection every 10ms
	 */
	@Override
	public void run() {
		this.suspiciousUsers.removeIf(user -> (System.currentTimeMillis() - user.timestamp) > 10000);
	}

	public void checkMessage(Message msg) {
		// check for link
		if (!this.containsLink(msg))
			return;

		// check for valid member
		if (msg.getMember() == null)
			return;


		// get or create user data
		var author = msg.getAuthor();
		var guild = msg.getGuild();
		var userData = this.get(author);
		if (userData == null)
			suspiciousUsers.add(userData = new UserData(author, System.currentTimeMillis(), new ArrayList<>()));

		// register user message
		userData.messages().add(msg);

		// kick user on third post in separate channels
		var count = userData.messages().stream().map(Message::getChannel).distinct().count();
		if (count == 3) {
			LOGGER.info("Trying to kick {}", author.getName());

			guild.kick(msg.getMember()).reason("Spam protection").queue();
			userData.messages().forEach(c -> c.delete().queue());

			suspiciousUsers.remove(userData);
		}

	}

	private record UserData(User user, long timestamp, List<Message> messages) {}

	/**
	 * Check if message contains link
	 * @param message Message
	 * @return Contains link
	 */
	private boolean containsLink(Message message) {
		var content = message.getContentStripped();
		if (content.isEmpty())
			return false;

		var words = content.split(" ");

		for (var word : words)
			if (word.matches("https?://.+\\..+"))
				return true;

		return false;
	}

	/**
	 * Get user data from user
	 * @param user User
	 * @return User data or null
	 */
	private UserData get(User user) {
		for (UserData userData : this.suspiciousUsers)
			if (userData.user.getIdLong() == user.getIdLong())
				return userData;

		return null;
	}

}
