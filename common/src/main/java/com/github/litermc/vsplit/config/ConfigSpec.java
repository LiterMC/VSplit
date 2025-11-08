package com.github.litermc.vsplit.config;

import com.github.litermc.vsplit.platform.PlatformHelper;

import java.nio.file.Path;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	public static final ConfigFile.Value<Boolean> ENABLE_SHIP_SPLIT;

	private ConfigSpec() {}

	static {
		final ConfigFile.Builder builder = PlatformHelper.get().createConfigBuilder();
		{
			builder
				.comment("General settings")
				.push("general");

			ENABLE_SHIP_SPLIT = builder
				.comment("Should try split ship when block changed")
				.define("enable_ship_split", Config.enableShipSplit);

			builder.pop();
		}

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		Config.enableShipSplit = ENABLE_SHIP_SPLIT.get();
	}

	public static void syncClient(Path path) {
	}
}
