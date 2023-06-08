package de.pfannekuchen.tas8999;

import java.io.File;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vdurmont.emoji.EmojiManager;

import de.pfannekuchen.tas8999.reactionroles.ReactionRoles;
import de.pfannekuchen.tas8999.util.SpamProtection;
import de.pfannekuchen.tas8999.util.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class TAS8999 extends ListenerAdapter implements Runnable {

	private final JDA jda;
	private static TAS8999 instance;
	private final SpamProtection protecc=new SpamProtection();
	private static final Logger LOGGER = LoggerFactory.getLogger("TAS8999");
	private final CommandHandler commandHandler;
	
	private final int color=0x05808e;
	private final ReactionRoles reactionroles;
	
	public TAS8999(String token) throws InterruptedException, LoginException {
		final JDABuilder builder = JDABuilder.createDefault(token)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(this);
		this.jda = builder.build();
		this.jda.awaitReady();
		instance=this;
		commandHandler = new CommandHandler("CommandHandler", new File("commands"), LOGGER);
		this.reactionroles=new ReactionRoles(jda.getGuilds(), color);
	}
	
	public static TAS8999 getBot() {
		return instance;
	}
	
	@Override
	public void run() {
		/* Register the Commands */
		LOGGER.info("Preparing bot...");
		for (Guild guild : jda.getGuilds()) {
			prepareGuild(guild);
		}
		LOGGER.info("Done preparing bot!");
	}
	
	private void prepareGuild(Guild guild) {
		LOGGER.info("Joining new guild {}", guild.getName());
		createCommands(guild);
		commandHandler.loadForGuild(guild);
	}
	
	private void createCommands(Guild guild) {
		CommandListUpdateAction updater = guild.updateCommands();
		
		CommandDataImpl reactionRoleCommand=new CommandDataImpl("reactionrole", "Adds a reactionrole message to the channel");
		
		SubcommandData addSubCommand=new SubcommandData("add", "Add a new reaction role");
		addSubCommand.addOption(OptionType.STRING, "arguments", "The emotes and roles to add");
		
		SubcommandData editSubCommand=new SubcommandData("edit", "Edits an existing bot message");
		editSubCommand.addOptions(new OptionData(OptionType.STRING, "messageid", "The messageid to edit"), new OptionData(OptionType.STRING, "arguments", "The emotes and roles to add"));
		
		reactionRoleCommand.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));
		
		reactionRoleCommand.addSubcommands(addSubCommand, editSubCommand);
		
		updater.addCommands(reactionRoleCommand);
		updater.queue();
	}

	public JDA getJDA() {
		return jda;
	}
	
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		prepareGuild(event.getGuild());
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		try {
			event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
				protecc.checkMessage(msg);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
			if (Util.hasDebugRole(msg.getMember())) {

				String raw = msg.getContentRaw();

				if (raw.startsWith("!debug")) {
				}
			}
		});
		
		super.onMessageReceived(event);
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		
		
		//Display the "Thinking" message
		event.deferReply().queue(hook -> {
			
			String commandPath = event.getFullCommandName().replace(" ", "/");
			
			//Rectionrole part
			if (event.getName().equals("reactionrole")) {
				
				//Check for edit permissions
				if (!Util.hasEditPerms(event.getMember())) {
					Util.sendSelfDestructingMessage(event.getChannel(), "You do not have the correct permissions!",	5);
				}
				
				//Reactionrole add
				if (commandPath.equals("reactionrole/add")) {

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
						MessageCreateData msg=new MessageCreateBuilder()
								.setEmbeds(new EmbedBuilder().setTitle("Usage:").addField("/reactionrole add `<reactionlist>`", "Example: /reactionrole add `:emote: @Role description, :secondemote: @SecondRole seconddescription`", false).setColor(color).build())
								.build();
						Util.sendDeletableMessage(event.getChannel(), msg);
					}
					
				//Reactionrole edit
				} else if (commandPath.equals("reactionrole/edit")) {
					
					if(event.getOption("messageid")!=null && event.getOption("arguments")!=null) {
						try {
							reactionroles.editMessage(event.getGuild(), event.getChannel(), event.getOption("messageid").getAsLong(), event.getOption("arguments").getAsString());
						} catch (Exception e) {
							Util.sendErrorMessage(event.getChannel(), e);
							e.printStackTrace();
						} 
					// Display usage
					}else if(event.getOption("messageid")==null && event.getOption("arguments")==null) {
						MessageCreateData msg=new MessageCreateBuilder()
								.setEmbeds(new EmbedBuilder().setTitle("Usage:").addField("/reactionrole edit `<messageid>` `<reactionlist>`", "Example: /reactionrole edit `373167715969531904` `:emote: @Role description, :secondemote: @SecondRole seconddescription`", false).setColor(color).build())
								.build();
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
		if (!Util.isThisUserThisBot(event.getUser())) {

			MessageReaction reactionEmote = event.getReaction();

			String roleId = reactionroles.getRole(event.getGuild(), event.getMessageIdLong(), reactionEmote.getEmoji().getFormatted());
			
			if(!roleId.isEmpty()) {
				Guild guild=event.getGuild();
				Role role=guild.getRoleById(roleId);
				guild.addRoleToMember(event.getUser(), role).queue();
					
			}
			else if (reactionEmote.getEmoji().asUnicode().equals(EmojiManager.getForAlias(":x:").getUnicode())) {

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
				MessageReaction reactionEmote = event.getReaction();
		
				String roleId = reactionroles.getRole(event.getGuild(), event.getMessageIdLong(),
						reactionEmote.getMessageId());
		
				if (!roleId.isEmpty()) {
					Guild guild = event.getGuild();
					Role role = guild.getRoleById(roleId);
					guild.removeRoleFromMember(user, role).queue();
				}
			}
		});
	}
}
