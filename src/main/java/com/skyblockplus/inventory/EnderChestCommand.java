package com.skyblockplus.inventory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class EnderChestCommand extends Command {
    private String missingEmoji;

    public EnderChestCommand() {
        this.name = "enderchest";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"echest"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        if (args.length == 2 || args.length == 3) {
            List<String[]> playerEnderChest;
            if (args.length == 3) {
                playerEnderChest = getPlayerEnderChest(args[1], args[2]);
            } else {
                playerEnderChest = getPlayerEnderChest(args[1], null);
            }

            if (playerEnderChest != null) {
                ebMessage.delete().queue();
                ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(missingEmoji).build()).queue();

                jda.addEventListener(new EnderChestPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor()));
            } else {
                ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable to fetch data").build()).queue();
            }
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private List<String[]> getPlayerEnderChest(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            List<String[]> enderChestPages = player.getEnderChest();
            if (enderChestPages != null) {
                this.missingEmoji = player.invMissing;
                return enderChestPages;
            }
        }
        return null;
    }
}