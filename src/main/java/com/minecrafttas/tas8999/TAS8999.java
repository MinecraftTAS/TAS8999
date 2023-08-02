package com.minecrafttas.tas8999;

import com.minecrafttas.tas8999.modules.CustomCommands;
import com.minecrafttas.tas8999.modules.SpamProtection;
import com.minecrafttas.tas8999.modules.SubmissionManagement;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
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

	public static final long NEW_TAS_THINGS = 399223722491510794L;
	public static final long NEW_MISC_THINGS = 555113094570049577L;
	public static final long TB_VIDEOS = 803651015383187456L;

	private final SpamProtection spamProtection;
	private final CustomCommands commandHandler;
	private final SubmissionManagement submissionManagement;

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
		this.submissionManagement = new SubmissionManagement(jda);

		LOGGER.info("TAS8999 fully loaded");
	}

	/**
	 * Handle reactions on submissions
	 * @param event Reaction added event
	 */
	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue(message -> this.submissionManagement.onReaction(event, message));
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
                            commandHandler.addCommand(event, event.getGuild(), true, name, event.getOption("description").getAsString(), message.getContentRaw()));
                case "customcommand/rename" ->
						commandHandler.renameCommand(event, name, event.getOption("newname").getAsString());
                case "customcommand/remove" ->
						commandHandler.removeCommand(event, name);
            }
		} else if (commandPath.startsWith("submit/")) {
			var url = event.getOption("url").getAsString();
			var comment = event.getOption("comment", null, option -> option.getAsString());

			switch (commandPath) {
				case "submit/tas" -> this.submissionManagement.onMiscSubmission(event, 399223722491510794L, url, comment, true);
				case "submit/misc" -> this.submissionManagement.onMiscSubmission(event, 555113094570049577L, url, comment, false);
				case "submit/tasbattle" -> this.submissionManagement.onMiscSubmission(event, 803651015383187456L, url, comment, true);
			}
		}
	}

}
