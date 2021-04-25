package de.pfannekuchen.tasdiscordbot;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import de.pfannekuchen.tasdiscordbot.parser.CommandParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction.CommandData;

public class TASDiscordBot extends ListenerAdapter implements Runnable {

	private final JDA jda;
	private final Properties configuration;
	private final ArrayList<CommandParser> commands;
	
	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		for (CommandParser cmd : commands) 
			if (cmd.getCommand().equalsIgnoreCase(event.getName())) 
				event.reply(cmd.run(event.getTextChannel(), event.getUser())).complete();
	}
	
	public TASDiscordBot(Properties configuration) throws InterruptedException, LoginException {
		this.configuration = configuration;
		final JDABuilder builder = JDABuilder.createLight(this.configuration.getProperty("token"), EnumSet.noneOf(GatewayIntent.class)).addEventListeners(this);
		this.jda = builder.build();
		this.jda.awaitReady();
		this.commands = new ArrayList<CommandParser>();
	}

	@Override
	public void run() {
		/* Parse the Configuration and register the Commands */
		System.out.println("[TAS8999] Parsing Configuration...");
		final String[] commands = configuration.getProperty("commands", "null").split(",");
		System.out.println("[TAS8999] Found " + commands.length + " Commands.");
		CommandUpdateAction updater = jda.getGuilds().get(0).updateCommands();
		for (int i = 0; i < commands.length; i++) {
			CommandParser cmd;
			this.commands.add(cmd = CommandParser.parseMessage(commands[i], configuration.getProperty(commands[i], "No command registered!")));
			updater.addCommands(new CommandData(cmd.getCommand(), configuration.getProperty(commands[i] + "description", "Does not have a description")));
			System.out.println("[TAS8999] Successfully registered a new command");
		}
		updater.queue();
	}
	
}
