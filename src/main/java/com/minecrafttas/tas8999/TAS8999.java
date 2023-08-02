package com.minecrafttas.tas8999;

import com.minecrafttas.tas8999.modules.CustomCommands;
import com.minecrafttas.tas8999.modules.SpamProtection;
import com.minecrafttas.tas8999.util.Util;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TAS8999 extends ListenerAdapter {
	public static void main(String[] args) throws Exception { new TAS8999(); }

	public static final Logger LOGGER = LoggerFactory.getLogger("TAS8999");
	public static final int COLOR = 0x05808e;

	private final SpamProtection spamProtection;
	private final CustomCommands commandHandler;


	public TAS8999() throws Exception {
		var token = System.getenv("TAS8999_TOKEN");
		JDA jda = JDABuilder.createDefault(token)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
				.addEventListeners(this)
				.build().awaitReady();

		this.spamProtection = new SpamProtection();
		this.commandHandler = new CustomCommands(jda);

		// register the commands
		LOGGER.info("Preparing bot...");
		for (Guild guild : jda.getGuilds())
			prepareGuild(guild);

		LOGGER.info("Done preparing bot!");
	}

	private void prepareGuild(Guild guild) {
		createCommands(guild);
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

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		LOGGER.info("Joining new guild {}", event.getGuild().getName());
		prepareGuild(event.getGuild());
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		try {
			event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
				this.spamProtection.checkMessage(msg);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		event.getChannel().retrieveMessageById(event.getMessageId()).submit().whenComplete((msg, stage) -> {
			if (msg.getMember().hasPermission(Permission.ADMINISTRATOR)) {

				String raw = msg.getContentRaw();

				if (raw.startsWith("!debug")) {
				}
			}
		});

		super.onMessageReceived(event);
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String commandPath = event.getFullCommandName().replace(' ', '/');

		if (!commandHandler.executeCommand(event, event.getName()) && commandPath.startsWith("customcommand/")) {
			var name = event.getOption("name").getAsString();

            switch (commandPath) {
                case "customcommand/upsert" ->
                        event.getChannel().retrieveMessageById(event.getOption("messageid").getAsString()).queue(message ->
                            commandHandler.addCommand(event, event.getGuild(), name, event.getOption("description").getAsString(), message.getContentRaw()));
                case "customcommand/rename" ->
						commandHandler.renameCommand(event, name, event.getOption("newname").getAsString());
                case "customcommand/remove" ->
						commandHandler.removeCommand(event, name);
            }
		}
	}

}
