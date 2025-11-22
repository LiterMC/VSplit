package com.github.litermc.vsplit;

import com.github.litermc.vsplit.block.BlockCapabilityProviders;
import com.github.litermc.vsplit.command.VSplitCommands;
import com.github.litermc.vsplit.config.ConfigSpec;
import com.github.litermc.vsplit.platform.FabricConfigFile;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.storage.LevelResource;

public class ModEntry implements ModInitializer {
	private static final String SERVERCONFIG = "serverconfig";

	@Override
	public void onInitialize() {
		VSplitListeners.onModInit();
		BlockCapabilityProviders.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> VSplitCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec))
				.load(server.getWorldPath(LevelResource.ROOT).resolve(SERVERCONFIG).resolve(Constants.MOD_ID + "-server.toml"));
		});

		ServerLifecycleEvents.SERVER_STARTED.register(VSplitListeners::onServerStarted);

		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec)).unload();
		});

		ServerWorldEvents.LOAD.register((server, level) -> {
			VSplitListeners.onServerLevelLoad(level);
		});

		ServerWorldEvents.UNLOAD.register((server, level) -> {
			VSplitListeners.onServerLevelUnload(level);
		});

		ServerTickEvents.START_SERVER_TICK.register(VSplitListeners::preServerTick);
		ServerTickEvents.END_SERVER_TICK.register(VSplitListeners::postServerTick);
	}
}
