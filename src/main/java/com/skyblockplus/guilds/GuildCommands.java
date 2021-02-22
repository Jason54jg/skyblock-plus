package com.skyblockplus.guilds;

import static com.skyblockplus.utils.BotUtils.HYPIXEL_API_KEY;
import static com.skyblockplus.utils.BotUtils.botColor;
import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.errorMessage;
import static com.skyblockplus.utils.BotUtils.fixUsername;
import static com.skyblockplus.utils.BotUtils.formatNumber;
import static com.skyblockplus.utils.BotUtils.getJson;
import static com.skyblockplus.utils.BotUtils.globalCooldown;
import static com.skyblockplus.utils.BotUtils.higherDepth;
import static com.skyblockplus.utils.BotUtils.usernameToUuidUsername;
import static com.skyblockplus.utils.BotUtils.uuidToUsername;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class GuildCommands extends Command {
    private final EventWaiter waiter;
    private int counter = 0;

    public GuildCommands(EventWaiter waiter) {
        this.name = "guild";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
        this.aliases = new String[] { "g" };
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String content = event.getMessage().getContentRaw();

        String[] args = content.split(" ");

        if (content.contains("g-")) {
            if (args.length < 3) {
                eb.setTitle(errorMessage(this.name));
                ebMessage.editMessage(eb.build()).queue();
                return;
            }
        } else if (args.length != 3) {
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        if (content.contains("u-") || content.contains("g-")) {
            if (args[2].endsWith("-")) {
                eb.setTitle(errorMessage(this.name));
                ebMessage.editMessage(eb.build()).queue();
                return;
            }
        }

        System.out.println(content);

        switch (args[1]) {
            case "experience":
            case "exp":
                if (args[2].toLowerCase().startsWith("u-")) {
                    String username = args[2].split("-")[1];
                    GuildStruct guildExp = getGuildExp(username);
                    if (guildExp.outputArr.length == 0) {
                        eb = guildExp.eb;
                    } else {
                        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(2)
                                .setItemsPerPage(20).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                                    try {
                                        m.clearReactions().queue();
                                    } catch (PermissionException ex) {
                                        m.delete().queue();
                                    }
                                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                                .setColor(botColor).setCommandUser(event.getAuthor());

                        paginateBuilder.addItems(guildExp.outputArr);

                        ebMessage.delete().queue();
                        paginateBuilder.build().paginate(event.getChannel(), 0);

                        return;
                    }
                } else {
                    eb.setTitle(errorMessage(this.name));
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
                break;
            case "player":
                String username = args[2];
                eb = getGuildPlayer(username);
                break;
            case "info":
                if (args[2].toLowerCase().startsWith("u-")) {
                    String usernameInfo = args[2].split("-")[1];
                    eb = getGuildInfo(usernameInfo);
                } else if (args[2].toLowerCase().startsWith("g-")) {
                    String guildName = content.split("-")[1];
                    eb = guildInfoFromGuildName(guildName);
                } else {
                    eb.setTitle(errorMessage(this.name));
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
                break;
            case "members":
                if (args[2].toLowerCase().startsWith("u-")) {
                    String usernameMembers = args[2].split("-")[1];
                    GuildStruct guildMembers = getGuildMembers(usernameMembers);
                    if (guildMembers.outputArr.length == 0) {
                        eb = guildMembers.eb;
                    } else {
                        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(3)
                                .setItemsPerPage(27).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                                    try {
                                        m.clearReactions().queue();
                                    } catch (PermissionException ex) {
                                        m.delete().queue();
                                    }
                                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                                .setColor(botColor).setCommandUser(event.getAuthor());

                        paginateBuilder.addItems(guildMembers.outputArr);

                        ebMessage.delete().queue();
                        paginateBuilder.build().paginate(event.getChannel(), 0);
                        return;
                    }

                } else {
                    eb.setTitle(errorMessage(this.name));
                    ebMessage.editMessage(eb.build()).queue();
                    return;
                }
                break;
            default:
                eb.setTitle(errorMessage(this.name));
                ebMessage.editMessage(eb.build()).queue();
                return;
        }

        ebMessage.editMessage(eb.build()).queue();

    }

    private GuildStruct getGuildExp(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return new GuildStruct(defaultEmbed("Error fetching player data"), null);
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data"), null);
        }

        JsonElement members = higherDepth(higherDepth(guildJson, "guild"), "members");
        JsonArray membersArr = members.getAsJsonArray();
        Map<String, Integer> guildExpMap = new HashMap<>();

        for (int i = 0; i < membersArr.size(); i++) {
            String expHistory = higherDepth(membersArr.get(i), "expHistory").toString();
            String[] playerExpArr = expHistory.substring(1, expHistory.length() - 1).split(",");
            int totalPlayerExp = 0;

            for (String value : playerExpArr) {
                totalPlayerExp += Integer.parseInt(value.split(":")[1]);
            }

            String currentUsername = uuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString());
            if (currentUsername != null) {
                guildExpMap.put(currentUsername, totalPlayerExp);
            }
        }

        String[] outputStrArr = new String[guildExpMap.size()];

        guildExpMap.entrySet().stream().sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue())).forEach(k -> {
            outputStrArr[counter] = "**" + (counter + 1) + ")** " + fixUsername(k.getKey()) + ": "
                    + formatNumber(k.getValue()) + " EXP\n";
            counter++;
        });

        return new GuildStruct(null, outputStrArr);
    }

    private EmbedBuilder getGuildPlayer(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        try {
            String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            EmbedBuilder eb = defaultEmbed(uuidUsername.playerUsername + " is in " + guildName);
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            return eb;
        } catch (Exception e) {
            return defaultEmbed(uuidUsername.playerUsername + " is not in a guild");
        }

    }

    private EmbedBuilder getGuildInfo(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        EmbedBuilder eb = defaultEmbed(guildName + " information");
        eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

        return eb;
    }

    private EmbedBuilder guildInfoFromGuildName(String guildName) {
        try {
            String guildId = higherDepth(getJson("https://api.hypixel.net/findGuild?key=" + HYPIXEL_API_KEY + "&byName="
                    + guildName.replace(" ", "%20")), "guild").getAsString();
            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);
            if (guildJson == null) {
                return defaultEmbed("Error fetching guild data");
            }
            guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

            EmbedBuilder eb = defaultEmbed(guildName + " information", null);
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            return eb;
        } catch (Exception e) {
            return defaultEmbed("Error fetching guild data");
        }
    }

    private String getGuildInfo(JsonElement guildJson) {
        String guildInfo = "";
        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        JsonElement created = higherDepth(higherDepth(guildJson, "guild"), "created");
        String[] date = Date.from(Instant.ofEpochMilli(created.getAsLong())).toString().split(" ");
        guildInfo += ("• " + guildName + " was created on " + date[1] + " " + date[2] + ", " + date[5]) + "\n";

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        for (int i = 0; i < guildMembers.size(); i++) {
            JsonElement currentMember = guildMembers.get(i).getAsJsonObject();
            if (higherDepth(currentMember, "rank").getAsString().equals("Guild Master")) {
                guildInfo += ("• " + guildName + "'s guild master is "
                        + uuidToUsername(higherDepth(currentMember, "uuid").getAsString())) + "\n";
                break;
            }
        }

        int numGuildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray().size();
        guildInfo += ("• " + guildName + " has " + numGuildMembers + " members") + "\n";
        JsonArray preferredGames;
        try {
            preferredGames = higherDepth(higherDepth(guildJson, "guild"), "preferredGames").getAsJsonArray();
        } catch (Exception e) {
            preferredGames = new JsonArray();
        }
        if (preferredGames.size() > 1) {
            String prefString = preferredGames.toString();
            prefString = prefString.substring(1, prefString.length() - 1).toLowerCase().replace("\"", "").replace(",",
                    ", ");
            String firstHalf = prefString.substring(0, prefString.lastIndexOf(","));
            String lastHalf = prefString.substring(prefString.lastIndexOf(",") + 1);
            if (preferredGames.size() > 2) {
                guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + ", and" + lastHalf) + "\n";
            } else {
                guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + " and" + lastHalf) + "\n";
            }
        } else if (preferredGames.size() == 1) {
            guildInfo += ("• " + guildName + "'s preferred game is "
                    + preferredGames.get(0).getAsString().toLowerCase()) + "\n";
        }

        int guildExp = higherDepth(higherDepth(guildJson, "guild"), "exp").getAsInt();

        guildInfo += ("• " + guildName + " is guild level " + guildExpToLevel(guildExp)) + "\n";

        return guildInfo;
    }

    private GuildStruct getGuildMembers(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return new GuildStruct(defaultEmbed("Error fetching player data"), null);
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data"), null);
        }

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        String[] members = new String[guildMembers.size()];
        for (int i = 0; i < guildMembers.size(); i++) {
            String currentMember = uuidToUsername(
                    higherDepth(guildMembers.get(i).getAsJsonObject(), "uuid").getAsString());
            members[i] = "• " + fixUsername(currentMember) + "  \n";
        }

        return new GuildStruct(null, members);
    }

    private int guildExpToLevel(int guildExp) {
        int[] guildExpTable = new int[] { 100000, 150000, 250000, 500000, 750000, 1000000, 1250000, 1500000, 2000000,
                2500000, 2500000, 2500000, 2500000, 2500000, 3000000 };
        int guildLevel = 0;

        for (int i = 0;; i++) {
            int expNeeded = i >= guildExpTable.length ? guildExpTable[guildExpTable.length - 1] : guildExpTable[i];
            guildExp -= expNeeded;
            if (guildExp < 0) {
                return guildLevel;
            } else {
                guildLevel++;
            }
        }

    }

}