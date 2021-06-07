package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				switch (args[1]) {
					case "get":
						{
							Guild guild = jda.getGuildById(args[2]);
							List<TextChannel> channels = guild.getTextChannels();
							System.out.println(guild.getName() + " - " + guild.getId() + " - " + guild.getMemberCount());
							for (TextChannel channel : channels) {
								System.out.println(channel.getName() + " - " + channel.getId());
							}
							break;
						}
					case "send_silent":
						{
							Guild guild = jda.getGuildById(args[2]);
							guild.leave().complete();
							System.out.println("Left " + guild.getName() + " - " + guild.getId());
							break;
						}
					case "list":
						{
							List<Guild> guilds = new LinkedList<>(jda.getGuilds());

							guilds.sort(Comparator.comparingInt(Guild::getMemberCount));

							for (Guild guild : guilds) {
								if (guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
									continue;
								}

								System.out.println(
									guild.getName() +
									" (" +
									guild.getMemberCount() +
									") | Id: " +
									guild.getId() +
									" | Owner: " +
									guild.getOwner().getEffectiveName() +
									" (" +
									guild.getOwnerId() +
									")"
								);
							}
							break;
						}
				}

				ebMessage.editMessage(defaultEmbed("Done").build()).queue();
			}
		)
			.start();
	}
}
/*
File dir = new File("src/main/java/com/skyblockplus/price/items");
File[] directoryListing = dir.listFiles();
JsonObject arrayJson = new JsonObject();
for (File child : directoryListing) {
	try {
		JsonElement json = JsonParser.parseReader(new FileReader(child));
		String itemName = parseMcCodes(higherDepth(json, "displayname").getAsString()).replace("�", "");
		String internalName = higherDepth(json, "internalname").getAsString();
		if(itemName.contains("(")){
			continue;
		}
		if(itemName.startsWith("[Lvl")){
			itemName = internalName;
		}
		if(itemName.equals("Enchanted Book")){
			itemName = internalName;
		}
		if(itemName.contains("⚚")){
			itemName = itemName.replace("⚚ ", "STARRED ");
		}
		if(itemName.contains("Melody\\u0027s Hair")){
			itemName = "MELODY_HAIR";
		}
		itemName = itemName.replace("™", "").replace("\u0027s", "").toUpperCase().replace("\u0027", "").replace(" ", "_");
		if(itemName.contains("MELODY_HAIR")){
			itemName = "MELODY_HAIR";
		}
		if(itemName.equals(internalName)){
			continue;
		}
		arrayJson.addProperty(itemName, internalName);
	} catch (Exception e) {
		e.printStackTrace();
	}
}
System.out.println(makeHastePost(new GsonBuilder().setPrettyPrinting().create().toJson(arrayJson)) + ".json");
*/
