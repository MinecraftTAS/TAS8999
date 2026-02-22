package com.minecrafttas.tas8999.modules;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

/**
 * TAS/TAS Battle video submission management
 * @author Pancake
 */
public class SubmissionManagement {

    public static final long SUBMISSIONS = 765575637447868417L;
    public static final Emoji CONFIRM_EMOJI = Emoji.fromUnicode("\u2705");
    public static final Emoji REJECT_EMOJI = Emoji.fromUnicode("\u274C");


    /**
     * Initialize submission management
     * @param bot JDA instance
     */
    public SubmissionManagement(JDA bot) {
        // load commands
        for (var guild : bot.getGuilds()) {
            var updater = guild.updateCommands();

            var submitCommand = new CommandDataImpl("submit", "Submit a TAS");
            var tasSubCommand = new SubcommandData("tas", "Submit to #new-tas-things");
            var miscSubCommand = new SubcommandData("misc", "Submit to #new-misc-things (does not require confirmation through moderator)");
            var tasbatteSubCommand = new SubcommandData("tasbattle", "Submit to #tb-videos");

            var urlOption = new OptionData(OptionType.STRING, "url", "Video URL", true);
            var commentOption = new OptionData(OptionType.STRING, "comment", "Optional comment", false);

            tasSubCommand.addOptions(urlOption, commentOption);
            miscSubCommand.addOptions(urlOption, commentOption);
            tasbatteSubCommand.addOptions(urlOption, commentOption);

            submitCommand.addSubcommands(tasSubCommand, miscSubCommand, tasbatteSubCommand);

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
    public void onMiscSubmission(SlashCommandInteractionEvent event, long channelId, String url, String comment, boolean requiresConfirmation) {
        var author = event.getMember();
        if (author == null)
            return;

        var guild = event.getGuild();
        if (guild == null)
            return;

        var channel = guild.getTextChannelById(channelId);
        if (channel == null)
            return;

        var message = (comment == null ? "No comment." : '"' + comment + '"') + '\n' + url + '\n' + "Submitted by " + author.getAsMention();
        if (requiresConfirmation) {
            var submChannel = guild.getTextChannelById(SUBMISSIONS);
            if (submChannel == null)
                return;

            submChannel.sendMessage(message + '\n' + "Submitted to: " + channel.getAsMention()).queue(msg -> {
                msg.addReaction(CONFIRM_EMOJI).queue();
                msg.addReaction(REJECT_EMOJI).queue();
            });

            event.reply("Your submission has been posted to the moderators for confirmation. You will get notified via dm if your tas was rejected.").setEphemeral(true).queue();
        } else {
            channel.sendMessage(message).queue(msg -> {
                channel.createThreadChannel("Discussion", msg.getIdLong()).queue();
                event.reply("Your submission has been saved").setEphemeral(true).queue();
            });
        }
    }

    /**
     * Run confirmation on reaction
     * @param event Event
     * @param  message Message
     */
    public void onReaction(MessageReactionAddEvent event, Message message) {
        if (event.getChannel().getIdLong() != SUBMISSIONS)
            return;

        if (event.getMember().getUser().isBot())
            return;

        if (!message.getAuthor().isBot())
            return;

        var emoji = event.getEmoji().getName();

        var text = message.getContentRaw();
        if (emoji.equals(CONFIRM_EMOJI.getName())) {
            var channels = message.getMentions().getChannels();
            var channel = (TextChannel) channels.get(channels.size() - 1);

            channel.sendMessage(text.substring(0, text.lastIndexOf("\n"))).queue(msg ->
                    channel.createThreadChannel("Discussion", msg.getIdLong()).queue());
            message.delete().queue();
        } else if (emoji.equals(REJECT_EMOJI.getName())) {
            var users = message.getMentions().getUsers();
            var user = users.get(users.size() - 1);

            user.openPrivateChannel().queue(channel ->
                channel.sendMessage("Your recent submission has been rejected.\nYour submission:\n" + text).queue());

            message.delete().queue();
        }
    }

}
