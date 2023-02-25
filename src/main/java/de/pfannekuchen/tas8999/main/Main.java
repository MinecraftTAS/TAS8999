package de.pfannekuchen.tas8999.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import de.pfannekuchen.tas8999.TASDiscordBot;

public class Main {

	private static final File propertiesFile = new File("bot.properties");
	
	public static void main(String[] args) throws LoginException, InterruptedException, FileNotFoundException, IOException {
		/* Load Configuration from File */
		final Properties configuration = new Properties();
		if (!propertiesFile.exists()) loadDefaultConfiguration();
		configuration.load(new FileInputStream(propertiesFile));
		
		/* Create and run Bot */
		final TASDiscordBot bot = new TASDiscordBot(configuration);
		new Thread(bot).run();
	}

	public static void loadDefaultConfiguration() throws IOException {
		propertiesFile.createNewFile();
		FileOutputStream stream = new FileOutputStream(propertiesFile);
		stream.write("# This is an auto-generated Configuration File. Please set a value for \"token\"\n#\n# Style\n#\n# %s will be replaced with the caller\n# %n will be replaced by the current channel\n# To have individual messages per channel use [#channel](message)\n#\n#\n# Add a Command\n#\n# Enter your command to the \"commands\" List (separeted by \',\')\n# commandnamehere=commandmessagehere\n# commandnameheredescription=descriptionhere\n#\n\ntoken=".getBytes(Charset.defaultCharset()));
		stream.close();
		System.exit(0);
	}
	
}
