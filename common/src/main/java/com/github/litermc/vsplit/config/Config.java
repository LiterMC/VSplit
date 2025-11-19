package com.github.litermc.vsplit.config;

public final class Config {
	/**
	 * Should try split ship when block changed.
	 */
	public static boolean enableShipSplit = true;

	/**
	 * Use async ship assembly, this will improve split performance but can also cause some unknown issues.
	 */
	public static boolean asyncShipSplit = false;

	private Config() {}
}
