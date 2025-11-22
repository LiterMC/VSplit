package com.github.litermc.vsplit.config;

import com.github.litermc.vsplit.platform.PlatformHelper;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	public static final ConfigFile.Value<Boolean> ENABLE_SHIP_SPLIT;
	public static final ConfigFile.Value<Boolean> ASYNC_SHIP_SPLIT;

	public static final ConfigFile.Value<Boolean> ENABLE_SHIP_PERIODIC_CLEANUP;
	public static final ConfigFile.Value<Integer> SHIP_CLEAN_PERIOD;

	public static final ConfigFile.Value<Boolean> ENABLE_BLOCK_COUNT_CLEANUP;
	public static final ConfigFile.Value<Integer> MINIMUM_BLOCKS_SHIP_NEEDS;
	public static final ConfigFile.Value<CoreBlockBehaviour> CORE_BLOCK_BEHAVIOUR;
	public static final ConfigFile.Value<List<? extends String>> CORE_BLOCKS;

	public static final ConfigFile.Value<Boolean> ENABLE_PLAYER_PROTECTION;
	public static final ConfigFile.Value<Integer> PLAYER_PROTECTION_RADIUS;

	private ConfigSpec() {}

	static {
		final ConfigFile.Builder builder = PlatformHelper.get().createConfigBuilder();
		{
			builder
				.comment("General settings")
				.push("general");

			ENABLE_SHIP_SPLIT = builder
				.comment("Should try split ship when block changed.")
				.define("enable_ship_split", Config.enableShipSplit);

			ASYNC_SHIP_SPLIT = builder
				.comment("Use async ship assembly, this will improve split performance but can also cause some unknown issues.")
				.define("async_ship_split", Config.asyncShipSplit);

			builder.pop();
		}

		{
			builder
				.comment("Ship cleanup settings")
				.push("cleanup");

			ENABLE_SHIP_PERIODIC_CLEANUP = builder
				.comment("Should mark and clean ships periodically.")
				.define("enable_ship_periodic_cleanup", Config.enableShipPeriodicCleanup);

			SHIP_CLEAN_PERIOD = builder
				.comment("Ship cleanup period in seconds. Note that ship's actual removal time is inbetween this period and double of it.")
				.defineInRange("ship_clean_period", Config.shipCleanPeriod, 0, Integer.MAX_VALUE);

			{
				builder
					.comment("Block count cleaner settings")
					.push("block_count_cleaner");

				ENABLE_BLOCK_COUNT_CLEANUP = builder
					.comment("Cleanup ships that do not have enough blocks and/or do not have Core blocks present.")
					.define("enable_block_count_cleanup", Config.enableBlockCountCleanup);

				MINIMUM_BLOCKS_SHIP_NEEDS = builder
					.comment("The minimum blocks a ship needs to prevent cleanup.")
					.defineInRange("minimum_blocks_ship_needs", Config.minimumBlocksShipNeeds, 0, Integer.MAX_VALUE);

				CORE_BLOCK_BEHAVIOUR = builder
					.comment(
	 					"Core blocks behaviour during ship cleanup.\n" +
	 					"IGNORED: Cores do not have any effect to ship cleanup at all.\n" +
	 					"OPTIONAL: A ship will not be cleaned when it has enough blocks or has a core block.\n" +
	 					"REQUIRED: A ship must has enough blocks and a core block present to not be cleaned."
	 				)
	 				.defineEnum("core_block_behaviour", Config.coreBlockBehaviour);

				CORE_BLOCKS = builder
					.comment("Blocks that are considered as Cores.")
					.defineList(
						"core_blocks",
						Config.coreBlocks.stream().map(ResourceLocation::toString).sorted().toList(),
						(o) -> o instanceof String value && value.length() > 0
					);

				builder.pop();
			}

			{
				builder
					.comment("Block count cleaner settings")
					.push("block_count_cleaner");

				ENABLE_PLAYER_PROTECTION = builder
					.comment("Prevent ship cleanup within player's certain distance.")
					.define("enable_player_protection", Config.enablePlayerProtection);

				PLAYER_PROTECTION_RADIUS = builder
					.comment("The distance in chunks a player can prevent ship cleanup.")
					.defineInRange("player_protection_radius", Config.playerProtectionRadius, 0, 256 * 256);

				builder.pop();
			}

			builder.pop();
		}

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		Config.enableShipSplit = ENABLE_SHIP_SPLIT.get();
		Config.asyncShipSplit = ASYNC_SHIP_SPLIT.get();
		Config.enableShipPeriodicCleanup = ENABLE_SHIP_PERIODIC_CLEANUP.get();
		Config.shipCleanPeriod = SHIP_CLEAN_PERIOD.get();
		Config.enableBlockCountCleanup = ENABLE_BLOCK_COUNT_CLEANUP.get();
		Config.minimumBlocksShipNeeds = MINIMUM_BLOCKS_SHIP_NEEDS.get();
		Config.coreBlockBehaviour = CORE_BLOCK_BEHAVIOUR.get();
		Config.coreBlocks = Set.copyOf(CORE_BLOCKS.get().stream().map(ResourceLocation::new).toList());
		Config.enablePlayerProtection = ENABLE_PLAYER_PROTECTION.get();
		Config.playerProtectionRadius = PLAYER_PROTECTION_RADIUS.get();
	}

	public static void syncClient(Path path) {
	}
}
