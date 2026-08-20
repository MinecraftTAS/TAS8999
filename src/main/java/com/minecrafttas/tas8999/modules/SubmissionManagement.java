package com.minecrafttas.tas8999.modules;

import java.util.List;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

/**
 * TAS/TAS Battle video submission management
 * @author Pancake
 */
public class SubmissionManagement {

	public static final long SUBMISSIONS = 765575637447868417L;

	/**
	 * Initialize submission management
	 * @param bot JDA instance
	 */
	public SubmissionManagement(JDA bot) {
		// load commands
		for (Guild guild : bot.getGuilds()) {
			CommandListUpdateAction updater = guild.updateCommands();

			CommandDataImpl submitCommand = new CommandDataImpl("submit", "Submit a TAS");
			SubcommandData tasSubCommand = new SubcommandData("tas", "Submit to #new-tas-things");
			SubcommandData miscSubCommand = new SubcommandData("misc", "Submit to #new-misc-things (does not require confirmation through moderator)");
//			SubcommandData tasbatteSubCommand = new SubcommandData("tasbattle", "Submit to #tb-videos");

			OptionData urlOption = new OptionData(OptionType.STRING, "url", "Video URL", true);
			OptionData commentOption = new OptionData(OptionType.STRING, "comment", "Optional comment", false);

			tasSubCommand.addOptions(urlOption, commentOption);
			miscSubCommand.addOptions(urlOption, commentOption);
//			tasbatteSubCommand.addOptions(urlOption, commentOption);

			submitCommand.addSubcommands(tasSubCommand, miscSubCommand/*, tasbatteSubCommand*/);

			updater.addCommands(submitCommand);
			updater.queue();
		}
	}

	/**
	 * Submit TAS to new-misc-things
	 * @param event Event
	 * @param channelId Submission channel
	 * @param url Video URL
	 * @param comment Comment
	 * @param requiresConfirmation Requires confirmation
	 */
	public void onSubmission(SlashCommandInteractionEvent event, long channelId, String url, String comment, boolean requiresConfirmation) {
		Member author = event.getMember();
		if (author == null)
			return;

		Guild guild = event.getGuild();
		if (guild == null)
			return;

		TextChannel channel = guild.getTextChannelById(channelId);
		if (channel == null)
			return;

		String message = (comment == null ? "No comment." : '"' + comment + '"') + '\n' + url + '\n' + "Submitted by " + author.getAsMention();
		if (requiresConfirmation) {
			TextChannel submChannel = guild.getTextChannelById(SUBMISSIONS);
			if (submChannel == null)
				return;

			MessageCreateBuilder msgBuilder = new MessageCreateBuilder();
			//@formatter:off
			msgBuilder.setContent(message + '\n' + "Submitted to: " + channel.getAsMention())
				.addComponents(
						ActionRow.of(
								Button.success("approve", "Approve"),
								Button.danger("reject", "Reject")
								)
						);
			//@formatter:on

			submChannel.sendMessage(msgBuilder.build()).queue();

			event.reply("Your submission has been posted to the moderators for confirmation. You will get notified via dm if your tas was rejected.").setEphemeral(true).queue();
		} else {
			channel.sendMessage(message).queue(msg -> {
				channel.createThreadChannel("Discussion", msg.getIdLong()).queue();
				event.reply("Your submission has been saved").setEphemeral(true).queue();
			});
		}
	}

	/**
	 * Run confirmation on button
	 * @param event Event
	 * @param  message Message
	 */
	public void onButton(ButtonInteractionEvent event, Message message) {
		if (event.getChannel().getIdLong() != SUBMISSIONS)
			return;

		String text = message.getContentRaw();
		if (event.getComponentId().equals("approve")) {
			List<GuildChannel> channels = message.getMentions().getChannels();
			TextChannel channel = (TextChannel) channels.get(channels.size() - 1);

			channel.sendMessage(text.substring(0, text.lastIndexOf("\n"))).queue(msg -> channel.createThreadChannel("Discussion", msg.getIdLong()).queue());
			disableButtons(event);

		} else if (event.getComponentId().equals("reject")) {
			List<User> users = message.getMentions().getUsers();
			User user = users.getLast();

			user.openPrivateChannel().queue(channel -> channel.sendMessage("Your recent submission has been rejected.\nYour submission:\n" + text).queue());
			disableButtons(event);
		}
	}

	private void disableButtons(ButtonInteractionEvent event) {
		Button approve = event.getMessage().getComponents().get(0).asActionRow().getButtons().get(0).asDisabled();
		Button reject = event.getMessage().getComponents().get(0).asActionRow().getButtons().get(1).asDisabled();

		ActionRow editedRow = ActionRow.of(approve, reject);
		event.editComponents(editedRow).queue();
	}
}
