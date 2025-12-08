package com.github.litermc.vsplit.api.clean;

import com.github.litermc.vsplit.Constants;
import com.github.litermc.vsplit.accessor.ShipObjectServerAccessor;
import com.github.litermc.vsplit.config.Config;
import com.github.litermc.vsplit.impl.attachment.ShipCleanAttachment;
import com.github.litermc.vsplit.impl.clean.ShipBlockCountCleaner;
import com.github.litermc.vsplit.impl.clean.ShipPlayerProtectionCleaner;
import com.github.litermc.vtil.api.assemble.ShipAllocator;
import com.github.litermc.vtil.api.connectivity.ShipConnectivityApi;
import com.github.litermc.vtil.util.LevelUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ShipCleaner {
	private static LinkedHashSet<ICleanListener> REGISTERING_LISTENERS = new LinkedHashSet<>();
	private static List<ICleanListener> LISTENERS = null;

	static {
		registerListener(ShipBlockCountCleaner.INSTANCE);
		registerListener(ShipPlayerProtectionCleaner.INSTANCE);
	}

	private ShipCleaner() {}

	/**
	 * Register a global ship clean listener.
	 * Duplicated listener will be ignored.
	 * Listener should only be registered during mod initializing stage.
	 * Invoke at whenever else may result in unexpected behaviour.
	 * 
	 * @param listener The listener to be registered.
	 */
	public static void registerListener(final ICleanListener listener) {
		if (REGISTERING_LISTENERS == null) {
			throw new IllegalStateException("registerListener should only be invoked while mod initializing");
		}
		if (!REGISTERING_LISTENERS.contains(listener)) {
			REGISTERING_LISTENERS.add(listener);
		}
	}

	private static List<ICleanListener> getListeners() {
		if (LISTENERS == null) {
			final ArrayList<ICleanListener> listeners = new ArrayList<>(REGISTERING_LISTENERS);
			REGISTERING_LISTENERS = null;
			listeners.sort((a, b) -> {
				return Integer.compare(a.getShipCleanPriority(), b.getShipCleanPriority());
			});
			LISTENERS = List.copyOf(listeners);
		}
		return LISTENERS;
	}

	/**
	 * Add/remove a protect flag to prevent a ship from cleaning.
	 *
	 * @param ship    The ship.
	 * @param protect Whether or not the ship should be prevented from cleaning.
	 */
	public static void setShipProtected(final ServerShip ship, final boolean protect) {
		final ShipCleanAttachment attachment = ShipCleanAttachment.get(ship);
		attachment.setProtected(protect);
		if (protect) {
			attachment.setMarked(false);
		}
	}

	/**
	 * Force trigger a ship clean cycle.
	 * Should only be invoked from main server thread.
	 *
	 * @param server      The minecraft server instance.
	 * @param forceRemove Force remove all cleanable ships even they was not previously marked.
	 * @return clean ships count
	 */
	public static int clean(final MinecraftServer server, final boolean forceRemove) {
		Constants.LOG.info("[vsplit]: Cleaning ships. forceRemove = {}", forceRemove);
		if (Config.cleanupMessageLevel.showGeneral()) {
			server.getPlayerList().broadcastSystemMessage(
				Constants.MESSAGE_PREFIX.copy().append(Component.translatable("vsplit.message.clean.start")),
				false
			);
		}
		int count = 0;
		final ShipAllocator allocator = ShipAllocator.get(server);
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(server);
		final List<ICleanListener> generalListeners = getListeners();
		for (final ServerShip ship : new ShipAllocator.SafeShipIterable<>(world.getAllShips())) {
			final ServerLevel level = LevelUtil.getLevel(ship.getChunkClaimDimension());
			final ShipCleanAttachment cleanAttachment = ShipCleanAttachment.get(ship);
			if (cleanAttachment.isProtected()) {
				continue;
			}

			final Collection<ICleanListener> shipListeners = ship instanceof final ShipObjectServerAccessor shipObject
				? shipObject.vsplit$getCleanListeners()
				: Collections.emptyList();
			final List<ICleanListener> listeners;
			if (shipListeners.isEmpty()) {
				listeners = generalListeners;
			} else {
				// generalListeners will never be empty
				listeners = new ArrayList<>(generalListeners.size() + shipListeners.size());
				final Iterator<ICleanListener> gIter = generalListeners.iterator();
				final Iterator<ICleanListener> sIter = shipListeners.iterator();
				ICleanListener gCurrent = gIter.next(), sCurrent = sIter.next();
				while (true) {
					if (gCurrent.getShipCleanPriority() <= sCurrent.getShipCleanPriority()) {
						listeners.add(gCurrent);
						if (!gIter.hasNext()) {
							listeners.add(sCurrent);
							sIter.forEachRemaining(listeners::add);
							break;
						}
						gCurrent = gIter.next();
					} else {
						listeners.add(sCurrent);
						if (!sIter.hasNext()) {
							listeners.add(gCurrent);
							gIter.forEachRemaining(listeners::add);
							break;
						}
						sCurrent = sIter.next();
					}
				}
			}

			final boolean marked = cleanAttachment.isMarked();
			final CleanContext context = new CleanContext(level, ship, marked);
			for (final ICleanListener listener : listeners) {
				listener.onShipClean(context);
			}
			final boolean shouldClean = context.getCleanSuggestion();
			if (shouldClean != marked) {
				Constants.LOG.debug("[vsplit]: Ship {} marked = {}", ship.getId(), shouldClean);
				cleanAttachment.setMarked(shouldClean);
			}
			if (shouldClean && (forceRemove || marked) && !ShipConnectivityApi.isConnectedToGround(ship.getId())) {
				final Set<ServerShip> ships = ShipConnectivityApi.getAllConnectedShipsAndSelf(ship.getId());
				boolean clean = true;
				if (ships.size() > 1) {
					for (final ServerShip part : ships) {
						final ShipCleanAttachment ca = ShipCleanAttachment.get(part);
						if (!ca.isMarked()) {
							clean = false;
							break;
						}
					}
				}
				if (clean) {
					// TODO: make a ship backup?
					for (final ServerShip part : ships) {
						Constants.LOG.info("[vsplit]: Cleaning ship {} [slug={}, parts={}]", part.getId(), part.getSlug(), ships.size());
						if (Config.shipCleanMethod.isDestroy()) {
							final AABBic box = part.getShipAABB();
							if (box != null) {
								for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
									level.destroyBlock(pos, true, null, 1);
								}
							}
						}
						allocator.putShip(part);
						count++;
					}
				}
			}
		}
		if (count == 0) {
			if (Config.cleanupMessageLevel.showGeneral()) {
				server.getPlayerList().broadcastSystemMessage(
					Constants.MESSAGE_PREFIX.copy().append(Component.translatable("vsplit.message.clean.success.none")),
					false
				);
			}
		} else {
			if (Config.cleanupMessageLevel.showCleaned()) {
				server.getPlayerList().broadcastSystemMessage(
					Constants.MESSAGE_PREFIX.copy().append(Component.translatable("vsplit.message.clean.success", count)),
					false
				);
			}
		}
		return count;
	}

	private static final class CleanContext implements ICleanListener.Context {
		private final ServerLevel level;
		private final ServerShip ship;
		private final boolean marked;
		private boolean cleanSuggestion = false;
		private boolean skipFlag = false;

		private CleanContext(final ServerLevel level, final ServerShip ship, final boolean marked) {
			this.level = level;
			this.ship = ship;
			this.marked = marked;
		}

		@Override
		public ServerLevel getLevel() {
			return this.level;
		}

		@Override
		public ServerShip getShip() {
			return this.ship;
		}

		@Override
		public boolean wasMarked() {
			return this.marked;
		}

		@Override
		public boolean getCleanSuggestion() {
			return this.cleanSuggestion;
		}

		@Override
		public void setCleanSuggestion(final boolean suggestion) {
			this.cleanSuggestion = suggestion;
		}

		@Override
		public void skipCheck() {
			this.skipFlag = true;
		}
	}
}
