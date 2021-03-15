package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class GetAllGuildsIn extends Command {
    final EventWaiter waiter;

    public GetAllGuildsIn(EventWaiter waiter) {
        this.name = "d-servers";
        this.ownerCommand = true;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (args.length == 2) {
            if (args[1].equals("list")) {
                CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(5)
                        .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ex) {
                                m.delete().queue();
                            }
                        }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).setColor(botColor).setCommandUser(event.getAuthor());

                for (Guild guild : jda.getGuilds()) {
                    if (guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
                        continue;
                    }

                    try {
                        List<Invite> invites = guild.retrieveInvites().complete();

                        if (invites.size() > 0) {
                            paginateBuilder.addItems("**" + guild.getName() + " (" + guild.getMemberCount() + ")**\nInvite Link: " + invites.get(0).getUrl() + "\nId: " + guild.getId() + "\nOwner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")\n");
                        } else {
                            paginateBuilder.addItems("**" + guild.getName() + " (" + guild.getMemberCount() + ")**\nInvite Link: " + guild.getChannels().get(0).createInvite().setMaxAge(0)
                                    .complete().getUrl() + "\nId: " + guild.getId() + "\nOwner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")\n");
                        }

                    } catch (Exception e) {
                        paginateBuilder.addItems("**" + guild.getName() + " (" + guild.getMemberCount() + ")**\nId: " + guild.getId() + "\nOwner: " + guild.getOwner().getEffectiveName() + " (" + guild.getOwnerId() + ")\n");
                    }
                }

                String[] pageTitles = new String[jda.getGuilds().size() / 5];
                Arrays.fill(pageTitles, "Server List");
                paginateBuilder.setPageTitles(pageTitles);

                ebMessage.delete().queue();
                paginateBuilder.build().paginate(event.getChannel(), 0);
                return;
            } else if (args[1].equals("count")) {
                eb = defaultEmbed("Server Count").addField("Total guild count", jda.getGuilds().size() + " servers", false);

                int guildCount = 0;
                for (Guild guild : jda.getGuilds()) {
                    if (!guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
                        guildCount++;
                    }
                }


                eb.addField("Total guild count without emoji servers", guildCount + " servers", false);
                eb.addField("Total users", "" + jda.getUsers().size(), false);

                ebMessage.editMessage(eb.build()).queue();
                return;
            }
        }

        ebMessage.editMessage(defaultEmbed("Invalid input").build()).queue();
    }
}