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

package com.skyblockplus.features.setup;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class SetupCommand extends Command {

	public SetupCommand() {
		this.name = "setup";
		this.cooldown = globalCooldown;
		this.botPermissions = defaultPerms();
	}

	public static List<ActionRow> getSetupActionRow() {
		return Arrays.asList(
			ActionRow.of(
				Button.primary("setup_command_verify", "Verification"),
				Button.primary("setup_command_guild", "Guild Application, Roles & Ranks"),
				Button.primary("setup_command_roles", "Skyblock Roles"),
				Button.primary("setup_command_prefix", "Prefix")
			),
			ActionRow.of(
				Button.primary("setup_command_jacob", "Farming Event Notifications"),
				Button.primary("setup_command_mayor", "Mayor Notifications"),
				Button.primary("setup_command_fetchur", "Fetchur Notifications")
			)
		);
	}

	public static EmbedBuilder getSetupEmbed() {
		return defaultEmbed("Setup")
			.setDescription(
				"Choose one of the buttons below to setup the corresponding feature. Note that setting a feature can/will override previous settings."
			);
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				ebMessage.editMessageEmbeds(getSetupEmbed().build()).setActionRows(getSetupActionRow()).queue();
			}
		}
			.setAdminCommand(true)
			.queue();
	}
}
