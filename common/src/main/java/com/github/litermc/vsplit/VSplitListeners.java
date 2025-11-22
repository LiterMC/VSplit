package com.github.litermc.vsplit;

import com.github.litermc.vsplit.impl.clean.CleanScheduler;
import com.github.litermc.vsplit.util.SplitUtil;
import com.github.litermc.vsplit.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class VSplitListeners {
	private VSplitListeners() {}

	public static void onModInit() {
		VSplitRegistry.register();
	}

	public static void onServerLevelLoad(final ServerLevel level) {
	}

	public static void onServerLevelUnload(final ServerLevel level) {
	}

	public static void onServerStarted(final MinecraftServer server) {
		CleanScheduler.onServerStarted(server);
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
	}

	public static void postServerTick(final MinecraftServer server) {
		SplitUtil.postServerTick();
		TaskUtil.postServerTick();
		CleanScheduler.postServerTick(server);
	}
}
