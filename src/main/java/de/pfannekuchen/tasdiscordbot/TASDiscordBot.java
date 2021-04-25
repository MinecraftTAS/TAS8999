package de.pfannekuchen.tasdiscordbot;

import java.util.Properties;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class TASDiscordBot implements Runnable {

	private final JDA jda;
	private final Properties configuration;
	
	public TASDiscordBot(Properties configuration) throws InterruptedException, LoginException {
		this.configuration = configuration;
		JDABuilder builder = JDABuilder.createDefault(this.configuration.getProperty("token"));
		this.jda = builder.build();
		this.jda.awaitReady();
	}

	@Override
	public void run() {
		/* Parse the Configuration and register the Commands */
		System.out.println("[TAS8999] Parsing Configuration...");
		final String[] commands = configuration.getProperty("commands", "null").split(",");
		System.out.println("[TAS8999] Found " + commands.length + " Commands.");
		for (int i = 0; i < commands.length; i++) {
			// TODO: Register Command
		}
	}
	
}
