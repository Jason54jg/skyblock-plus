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

package com.skyblockplus.features.jacob;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.miscellaneous.TimeCommand.getSkyblockYear;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.api.miscellaneous.JacobContest;
import com.skyblockplus.api.miscellaneous.JacobData;
import com.skyblockplus.features.listeners.AutomaticGuild;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class JacobHandler {

	private static JacobData jacobData = null;
	private static boolean isRunning = false;

	public static void initialize() {
		// TODO: cache this when stop and start
		setJacobData(gson.fromJson(getJson("https://hst.sh/raw/ceziketoko.json"), JacobData.class));
	}

	public static boolean needsUpdate() {
		return jacobData == null || jacobData.getYear() != getSkyblockYear();
	}

	public static void setJacobData(JacobData jacobData) {
		JacobHandler.jacobData = jacobData;
		if (!isRunning) {
			queue();
		}
	}

	private static void queue() {
		if (jacobData == null) {
			isRunning = false;
			return;
		}

		JacobContest nextContest = jacobData.getNextContest();
		if (nextContest != null) {
			isRunning = true;
			scheduler.schedule(
					() -> {
						try {
							MessageEmbed embed = defaultEmbed("Jacob's Contest")
									.setDescription(
											"The next farming contest is starting <t:" + nextContest.getTimeInstant().getEpochSecond() + ":R>\n"
									)
									.addField("Crops", nextContest.getCropsFormatted(), false)
									.build();
							for (AutomaticGuild guild : guildMap.values()) {
								guild.onFarmingContest(nextContest.getCrops(), embed);
							}
							queue();
						} catch (Exception e) {
							isRunning = false;
							e.printStackTrace();
						}
					},
					nextContest.getDurationUntil().minusMinutes(5).toMillis(),
					TimeUnit.MILLISECONDS
			);
		} else {
			isRunning = false;
		}
	}
}