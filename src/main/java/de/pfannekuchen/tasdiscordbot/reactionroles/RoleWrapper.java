package de.pfannekuchen.tasdiscordbot.reactionroles;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.entities.Guild;

public class RoleWrapper {

	private String printText;

	private String id;

	private static final Pattern rolePattern = Pattern.compile("<@&(\\d+)>");

	public RoleWrapper(Guild guild, String roleIdIn) throws IllegalArgumentException {
		if (!isRoleAvailable(guild, roleIdIn)) {
			throw new IllegalArgumentException("The bot can't access this role: " + roleIdIn);
		}

		id = extractRole(roleIdIn);
		printText = roleIdIn;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return printText;
	}

	public static boolean isRoleAvailable(Guild guild, String roleIn) {
		try {
			return guild.getRoleById(extractRole(roleIn)) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static String extractRole(String roleIn) {
		Matcher matcher = rolePattern.matcher(roleIn);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return roleIn;
	}
}
