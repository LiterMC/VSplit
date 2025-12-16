package com.github.litermc.vsplit.impl.clean;

import com.github.litermc.vsplit.api.clean.ICleanListener;
import com.github.litermc.vsplit.config.Config;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ShipTransformProviderProtectionCleaner implements ICleanListener {
	public static final ShipTransformProviderProtectionCleaner INSTANCE = new ShipTransformProviderProtectionCleaner();

	@Override
	public int getShipCleanPriority() {
		return 10080;
	}

	@Override
	public void onShipClean(final Context context) {
		if (!Config.enableTransformProviderProtection) {
			return;
		}
		if (!context.getCleanSuggestion()) {
			return;
		}
		if (context.getShip().getTransformProvider() != null) {
			context.setCleanSuggestion(false);
			return;
		}
	}

	@Override
	public void checkShipCleanStatus(final Context context, final Consumer<Component> messageConsumer) {}
}
