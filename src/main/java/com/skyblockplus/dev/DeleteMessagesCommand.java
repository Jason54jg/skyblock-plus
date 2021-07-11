package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;

public class DeleteMessagesCommand extends Command {

	public DeleteMessagesCommand() {
		this.name = "d-purge";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2) {
					try {
						int messageCount = Math.min(Integer.parseInt(args[1]) + 1, 100);
						List<Message> toDelete = event.getChannel().getHistory().retrievePast(messageCount).complete();
						event.getChannel().purgeMessages(toDelete);

						event
							.getChannel()
							.sendMessageEmbeds(
								defaultEmbed("Purged messages").build()
							)
							.complete().delete().queueAfter(3, TimeUnit.SECONDS);
						return;
					} catch (Exception ignored) {
					}
				}

				event.getChannel().sendMessageEmbeds(errorEmbed(this.name).build()).queue();
			}
		);
	}
}
