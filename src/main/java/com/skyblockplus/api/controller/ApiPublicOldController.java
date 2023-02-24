/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
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

package com.skyblockplus.api.controller;

import static com.skyblockplus.utils.ApiHandler.cacheDatabase;
import static com.skyblockplus.utils.Utils.client;
import static com.skyblockplus.utils.Utils.jda;

import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/public")
public class ApiPublicOldController {

	@PostMapping(value = "/post/jacob", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> postJacobData(@RequestBody JacobData jacobData, @RequestHeader String key) {
		if (key.equals("2d7569ff0decff164a46e8d417e7b692")) {
			if (jacobData.getContests().isEmpty()) {
				return new ResponseEntity<>(
					DataObject.empty().put("success", false).put("cause", "Contests list empty").toMap(),
					HttpStatus.BAD_REQUEST
				);
			}

			JacobHandler.setJacobData(jacobData);
			cacheDatabase.cacheJacobData();
			jda.getTextChannelById("937894945564545035").sendMessage(client.getSuccess() + " Received jacob data").queue();
			return new ResponseEntity<>(DataObject.empty().put("success", true).toMap(), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(
				DataObject.empty().put("success", false).put("cause", "Not authorized").toMap(),
				HttpStatus.FORBIDDEN
			);
		}
	}
}