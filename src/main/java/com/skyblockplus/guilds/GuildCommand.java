package com.skyblockplus.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.asyncHttpClient;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

public class GuildCommand extends Command {
    public GuildCommand() {
        this.name = "guild";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"g"};
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 3 && ("experience".equals(args[1]) || "exp".equals(args[1]))) {
                if (args[2].toLowerCase().startsWith("u-")) {
                    String username = args[2].split("-")[1];
                    eb = getGuildExp(username, event);
                    if (eb == null) {
                        ebMessage.delete().queue();
                    } else {
                        ebMessage.editMessage(eb.build()).queue();
                    }
                    return;
                }
            } else if (args.length >= 3 && "info".equals(args[1])) {
                if (args[2].toLowerCase().startsWith("u-")) {
                    String usernameInfo = args[2].split("-")[1];
                    ebMessage.editMessage(getGuildInfo(usernameInfo).build()).queue();
                    return;
                } else if (args[2].toLowerCase().startsWith("g-")) {
                    String guildName = content.split("-")[1];
                    ebMessage.editMessage(guildInfoFromGuildName(guildName).build()).queue();
                    return;
                }
            } else if (args.length == 3 && "members".equals(args[1])) {
                if (args[2].toLowerCase().startsWith("u-")) {
                    String usernameMembers = args[2].split("-")[1];
                    eb = getGuildMembers(usernameMembers, event);
                    if (eb == null) {
                        ebMessage.delete().queue();
                    } else {
                        ebMessage.editMessage(eb.build()).queue();
                    }
                    return;
                }
            } else if (args.length == 2) {
                ebMessage.editMessage(getGuildPlayer(args[1]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getGuildExp(String username, CommandEvent event) {
        UsernameUuidStruct uuidUsername = usernameToUuid(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        JsonElement members = higherDepth(higherDepth(guildJson, "guild"), "members");
        JsonArray membersArr = members.getAsJsonArray();
        Map<String, Integer> guildExpMap = new HashMap<>();

        CountDownLatch httpGetsFinishedLatch = new CountDownLatch(1);
        for (int i = 0; i < membersArr.size(); i++) {
            int finalI = i;
            asyncHttpClient
                    .prepareGet("https://api.ashcon.app/mojang/v2/user/"
                            + higherDepth(membersArr.get(i), "uuid").getAsString())
                    .execute().toCompletableFuture().thenApply(uuidToUsernameResponse -> {
                try {
                    String currentUsername = higherDepth(
                            JsonParser.parseString(uuidToUsernameResponse.getResponseBody()), "username")
                            .getAsString();
                    JsonElement expHistory = higherDepth(membersArr.get(finalI), "expHistory");
                    List<String> keys = getJsonKeys(expHistory);
                    int totalPlayerExp = 0;

                    for (String value : keys) {
                        totalPlayerExp += higherDepth(expHistory, value).getAsInt();
                    }

                    guildExpMap.put(currentUsername, totalPlayerExp);
                } catch (Exception e) {
                    guildExpMap.put("@null" + finalI, 0);
                }
                return true;
            }).whenComplete((aBoolean, throwable) -> {
                if (guildExpMap.size() == membersArr.size()) {
                    httpGetsFinishedLatch.countDown();
                }
            });
        }

        try {
            httpGetsFinishedLatch.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("== Stack Trace (Guild Exp Latch) ==");
            e.printStackTrace();
        }

        LinkedHashMap<String, Integer> reverseSortedMap = new LinkedHashMap<>();

        guildExpMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> {
                    try {
                        reverseSortedMap.put(x.getKey(), x.getValue());
                    } catch (Exception ignored) {
                    }
                });

        CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(2)
                .setItemsPerPage(20);

        String rankStr = "**Player:** " + uuidUsername.playerUsername + "\n**Guild Rank:** #"
                + (new ArrayList<>(reverseSortedMap.keySet()).indexOf(uuidUsername.playerUsername) + 1) + "\n**Exp:** "
                + formatNumber(reverseSortedMap.get(uuidUsername.playerUsername));
        paginateBuilder
                .setPaginatorExtras(new PaginatorExtras()
                        .setEveryPageTitle(higherDepth(higherDepth(guildJson, "guild"), "name").getAsString())
                        .setEveryPageThumbnail("https://hypixel-leaderboard.senither.com/guilds/"
                                + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString())
                        .setEveryPageText(rankStr));

        int counter = 0;
        for (Map.Entry<String, Integer> k : reverseSortedMap.entrySet()) {
            if (!k.getKey().startsWith("@null")) {
                paginateBuilder.addItems("`" + (counter + 1) + ")` " + fixUsername(k.getKey()) + ": "
                        + formatNumber(k.getValue()) + " EXP\n");
            }

            counter++;
        }

        paginateBuilder.build().paginate(event.getChannel(), 0);
        return null;
    }

    private EmbedBuilder getGuildPlayer(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuid(username);
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
            EmbedBuilder eb = defaultEmbed(uuidUsername.playerUsername + " is in " + guildName,
                    "https://hypixel-leaderboard.senither.com/guilds/"
                            + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString());
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            eb.setThumbnail("https://cravatar.eu/helmavatar/" + uuidUsername.playerUuid + "/64.png");
            return eb;
        } catch (Exception e) {
            return defaultEmbed(uuidUsername.playerUsername + " is not in a guild");
        }

    }

    private EmbedBuilder getGuildInfo(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuid(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        EmbedBuilder eb = defaultEmbed(guildName, "https://hypixel-leaderboard.senither.com/guilds/"
                + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString());
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

            EmbedBuilder eb = defaultEmbed(guildName, "https://hypixel-leaderboard.senither.com/guilds/"
                    + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString());
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

    private EmbedBuilder getGuildMembers(String username, CommandEvent event) {
        UsernameUuidStruct uuidUsername = usernameToUuid(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        JsonArray membersArr = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();

        List<String> guildMembers = new ArrayList<>();
        CountDownLatch httpGetsFinishedLatch = new CountDownLatch(1);
        for (int i = 0; i < membersArr.size(); i++) {
            asyncHttpClient
                    .prepareGet("https://api.ashcon.app/mojang/v2/user/"
                            + higherDepth(membersArr.get(i), "uuid").getAsString())
                    .execute().toCompletableFuture().thenApply(uuidToUsernameResponse -> {
                try {
                    guildMembers
                            .add(higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()),
                                    "username").getAsString());
                } catch (Exception e) {
                    guildMembers.add(null);
                }
                return true;
            }).whenComplete((aBoolean, throwable) -> {
                if (guildMembers.size() == membersArr.size()) {
                    httpGetsFinishedLatch.countDown();
                }
            });
        }

        try {
            httpGetsFinishedLatch.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("== Stack Trace (Guild Members Latch) ==");
            e.printStackTrace();
        }

        CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(3)
                .setItemsPerPage(27);

        paginateBuilder.setPaginatorExtras(new PaginatorExtras()
                .setEveryPageTitle(higherDepth(higherDepth(guildJson, "guild"), "name").getAsString())
                .setEveryPageThumbnail("https://hypixel-leaderboard.senither.com/guilds/"
                        + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString()));

        for (String member : guildMembers) {
            if (member != null) {
                paginateBuilder.addItems("• " + fixUsername(member) + "  \n");
            }

        }

        paginateBuilder.build().paginate(event.getChannel(), 0);
        return null;
    }

    private int guildExpToLevel(int guildExp) {
        int[] guildExpTable = new int[]{100000, 150000, 250000, 500000, 750000, 1000000, 1250000, 1500000, 2000000,
                2500000, 2500000, 2500000, 2500000, 2500000, 3000000};
        int guildLevel = 0;

        for (int i = 0; ; i++) {
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
