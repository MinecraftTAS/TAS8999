package com.minecrafttas.tas8999;

import com.minecrafttas.tas8999.modules.CustomCommands;
import com.minecrafttas.tas8999.modules.SpamProtection;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TAS8999 discord bot main class
 * @author Pancake
 */
public class TAS8999 extends ListenerAdapter {
	public static void main(String[] args) throws Exception { new TAS8999(); }

	public static final Logger LOGGER = LoggerFactory.getLogger("TAS8999");

	private final SpamProtection spamProtection;
	private final CustomCommands commandHandler;


	/**
	 * Initialize TAS8999
	 * @throws Exception JDA Exception
	 */
	public TAS8999() throws Exception {
		var token = System.getenv("TAS8999_TOKEN");
		JDA jda = JDABuilder.createDefault(token)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
				.addEventListeners(this)
				.build().awaitReady();

		// initialize modules
		this.spamProtection = new SpamProtection();
		this.commandHandler = new CustomCommands(jda);

		LOGGER.info("TAS8999 fully loaded");
	}

	/**
	 * Handle incoming messages
	 * @param event Message received event
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		this.spamProtection.checkMessage(event.getMessage());
	}

	/**
	 * Handle slash commands
	 * @param event Slash command event
	 */
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var commandPath = event.getFullCommandName().replace(' ', '/');

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
