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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;
import static com.skyblockplus.utils.database.LeaderboardDatabase.isValidType;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCommand extends Command {

	public LeaderboardCommand() {
		this.name = "leaderboard";
		this.cooldown = globalCooldown + 3;
		this.aliases = new String[] { "lb" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getLeaderboard(
		String lbType,
		String username,
		Player.Gamemode gamemode,
		int page,
		int rank,
		double amount,
		PaginatorEvent event
	) {
		lbType = getType(lbType);

		if (!isValidType(lbType)) {
			return invalidEmbed(lbType + " is an invalid leaderboard type. Use `/help leaderboard` to see valid types");
		}

		Player player = new Player(username);
		if (!player.isValid()) {
			return player.getFailEmbed();
		}

		CustomPaginator.Builder paginateBuilder = event.getPaginator().setColumns(2).setItemsPerPage(20);
		Map<String, Double> cacheList = leaderboardDatabase.getLeaderboard(lbType, gamemode);

		int playerRank = -1;
		double closestAmt = -1;
		int idx = 1;
		String amt = "Not on leaderboard";
		int curRank = 1;
		for (Map.Entry<String, Double> entry : cacheList.entrySet()) {
			double curAmount = lbType.equals("networth") ? entry.getValue().longValue() : entry.getValue();
			String formattedAmt = roundAndFormat(curAmount);
			paginateBuilder.addItems("`" + (curRank) + ")` " + fixUsername(entry.getKey()) + ": " + formattedAmt);

			if (entry.getKey().equals(player.getUsername())) {
				playerRank = curRank;
				amt = formattedAmt;
			}

			if (amount != -1 && (closestAmt == -1 || Math.abs(curAmount - amount) < closestAmt)) {
				closestAmt = Math.abs(curAmount - amount);
				idx = curRank;
			}
			curRank++;
		}
		if (rank != -1) {
			page = (rank - 1) / 20 + 1;
		} else if (amount != -1) {
			page = (idx - 1) / 20 + 1;
		} else if (page == -1) {
			page = (playerRank - 1) / 20 + 1;
		}

		String ebStr =
			"**Player:** " +
			player.getUsernameFixed() +
			"\n**Rank:** " +
			(playerRank == -1 ? "Not on leaderboard" : "#" + (playerRank)) +
			"\n**" +
			capitalizeString(lbType.replace("_", " ")) +
			":** " +
			amt;

		paginateBuilder.setPaginatorExtras(
			new PaginatorExtras()
				.setEveryPageText(ebStr)
				.setEveryPageTitle("Global Leaderboard | " + capitalizeString(gamemode.toString()))
				.setEveryPageTitleUrl("https://hypixel-leaderboard.senither.com/players")
		);

		event.paginate(paginateBuilder, page);
		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				Player.Gamemode gamemode = getGamemodeOption("mode", Player.Gamemode.ALL);
				int page = getIntOption("page", -1);
				int rank = getIntOption("rank", -1);
				double amount = getDoubleOption("amount", -1);

				if (args.length == 3 || args.length == 2) {
					if (getMentionedUsername(args.length == 2 ? -1 : 2)) {
						return;
					}

					paginate(getLeaderboard(args[1], player, gamemode, page, rank, amount, new PaginatorEvent(event)));
					return;
				}

				sendErrorEmbed();
			}
		}
			.queue();
	}
}
