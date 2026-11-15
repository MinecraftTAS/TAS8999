package com.minecrafttas.tas8999.modules;

import static com.minecrafttas.tas8999.TAS8999.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minecrafttas.tas8999.utils.GuildStorage;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class PingPermissions {

	private final List<PingPermissionData> permissionList = new ArrayList<>();
	private final GuildStorage storage;

	public PingPermissions(JDA bot) {

		// load guilds
		this.storage = new GuildStorage("ping_permissions");
		for (Guild guild : bot.getGuilds()) {
			// load permissions
			Properties prop = this.storage.getGuildProperties(guild);
			prop.forEach((key, value) -> {
				String cmd = (String) key;
				LOGGER.info("{{}} Readding pingpermission: {}", guild.getName(), cmd);

				String[] data = ((String) value).split("|", 3);

				Role pingableRole = guild.getRoleById(data[0]);

				List<Member> members = new ArrayList<>();
				for (String stringUser : data[1].split(";")) {
					members.add(guild.getMemberById(stringUser));
				}

				List<Role> roles = new ArrayList<>();
				for (String stringRole : data[2].split(";")) {
					roles.add(guild.getRoleById(stringRole));
				}

				List<Channel> channels = new ArrayList<>();
				for (String stringChannel : data[3].split(";")) {
					channels.add(guild.getNewsChannelById(stringChannel));
				}

				addPingPermission(pingableRole, members, roles, channels);
			});

			CommandDataImpl pingpermissionCommand = CommandDataImpl.of(Command.Type.SLASH, "pingpermission", "Modify PingPermissions");

			OptionData pingableRoleOption = new OptionData(OptionType.ROLE, "pingablerole", "The role that is pinged by the bot");
			OptionData permittedMemberOption = new OptionData(OptionType.USER, "permitteduser", "The user that is permitted to use the ping");
			OptionData permittedRoleOption = new OptionData(OptionType.ROLE, "permittedrole", "The role that is permitted to use the ping");
			OptionData permittedChannelOption = new OptionData(OptionType.CHANNEL, "permittedchannel", "The channel that the ping is allowed");

			SubcommandGroupData userGroupData = new SubcommandGroupData("user", "userSubcommand");
			SubcommandData addMemberSubCommand = new SubcommandData("add", "Add a user to the permissions");
			addMemberSubCommand.addOptions(pingableRoleOption, permittedMemberOption);
			SubcommandData removeMemberSubCommand = new SubcommandData("remove", "Remove a user to the permissions");
			removeMemberSubCommand.addOptions(pingableRoleOption, permittedMemberOption);
			userGroupData.addSubcommands(addMemberSubCommand, removeMemberSubCommand);

			SubcommandGroupData roleGroupData = new SubcommandGroupData("role", "roleSubcommand");
			SubcommandData addRoleSubCommand = new SubcommandData("add", "Add a role to the permissions");
			addRoleSubCommand.addOptions(pingableRoleOption, permittedRoleOption);
			SubcommandData removeRoleSubCommand = new SubcommandData("remove", "Remove a role to the permissions");
			removeRoleSubCommand.addOptions(pingableRoleOption, permittedRoleOption);
			roleGroupData.addSubcommands(addRoleSubCommand, removeRoleSubCommand);

			SubcommandGroupData channelGroupData = new SubcommandGroupData("channel", "channelSubcommand");
			SubcommandData addChannelSubCommand = new SubcommandData("add", "Add a channel to the permissions");
			addChannelSubCommand.addOptions(pingableRoleOption, permittedChannelOption);
			SubcommandData removeChannelSubCommand = new SubcommandData("remove", "Remove a channel to the permissions");
			removeChannelSubCommand.addOptions(pingableRoleOption, permittedChannelOption);
			channelGroupData.addSubcommands(addChannelSubCommand, removeChannelSubCommand);

			pingpermissionCommand.addSubcommandGroups(userGroupData, roleGroupData, channelGroupData);
			guild.upsertCommand(pingpermissionCommand).queue();
		}
	}

	public record PingPermissionData(Role pingableRole, List<Member> permittedMembers, List<Role> permittedRoles, List<Channel> permittedChannels) {

		@Override
		public final String toString() {
			List<String> users = new ArrayList<>();
			permittedMembers.forEach(user -> {
				users.add(user.getId());
			});
			List<String> roles = new ArrayList<>();
			permittedRoles.forEach(role -> {
				roles.add(role.getId());
			});
			List<String> channels = new ArrayList<>();
			permittedChannels.forEach(channel -> {
				channels.add(channel.getId());
			});
			return String.format("%s|%s|%s|%s", pingableRole.getId(), String.join(";", users), String.join(";", channels));
		}

		public boolean isMemberPermitted(Member member) {
			for (Member permittedMember : permittedMembers) {
				if (permittedMember.getIdLong() == member.getIdLong())
					return true;
			}

			for (Role memberRole : member.getRoles()) {
				if (isRolePermitted(memberRole))
					return true;
			}

			return false;
		}

		public boolean isRolePermitted(Role role) {
			for (Role permittedRole : permittedRoles) {
				if (permittedRole.getIdLong() == role.getIdLong())
					return true;
			}
			return false;
		}

		public boolean isChannelPermitted(Channel channel) {
			for (Channel permittedChannel : permittedChannels) {
				if (permittedChannel.getIdLong() == channel.getIdLong())
					return true;
			}
			return false;
		}

	}

	public void addUser(Role pingableRole, Member... permittedMember) {
		PingPermissionData permission = get(pingableRole);
		for (Member newMember : permittedMember) {
			permission.permittedMembers.add(newMember);
		}
	}

	public void removeUser(Role pingableRole, Member... permittedMember) {
		PingPermissionData permission = get(pingableRole);
		for (Member newMember : permittedMember) {
			permission.permittedMembers.remove(newMember);
		}
	}

	public void addRole(Role pingableRole, Role... permittedRole) {
		PingPermissionData permission = get(pingableRole);
		for (Role newMember : permittedRole) {
			permission.permittedRoles.add(newMember);
		}
	}

	public void removeRole(Role pingableRole, Role... permittedRole) {
		PingPermissionData permission = get(pingableRole);
		for (Role newMember : permittedRole) {
			permission.permittedRoles.remove(newMember);
		}
	}

	public void addChannel(Role pingableRole, Channel... permittedChannel) {
		PingPermissionData permission = get(pingableRole);
		for (Channel newChannel : permittedChannel) {
			permission.permittedChannels.add(newChannel);
		}
	}

	public void removeChannel(Role pingableRole, Channel... permittedChannel) {
		PingPermissionData permission = get(pingableRole);
		for (Channel newChannel : permittedChannel) {
			permission.permittedChannels.remove(newChannel);
		}
	}

	public PingPermissionData get(Role pingableRole) {
		for (PingPermissionData permission : permissionList) {
			if (permission.pingableRole.getIdLong() == pingableRole.getIdLong()) {
				return permission;
			}
		}

		PingPermissionData newPermission = new PingPermissionData(pingableRole, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		permissionList.add(newPermission);
		return newPermission;
	}

	public void printPermissions(Role pingableRole) {

	}

	public void addPingPermission(Role pingableRole, List<Member> members, List<Role> permittedRoles, List<Channel> permittedChannels) {
		permissionList.add(new PingPermissionData(pingableRole, members, permittedRoles, permittedChannels));
	}

	public void onSlashCommand(SlashCommandInteractionEvent event) {
		switch (event.getSubcommandGroup()) {
			case "user":
				Role pingableRole = event.getOption("pingablerole").getAsRole();
				Member permittedUser = event.getOption("permitteduser").getAsMember();
				switch (event.getSubcommandName()) {
					case "add":
						addUser(pingableRole, permittedUser);
						event.reply(String.format("Added user %s to role %s", permittedUser.getUser().getName(), pingableRole.getName())).setEphemeral(true).queue();
						break;
					case "remove":
						removeUser(pingableRole, permittedUser);
						event.reply(String.format("Removed user %s to role %s", permittedUser.getUser().getName(), pingableRole.getName())).setEphemeral(true).queue();
						break;
				}
				break;
			case "role":
				Role pingableRole2 = event.getOption("pingablerole").getAsRole();
				Role permittedRole = event.getOption("permittedrole").getAsRole();
				switch (event.getSubcommandName()) {
					case "add":
						addRole(pingableRole2, permittedRole);
						event.reply(String.format("Added role %s to role %s", permittedRole.getName(), pingableRole2.getName())).setEphemeral(true).queue();
						break;
					case "remove":
						removeRole(pingableRole2, permittedRole);
						event.reply(String.format("Removed role %s to role %s", permittedRole.getName(), pingableRole2.getName())).setEphemeral(true).queue();
						break;
				}
				break;
			case "channel":
				Role pingableRole3 = event.getOption("pingablerole").getAsRole();
				Channel permittedChannel = event.getOption("permittedchannel").getAsChannel();
				switch (event.getSubcommandName()) {
					case "add":
						addChannel(pingableRole3, permittedChannel);
						event.reply(String.format("Added channel %s to role %s", permittedChannel.getName(), pingableRole3.getName())).setEphemeral(true).queue();
						break;
					case "remove":
						removeChannel(pingableRole3, permittedChannel);
						event.reply(String.format("Removed channel %s to role %s", permittedChannel.getName(), pingableRole3.getName())).setEphemeral(true).queue();
						break;
				}
				break;
		}
	}

	public void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		String msgText = msg.getContentRaw();

		Pattern rolePattern = Pattern.compile("<@&(\\d{19})>");
		Matcher roleMatcher = rolePattern.matcher(msgText);

		if (!roleMatcher.find()) {
			return;
		}

		Guild guild = event.getGuild();
		Member author = event.getMember();
		Channel channel = event.getChannel();

		String pingedRoleId = roleMatcher.group(1);
		Role pingedRole = guild.getRoleById(pingedRoleId);

//		if (author.isOwner())
//			return;

		PingPermissionData data = get(pingedRole);
		if (!data.isChannelPermitted(channel) || !data.isMemberPermitted(author))
			return;

		if (!channel.getType().isMessage()) {
			return;
		}

		MessageChannel msgChannel = (MessageChannel) channel;

		MessageCreateBuilder builder = new MessageCreateBuilder();
		builder.setAllowedMentions(List.of(MentionType.ROLE));
		builder.setContent(msgText);
		msgChannel.sendMessage(builder.build()).queue(createdMessage -> {
			createdMessage.delete().queue();
		});
	}
}
