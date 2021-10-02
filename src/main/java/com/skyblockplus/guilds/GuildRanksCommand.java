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

package com.skyblockplus.guilds;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Hypixel.getGuildFromPlayer;
import static com.skyblockplus.utils.Hypixel.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.GuildRanksStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.io.FileReader;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildRanksCommand extends Command {

	public GuildRanksCommand() {
		this.name = "guild-rank";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-rank", "g-ranks" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2 && args[1].toLowerCase().startsWith("u:")) {
					paginate(getLeaderboard(args[1].split(":")[1], event));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getLeaderboard(String username, CommandEvent event) {
		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (usernameUuid.isNotValid()) {
			return invalidEmbed(usernameUuid.getFailCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.getUuid());
		if (guildResponse.isNotValid()) {
			return invalidEmbed(guildResponse.getFailCause());
		}

		JsonElement guildJson = guildResponse.getResponse();

		String guildId = higherDepth(guildJson, "_id").getAsString();
		String guildName = higherDepth(guildJson, "name").getAsString();
		if (!guildName.equals("Skyblock Forceful") && !guildName.equals("Skyblock Gods")) {
			return defaultEmbed("Only for SBF or SBG right now");
		}

		List<String> staffRankNames = new ArrayList<>();
		JsonElement lbSettings;
		List<String> rankTypes = new ArrayList<>();

		try {
			lbSettings =
				higherDepth(
					JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json")),
					guildId + ".guild_leaderboard"
				);

			for (JsonElement i : higherDepth(lbSettings, "staff_ranks").getAsJsonArray()) {
				staffRankNames.add(i.getAsString().toLowerCase());
			}

			for (JsonElement i : higherDepth(lbSettings, "types").getAsJsonArray()) {
				rankTypes.add(i.getAsString().toLowerCase());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return defaultEmbed("Error getting data");
		}

		boolean ignoreStaff = higherDepth(lbSettings, "ignore_staff").getAsBoolean();

		JsonArray guildMembers = higherDepth(guildJson, "members").getAsJsonArray();
		Map<String, String> ranksMap = new HashMap<>();
		for (JsonElement guildM : guildMembers) {
			ranksMap.put(higherDepth(guildM, "uuid").getAsString(), higherDepth(guildM, "rank").getAsString().toLowerCase());
		}

		List<String> uniqueGuildUuid = new ArrayList<>();
		List<GuildRanksStruct> gMembers = new ArrayList<>();
		JsonArray guildLbJson = higherDepth(getJson("https://hypixel-app-api.senither.com/leaderboard/players/" + guildId), "data")
			.getAsJsonArray();
		for (JsonElement lbM : guildLbJson) {
			String lbUuid = higherDepth(lbM, "uuid").getAsString().replace("-", "");
			String curRank = ranksMap.get(lbUuid);

			if (curRank != null) {
				if (ignoreStaff && staffRankNames.contains(curRank)) {
					continue;
				}

				gMembers.add(
					new GuildRanksStruct(
						higherDepth(lbM, "username").getAsString(),
						higherDepth(lbM, "average_skill_progress").getAsDouble(),
						higherDepth(lbM, "total_slayer").getAsDouble(),
						higherDepth(lbM, "catacomb").getAsDouble(),
						higherDepth(lbM, "weight").getAsDouble(),
						curRank
					)
				);
				uniqueGuildUuid.add(higherDepth(lbM, "username").getAsString());
			}
		}

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSlayer()));
		ArrayList<GuildRanksStruct> guildSlayer = new ArrayList<>(gMembers);

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getSkills()));
		ArrayList<GuildRanksStruct> guildSkills = new ArrayList<>(gMembers);

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getCatacombs()));
		ArrayList<GuildRanksStruct> guildCatacombs = new ArrayList<>(gMembers);

		gMembers.sort(Comparator.comparingDouble(o1 -> -o1.getWeight()));
		ArrayList<GuildRanksStruct> guildWeight = new ArrayList<>(gMembers);

		for (String s : uniqueGuildUuid) {
			int slayerRank = -1;
			int skillsRank = -1;
			int catacombsRank = -1;
			int weightRank = -1;

			if (rankTypes.contains("slayer")) {
				for (int j = 0; j < guildSlayer.size(); j++) {
					try {
						if (s.equals(guildSlayer.get(j).getName())) {
							slayerRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (rankTypes.contains("skills")) {
				for (int j = 0; j < guildSkills.size(); j++) {
					try {
						if (s.equals(guildSkills.get(j).getName())) {
							skillsRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (rankTypes.contains("catacombs")) {
				for (int j = 0; j < guildCatacombs.size(); j++) {
					try {
						if (s.equals(guildCatacombs.get(j).getName())) {
							catacombsRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (rankTypes.contains("weight")) {
				for (int j = 0; j < guildWeight.size(); j++) {
					try {
						if (s.equals(guildWeight.get(j).getName())) {
							weightRank = j;
							break;
						}
					} catch (NullPointerException ignored) {}
				}
			}

			if (guildName.equals("Skyblock Forceful")) {
				if (slayerRank < skillsRank) {
					guildSkills.set(skillsRank, null);
					if (slayerRank < catacombsRank) {
						guildCatacombs.set(catacombsRank, null);
					} else {
						guildSlayer.set(slayerRank, null);
					}
				} else {
					guildSlayer.set(slayerRank, null);
					if (skillsRank < catacombsRank) {
						guildCatacombs.set(catacombsRank, null);
					} else {
						guildSkills.set(skillsRank, null);
					}
				}
			}
		}

		ArrayList<ArrayList<GuildRanksStruct>> guildLeaderboards = new ArrayList<>();

		if (rankTypes.contains("slayer")) {
			guildLeaderboards.add(guildSlayer);
		}
		if (rankTypes.contains("skills")) {
			guildLeaderboards.add(guildSkills);
		}
		if (rankTypes.contains("catacombs")) {
			guildLeaderboards.add(guildCatacombs);
		}
		if (rankTypes.contains("weight")) {
			guildLeaderboards.add(guildWeight);
		}

		JsonArray ranksArr = higherDepth(lbSettings, "ranks").getAsJsonArray();

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(20);
		int totalChange = 0;
		for (ArrayList<GuildRanksStruct> currentLeaderboard : guildLeaderboards) {
			for (int i = 0; i < currentLeaderboard.size(); i++) {
				GuildRanksStruct currentPlayer = currentLeaderboard.get(i);
				if (currentPlayer == null) {
					continue;
				}

				if (staffRankNames.contains(currentPlayer.getGuildRank())) {
					continue;
				}

				String playerRank = currentPlayer.getGuildRank().toLowerCase();
				String playerUsername = currentPlayer.getName();

				for (JsonElement rank : ranksArr) {
					if (i <= higherDepth(rank, "range", 0) - 1) {
						JsonArray rankNames = higherDepth(rank, "names").getAsJsonArray();
						List<String> rankNamesList = new ArrayList<>();
						for (JsonElement rankName : rankNames) {
							rankNamesList.add(rankName.getAsString());
						}

						if (!rankNamesList.contains(playerRank.toLowerCase())) {
							paginateBuilder.addItems(("- /g setrank " + fixUsername(playerUsername) + " " + rankNamesList.get(0)));
							totalChange++;
						}
						break;
					}
				}
			}
		}

		paginateBuilder
			.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle("Rank changes for " + guildName)
					.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/guilds/" + guildId)
					.setEveryPageText("**Total rank changes:** " + totalChange)
			)
			.build()
			.paginate(event.getChannel(), 0);

		return null;
	}
}
