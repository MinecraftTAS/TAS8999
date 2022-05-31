package de.pfannekuchen.tas8999;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import com.vdurmont.emoji.EmojiManager;

import de.pfannekuchen.tas8999.parser.CommandParser;
import de.pfannekuchen.tas8999.reactionroles.EmoteWrapper;
import de.pfannekuchen.tas8999.reactionroles.ReactionRoles;
import de.pfannekuchen.tas8999.util.SpamProtection;
import de.pfannekuchen.tas8999.util.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class TASDiscordBot extends ListenerAdapter implements Runnable {

	private final JDA jda;
	private final Properties configuration;
	private final List<String> blacklist;
	private final ArrayList<CommandParser> commands;
	private final SpamProtection protecc=new SpamProtection();
	
	private final int color=0x05808e;
	private final ReactionRoles reactionroles;
	
//	@Override
//	public void onGenericPrivateMessage(GenericPrivateMessageEvent event) {
//		for (Role role : jda.getGuildById(373166430478401555L).retrieveMember(event.getChannel().getUser()).complete().getRoles()) {
//			if (role.getIdLong() == 776544617956769802L) {
//				Message msg = null;
//				try {
//					msg = event.getChannel().retrieveMessageById(event.getMessageIdLong()).complete();
//				} catch (Exception e3) {
//					return;
//				}
//				if (!msg.getAuthor().isBot()) {
//					if (new File("submissions").exists()) {
//						Message e = event.getChannel().sendMessage("Your TAS Competition Submission has been changed to:\n " + msg.getContentStripped()).complete();
//						new Thread(() -> {
//							try {
//								Thread.sleep(5000);
//							} catch (InterruptedException e1) {
//								e1.printStackTrace();
//							}
//							event.getChannel().deleteMessageById(e.getIdLong()).queueAfter(5L, TimeUnit.SECONDS);
//						}).start();
//						File f = new File("submissions/" + (event.getChannel().getUser().getAsTag()));
//						try {
//							f.createNewFile();
//							Files.write(f.toPath(), msg.getContentStripped().getBytes(StandardCharsets.UTF_8));
//							File[] submissions = new File("submissions").listFiles();
//							TextChannel channel = jda.getGuildById(373166430478401555L).getTextChannelById(904465448932892702L);
//							for (Message msgs : channel.getHistoryFromBeginning(99).complete().getRetrievedHistory()) {
//								String message = msgs.getContentStripped();
//								if (!message.contains("OLD SUBMISSION")) {
//									msgs.editMessage("~~" + message + "~~ - OLD SUBMISSION").complete();
//									msgs.suppressEmbeds(true).complete();
//								}
//							}
//							for (File file : submissions) channel.sendMessage(file.getName() + " -> " + Files.readAllLines(file.toPath()).get(0)).complete();
//						} catch (IOException e2) {
//							e2.printStackTrace();
//						}
//					}
//				}
//			}
//		}
//	}
	
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
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
							Util.deleteMessage(msg, "Blacklisted Word");
							return;
						}
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
			if (Util.hasRole(msg.getMember(), "Debug")) {

				String raw = msg.getContentRaw();

				if (raw.startsWith("!debug")) {
				}
			}
		});
		
		super.onMessageReceived(event);
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		for (CommandParser cmd : commands) 
			if (cmd.getCommand().equalsIgnoreCase(event.getName())) 
				event.reply(new MessageBuilder().setEmbeds(cmd.run(event.getTextChannel(), event.getUser())).build()).complete();
		
		//Display the "Thinking" message
		event.deferReply().queue(hook -> {
			
			//Rectionrole part
			if (event.getName().equals("reactionrole")) {
				
				//Check for edit permissions
				if (!Util.hasEditPerms(event.getMember())) {
					Util.sendSelfDestructingMessage(event.getChannel(), "You do not have the correct permissions!",	5);
				}
				
				//Reactionrole add
				if (event.getCommandPath().equals("reactionrole/add")) {

					//Check if the arguments are null
					if(event.getOption("arguments")!=null) {		
						try {
							reactionroles.addNewMessage(event.getGuild(), event.getChannel(), event.getOption("arguments").getAsString());
						} catch (Exception e) {
							Util.sendErrorMessage(event.getChannel(), e);
							e.printStackTrace();
						}
						
					//Display usage
					}else {
						Message msg=new MessageBuilder(new EmbedBuilder().setTitle("Usage:").addField("/reactionrole add `<reactionlist>`", "Example: /reactionrole add `:emote: @Role description, :secondemote: @SecondRole seconddescription`", false).setColor(color)).build();
						Util.sendDeletableMessage(event.getChannel(), msg);
					}
					
				//Reactionrole edit
				} else if (event.getCommandPath().equals("reactionrole/edit")) {
					
					if(event.getOption("messageid")!=null && event.getOption("arguments")!=null) {
						try {
							reactionroles.editMessage(event.getGuild(), event.getChannel(), event.getOption("messageid").getAsLong(), event.getOption("arguments").getAsString());
						} catch (Exception e) {
							Util.sendErrorMessage(event.getChannel(), e);
							e.printStackTrace();
						} 
					// Display usage
					}else if(event.getOption("messageid")==null && event.getOption("arguments")==null) {
						Message msg=new MessageBuilder(new EmbedBuilder().setTitle("Usage:").addField("/reactionrole edit `<messageid>` `<reactionlist>`", "Example: /reactionrole edit `373167715969531904` `:emote: @Role description, :secondemote: @SecondRole seconddescription`", false).setColor(color)).build();
						Util.sendDeletableMessage(event.getChannel(), msg);
					}
				}
			}
			
			hook.deleteOriginal().queue();
		});
	}
	
	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
		if(event.isFromGuild()) {
			reactionroles.removeMessage(event.getGuild(), event.getMessageIdLong());
		}
	}
	
	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getUser().getIdLong() == 464843391771869185L
				&& event.getTextChannel().getName().equalsIgnoreCase(configuration.getProperty("rconchannel"))
				&& event.retrieveMessage().complete().getEmbeds().size() >= 1) {
			event.getChannel().deleteMessageById(event.getMessageId()).complete();
		}

		if (!Util.isThisUserThisBot(event.getUser())) {

			ReactionEmote reactionEmote = event.getReactionEmote();

			String roleId = reactionroles.getRole(event.getGuild(), event.getMessageIdLong(), EmoteWrapper.getReactionEmoteId(reactionEmote));
			
			if(!roleId.isEmpty()) {
				Guild guild=event.getGuild();
				Role role=guild.getRoleById(roleId);
				guild.addRoleToMember(event.getUser(), role).queue();
					
			}
			else if (EmoteWrapper.getReactionEmoteId(reactionEmote).equals(EmojiManager.getForAlias(":x:").getUnicode())) {

				event.retrieveMessage().queue(msg -> {
					if (Util.isThisUserThisBot(msg.getAuthor())) {

						if (Util.hasBotReactedWith(msg, EmojiManager.getForAlias(":x:").getUnicode())) {
							Util.deleteMessage(msg);
						}
					}
				});
			}
		}
	}
	
	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		event.retrieveUser().queue(user ->{
			if (!Util.isThisUserThisBot(user)) {
				ReactionEmote reactionEmote = event.getReactionEmote();
		
				String roleId = reactionroles.getRole(event.getGuild(), event.getMessageIdLong(),
						EmoteWrapper.getReactionEmoteId(reactionEmote));
		
				if (!roleId.isEmpty()) {
					Guild guild = event.getGuild();
					Role role = guild.getRoleById(roleId);
					guild.removeRoleFromMember(user, role).queue();
				}
			}
		});
	}

	private static TASDiscordBot instance;
	
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
		instance=this;
		this.reactionroles=new ReactionRoles(jda.getGuilds(), color);
	}
	
	public static TASDiscordBot getBot() {
		return instance;
	}
	
	@Override
	public void run() {
		/* Parse the Configuration and register the Commands */
		System.out.println("[TAS8999] Parsing Configuration...");
		final String[] commands = configuration.getProperty("commands", "null").split(",");
		System.out.println("[TAS8999] Found " + commands.length + " Commands.");
		for (Guild guild : jda.getGuilds()) {
			CommandListUpdateAction updater = guild.updateCommands();
			for (int i = 0; i < commands.length; i++) {
				CommandParser cmd;
				this.commands.add(cmd = CommandParser.parseMessage(commands[i], configuration.getProperty(commands[i], "No command registered!")));
				updater.addCommands(new CommandDataImpl(cmd.getCommand(), configuration.getProperty(commands[i] + "description", "Does not have a description")));
				
				System.out.println("[TAS8999] Successfully registered a new command");
			}
			
			CommandDataImpl reactionRoleCommand=new CommandDataImpl("reactionrole", "Adds a reactionrole message to the channel");
			
			SubcommandData addSubCommand=new SubcommandData("add", "Add a new reaction role");
			addSubCommand.addOption(OptionType.STRING, "arguments", "The emotes and roles to add");
			
			SubcommandData editSubCommand=new SubcommandData("edit", "Edits an existing bot message");
			editSubCommand.addOptions(new OptionData(OptionType.STRING, "messageid", "The messageid to edit"), new OptionData(OptionType.STRING, "arguments", "The emotes and roles to add"));
			
			reactionRoleCommand.setDefaultEnabled(false);
			
			reactionRoleCommand.addSubcommands(addSubCommand, editSubCommand);
			
			updater.addCommands(reactionRoleCommand);
			updater.queue();
		}
		
		
	}
	
	public JDA getJDA() {
		return jda;
	}
}
