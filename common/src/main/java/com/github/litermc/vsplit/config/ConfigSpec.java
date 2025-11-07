package com.github.litermc.vsplit.config;

import com.github.litermc.vsplit.platform.PlatformHelper;

import java.nio.file.Path;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	// public static final ConfigFile.Value<Boolean> FORCE_LOAD_ALL_SHIPS;

	private ConfigSpec() {}

	static {
		final ConfigFile.Builder builder = PlatformHelper.get().createConfigBuilder();
		{
			builder
				.comment("General settings")
				.push("general");

			FORCE_LOAD_ALL_SHIPS = builder
				.comment("Should try split ship when block changed")
				.define("enable_ship_split", Config.forceLoadAllShips);

			builder.pop();
		}

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		// Config.forceLoadAllShips = FORCE_LOAD_ALL_SHIPS.get();
	}

	public static void syncClient(Path path) {
	}
}
