package com.minecrafttas.tas8999.modules;

import com.minecrafttas.tas8999.util.GuildStorage;
import com.minecrafttas.tas8999.util.MD2Embed;
import com.minecrafttas.tas8999.util.Util;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import static com.minecrafttas.tas8999.TAS8999.COLOR;
import static com.minecrafttas.tas8999.TAS8999.LOGGER;

/**
 * Custom commands upsertable using slash commands
 * @author Scribble
 */
public class CustomCommands {
	public static final String SEPARATOR = ";:"; // separator for the command description and the command body

	private final GuildStorage storage;

	/**
	 * Initialize custom commands
	 * @param bot JDA instance
	 */
	public CustomCommands(JDA bot) {

		// load custom commands
		this.storage = new GuildStorage("custom_commands");
		for (var guild : bot.getGuilds()) {
			var prop = this.storage.getGuildProperties(guild);
			prop.forEach((key, value) -> {
				var cmd = (String) key;
				LOGGER.info("{{}} Loading custom command: {}", guild.getName(), cmd);

				var data = ((String) value).split(SEPARATOR, 3);
				this.addCommand(null, guild, cmd, data[1], data[2]);
			});
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
		event.reply(MD2Embed.parseMessage(this.getMarkdownByName(guild, name), COLOR).build()).setEphemeral(false).queue();
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
				var message = MD2Embed.parseMessage(markdown, COLOR);

				this.storage.set(guild, name, command.getId() + SEPARATOR + description + SEPARATOR + markdown);
				if (event != null)
					event.reply(message.build()).setEphemeral(true).queue();
			} catch (Exception e) {
				if (event != null)
					event.reply(Util.constructErrorMessage(e)).setEphemeral(true).queue();
				else
					LOGGER.error("{{}} Error loading custom command", guild.getName(), e);
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
