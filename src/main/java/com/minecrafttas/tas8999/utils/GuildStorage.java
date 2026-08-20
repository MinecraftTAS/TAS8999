package com.minecrafttas.tas8999.utils;

import static com.minecrafttas.tas8999.TAS8999.LOGGER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

import net.dv8tion.jda.api.entities.Guild;

/**
 * Storage class storing properties for each individual guild
 * @author Scribble
 */
public class GuildStorage {

	private final String name;
	private final Map<Long, Properties> properties;
	private final Path storageDir;

	/**
	 * Initialize guild storage
	 * @param name Storage name
	 */
	public GuildStorage(String name) {
		this.name = name;
		this.properties = new HashMap<>();
		this.storageDir = Path.of(name);
		try {
			Files.createDirectories(storageDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load properties for guild
	 * @param guild Guild
	 */
	private Properties loadGuild(Guild guild) {
		LOGGER.info("{{}} Loading {}", guild.getName(), name);

		// put new properties
		Properties prop = new Properties();
		this.properties.put(guild.getIdLong(), prop);

		// check storage file
		Path storageFile = this.storageDir.resolve(guild.getId() + ".xml");
		if (!Files.exists(storageFile))
			return prop;

		// load properties
		try {
			prop.loadFromXML(Files.newInputStream(storageFile));
		} catch (InvalidPropertiesFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return prop;
	}

	/**
	 * Save properties for guild
	 * @param guild Guild
	 */
	public void saveGuild(Guild guild) {
		LOGGER.info("{{}} Saving {}", guild.getName(), name);

		// check properties
		Properties prop = this.properties.get(guild.getIdLong());
		if (prop == null)
			return;

		// save properties
		Path storageFile = this.storageDir.resolve(guild.getId() + ".xml");
		try {
			prop.storeToXML(Files.newOutputStream(storageFile), String.format("Guild %s for guild: %s", name, guild.getName()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get or load guild properties from file
	 * @param guild Guild
	 * @return Properties
	 */
	public Properties getGuildProperties(Guild guild) {
		Properties props = this.properties.get(guild.getIdLong());
		if (props == null)
			props = this.loadGuild(guild);

		return props;
	}

	/**
	 * Check if guild property contains a key
	 * @param guild Guild
	 * @param key key
	 * @return Contains key
	 */
	public boolean contains(Guild guild, String key) {
		return this.getGuildProperties(guild).containsKey(key);
	}

	/**
	 * Get guild property value
	 * @param guild Guild
	 * @param key key
	 * @return Value
	 */
	public String get(Guild guild, String key) {
		return this.getGuildProperties(guild).getProperty(key);
	}
	
	public String getOrDefault(Guild guild, String key, String defaultValue) {
		String value = get(guild, key);
		return value != null ? value : defaultValue;
	}

	/**
	 * Set guild property value
	 * @param guild Guild
	 * @param key key
	 * @param value Value
	 */
	public void set(Guild guild, String key, String value) {
		this.getGuildProperties(guild).put(key, value);
	}

	/**
	 * Remove guild property key
	 * @param guild Guild
	 * @param key key
	 */
	public String remove(Guild guild, String key) {
		return (String) this.getGuildProperties(guild).remove(key);
	}

}
