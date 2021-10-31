package de.pfannekuchen.tasdiscordbot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import de.pfannekuchen.tasdiscordbot.core.Rcon;
import de.pfannekuchen.tasdiscordbot.parser.CommandParser;
import de.pfannekuchen.tasdiscordbot.util.ModUtil;
import de.pfannekuchen.tasdiscordbot.util.SpamProtection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.api.events.message.priv.GenericPrivateMessageEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class TASDiscordBot extends ListenerAdapter implements Runnable {

	private final JDA jda;
	private final Properties configuration;
	private final List<String> blacklist;
	private final ArrayList<CommandParser> commands;
	private final SpamProtection protecc=new SpamProtection();
	
	@Override
	public void onGenericPrivateMessage(GenericPrivateMessageEvent event) {
		for (Role role : jda.getGuildById(373166430478401555L).retrieveMember(event.getChannel().getUser()).complete().getRoles()) {
			if (role.getIdLong() == 776544617956769802L) {
				Message msg = event.getChannel().retrieveMessageById(event.getMessageIdLong()).complete();
				if (SpamProtection.containsLink(msg) && !msg.getAuthor().isBot()) {
					if (new File("submissions").exists()) {
						event.getChannel().sendMessage("Your TAS Competition Submission has been changed to:\n " + msg.getContentStripped()).complete();
						File f = new File("submissions/" + event.getChannel().getUser().getName().toLowerCase());
						try {
							f.createNewFile();
							Files.write(f.toPath(), msg.getContentStripped().getBytes(StandardCharsets.UTF_8));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onGenericGuildMessage(GenericGuildMessageEvent event) {
		try {
			event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
				protecc.checkMessage(msg);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
				String[] words = msg.getContentRaw().split(" ");
				for (String word : words) {
					for (String blacklistedWord : blacklist) {
						if (word.toLowerCase().contains(blacklistedWord.toLowerCase())) {
							ModUtil.deleteMessage(msg, "Blacklisted Word");
							return;
						}
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onGenericGuildMessage(event);
	}
	
	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		for (CommandParser cmd : commands) 
			if (cmd.getCommand().equalsIgnoreCase(event.getName())) 
				event.reply(new MessageBuilder().setEmbeds(cmd.run(event.getTextChannel(), event.getUser())).build()).complete();
	}
	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getUser().getIdLong() == 464843391771869185L && event.getTextChannel().getName().equalsIgnoreCase(configuration.getProperty("rconchannel")) && event.retrieveMessage().complete().getEmbeds().size() >= 1) {
			event.getChannel().deleteMessageById(event.getMessageId()).complete();
		}
		super.onMessageReactionAdd(event);
	}
	
	public TASDiscordBot(Properties configuration) throws InterruptedException, LoginException {
		this.configuration = configuration;
		final JDABuilder builder = JDABuilder.createDefault(this.configuration.getProperty("token"))
				.setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(this);
		this.jda = builder.build();
		this.jda.awaitReady();
		this.blacklist = Arrays.asList(this.configuration.getProperty("blacklist").split(";"));
		this.commands = new ArrayList<CommandParser>();
	}

	@Override
	public void run() {
		/* Parse the Configuration and register the Commands */
		System.out.println("[TAS8999] Parsing Configuration...");
		final String[] commands = configuration.getProperty("commands", "null").split(",");
		System.out.println("[TAS8999] Found " + commands.length + " Commands.");
		for (Guild g : jda.getGuilds()) {
			CommandListUpdateAction updater = g.updateCommands();
			for (int i = 0; i < commands.length; i++) {
				CommandParser cmd;
				this.commands.add(cmd = CommandParser.parseMessage(commands[i], configuration.getProperty(commands[i], "No command registered!")));
				updater.addCommands(new CommandData(cmd.getCommand(), configuration.getProperty(commands[i] + "description", "Does not have a description")));
				System.out.println("[TAS8999] Successfully registered a new command");
			}
			updater.queue();
		}
		
		/* Server Data for the message */
		TextChannel channel = jda.getTextChannelsByName(configuration.getProperty("rconchannel"), true).get(0);
		String rconpw;
		final long message = channel.sendMessageEmbeds(new EmbedBuilder().setTitle("TAS Battle Server").addField("Players", "There are currently %x players online", true).build()).complete().getIdLong();
		if (!(rconpw = configuration.getProperty("rconpw", "none")).equalsIgnoreCase("none")) {
			Thread d = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(2000);
						Rcon rcon = new Rcon("mgnet.work", 25575, rconpw.getBytes(StandardCharsets.UTF_8));
						String input = rcon.command("list");
						int playerCount = Integer.parseInt(input.split("are")[1].split("of")[0].trim());
						channel.editMessageEmbedsById(message, new EmbedBuilder().setTitle("TAS Battle Server").addField("Players", "There " + (playerCount == 1 ? "is" : "are") + " currently %x player".replaceAll("%x", playerCount + "") + (playerCount == 1 ? "" : "s") + " online", true).build()).queue();
					} catch (Exception e1) {
						System.err.println("RCON Connection didn't work");
					}
				}
			});
			d.setDaemon(true);
			d.start();
		}
		
	}
}
