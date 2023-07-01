package com.minecrafttas.tas8999.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.slf4j.Logger;

import net.dv8tion.jda.api.entities.Guild;

public class Storable {
	private String name;
	
	private final HashMap<Long, Properties> guildProperties = new HashMap<>();
	protected final File storageDir;
	protected final Logger LOGGER;
	
	public Storable(String name, File directory, Logger logger) {
		this.name = name;
		this.storageDir = directory;
		if(!storageDir.exists()) {
			storageDir.mkdir();
		}
		this.LOGGER = logger;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	protected String getName() {
		return name;
	}
	
	public void loadForGuild(Guild guild) {
		Properties prop = new Properties();

		File submissionFile = new File(storageDir, guild.getId() + ".xml");
		if (submissionFile.exists()) {
			prop = load(guild, submissionFile);
			guildProperties.put(guild.getIdLong(), prop);
		}
	}
	
	public void save(Guild guild, Properties prop) {

		LOGGER.info("{{}} Saving {}", guild.getName(), name);
		File submissionFile = new File(storageDir, guild.getId() + ".xml");

		try {
			FileOutputStream fos = new FileOutputStream(submissionFile);
			prop.storeToXML(fos, String.format("Guild %s for guild: %s", name, guild.getName()), "UTF-8");
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected Properties load(Guild guild, File file) {
		LOGGER.info("{{}} Loading {}", guild.getName(), name);
		Properties guildConfig = new Properties();
		try {
			FileInputStream fis = new FileInputStream(file);
			guildConfig.loadFromXML(fis);
			fis.close();
		} catch (InvalidPropertiesFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return guildConfig;
	}
	
	protected boolean containsKey(Guild guild, String key) {
		return getGuildProperty(guild).containsKey(key);
	}
	
	protected String get(Guild guild, String key) {
		return getGuildProperty(guild).getProperty(key);
	}
	
	protected void put(Guild guild, String key, String value) {
		Properties prop = getGuildProperty(guild);
		prop.put(key, value);
		putGuildProperty(guild, prop);
	}
	
	protected void remove(Guild guild, String key) {
		Properties prop = getGuildProperty(guild);
		prop.remove(key);
		putGuildProperty(guild, prop);
	}
	
	protected Properties getGuildProperty(Guild guild) {
		return guildProperties.getOrDefault(guild.getIdLong(), new Properties());
	}
	
	protected void putGuildProperty(Guild guild, Properties property) {
		guildProperties.put(guild.getIdLong(), property);
		save(guild, property);
	}
	
	protected void removeGuildProperty(Guild guild) {
		File submissionFile = new File(storageDir, guild.getId() + ".xml");
		if (submissionFile.exists()) {
			submissionFile.delete();
		}
	}
}
