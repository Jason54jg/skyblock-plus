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

package com.skyblockplus.skills;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.SkillsStruct;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;

public class SkillsCommand extends Command {

	public SkillsCommand() {
		this.name = "skills";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "skill" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerSkill(String username, String profileName, PaginatorEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);

		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = event.getPaginator();
			PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_PAGES);

			EmbedBuilder eb = player.defaultPlayerEmbed();
			double trueSA = 0;
			double progressSA = 0;
			for (String skillName : ALL_SKILL_NAMES) {
				SkillsStruct skillInfo = player.getSkill(skillName);
				if (skillInfo != null) {
					eb.addField(
						SKILLS_EMOJI_MAP.get(skillName) + " " + capitalizeString(skillInfo.name()) + " (" + skillInfo.currentLevel() + ")",
						simplifyNumber(skillInfo.expCurrent()) +
						" / " +
						simplifyNumber(skillInfo.expForNext()) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.totalExp()) +
						"\nProgress: " +
						(skillInfo.isMaxed() ? "MAX" : roundProgress(skillInfo.progressToNext())),
						true
					);
					if (!COSMETIC_SKILL_NAMES.contains(skillName)) {
						trueSA += skillInfo.currentLevel();
						progressSA += skillInfo.getProgressLevel();
					}
				} else {
					eb.addField(SKILLS_EMOJI_MAP.get(skillName) + " " + capitalizeString(skillName) + " (?) ", "Unable to retrieve", true);
				}
			}
			trueSA /= SKILL_NAMES.size();
			progressSA /= SKILL_NAMES.size();
			eb.setDescription("True skill average: " + roundAndFormat(trueSA) + "\nProgress skill average: " + roundAndFormat(progressSA));
			extras.addEmbedPage(eb);

			eb = player.defaultPlayerEmbed();
			JsonElement jacobStats = higherDepth(player.profileJson(), "jacob2");
			int bronze = higherDepth(jacobStats, "medals_inv.bronze", 0);
			int silver = higherDepth(jacobStats, "medals_inv.silver", 0);
			int gold = higherDepth(jacobStats, "medals_inv.gold", 0);
			eb.addField(
				"Medals | " + (bronze + silver + gold),
				"\uD83E\uDD49 Bronze: " + bronze + "\n\uD83E\uDD48 Silver: " + silver + "\n\uD83E\uDD47 Gold: " + gold,
				false
			);
			if (higherDepth(jacobStats, "unique_golds2") != null && !higherDepth(jacobStats, "unique_golds2").getAsJsonArray().isEmpty()) {
				eb.addField(
					"Unique Golds | " + higherDepth(jacobStats, "unique_golds2").getAsJsonArray().size(),
					streamJsonArray(higherDepth(jacobStats, "unique_golds2").getAsJsonArray())
						.map(i ->
							getEmoji(i.getAsString().equals("MUSHROOM_COLLECTION") ? "RED_MUSHROOM" : i.getAsString()) +
							" " +
							idToName(i.getAsString())
						)
						.collect(Collectors.joining("\n ")),
					false
				);
			} else {
				eb.addField("Unique Golds", "None", false);
			}
			if (higherDepth(jacobStats, "perks") != null && higherDepth(jacobStats, "perks").getAsJsonObject().size() > 0) {
				StringBuilder ebStr = new StringBuilder();
				for (Map.Entry<String, JsonElement> perk : higherDepth(jacobStats, "perks").getAsJsonObject().entrySet()) {
					ebStr
						.append("\n⭐ ")
						.append(capitalizeString(perk.getKey().replace("_", " ")))
						.append(": ")
						.append(perk.getValue().getAsInt());
				}
				eb.addField("Perks", ebStr.toString(), false);
			} else {
				eb.addField("Perks", "None", false);
			}
			Map<String, Long> contests = higherDepth(jacobStats, "contests")
				.getAsJsonObject()
				.keySet()
				.stream()
				.map(s -> s.endsWith("INK_SACK:3") ? "INK_SACK:3" : s.substring(s.lastIndexOf(":") + 1))
				.collect(Collectors.groupingBy(c -> c, Collectors.counting()));
			StringBuilder ebStr = new StringBuilder();
			for (Map.Entry<String, Long> entry : contests.entrySet()) {
				ebStr
					.append("\n")
					.append(getEmoji(entry.getKey().equals("MUSHROOM_COLLECTION") ? "RED_MUSHROOM" : entry.getKey()))
					.append(" ")
					.append(idToName(entry.getKey()))
					.append(" - ")
					.append(entry.getValue());
			}
			eb.addField("Participated Contests | " + higherDepth(jacobStats, "contests").getAsJsonObject().size(), ebStr.toString(), false);

			extras.addEmbedPage(eb);

			event.paginate(paginateBuilder.setPaginatorExtras(extras));
			return null;
		}
		return player.getFailEmbed();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerSkill(player, args.length == 3 ? args[2] : null, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
