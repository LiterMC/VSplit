package com.github.litermc.vsplit.mixin;

import com.github.litermc.vsplit.accessor.ShipObjectServerAccessor;
import com.github.litermc.vsplit.api.attachment.ISplitListener;

import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashMap;

@Mixin(ShipObjectServer.class)
public abstract class MixinShipObjectServer implements ShipObjectServerAccessor {

	@Unique
	private final HashMap<Class<?>, ISplitListener> splitListeners = new HashMap<>();

	@Override
	public Collection<ISplitListener> vsplit$getSplitListeners() {
		return this.splitListeners.values();
	}

	@Inject(method = "applyAttachmentInterfaces", at = @At("HEAD"), remap = false)
	private void applyAttachmentInterfaces(final Class<?> clazz, final Object value, final CallbackInfo ci) {
		if (value == null) {
			this.splitListeners.remove(clazz);
		} else if (value instanceof final ISplitListener listener) {
			this.splitListeners.put(clazz, listener);
		}
	}
}
