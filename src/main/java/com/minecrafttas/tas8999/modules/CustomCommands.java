package com.minecrafttas.tas8999.modules;

import static com.minecrafttas.tas8999.TAS8999.LOGGER;

import java.util.Properties;

import com.minecrafttas.tas8999.utils.GuildStorage;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

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
		for (Guild guild : bot.getGuilds()) {
			// load custom commands
			Properties prop = this.storage.getGuildProperties(guild);
			prop.forEach((key, value) -> {
				String cmd = (String) key;
				LOGGER.info("{{}} Adding command: {}", guild.getName(), cmd);

				String[] data = ((String) value).split(SEPARATOR, 3);
				this.addCommand(null, guild, false, cmd, data[1], data[2]);
			});

			// load main commands
			CommandListUpdateAction updater = guild.updateCommands();

			CommandDataImpl customCommand = new CommandDataImpl("customcommand", "Command for adding and upating customcommands");
			SubcommandData upsertSubCommand = new SubcommandData("upsert", "Adds or updates a new custom command");
			SubcommandData renameSubCommand = new SubcommandData("rename", "Renames a custom command");
			SubcommandData removeSubCommand = new SubcommandData("remove", "Removes a custom command");

			OptionData commandNameOption = new OptionData(OptionType.STRING, "name", "The custom command name", true);
			OptionData commandNewNameOption = new OptionData(OptionType.STRING, "newname", "The new custom command name", true);
			OptionData commandDescriptionOption = new OptionData(OptionType.STRING, "description", "The custom command description", true);
			OptionData commandBodyOption = new OptionData(OptionType.STRING, "messageid", "The messageid of the commandtext", true);

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
	public boolean executeCommand(SlashCommandInteractionEvent event, String name) {
		Guild guild = event.getGuild();
		if (guild == null)
			return false;

		if (!this.storage.contains(guild, name))
			return false;

		// build and reply
		MessageCreateBuilder msgBuilder = new MessageCreateBuilder();
		EmbedBuilder embedBuilder = new EmbedBuilder();
	
		embedBuilder
			.setDescription(this.getTextByName(guild, name))
			.setColor(COLOR)
			.setFooter(String.format("/%s",name))
			;
		
		msgBuilder.addEmbeds(embedBuilder.build());
		
		try {
			event.reply(msgBuilder.build()).setEphemeral(false).queue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Add custom command to guild
	 * @param event Event to respond to (or null)
	 * @param guild Guild to add command in
	 * @param shouldSave Should save properties
	 * @param name Command name
	 * @param description Command description
	 * @param message Command markdown
	 */
	public void addCommand(SlashCommandInteractionEvent event, Guild guild, boolean shouldSave, String name, String description, String message) {
		guild.upsertCommand(name, description).queue(command -> {
			try {

				MessageCreateBuilder msgBuilder = new MessageCreateBuilder();
				EmbedBuilder embedBuilder = new EmbedBuilder();
			
				embedBuilder
					.setDescription(this.getTextByName(guild, name))
					.setColor(COLOR)
					.setFooter(String.format("/%s",name))
					;
				
				msgBuilder.addEmbeds(embedBuilder.build());
				
				this.storage.set(guild, name, command.getId() + SEPARATOR + description + SEPARATOR + message);
				if (shouldSave)
					this.storage.saveGuild(guild);

				if (event != null)
					event.reply(msgBuilder.build()).setEphemeral(true).queue();
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
		Guild guild = event.getGuild();
		if (guild == null)
			return;

		String commandId = this.getIdByName(guild, name);
		if (commandId == null)
			return;

		this.storage.remove(guild, name);
		this.storage.saveGuild(guild);
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
		Guild guild = event.getGuild();
		if (guild == null)
			return;

		String commandId = this.getIdByName(guild, name);
		if (commandId == null)
			return;

		this.storage.set(guild, newName, this.storage.remove(guild, name));
		this.storage.saveGuild(guild);
		guild.editCommandById(Command.Type.SLASH, name).setName(newName).queue();
		event.reply("Renamed custom command `" + name + "` to `" + newName + "`").setEphemeral(true).queue();
	}

	/**
	 * Get command by name
	 * @param guild Guild
	 * @param name Command name
	 * @return Command id or null
	 */
	public String getIdByName(Guild guild, String name) {
		String value = this.storage.get(guild, name);
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
	public String getTextByName(Guild guild, String name) {
		String value = this.storage.getOrDefault(guild, name, null);

		return value.split(SEPARATOR, 3)[2];
	}
}
