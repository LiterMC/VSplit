package com.github.litermc.vsplit.mixin.shadow;

import com.github.litermc.vsplit.accessor.ShipObjectServerAccessor;
import com.github.litermc.vsplit.api.attachment.ISplitListener;
import com.github.litermc.vsplit.api.clean.ICleanListener;
import com.github.litermc.vsplit.util.MapEntryValueIterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

@Mixin(org.valkyrienskies.core.impl.shadow.Es.class)
public abstract class MixinShipObjectServer implements ShipObjectServerAccessor {
	@Unique
	private final static String M_applyAttachmentInterfaces = "a";

	@Unique
	private final HashMap<Class<?>, ICleanListener> cleanListeners = new HashMap<>();

	@Unique
	private final TreeSet<Map.Entry<Class<?>, ICleanListener>> sortedCleanListenerEntries = new TreeSet<>(
		(a, b) -> Integer.compare(a.getValue().getShipCleanPriority(), b.getValue().getShipCleanPriority())
	);

	@Unique
	private final Collection<ICleanListener> sortedCleanListeners = new AbstractCollection<>() {
		@Override
		public int size() {
			return MixinShipObjectServer.this.sortedCleanListenerEntries.size();
		}

		@Override
		public Iterator<ICleanListener> iterator() {
			return new MapEntryValueIterator<>(MixinShipObjectServer.this.sortedCleanListenerEntries.iterator());
		}
	};

	@Unique
	private final HashMap<Class<?>, ISplitListener> splitListeners = new HashMap<>();

	@Override
	public Collection<ICleanListener> vsplit$getCleanListeners() {
		return this.sortedCleanListeners;
	}

	@Override
	public Collection<ISplitListener> vsplit$getSplitListeners() {
		return this.splitListeners.values();
	}

	@Inject(method = M_applyAttachmentInterfaces + "(Ljava/lang/Class;Ljava/lang/Object;)V", at = @At("HEAD"), remap = false)
	private void applyAttachmentInterfaces(final Class<?> clazz, final Object value, final CallbackInfo ci) {
		if (value == null) {
			if (ICleanListener.class.isAssignableFrom(clazz)) {
				this.sortedCleanListenerEntries.remove(Map.entry(clazz, this.cleanListeners.remove(clazz)));
				return;
			}
			this.splitListeners.remove(clazz);
			return;
		}
		if (value instanceof final ICleanListener listener) {
			this.sortedCleanListenerEntries.add(Map.entry(clazz, listener));
			return;
		}
		if (value instanceof final ISplitListener listener) {
			this.splitListeners.put(clazz, listener);
			return;
		}
	}
}
