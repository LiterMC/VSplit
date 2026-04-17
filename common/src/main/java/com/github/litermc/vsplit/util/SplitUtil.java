package com.github.litermc.vsplit.util;

import com.github.litermc.vsplit.accessor.ShipObjectServerAccessor;
import com.github.litermc.vsplit.api.attachment.ISplitListener;
import com.github.litermc.vsplit.config.Config;
import com.github.litermc.vtil.api.assemble.AssembleApi;
import com.github.litermc.vtil.api.connectivity.BlockConnectivityApi;
import com.github.litermc.vtil.util.LevelUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

public final class SplitUtil {
	private SplitUtil() {}

	private static final int ASYNC_SHIP_SPLIT_TIMEOUT = 20;
	private static final Map<LoadedServerShip, Map<BlockPos, OldStateHolder>> UPDATING_SHIP = new HashMap<>();
	private static int PART_COUNTER = 0;

	/**
	 * module-private
	 */
	public static void onBlockUpdated(final ServerLevel level, final BlockPos pos, final BlockState oldState, final BlockState newState) {
		if (!Config.enableShipSplit) {
			return;
		}
		final LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(level, pos);
		if (ship == null) {
			return;
		}
		final Map<BlockPos, OldStateHolder> updates = UPDATING_SHIP.computeIfAbsent(ship, (s) -> new HashMap<>());
		if (updates.containsKey(pos)) {
			return;
		}
		if (BlockConnectivityApi.isAir(oldState)) {
			updates.put(pos, new OldStateHolder(oldState, List.of()));
		} else {
			final List<BlockPos> prevConns = new ArrayList<>(6);
			BlockConnectivityApi.getPossibleConnectableBlocks(level, pos, oldState, prevConns);
			updates.put(pos, new OldStateHolder(oldState, prevConns));
		}
	}

	/**
	 * module-private
	 */
	public static void postServerTick() {
		if (UPDATING_SHIP.isEmpty()) {
			return;
		}
		final Map<LoadedServerShip, Map<BlockPos, OldStateHolder>> updating = Map.copyOf(UPDATING_SHIP);
		UPDATING_SHIP.clear();
		final List<ISplitListener> listeners = new ArrayList<>(0);
		for (final Map.Entry<LoadedServerShip, Map<BlockPos, OldStateHolder>> entry : updating.entrySet()) {
			final LoadedServerShip ship = entry.getKey();
			final AABBic shipArea = ship.getShipAABB();
			if (shipArea == null || (shipArea.minX() == shipArea.maxX() && shipArea.minY() == shipArea.maxY() && shipArea.minZ() == shipArea.maxZ())) {
				continue;
			}
			final ShipObjectServerAccessor slGetter = ship instanceof final ShipObjectServerAccessor slGetter0 ? slGetter0 : null;
			final Map<BlockPos, OldStateHolder> updates = entry.getValue();
			final ServerLevel level = LevelUtil.getLevel(ship.getChunkClaimDimension());
			final List<Set<BlockPos>> parts = getSeparatedParts(level, ship, updates);
			if (parts == null) {
				continue;
			}
			final String slug = extractBaseSlug(ship.getSlug());
			for (final Set<BlockPos> part : parts) {
				final List<Consumer<LoadedServerShip>> callbacks;
				if (slGetter == null) {
					callbacks = null;
				} else {
					listeners.addAll(slGetter.vsplit$getSplitListeners());
					callbacks = new ArrayList<>(listeners.size());
					final SplitContext context = new SplitContext(level, ship, Collections.unmodifiableSet(part), callbacks);
					for (final ISplitListener listener : listeners) {
						listener.onShipSplit(context);
					}
					listeners.clear();
				}
				if (Config.asyncShipSplit) {
					AssembleApi.createShipAsync(level, part, ASYNC_SHIP_SPLIT_TIMEOUT).thenAccept((splittedShip) -> {
						if (splittedShip == null) {
							return;
						}
						splittedShip.setSlug(addPartSlug(slug));
						if (callbacks == null) {
							return;
						}
						for (final Consumer<LoadedServerShip> callback : callbacks) {
							callback.accept((LoadedServerShip) splittedShip);
						}
					});
					continue;
				}
				final ServerShip splittedShip = AssembleApi.createShip(level, part);
				if (splittedShip == null) {
					continue;
				}
				splittedShip.setSlug(addPartSlug(slug));
				if (callbacks == null) {
					continue;
				}
				for (final Consumer<LoadedServerShip> callback : callbacks) {
					callback.accept((LoadedServerShip) splittedShip);
				}
			}
		}
	}

	private static String extractBaseSlug(final String slug) {
		if (slug == null) {
			return null;
		}
		final int partIndex = slug.lastIndexOf("+part-");
		if (partIndex == -1) {
			return slug;
		}
		return slug.substring(0, partIndex);
	}

	private static String addPartSlug(final String slug) {
		if (slug == null) {
			return null;
		}
		PART_COUNTER++;
		if (PART_COUNTER >= 10000) {
			PART_COUNTER = 1;
		}
		return String.format("%s+part-%04d", slug, PART_COUNTER);
	}

	/**
	 * Split a ship after block updates.
	 * @param level   The level the ship is in.
	 * @param ship    The ship.
	 * @param updates Updated block positions and its last connections.
	 * @return
	 *     {@code null} if the ship should not split.
	 *     Otherwise, one or more disjoint BlockPos sets will returns to indicate the parts to be split out.
	 */
	public static List<Set<BlockPos>> getSeparatedParts(final Level level, final Ship ship, final Map<BlockPos, OldStateHolder> updates) {
		final Map<BlockPos, PartHolder> visited = new HashMap<>(32);
		final Set<PartHolder> parts = new HashSet<>(updates.size() * 2);

		final Set<BlockPos> nextPosSet = new HashSet<>(6);
		for (final Map.Entry<BlockPos, OldStateHolder> update : updates.entrySet()) {
			final BlockPos startBlock = update.getKey();
			final OldStateHolder prevState = update.getValue();
			final BlockState newState = level.getBlockState(startBlock);
			if (!BlockConnectivityApi.willConnectivityChange(level, startBlock, prevState.state(), newState)) {
				continue;
			}
			if (!BlockConnectivityApi.isAir(newState)) {
				if (!visited.containsKey(startBlock)) {
					final PartHolder p = new PartHolder(new Part(startBlock));
					parts.add(p);
					visited.put(startBlock, p);
				}
				BlockConnectivityApi.getPossibleConnectableBlocks(level, startBlock, newState, nextPosSet);
			}
			if (prevState.connections().isEmpty()) {
				// New block placed, check if it can connect to the ship.
				for (final BlockPos conn : nextPosSet) {
					if (
						!visited.containsKey(conn) &&
						!BlockConnectivityApi.isAir(level.getBlockState(conn))
					) {
						final PartHolder p = new PartHolder(new Part(conn));
						parts.add(p);
						visited.put(conn, p);
					}
				}
			} else {
				for (final BlockPos conn : prevState.connections()) {
					if (
						!nextPosSet.contains(conn) &&
						!visited.containsKey(conn) &&
						!BlockConnectivityApi.isAir(level.getBlockState(conn))
					) {
						final PartHolder p = new PartHolder(new Part(conn));
						parts.add(p);
						visited.put(conn, p);
					}
				}
			}
			nextPosSet.clear();
		}
		if (parts.size() <= 1) {
			return null;
		}

		final List<BlockPos> nextPoses = new ArrayList<>(6);

		final List<Set<BlockPos>> scannedParts = new ArrayList<>();
		final Set<PartHolder> partsCopy = new HashSet<>(parts.size());
		int polled;
		do {
			polled = 0;
			partsCopy.clear();
			partsCopy.addAll(parts);
			for (final PartHolder holder : partsCopy) {
				if (!parts.contains(holder)) {
					continue;
				}
				final PartHolder holder1 = holder.getForward();
				final Part part = holder1.part;
				if (part.complete) {
					throw new RuntimeException("unreachable");
				}
				final BlockPos pos = part.poll();
				if (pos == null) {
					part.complete = true;
					scannedParts.add(part.blocks);
					parts.removeIf((h) -> h.getForward() == holder1);
					continue;
				}
				polled++;
				final BlockState state = level.getBlockState(pos);
				// assert !isAir(state);
				nextPoses.clear();
				BlockConnectivityApi.getPossibleConnectableBlocks(level, pos, state, nextPoses);
				for (final BlockPos p : nextPoses) {
					if (part.blocks.contains(p)) {
						continue;
					}
					final BlockState s = level.getBlockState(p);
					if (BlockConnectivityApi.isAir(s)) {
						continue;
					}
					if (!BlockConnectivityApi.isBlockConnectable(level, p, s, pos, state)) {
						continue;
					}
					PartHolder otherHolder = visited.get(p);
					if (otherHolder != null) {
						otherHolder = otherHolder.getForward();
						final Part otherPart = otherHolder.part;
						if (otherPart != part) {
							part.merge(otherPart);
							otherPart.complete = true;
							otherHolder.forward = holder;
							parts.remove(otherHolder);
							if (scannedParts.isEmpty() && parts.size() <= 1) {
								return null;
							}
						}
						continue;
					}
					part.add(p);
					visited.put(p, holder);
				}
			}
		} while (polled > 1);

		if (parts.isEmpty()) {
			scannedParts.remove(scannedParts.size() - 1);
		}
		return scannedParts;
	}

	public record OldStateHolder(BlockState state, Collection<BlockPos> connections) {}

	private static final class PartHolder {
		final Part part;
		PartHolder forward = null;

		PartHolder(final Part part) {
			this.part = part;
		}

		PartHolder getForward() {
			if (this.forward != null) {
				return this.forward.getForward();
			}
			return this;
		}
	}

	private static final class Part {
		final Set<BlockPos> blocks = new HashSet<>();
		final Queue<BlockPos> pending = new ArrayDeque<>();
		boolean complete = false;

		Part(final BlockPos startBlock) {
			this.add(startBlock);
		}

		void merge(final Part other) {
			if (other.complete) {
				throw new RuntimeException("Unexpected early stopped scan. Connectivity inconsistent?");
			}
			this.blocks.addAll(other.blocks);
			while (true) {
				final BlockPos pos = other.poll();
				if (pos == null) {
					break;
				}
				this.pending.add(pos);
			}
		}

		BlockPos poll() {
			return this.pending.poll();
		}

		void add(final BlockPos pos) {
			if (this.blocks.add(pos)) {
				this.pending.add(pos);
			}
		}
	}

	private static final class SplitContext implements ISplitListener.Context {
		private final ServerLevel level;
		private final LoadedServerShip ship;
		private final Set<BlockPos> blocks;
		private final List<Consumer<LoadedServerShip>> callbacks;

		private SplitContext(
			final ServerLevel level,
			final LoadedServerShip ship,
			final Set<BlockPos> blocks,
			final List<Consumer<LoadedServerShip>> callbacks
		) {
			this.level = level;
			this.ship = ship;
			this.blocks = blocks;
			this.callbacks = callbacks;
		}

		@Override
		public ServerLevel getLevel() {
			return this.level;
		}

		@Override
		public LoadedServerShip getShip() {
			return this.ship;
		}

		@Override
		public Set<BlockPos> getBlocks() {
			return this.blocks;
		}

		@Override
		public void addAfterSplit(final Consumer<LoadedServerShip> callback) {
			if (callback == null) {
				throw new IllegalArgumentException("callback cannot be null");
			}
			this.callbacks.add(callback);
		}
	}
}
