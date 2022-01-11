/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.features.apply;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class ApplyGuild {

	public final List<ApplyUser> applyUserList;
	public final Message reactMessage;
	public final JsonElement currentSettings;
	public final boolean enable = true;
	public TextChannel waitInviteChannel = null;

	public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
		this.reactMessage = reactMessage;
		this.currentSettings = currentSettings;
		this.applyUserList =
			getApplyGuildUsersCache(reactMessage.getGuild().getId(), higherDepth(currentSettings, "guildName").getAsString());
		try {
			this.waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "applyWaitingChannel").getAsString());
		} catch (Exception ignored) {}
	}

	public ApplyGuild(Message reactMessage, JsonElement currentSettings, List<ApplyUser> prevApplyUsers) {
		this(reactMessage, currentSettings);
		applyUserList.addAll(prevApplyUsers);
	}

	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!enable) {
			return;
		}

		onMessageReactionAdd_ExistingApplyUser(event);
	}

	public void onMessageReactionAdd_ExistingApplyUser(MessageReactionAddEvent event) {
		ApplyUser findApplyUser = applyUserList
			.stream()
			.filter(applyUser -> applyUser.reactMessageId.equals(event.getMessageId()))
			.findFirst()
			.orElse(null);

		if (findApplyUser != null) {
			if (findApplyUser.onMessageReactionAdd(event)) {
				applyUserList.remove(findApplyUser);
			}
		}
	}

	public void onTextChannelDelete(ChannelDeleteEvent event) {
		applyUserList.removeIf(applyUser ->
			(applyUser.applicationChannelId != null && applyUser.applicationChannelId.equals(event.getChannel().getId())) ||
			(applyUser.staffChannelId != null && applyUser.staffChannelId.equals(event.getChannel().getId()))
		);
	}

	public String onButtonClick_NewApplyUser(ButtonInteractionEvent event) {
		if (!event.getMessageId().equals(reactMessage.getId())) {
			return null;
		}

		if (!event.getButton().getId().equals("create_application_button_" + higherDepth(currentSettings, "guildName").getAsString())) {
			return null;
		}

		ApplyUser runningApplication = applyUserList
			.stream()
			.filter(o1 -> o1.applyingUserId.equals(event.getUser().getId()))
			.findFirst()
			.orElse(null);

		if (runningApplication != null) {
			return "❌ There is already an application open in <#" + runningApplication.applicationChannelId + ">";
		}

		LinkedAccount linkedAccount = database.getByDiscord(event.getUser().getId());
		if (linkedAccount == null) {
			return "❌ You are not linked to the bot. Please run `+link [IGN]` and try again.";
		} else if (!linkedAccount.discord().equals(event.getUser().getId())) {
			return (
				"❌ Account " +
				linkedAccount.username() +
				" is linked with the Discord tag " +
				jda.retrieveUserById(linkedAccount.discord()).complete().getAsTag() +
				"\nYour current Discord tag is " +
				event.getUser().getAsTag() +
				".\nPlease relink and try again"
			);
		}

		JsonElement blacklisted = streamJsonArray(database.getApplyBlacklist(event.getGuild().getId()))
			.filter(blacklist ->
				higherDepth(blacklist, "uuid").getAsString().equals(linkedAccount.uuid()) ||
				higherDepth(blacklist, "username").getAsString().equals(linkedAccount.username())
			)
			.findFirst()
			.orElse(null);
		if (blacklisted != null) {
			return "❌ You have been blacklisted with reason `" + higherDepth(blacklisted, "reason").getAsString() + "`";
		}

		if (higherDepth(currentSettings, "applyScammerCheck", false)) {
			String scammerReason = getScammerReason(linkedAccount.uuid());
			if (scammerReason != null) {
				return "SBZ_SCAMMER_CHECK_" + scammerReason;
			}
		}

		Player player = new Player(linkedAccount.username());
		if (!player.isValid()) {
			return "❌ Unable to fetch player data. Failed cause: `" + player.getFailCause() + "`";
		} else {
			if (higherDepth(currentSettings, "ironmanOnly", false) && player.getAllProfileNames(true).length == 0) {
				return "❌ You have no ironman profiles created";
			}
		}

		ApplyUser toAdd = new ApplyUser(event, currentSettings, linkedAccount.username());
		if (toAdd.failCause != null) {
			return "❌ " + toAdd.failCause;
		}

		applyUserList.add(toAdd);

		return "✅ A new application was created in " + event.getGuild().getTextChannelById(toAdd.applicationChannelId).getAsMention();
	}

	public String onButtonClick(ButtonInteractionEvent event) {
		String waitingForInvite = onButtonClick_WaitingForInviteApplyUser(event);
		if (waitingForInvite != null) {
			return waitingForInvite;
		}

		boolean currentApplyUser = onButtonClick_CurrentApplyUser(event);
		if (currentApplyUser) {
			return "IGNORE_INTERNAL";
		}

		return onButtonClick_NewApplyUser(event);
	}

	public boolean onButtonClick_CurrentApplyUser(ButtonInteractionEvent event) {
		ApplyUser findApplyUser = applyUserList
			.stream()
			.filter(applyUser -> applyUser.reactMessageId.equals(event.getMessageId()))
			.findFirst()
			.orElse(null);

		return findApplyUser != null && findApplyUser.onButtonClick(event, this, false);
	}

	public String onButtonClick_WaitingForInviteApplyUser(ButtonInteractionEvent event) {
		if (waitInviteChannel == null) {
			return null;
		}

		if (!event.getChannel().getId().equals(waitInviteChannel.getId())) {
			return null;
		}

		if (!event.getComponentId().startsWith("apply_user_wait_" + higherDepth(currentSettings, "guildName").getAsString())) {
			return null;
		}

		if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
			JsonArray staffPingRoles = higherDepth(currentSettings, "applyStaffRoles").getAsJsonArray();
			boolean hasStaffRole = false;
			if (staffPingRoles.size() != 0) {
				for (JsonElement staffPingRole : staffPingRoles) {
					if (event.getMember().getRoles().contains(event.getGuild().getRoleById(staffPingRole.getAsString()))) {
						hasStaffRole = true;
						break;
					}
				}
			}

			if (!hasStaffRole) {
				return "❌ You are missing the required permissions in this guild to use that!";
			}
		}

		try {
			String[] channelRoleSplit = event
				.getComponentId()
				.split("apply_user_wait_" + higherDepth(currentSettings, "guildName").getAsString() + "_")[1].split("_");
			TextChannel toCloseChannel = event.getGuild().getTextChannelById(channelRoleSplit[0]);
			try {
				event.getGuild().addRoleToMember(channelRoleSplit[1], event.getGuild().getRoleById(channelRoleSplit[2])).queue();
			} catch (Exception ignored) {}
			applyUserList
				.stream()
				.filter(applyUser -> applyUser.applicationChannelId.equals(toCloseChannel.getId()))
				.findFirst()
				.ifPresent(applyUser -> applyUser.onButtonClick(event, this, true));
		} catch (Exception ignored) {}

		event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
		return "✅ Player was invited";
	}

	public boolean onGuildMessageReceived(MessageReceivedEvent event) {
		for (ApplyUser applyUser : applyUserList) {
			if (applyUser.onGuildMessageReceived(event)) {
				return true;
			}
		}
		return false;
	}

	public boolean onGuildMessageUpdate(MessageUpdateEvent event) {
		for (ApplyUser applyUser : applyUserList) {
			if (applyUser.onGuildMessageUpdate(event)) {
				return true;
			}
		}
		return false;
	}

	public boolean onGuildMessageDelete(MessageDeleteEvent event) {
		for (ApplyUser applyUser : applyUserList) {
			if (applyUser.onGuildMessageDelete(event)) {
				return true;
			}
		}
		return false;
	}
}
