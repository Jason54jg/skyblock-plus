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

package com.skyblockplus.utils.structs;

import com.skyblockplus.utils.Constants;

public record SkillsStruct(
	String name,
	int currentLevel,
	int maxLevel,
	long totalExp,
	long expCurrent,
	long expForNext,
	double progressToNext
) {
	public boolean isMaxed() {
		return currentLevel == maxLevel;
	}

	public double getProgressLevel() {
		return currentLevel + progressToNext;
	}

	public double getMaxXp(){
		return maxLevel == 50 ? Constants.SKILLS_LEVEL_50_XP : Constants.SKILLS_LEVEL_60_XP;
	}
}
