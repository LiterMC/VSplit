package com.github.litermc.vsplit.impl.clean;

import com.github.litermc.vsplit.api.clean.ICleanListener;
import com.github.litermc.vsplit.config.Config;
import com.github.litermc.vsplit.platform.PlatformHelper;
import com.github.litermc.vtil.api.connectivity.BlockConnectivityApi;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.function.Consumer;

public final class ShipPlayerProtectionCleaner implements ICleanListener {
	public static final ShipPlayerProtectionCleaner INSTANCE = new ShipPlayerProtectionCleaner();

	@Override
	public int getShipCleanPriority() {
		return 10090;
	}

	@Override
	public void onShipClean(final Context context) {
		if (!Config.enablePlayerProtection) {
			return;
		}
		if (!context.getCleanSuggestion()) {
			return;
		}
		final PlatformHelper helper = PlatformHelper.get();
		final ServerLevel level = context.getLevel();
		final ServerShip ship = context.getShip();
		final AABBdc box = ship.getWorldAABB();
		final double detectRange = Config.playerProtectionRadius * 16;
		final AABBd detectBox = new AABBd(
			box.minX() - detectRange, box.minY() - detectRange, box.minZ() - detectRange,
			box.maxX() + detectRange, box.maxY() + detectRange, box.maxZ() + detectRange
		);
		for (final ServerPlayer player : level.players()) {
			if (!helper.isFakePlayer(player) && detectBox.containsPoint(player.getX(), player.getY(), player.getZ())) {
				context.setCleanSuggestion(false);
				return;
			}
		}
	}

	@Override
	public void checkShipCleanStatus(final Context context, final Consumer<Component> messageConsumer) {}
}
