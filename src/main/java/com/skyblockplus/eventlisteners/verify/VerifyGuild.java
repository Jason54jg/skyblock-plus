package com.skyblockplus.eventlisteners.verify;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;

public class VerifyGuild {
    private TextChannel messageChannel;
    private Message originalMessage;
    private boolean enable = true;

    public VerifyGuild(TextChannel messageChannel, Message originalMessage) {
        this.messageChannel = messageChannel;
        this.originalMessage = originalMessage;
    }

    public VerifyGuild() {
        this.enable = false;
    }

    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (!enable) {
            return false;
        }

        if (!event.getChannel().getId().equals(messageChannel.getId())) {
            return false;
        }

        if (event.getMessage().getId().equals(originalMessage.getId())) {
            return false;
        }

        if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
            if (event.getAuthor().isBot()) {
                return false;
            }

            if (!event.getMessage().getContentRaw().startsWith(BOT_PREFIX + "link ")) {
                event.getMessage().delete().queue();
                return true;
            }
        }

        event.getMessage().delete().queueAfter(7, TimeUnit.SECONDS);
        return true;
    }
}