package com.github.litermc.vsplit.impl.clean;

import com.github.litermc.vsplit.api.clean.ICleanListener;
import com.github.litermc.vsplit.config.Config;
import com.github.litermc.vsplit.config.CoreBlockBehaviour;
import com.github.litermc.vtil.api.connectivity.BlockConnectivityApi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.function.Consumer;

public final class ShipBlockCountCleaner implements ICleanListener {
	public static final ShipBlockCountCleaner INSTANCE = new ShipBlockCountCleaner();

	@Override
	public int getShipCleanPriority() {
		return 10;
	}

	@Override
	public void onShipClean(final Context context) {
		if (!Config.enableBlockCountCleanup) {
			return;
		}
		if (context.getCleanSuggestion()) {
			return;
		}
		final ServerLevel level = context.getLevel();
		final ServerShip ship = context.getShip();
		final int blocks = this.getShipBlocks(level, ship);
		if (blocks == Integer.MAX_VALUE) {
			return;
		}
		context.setCleanSuggestion(true);
	}

	@Override
	public void checkShipCleanStatus(final Context context, final Consumer<Component> messageConsumer) {
		if (!Config.enableBlockCountCleanup) {
			return;
		}
		final ServerLevel level = context.getLevel();
		final ServerShip ship = context.getShip();
		int blocks = this.getShipBlocks(level, ship);
		if (blocks == Integer.MAX_VALUE) {
			return;
		}
		final boolean hasCore = blocks >= 0;
		if (hasCore) {
			blocks = -blocks;
		} else {
			messageConsumer.accept(Component.translatable("vsplit.message.clean.core_block.hint"));
		}
		if (blocks < Config.minimumBlocksShipNeeds) {
			messageConsumer.accept(Component.translatable("vsplit.message.clean.block_count.hint", blocks, Config.minimumBlocksShipNeeds));
		}
		context.setCleanSuggestion(true);
	}

	private int getShipBlocks(final ServerLevel level, final ServerShip ship) {
		final AABBic box = ship.getShipAABB();
		if (box == null) {
			return 0;
		}
		final CoreBlockBehaviour behaviour = Config.coreBlockBehaviour;
		int blocks = 0;
		boolean hasCore = behaviour == CoreBlockBehaviour.IGNORED;
		for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			final BlockState state = level.getBlockState(pos);
			if (BlockConnectivityApi.isAir(state)) {
				continue;
			}
			if (!hasCore && Config.coreBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
				hasCore = true;
			}
			blocks++;
			if (blocks >= Config.minimumBlocksShipNeeds && (hasCore || behaviour != CoreBlockBehaviour.REQUIRED)) {
				return Integer.MAX_VALUE;
			}
		}
		return hasCore ? blocks : -blocks;
	}
}
