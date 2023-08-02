package com.minecrafttas.tas8999.modules;

import com.minecrafttas.tas8999.utils.GuildStorage;
import com.minecrafttas.tas8999.utils.MarkdownParser;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import static com.minecrafttas.tas8999.TAS8999.LOGGER;

/**
 * Custom commands upsertable using slash commands
 * @author Scribble
 */
public class CustomCommands {
	public static final String SEPARATOR = ";:"; // separator for the command description and the command body
	public static final int COLOR = 0x05808e;

	private final GuildStorage storage;

	/**
	 * Initialize custom commands
	 * @param bot JDA instance
	 */
	public CustomCommands(JDA bot) {

		// load guilds
		this.storage = new GuildStorage("custom_commands");
		for (var guild : bot.getGuilds()) {
			// load custom commands
			var prop = this.storage.getGuildProperties(guild);
			prop.forEach((key, value) -> {
				var cmd = (String) key;
				LOGGER.info("{{}} Loading custom command: {}", guild.getName(), cmd);

				var data = ((String) value).split(SEPARATOR, 3);
				this.addCommand(null, guild, cmd, data[1], data[2]);
			});

			// load main commands
			var updater = guild.updateCommands();

			var customCommand = new CommandDataImpl("customcommand", "Command for adding and upating customcommands");
			var upsertSubCommand = new SubcommandData("upsert", "Adds or updates a new custom command");
			var renameSubCommand = new SubcommandData("rename", "Renames a custom command");
			var removeSubCommand = new SubcommandData("remove", "Removes a custom command");

			var commandNameOption = new OptionData(OptionType.STRING, "name", "The custom command name", true);
			var commandNewNameOption = new OptionData(OptionType.STRING, "newname", "The new custom command name", true);
			var commandDescriptionOption = new OptionData(OptionType.STRING, "description", "The custom command description", true);
			var commandBodyOption = new OptionData(OptionType.STRING, "messageid", "The messageid of the commandtext", true);

			upsertSubCommand.addOptions(commandNameOption, commandDescriptionOption, commandBodyOption);
			renameSubCommand.addOptions(commandNameOption, commandNewNameOption);
			removeSubCommand.addOptions(commandNameOption);

			customCommand.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS));
			customCommand.addSubcommands(upsertSubCommand, renameSubCommand, removeSubCommand);

			updater.addCommands(customCommand);
			updater.queue();
		}
	}

	/**
	 * Execute custom command if found
	 * @param event Event
	 * @param name Command name
	 * @return Was sucessful
	 */
	@SneakyThrows
	public boolean executeCommand(SlashCommandInteractionEvent event, String name) {
		var guild = event.getGuild();
		if (guild == null)
			return false;

		if (!this.storage.contains(guild, name))
			return false;

		// build and reply
		event.reply(MarkdownParser.parseMessage(this.getMarkdownByName(guild, name), COLOR).build()).setEphemeral(false).queue();
		return true;
	}

	/**
	 * Add custom command to guild
	 * @param event Event to respond to (or null)
	 * @param guild Guild to add command in
	 * @param name Command name
	 * @param description Command description
	 * @param markdown Command markdown
	 */
	public void addCommand(SlashCommandInteractionEvent event, Guild guild, String name, String description, String markdown) {
		guild.upsertCommand(name, description).queue(command -> {
			try {
				var message = MarkdownParser.parseMessage(markdown, COLOR);

				this.storage.set(guild, name, command.getId() + SEPARATOR + description + SEPARATOR + markdown);
				if (event != null)
					event.reply(message.build()).setEphemeral(true).queue();
			} catch (Exception e) {
				if (event != null)
					event.reply("Something went wrong while trying to create this command, please check the console.").setEphemeral(true).queue();

				LOGGER.error("{{}} Error adding custom command", guild.getName(), e);
			}
		});
	}
	
	/**
	 * Remove custom command from guild
	 * @param event Event
	 * @param name Command parameter
	 */
	public void removeCommand(SlashCommandInteractionEvent event, String name) {
		var guild = event.getGuild();
		if (guild == null)
			return;

		var commandId = this.getIdByName(guild, name);
		if (commandId == null)
			return;

		this.storage.remove(guild, name);
		guild.deleteCommandById(commandId).queue();
		event.reply("Removed custom command `" + name + "`").setEphemeral(true).queue();
	}

	/**
	 * Rename custom command
	 * @param event Event
	 * @param name Old name
	 * @param newName New name
	 */
	public void renameCommand(SlashCommandInteractionEvent event, String name, String newName) {
		var guild = event.getGuild();
		if (guild == null)
			return;

		var commandId = this.getIdByName(guild, name);
		if (commandId == null)
			return;

		this.storage.set(guild, newName, this.storage.remove(guild, name));
		guild.editCommandById(commandId).setName(newName).queue();
		event.reply("Renamed custom command `" + name + "` to `" + newName + "`").setEphemeral(true).queue();
	}

	/**
	 * Get command by name
	 * @param guild Guild
	 * @param name Command name
	 * @return Command id or null
	 */
	public String getIdByName(Guild guild, String name) {
		var value = this.storage.get(guild, name);
		if (value == null)
			return null;

		return value.split(SEPARATOR, 3)[0];
	}

	/**
	 * Get command markdown by name
	 * @param guild Guild
	 * @param name Command name
	 * @return Command markdown or null
	 */
	public String getMarkdownByName(Guild guild, String name) {
		var value = this.storage.get(guild, name);
		if (value == null)
			return null;

		return value.split(SEPARATOR, 3)[2];
	}

}
