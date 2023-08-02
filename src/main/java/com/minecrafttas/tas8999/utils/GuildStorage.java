package com.minecrafttas.tas8999.utils;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.minecrafttas.tas8999.TAS8999.LOGGER;

/**
 * Storage class storing properties for each individual guild
 * @author Scribble
 */
public class GuildStorage {

	private final String name;
	private final Map<Long, Properties> properties;
	private final File storageDir;

	/**
	 * Initialize guild storage
	 * @param name Storage name
	 */
	public GuildStorage(String name) {
		this.name = name;
		this.properties = new HashMap<>();
		this.storageDir = new File(name);
		this.storageDir.mkdirs();
	}

	/**
	 * Load properties for guild
	 * @param guild Guild
	 */
	@SneakyThrows
	private Properties loadGuild(Guild guild) {
		LOGGER.info("{{}} Loading {}", guild.getName(), name);

		// put new properties
		var prop = new Properties();
		this.properties.put(guild.getIdLong(), prop);

		// check storage file
		var storageFile = new File(this.storageDir, guild.getId() + ".xml");
		if (!storageFile.exists())
			return prop;

		// load properties
		prop.loadFromXML(Files.newInputStream(storageFile.toPath()));

		return prop;
	}

	/**
	 * Save properties for guild
	 * @param guild Guild
	 */
	@SneakyThrows
	public void saveGuild(Guild guild) {
		LOGGER.info("{{}} Saving {}", guild.getName(), name);

		// check properties
		var prop = this.properties.get(guild.getIdLong());
		if (prop == null)
			return;

		// save properties
		var storageFile = new File(this.storageDir, guild.getId() + ".xml");
		prop.storeToXML(Files.newOutputStream(storageFile.toPath()),String.format("Guild %s for guild: %s", name, guild.getName()), StandardCharsets.UTF_8);
	}

	/**
	 * Get or load guild properties from file
	 * @param guild Guild
	 * @return Properties
	 */
	public Properties getGuildProperties(Guild guild) {
		var props = this.properties.get(guild.getIdLong());
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
