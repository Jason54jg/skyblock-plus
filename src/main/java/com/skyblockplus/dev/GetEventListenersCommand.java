package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.executor;
import static com.skyblockplus.utils.Utils.logCommand;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class GetEventListenersCommand extends Command {

	public GetEventListenersCommand() {
		this.name = "d-listeners";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				logCommand(event.getGuild(), event.getAuthor(), getGuildPrefix(event.getGuild().getId()) + "d-listeners");

				StringBuilder ebString = new StringBuilder();
				for (Object i : jda.getRegisteredListeners()) {
					ebString.append("\n• ").append(i);
				}

				event.reply(ebString.toString());
			}
		);
	}
}
