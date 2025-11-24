package com.github.litermc.vsplit.impl.clean;

import com.github.litermc.vsplit.Constants;
import com.github.litermc.vsplit.api.clean.ShipCleaner;
import com.github.litermc.vsplit.config.Config;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.Instant;

public final class CleanScheduler {
	private static Instant nextCleanup;
	private static final long[] HINT_DURATIONS = new long[]{
		10,
		60,
		60 * 3,
		60 * 5,
		60 * 10,
	};
	private static int lastHint = HINT_DURATIONS.length;

	public static void onServerStarted(final MinecraftServer server) {
		// TODO: save & recover last cleanup schedule
		refreshCleanupTime();
	}

	public static void postServerTick(final MinecraftServer server) {
		if (!Config.enableShipPeriodicCleanup) {
			return;
		}
		final Duration timeLeft = Duration.between(Instant.now(), nextCleanup);
		if (!timeLeft.isNegative()) {
			if (!Config.cleanupMessageLevel.showSchedule()) {
				return;
			}
			final long dur = timeLeft.getSeconds();
			for (int i = 0; i < lastHint; i++) {
				final long hint = HINT_DURATIONS[i];
				if (dur < hint) {
					lastHint = i;
					server.getPlayerList().broadcastSystemMessage(
						Constants.MESSAGE_PREFIX.copy().append(
							hint > 60
								? Component.translatable("vsplit.message.clean.warn.min", hint / 60)
								: Component.translatable("vsplit.message.clean.warn.sec", hint)
						),
						false
					);
					break;
				}
			}
			return;
		}
		ShipCleaner.clean(server, false);
		refreshCleanupTime();
	}

	private static void refreshCleanupTime() {
		final int period = Config.shipCleanPeriod;
		nextCleanup = Instant.now().plusSeconds(period);
		lastHint = HINT_DURATIONS.length;
		for (int i = 0; i < lastHint; i++) {
			if (period < HINT_DURATIONS[i]) {
				lastHint = i;
				break;
			}
		}
	}
}
