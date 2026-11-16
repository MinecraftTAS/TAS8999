package com.minecrafttas.tas8999.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.minecrafttas.tas8999.utils.GuildStorage;

import net.dv8tion.jda.api.EmbedBuilder;
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
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class PingPermissions {

	private final Logger logger;
	private final GuildStorage storage;
	public static final int COLOR = 0x05808e;

	public PingPermissions(JDA bot, Logger logger) {
		this.logger = logger;
		// load guilds
		this.storage = new GuildStorage("ping_permissions");
		for (Guild guild : bot.getGuilds()) {

			CommandDataImpl pingCommand = CommandDataImpl.of(Command.Type.SLASH, "ping", "Check if you are allowed to ping a restricted role");
			pingCommand.setDefaultPermissions(DefaultMemberPermissions.ENABLED);

			CommandDataImpl pingpermissionCommand = CommandDataImpl.of(Command.Type.SLASH, "pingpermission", "Modify PingPermissions");
			pingpermissionCommand.setDefaultPermissions(DefaultMemberPermissions.DISABLED);

			OptionData pingableRoleOption = new OptionData(OptionType.ROLE, "pingablerole", "The role that is pinged by the bot", true);
			OptionData permittedMemberOption = new OptionData(OptionType.USER, "permitteduser", "The user that is permitted to use the ping", true);
			OptionData permittedRoleOption = new OptionData(OptionType.ROLE, "permittedrole", "The role that is permitted to use the ping", true);
			OptionData permittedChannelOption = new OptionData(OptionType.CHANNEL, "permittedchannel", "The channel that the ping is allowed", true);

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

			SubcommandData displayPermissions = new SubcommandData("display", "Display ping permissions for this role");
			displayPermissions.addOptions(pingableRoleOption);

			SubcommandData removePermissions = new SubcommandData("removeall", "Removes all permissions from a role");
			removePermissions.addOptions(pingableRoleOption);

			pingpermissionCommand.addSubcommandGroups(userGroupData, roleGroupData, channelGroupData);
			pingpermissionCommand.addSubcommands(displayPermissions, removePermissions);

			pingCommand.addOptions(pingableRoleOption);
			guild.upsertCommand(pingCommand).queue();
			guild.upsertCommand(pingpermissionCommand).queue();
		}
	}

	public record PingPermissionData(List<Member> permittedMembers, List<Role> permittedRoles, List<Channel> permittedChannels) {

		public PingPermissionData() {
			this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}

		public PingPermissionData(Guild guild, String data) {
			Matcher matcher = Pattern.compile("([\\d;]+)?\\|([\\d;]+)?\\|([\\d;]+)?").matcher(data);

			String members = "";
			String roles = "";
			String channels = "";

			if (matcher.find()) {
				members = matcher.group(1) != null ? matcher.group(1) : "";
				roles = matcher.group(2) != null ? matcher.group(2) : "";
				channels = matcher.group(3) != null ? matcher.group(3) : "";
			}

			this(guild, members, roles, channels);
		}

		public PingPermissionData(Guild guild, String permittedMembers, String permittedRoles, String permittedChannels) {
			this(guild, permittedMembers.split(";"), permittedRoles.split(";"), permittedChannels.split(";"));
		}

		public PingPermissionData(Guild guild, String[] permittedMembers, String[] permittedRoles, String[] permittedChannels) {
			List<Member> members = new ArrayList<>();
			if (permittedMembers.length != 0)
				members = Stream.of(permittedMembers).filter(e -> !e.isEmpty()).map(e -> guild.retrieveMemberById(e).complete()).collect(Collectors.toList());

			List<Role> roles = new ArrayList<>();
			if (permittedRoles.length != 0)
				roles = Stream.of(permittedRoles).filter(e -> !e.isEmpty()).map(e -> guild.getRoleById(e)).collect(Collectors.toList());

			List<Channel> channels = new ArrayList<>();
			if (permittedRoles.length != 0)
				channels = Stream.of(permittedChannels).filter(e -> !e.isEmpty()).map(e -> (Channel) guild.getGuildChannelById(e)).collect(Collectors.toList());

			this(members, roles, channels);
		}

		public PingPermissionData(List<Member> permittedMembers, List<Role> permittedRoles, List<Channel> permittedChannels) {
			this.permittedMembers = permittedMembers;
			this.permittedRoles = permittedRoles;
			this.permittedChannels = permittedChannels;
		}

		@Override
		public final String toString() {
			//@formatter:off
			return String.format("%s|%s|%s", 
					permittedMembers.stream().map(e -> e.getId()).collect(Collectors.joining(";")),
					permittedRoles.stream().map(e -> e.getId()).collect(Collectors.joining(";")),
					permittedChannels.stream().map(e -> e.getId()).collect(Collectors.joining(";"))
			);
			//@formatter:on
		}

		public boolean isMemberPermitted(Member member) {
			for (Member permittedMember : permittedMembers) {
				if (permittedMember.getIdLong() == member.getIdLong())
					return true;
			}
			return false;
		}

		public void addMember(Member member) {
			if (!isMemberPermitted(member))
				permittedMembers.add(member);
		}

		public void removeMember(Member member) {
			for (Iterator<Member> iterator = permittedMembers.iterator(); iterator.hasNext();) {
				Member permittedMember = (Member) iterator.next();
				if (permittedMember.getIdLong() == member.getIdLong())
					iterator.remove();
			}
		}

		public boolean isRolePermitted(Role role) {
			for (Role permittedRole : permittedRoles) {
				if (permittedRole.getIdLong() == role.getIdLong())
					return true;
			}
			return false;
		}

		public void addRole(Role role) {
			if (!isRolePermitted(role))
				permittedRoles.add(role);
		}

		public void removeRole(Role role) {
			for (Iterator<Role> iterator = permittedRoles.iterator(); iterator.hasNext();) {
				Role permittedRole = (Role) iterator.next();
				if (permittedRole.getIdLong() == role.getIdLong())
					iterator.remove();
			}
		}

		public boolean isMemberOrRolePermitted(Member member) {
			for (Role memberRole : member.getRoles()) {
				if (isRolePermitted(memberRole))
					return true;
			}

			return isMemberPermitted(member);
		}

		public boolean isChannelPermitted(Channel channel) {
			for (Channel permittedChannel : permittedChannels) {
				if (permittedChannel.getIdLong() == channel.getIdLong())
					return true;
			}
			return false;
		}

		public void addChannel(Channel channel) {
			if (!isChannelPermitted(channel))
				permittedChannels.add(channel);
		}

		public void removeChannel(Channel channel) {
			for (Iterator<Channel> iterator = permittedChannels.iterator(); iterator.hasNext();) {
				Channel permittedChannel = (Channel) iterator.next();
				if (permittedChannel.getIdLong() == channel.getIdLong())
					iterator.remove();
			}
		}

		public boolean isEmpty() {
			return permittedMembers.isEmpty() && permittedRoles.isEmpty() && permittedChannels.isEmpty();
		}
	}

	public void addMember(Role pingableRole, Member... permittedMember) {
		PingPermissionData permission = get(pingableRole);
		for (Member newMember : permittedMember) {
			permission.addMember(newMember);
		}

		put(pingableRole, permission);
	}

	public void removeUser(Role pingableRole, Member... permittedMember) {
		PingPermissionData permission = get(pingableRole);
		for (Member newMember : permittedMember) {
			permission.removeMember(newMember);
		}

		put(pingableRole, permission);
	}

	public void addRole(Role pingableRole, Role... permittedRole) {
		PingPermissionData permission = get(pingableRole);
		for (Role newRole : permittedRole) {
			permission.addRole(newRole);
		}

		put(pingableRole, permission);
	}

	public void removeRole(Role pingableRole, Role... permittedRole) {
		PingPermissionData permission = get(pingableRole);
		for (Role newRole : permittedRole) {
			permission.removeRole(newRole);
		}

		put(pingableRole, permission);
	}

	public void addChannel(Role pingableRole, Channel... permittedChannel) {
		PingPermissionData permission = get(pingableRole);
		for (Channel newChannel : permittedChannel) {
			permission.addChannel(newChannel);
		}

		put(pingableRole, permission);
	}

	public void removeChannel(Role pingableRole, Channel... permittedChannel) {
		PingPermissionData permission = get(pingableRole);
		for (Channel newChannel : permittedChannel) {
			permission.removeChannel(newChannel);
		}

		put(pingableRole, permission);
	}

	public PingPermissionData get(Role pingableRole) {
		Guild guild = pingableRole.getGuild();
		String data = storage.get(guild, pingableRole.getId());
		if (data == null)
			return new PingPermissionData();
		return new PingPermissionData(guild, data);
	}

	public void put(Role pingableRole, PingPermissionData data) {
		Guild guild = pingableRole.getGuild();
		if (data.isEmpty())
			remove(pingableRole);

		storage.set(guild, pingableRole.getId(), data.toString());
		storage.saveGuild(guild);
	}

	public void remove(Role pingableRole) {
		Guild guild = pingableRole.getGuild();
		storage.remove(guild, pingableRole.getId());
		storage.saveGuild(guild);
	}

	public void printRoleToPing(SlashCommandInteractionEvent event, Role pingableRole) {
		logger.info("{{}} Printing role to ping for user {}", event.getGuild().getName(), event.getUser().getName());
		PingPermissionData data = get(pingableRole);

		Channel channel = event.getChannel();
		Member author = event.getMember();

		EmbedBuilder builder = new EmbedBuilder();

		if (pingableRole.isMentionable()) {
			builder.setColor(COLOR);
			builder.setDescription("This role is publically pingable and not restricted!\n\n"
					+ "Discord still has no good way of assigning ping permissions to restricted roles, so instead, the bot will ping on your behalf!\n"
					+ "This command is only good for moderators who have access to announcement channels.");
			event.replyEmbeds(builder.build()).setEphemeral(true).queue();
			return;
		}

		if (!data.isMemberOrRolePermitted(author)) {
			builder.setColor(0xB90000);
			builder.setDescription("You don't have permission to ping this role!");
			event.replyEmbeds(builder.build()).setEphemeral(true).queue();
			return;
		}

		if (!data.isChannelPermitted(channel)) {
			String whitelistedChannels = data.permittedChannels.stream().map(e -> e.getAsMention()).collect(Collectors.joining("\n"));
			if (whitelistedChannels.isBlank()) {
				whitelistedChannels = "*No whitelisted channels. An admin probably forgot to add this*";
			}
			builder.setColor(0xB90000);
			builder.setDescription("You have permission to ping this role, however this channel is not whitelisted.");
			builder.addField("Whitelisted Channels", whitelistedChannels, false);
			event.replyEmbeds(builder.build()).setEphemeral(true).queue();
			return;
		}

		builder.setColor(COLOR);
		builder.setDescription(String.format("You can ping in this channel! Righclick on the role then click `Mention`\n %s", pingableRole.getAsMention()));
		String whitelistedChannels = data.permittedChannels.stream().map(e -> e.getAsMention()).collect(Collectors.joining("\n"));
		if (whitelistedChannels.isBlank()) {
			whitelistedChannels = "*No whitelisted channels. An admin probably forgot to add this*";
		}
		builder.addField("All Whitelisted Channels", whitelistedChannels, false);
		event.replyEmbeds(builder.build()).setEphemeral(true).queue();
	}

	public void printPermissions(SlashCommandInteractionEvent event, Role pingableRole) {
		logger.info("{{}} Printing permissions for user {}", event.getGuild().getName(), event.getUser().getName());
		PingPermissionData data = get(pingableRole);
		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(COLOR);
		builder.setTitle(String.format("Ping permissions for role %s", pingableRole.getName()));

		//@formatter:off
		String members = data.permittedMembers.stream()
				.map(member -> member.getUser().getAsMention())
				.collect(Collectors.joining("\n"));
		//@formatter:on

		builder.addField("Members", members, false);

		//@formatter:off
		String roles = data.permittedRoles.stream()
				.map(role -> role.getAsMention())
				.collect(Collectors.joining("\n"));
		//@formatter:on

		builder.addField("Roles", roles, false);

		//@formatter:off
		String channels = data.permittedChannels.stream()
				.map(channel -> channel.getAsMention())
				.collect(Collectors.joining("\n"));
		//@formatter:on

		builder.addField("Channels", channels, false);

		event.replyEmbeds(builder.build()).setEphemeral(true).queue();
	}

	public void onSlashCommand(SlashCommandInteractionEvent event) {
		if (event.getFullCommandName().replace(" ", "/").concat("/").startsWith("ping/")) {
			printRoleToPing(event, event.getOption("pingablerole").getAsRole());
			return;
		}

		switch (event.getSubcommandName()) {
			case "display":
				Role pingableRole = event.getOption("pingablerole").getAsRole();
				printPermissions(event, pingableRole);
				return;
			case "removeall":
				Role pingableRole2 = event.getOption("pingablerole").getAsRole();
				remove(pingableRole2);
				event.reply(String.format("Removed all permissions from %s", pingableRole2.getAsMention())).setEphemeral(true).queue();
				return;
		}

		if (event.getSubcommandGroup() == null) {
			event.reply("No subcommand group found").setEphemeral(true).queue();
			return;
		}

		switch (event.getSubcommandGroup()) {
			case "user":
				Role pingableRole = event.getOption("pingablerole").getAsRole();
				Member permittedUser = event.getOption("permitteduser").getAsMember();
				switch (event.getSubcommandName()) {
					case "add":
						addMember(pingableRole, permittedUser);
						logger.info(String.format("{%s} Added user %s to role %s", event.getGuild().getName(), permittedUser.getUser().getName(), pingableRole.getName()));
						event.reply(String.format("Added user %s to role %s", permittedUser.getUser().getAsMention(), pingableRole.getAsMention())).setEphemeral(true).queue();
						return;
					case "remove":
						removeUser(pingableRole, permittedUser);
						logger.info(String.format("{%s} Removed user %s from role %s", event.getGuild().getName(), permittedUser.getUser().getName(), pingableRole.getName()));
						event.reply(String.format("Removed user %s from role %s", permittedUser.getUser().getAsMention(), pingableRole.getAsMention())).setEphemeral(true).queue();
						return;
				}
				break;
			case "role":
				Role pingableRole2 = event.getOption("pingablerole").getAsRole();
				Role permittedRole = event.getOption("permittedrole").getAsRole();
				switch (event.getSubcommandName()) {
					case "add":
						addRole(pingableRole2, permittedRole);
						logger.info(String.format("{%s} Added role %s to role %s", event.getGuild().getName(), permittedRole.getName(), pingableRole2.getName()));
						event.reply(String.format("Added role %s to role %s", permittedRole.getAsMention(), pingableRole2.getAsMention())).setEphemeral(true).queue();
						return;
					case "remove":
						removeRole(pingableRole2, permittedRole);
						logger.info(String.format("{%s} Removed role %s from role %s", event.getGuild().getName(), permittedRole.getName(), pingableRole2.getName()));
						event.reply(String.format("Removed role %s from role %s", permittedRole.getAsMention(), pingableRole2.getAsMention())).setEphemeral(true).queue();
						return;
				}
				break;
			case "channel":
				Role pingableRole3 = event.getOption("pingablerole").getAsRole();
				Channel permittedChannel = event.getOption("permittedchannel").getAsChannel();
				switch (event.getSubcommandName()) {
					case "add":
						addChannel(pingableRole3, permittedChannel);
						logger.info(String.format("{%s} Added channel %s to role %s", event.getGuild().getName(), permittedChannel.getName(), pingableRole3.getName()));
						event.reply(String.format("Added channel %s to role %s", permittedChannel.getAsMention(), pingableRole3.getAsMention())).setEphemeral(true).queue();
						return;
					case "remove":
						removeChannel(pingableRole3, permittedChannel);
						logger.info(String.format("{%s} Removed channel %s from role %s", event.getGuild().getName(), permittedChannel.getName(), pingableRole3.getName()));
						event.reply(String.format("Removed channel %s from role %s", permittedChannel.getAsMention(), pingableRole3.getAsMention())).setEphemeral(true).queue();
						return;
				}
				break;
		}
	}

	public void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		String msgText = msg.getContentRaw();

		Guild guild = event.getGuild();
		Member author = event.getMember();
		Channel channel = event.getChannel();

		if (author.isOwner() || isBot(guild, author))
			return;

		Pattern rolePattern = MentionType.ROLE.getPattern();
		Matcher roleMatcher = rolePattern.matcher(msgText);

		if (!roleMatcher.find())
			return;

		// Iterate through all roles in the message and stop if one is not permitted
		do {
			String pingedRoleId = roleMatcher.group(1);
			Role pingedRole = guild.getRoleById(pingedRoleId);

			if (pingedRole.isMentionable())
				continue;

			PingPermissionData data = get(pingedRole);
			if (!data.isChannelPermitted(channel) || !data.isMemberOrRolePermitted(author)) {
				logger.info("{{}} Role {} is INVALID for user {} for channel {}", guild.getName(), pingedRole.getName(), author.getUser().getName(), channel.getName());
				return;
			}

			logger.info("{{}} Role {} is VALID", guild.getName(), pingedRole.getName());
		} while (roleMatcher.find());

		// Ensure @everyone and @here not getting called
		if (MentionType.EVERYONE.getPattern().matcher(msgText).find() || MentionType.HERE.getPattern().matcher(msgText).find()) {
			return;
		}

		MessageChannel msgChannel = (MessageChannel) channel;

		MessageCreateBuilder builder = new MessageCreateBuilder();

		builder.setAllowedMentions(List.of(MentionType.ROLE));
		builder.setContent(msgText);

		logger.info("{{}} Sending ping from {} to channel {}", guild.getName(), author.getUser().getName(), channel.getName());

		guild.getSelfMember().modifyNickname(author.getEffectiveName()).complete();
		msgChannel.sendMessage(builder.build()).queue(createdMessage -> {
			createdMessage.delete().queue(_ -> {
				guild.getSelfMember().modifyNickname(null).queue();
			});
		});

	}

	private boolean isBot(Guild guild, Member member) {
		return member.getIdLong() == guild.getSelfMember().getIdLong();
	}
}
