package com.github.litermc.vsplit.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class Config {
	/**
	 * Should try split ship when block changed.
	 */
	public static boolean enableShipSplit = true;

	/**
	 * Use async ship assembly, this will improve split performance but can also cause some unknown issues.
	 */
	public static boolean asyncShipSplit = false;

	/**
	 * Player cutted tree will form a falling ship.
	 */
	public static boolean enableTreeFalling = true;

	/**
	 * Decay method.
	 * DEFAULT: just destroy decaying block.
	 * NO_SPLIT: destroy decaying block and blocks splitting out (better for performance).
	 */
	public static DecayMethod decayMethod = DecayMethod.DEFAULT;

	/**
	 * Should mark and clean ships periodically.
	 */
	public static boolean enableShipPeriodicCleanup = true;

	/**
	 * Ship cleanup period in seconds. Note that ship's actual removal time is inbetween this period and double of it.
	 */
	public static int shipCleanPeriod = 60 * 60 * 3;

	/**
	 * Cleanup ships that do not have enough blocks and/or do not have Core blocks present.
	 */
	public static boolean enableBlockCountCleanup = true;

	/**
	 * The minimum blocks a ship needs to prevent cleanup.
	 */
	public static int minimumBlocksShipNeeds = 20;

	/**
	 * Core blocks behaviour during ship cleanup.
	 * IGNORED: Cores do not have any effect to ship cleanup at all.
	 * OPTIONAL: A ship will not be cleaned when it has enough blocks or has a core block.
	 * REQUIRED: A ship must has enough blocks and a core block present to not be cleaned.
	 */
	public static CoreBlockBehaviour coreBlockBehaviour = CoreBlockBehaviour.OPTIONAL;

	/**
	 * Blocks that are considered as Cores.
	 */
	public static Set<ResourceLocation> coreBlocks = Set.of(
		new ResourceLocation("computercraft", "computer_advanced"),
		new ResourceLocation("computercraft", "computer_normal")
	);

	/**
	 * Prevent ship cleanup within player's certain distance.
	 */
	public static boolean enablePlayerProtection = true;

	/**
	 * The distance in chunks a player can prevent ship cleanup.
	 */
	public static int playerProtectionRadius = 2;

	private Config() {}
}
