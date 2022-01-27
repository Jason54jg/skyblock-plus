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

package com.skyblockplus;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.*;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.miscellaneous.PublicEndpoints;
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.*;
import com.skyblockplus.features.fetchur.FetchurHandler;
import com.skyblockplus.features.listeners.MainListener;
import com.skyblockplus.features.mayor.MayorHandler;
import com.skyblockplus.features.party.PartyCommand;
import com.skyblockplus.features.party.PartySlashCommand;
import com.skyblockplus.features.setup.SetupCommand;
import com.skyblockplus.features.setup.SetupSlashCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventSlashCommand;
import com.skyblockplus.general.*;
import com.skyblockplus.general.help.HelpCommand;
import com.skyblockplus.general.help.HelpSlashCommand;
import com.skyblockplus.guild.*;
import com.skyblockplus.inventory.*;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.miscellaneous.networth.NetworthCommand;
import com.skyblockplus.miscellaneous.networth.NetworthSlashCommand;
import com.skyblockplus.miscellaneous.weight.WeightCommand;
import com.skyblockplus.miscellaneous.weight.WeightSlashCommand;
import com.skyblockplus.price.*;
import com.skyblockplus.settings.*;
import com.skyblockplus.skills.*;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.slayer.SlayerSlashCommand;
import com.skyblockplus.utils.ApiHandler;
import com.skyblockplus.utils.AuctionFlipper;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.exceptionhandler.ExceptionEventListener;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import com.skyblockplus.utils.slashcommand.SlashCommandClient;
import java.io.File;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static final Logger log = LoggerFactory.getLogger(Main.class);
	public static JDA jda;
	public static Database database;
	public static EventWaiter waiter;
	public static GlobalExceptionHandler globalExceptionHandler;
	public static CommandClient client;
	public static SlashCommandClient slashCommandClient;

	public static void main(String[] args) throws LoginException, IllegalArgumentException {
		Main.globalExceptionHandler = new GlobalExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
		RestAction.setDefaultFailure(e -> globalExceptionHandler.uncaughtException(null, e));

		Utils.initialize();
		Constants.initialize();

		Main.database = SpringApplication.run(Main.class, args).getBean(Database.class);
		Main.waiter = new EventWaiter(scheduler, true);
		Main.client =
			new CommandClientBuilder()
				.setOwnerId("385939031596466176")
				.setEmojis("✅", "⚠️", "❌")
				.useHelpBuilder(false)
				.setPrefixFunction(event -> event.isFromGuild() ? getGuildPrefix(event.getGuild().getId()) : DEFAULT_PREFIX)
				.setListener(
					new CommandListener() {
						@Override
						public void onCommandException(CommandEvent event, Command command, Throwable throwable) {
							globalExceptionHandler.uncaughtException(event, command, throwable);
						}
					}
				)
				.setActivity(Activity.playing("Loading..."))
				.setManualUpsert(true)
				.addCommands(
					new InformationCommand(),
					new SlayerCommand(),
					new HelpCommand(),
					new GuildCommand(),
					new AuctionCommand(),
					new BinCommand(),
					new SkillsCommand(),
					new DungeonsCommand(),
					new RolesCommand(),
					new GuildRanksCommand(),
					new EssenceCommand(),
					new BankCommand(),
					new WardrobeCommand(),
					new TalismanBagCommand(),
					new InventoryCommand(),
					new SacksCommand(),
					new WeightCommand(),
					new HypixelCommand(),
					new UuidCommand(),
					new SettingsCommand(),
					new ReloadCommand(),
					new SetupCommand(),
					new CategoriesCommand(),
					new PartyFinderCommand(),
					new DevSettingsCommand(),
					new GetServerEmojisCommand(),
					new EnderChestCommand(),
					new LinkCommand(),
					new GetSettingsCommand(),
					new UnlinkCommand(),
					new LinkedUserCommand(),
					new BazaarCommand(),
					new AverageAuctionCommand(),
					new PetsCommand(),
					new SkyblockEventCommand(),
					new DeleteMessagesCommand(),
					new PlaceholderCommand(),
					new ProfilesCommand(),
					new NetworthCommand(),
					new PriceCommand(),
					new BidsCommand(),
					new BitsCommand(),
					new EvaluateCommand(),
					new GuildKickerCommand(),
					new MissingCommand(),
					new UpdateSlashCommands(),
					new CalculateCommand(),
					new EmojiFromUrlCommand(),
					new GuildLeaderboardCommand(),
					new ArmorCommand(),
					new FetchurCommand(),
					new CheckEmojisCommand(),
					new HarpCommand(),
					new CakesCommand(),
					new ActiveCoinsCommand(),
					new HotmCommand(),
					new VoteCommand(),
					new TrackAuctionsCommand(),
					new SkyblockCommand(),
					new GuildStatisticsCommand(),
					new PartyCommand(),
					new TimeCommand(),
					new ForgeCommand(),
					new ScammerCommand(),
					new FixApplicationCommand(),
						new NucleusCommand()
				)
				.build();

		slashCommandClient =
			new SlashCommandClient()
				.setOwnerId("385939031596466176")
				.addCommands(
					new InviteSlashCommand(),
					new VoteSlashCommand(),
					new HotmSlashCommand(),
					new InformationSlashCommand(),
					new LinkSlashCommand(),
					new UnlinkSlashCommand(),
					new SlayerSlashCommand(),
					new SkillsSlashCommand(),
					new DungeonsSlashCommand(),
					new EssenceSlashCommand(),
					new PartyFinderSlashCommand(),
					new GuildSlashCommand(),
					new HelpSlashCommand(),
					new AuctionsSlashCommand(),
					new BinSlashCommand(),
					new BazaarSlashCommand(),
					new AverageAuctionSlashCommand(),
					new BidsSlashCommand(),
					new PriceSlashCommand(),
					new BitsSlashCommand(),
					new RolesSlashCommand(),
					new BankSlashCommand(),
					new WeightSlashCommand(),
					new HypixelSlashCommand(),
					new ProfilesSlashCommand(),
					new MissingSlashCommand(),
					new CalculateSlashCommand(),
					new SetupSlashCommand(),
					new SkyblockEventSlashCommand(),
					new FetchurSlashCommand(),
					new NetworthSlashCommand(),
					new InventorySlashCommand(),
					new ArmorSlashCommand(),
					new EnderChestSlashCommand(),
					new TalismanBagSlashCommand(),
					new SacksSlashCommand(),
					new WardrobeSlashCommand(),
					new HarpSlashCommand(),
					new CakesSlashCommand(),
					new ActiveCoinsSlashCommand(),
					new GuildLeaderboardSlashCommand(),
					new GuildRanksSlashCommand(),
					new GuildKickerSlashCommand(),
					new PetsSlashCommand(),
					new UuidSlashCommand(),
					new TrackAuctionsSlashCommand(),
					new SkyblockSlashCommand(),
					new GuildStatisticsSlashCommand(),
					new PartySlashCommand(),
					new SettingsSlashCommand(),
					new TimeSlashCommand(),
					new ReloadSlashCommand(),
					new ForgeSlashCommand(),
					new ScammerSlashCommand(),
					new CategoriesSlashCommand(),
					new FixApplicationSlashCommand()
				);

		jda =
			JDABuilder
				.createDefault(BOT_TOKEN)
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.addEventListeners(
					new ExceptionEventListener(waiter),
					client,
					new ExceptionEventListener(slashCommandClient),
					new ExceptionEventListener(new MainListener())
				)
				.setActivity(Activity.playing("Loading..."))
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.disableCache(CacheFlag.VOICE_STATE)
				.enableIntents(GatewayIntent.GUILD_MEMBERS)
				.build();

		try {
			jda.awaitReady();
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}

		ApiHandler.initialize();
		TrackAuctionsCommand.initialize();
		AuctionFlipper.setEnable(true);
		AuctionFlipper.scheduleHerokuUpdate();
		PublicEndpoints.initialize();
		FetchurHandler.initialize();
		MayorHandler.initialize();
		File transcriptDir = new File("src/main/java/com/skyblockplus/json/application_transcripts/");
		if (!transcriptDir.exists()) {
			log.info((transcriptDir.mkdirs() ? "Successfully created" : "Failed to create") + " application transcript directory");
		}
	}

	@PreDestroy
	public void onExit() {
		log.info("Stopping");

		log.info("Caching Apply Users");
		cacheApplyGuildUsers();

		log.info("Caching Parties");
		cacheParties();

		log.info("Caching Command Uses");
		cacheCommandUses();

		log.info("Caching Jacob Data");
		cacheJacobData();

		log.info("Closing Http Client");
		closeHttpClient();

		log.info("Closing Async Http Client");
		closeAsyncHttpClient();

		log.info("Finished");
	}
}
