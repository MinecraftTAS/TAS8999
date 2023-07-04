package com.minecrafttas.tas8999;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minecrafttas.tas8999.util.SpamProtection;
import com.minecrafttas.tas8999.util.Util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class TAS8999 extends ListenerAdapter implements Runnable {

	private final JDA jda;
	private static TAS8999 instance;
	private final SpamProtection protecc = new SpamProtection();
	private static final Logger LOGGER = LoggerFactory.getLogger("TAS8999");
	private final CustomCommands commandHandler;

	public final int color = 0x05808e;
	private final ReactionRoles reactionroles;

	public TAS8999(String token) throws InterruptedException, LoginException {
		final JDABuilder builder = JDABuilder.createDefault(token).setMemberCachePolicy(MemberCachePolicy.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT).addEventListeners(this);
		this.jda = builder.build();
		this.jda.awaitReady();
		instance = this;
		commandHandler = new CustomCommands(LOGGER);
		this.reactionroles = new ReactionRoles(LOGGER);
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
		createCommands(guild);
		commandHandler.loadForGuild(guild);
		reactionroles.loadForGuild(guild);
	}

	private void createCommands(Guild guild) {
		CommandListUpdateAction updater = guild.updateCommands();

		CommandDataImpl reactionRoleCommand = new CommandDataImpl("reactionrole", "Adds a reactionrole message");
		SubcommandData createSubCommand = new SubcommandData("create", "Creates a new reactionrole message");
		SubcommandData addOptionSubCommand = new SubcommandData("upsert", "Adds or updates an emote");
		SubcommandData removeSubCommand = new SubcommandData("remove", "Removes an emote from the reactionrole message");

		OptionData msgIDOption = new OptionData(OptionType.STRING, "messageid", "The message id of the reactionrole message", true);
		OptionData emoteOption = new OptionData(OptionType.STRING, "emote", "The emote to add/update", true);
		OptionData roleOption = new OptionData(OptionType.ROLE, "role", "The role to link to the emote", true);
		OptionData descriptionOption = new OptionData(OptionType.STRING, "description", "The description for the reaction", true);

		addOptionSubCommand.addOptions(msgIDOption, emoteOption, roleOption, descriptionOption);
		removeSubCommand.addOptions(msgIDOption, emoteOption);

		reactionRoleCommand.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));

		reactionRoleCommand.addSubcommands(createSubCommand, addOptionSubCommand, removeSubCommand);

		// =============================

		CommandDataImpl customCommand = new CommandDataImpl("customcommand", "Command for adding and upating customcommands");

		SubcommandData upsertSubCommand = new SubcommandData("upsert", "Adds or updates a new custom command");
		SubcommandData renameSubCommand = new SubcommandData("rename", "Renames a custom command");
		removeSubCommand = new SubcommandData("remove", "Removes a custom command");

		OptionData commandNameOption = new OptionData(OptionType.STRING, "name", "The custom command name", true);
		OptionData commandNewNameOption = new OptionData(OptionType.STRING, "newname", "The new custom command name", true);
		OptionData commandDescriptionOption = new OptionData(OptionType.STRING, "description", "The custom command description", true);
		OptionData commandBodyOption = new OptionData(OptionType.STRING, "messageid", "The messageid of the commandtext", true);

		upsertSubCommand.addOptions(commandNameOption, commandDescriptionOption, commandBodyOption);
		renameSubCommand.addOptions(commandNameOption, commandNewNameOption);
		removeSubCommand.addOptions(commandNameOption);

		customCommand.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));

		customCommand.addSubcommands(upsertSubCommand, renameSubCommand, removeSubCommand);

		// =============================

		updater.addCommands(reactionRoleCommand, customCommand);
		updater.queue();
	}

	public JDA getJDA() {
		return jda;
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		LOGGER.info("Joining new guild {}", event.getGuild().getName());
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
			if (Util.hasAdminPerms(msg.getMember())) {

				String raw = msg.getContentRaw();

				if (raw.startsWith("!debug")) {
				}
			}
		});

		super.onMessageReceived(event);
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

		String commandPath = event.getFullCommandName().replace(" ", "/");

		try {
			if (commandHandler.executeCommand(event, event.getName())) {
				return;
			} else if (commandPath.startsWith("customcommand/")) {
				if (commandPath.equals("customcommand/upsert")) {
					event.getChannel().retrieveMessageById(event.getOption("messageid").getAsString()).queue(message -> {
						commandHandler.upsertCommand(event, event.getOption("name").getAsString(), event.getOption("description").getAsString(), message.getContentRaw());
					});
				} else if (commandPath.equals("customcommand/rename")) {
					commandHandler.renameCommand(event, event.getOption("name").getAsString(), event.getOption("newname").getAsString());
				} else if (commandPath.equals("customcommand/remove")) {
					String name = event.getOption("name").getAsString();
					commandHandler.removeCommand(event, name);
				}
			} else if (commandPath.startsWith("reactionrole/")) {
				if (commandPath.equals("reactionrole/create")) {
					reactionroles.createNewMessage(event);
				} else if (commandPath.equals("reactionrole/upsert")) {
					reactionroles.upsertOption(event, event.getOption("messageid").getAsString(), Emoji.fromFormatted(event.getOption("emote").getAsString()), event.getOption("role").getAsRole(), event.getOption("description").getAsString());
				} else if(commandPath.equals("reactionrole/remove")) {
					reactionroles.removeOption(event, event.getOption("messageid").getAsString(), Emoji.fromFormatted(event.getOption("emote").getAsString()));
				}
			}
		} catch (Exception e) {
			Util.sendErrorReply(event, e, false);
			e.printStackTrace();
		}
	}

	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
		if (event.isFromGuild()) {
			reactionroles.onDelete(event.getGuild(), event.getMessageId());
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!Util.isThisUserThisBot(event.getUser())) {
			if (reactionroles.onReactionAdd(event)) {
				return;
			} else {
				Util.deleteMessageOnReaction(event);
			}
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		reactionroles.onReactionRemove(event);
	}
}
