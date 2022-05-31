package de.pfannekuchen.tas8999.util;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class SpamProtection {
	SpamProtectionThread spamthread = new SpamProtectionThread();

	List<UserData> suspiciousUsers = new ArrayList<>();

	public SpamProtection() {
		spamthread.start();
	}

	public void checkMessage(Message msg) {
		if (containsLink(msg)) {
			User msgAuthor = msg.getAuthor();
			if (msg.getMember() == null) {
				System.out.println("[TAS8999] "+msgAuthor.getName() + " member is null");
			}
			if (isAlreadySus(msgAuthor)) {
				UserData data = get(msgAuthor);
				data.addMsg(msg);
				if (data.channelchanges == 3) {
					Guild guild = msg.getGuild();
					System.out.println("[TAS8999] Trying to kick " + msgAuthor.getName());
					guild.kick(msg.getMember(), "Spam protection").queue();
					data.pruneMsgs();
					suspiciousUsers.remove(data);
				}
			} else {
				suspiciousUsers.add(new UserData(msgAuthor, msg));
			}
		}
	}

	private void update() {
		for (UserData userData : new ArrayList<>(suspiciousUsers)) {
			userData.update();
			if (userData.cooldown <= 0) {
				suspiciousUsers.remove(userData);
			}
		}
	}

	public static boolean containsLink(Message msg) {
		String content = msg.getContentStripped();
		if (content.isEmpty())
			return false;

		String[] words = content.split(" ");

		for (String word : words) {
			if (word.matches("https?://.+\\..+")) {
				return true;
			}
		}
		return false;
	}

	private boolean isAlreadySus(User author) {
		return get(author) != null;
	}

	private UserData get(User author) {
		for (UserData userData : suspiciousUsers) {
			if (userData.user.getIdLong() == author.getIdLong()) {
				return userData;
			}
		}
		return null;
	}

	class SpamProtectionThread extends Thread {

		public SpamProtectionThread() {
			setDaemon(true);
			setName("SpamProtectionThread");
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				update();
			}
		}
	}

	class UserData {
		User user;
		int cooldown;
		List<Message> messages = new ArrayList<>();
		int channelchanges = 0;
		MessageChannel prevChannel = null;

		public UserData(User user, Message msg) {
			this.user = user;
			this.cooldown = 1000;
			prevChannel = msg.getChannel();
			channelchanges++;
			messages.add(msg);
		}

		public void update() {
			cooldown--;
		}

		public void addMsg(Message msg) {
			if (msg.getChannel() != prevChannel) {
				prevChannel = msg.getChannel();
				channelchanges++;
				messages.add(msg);
			}
		}

		public void pruneMsgs() {
			messages.forEach(msg -> {
				msg.delete().queue();
			});
		}
	}
}
