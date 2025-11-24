package com.github.litermc.vsplit;

import com.github.litermc.vsplit.config.Config;
import com.github.litermc.vsplit.impl.attachment.DecayAttachment;
import com.github.litermc.vsplit.impl.clean.CleanScheduler;
import com.github.litermc.vsplit.util.AssembleUtil;
import com.github.litermc.vsplit.util.SplitUtil;
import com.github.litermc.vsplit.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

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

	public static void onPlayerBreakBlock(final ServerPlayer player, final BlockPos pos) {
		System.out.println("Config.enableTreeFalling: " + Config.enableTreeFalling);
		if (!Config.enableTreeFalling) {
			return;
		}
		final ServerLevel level = player.serverLevel();
		if (VSGameUtilsKt.isBlockInShipyard(level, pos)) {
			return;
		}
		final BlockState state = level.getBlockState(pos);
		if (!state.is(BlockTags.OVERWORLD_NATURAL_LOGS)) {
			return;
		}
		if (state.getOptionalValue(RotatedPillarBlock.AXIS).map(axis -> axis != Direction.Axis.Y).orElse(true)) {
			return;
		}
		final BlockPos.MutableBlockPos leafPos = pos.mutable();
		final int maxY = Math.min(pos.getY() + 32, level.getMaxBuildHeight());
		for (int y = pos.getY() + 1; y <= maxY; y++) {
			leafPos.setY(y);
			final BlockState st = level.getBlockState(leafPos);
			if (st.is(BlockTags.LEAVES)) {
				if (!st.getOptionalValue(LeavesBlock.PERSISTENT).orElse(true)) {
					final ServerShip ship = AssembleUtil.assembleTree(level, pos, state);
					if (ship != null) {
						final int DEFAULT_FORCE_DECAY_TIMEOUT = 20 * 60 * 10; // 10 min
						ship.setSlug(DecayAttachment.DECAY_PREFIX + "tree-" + ship.getId());
						ship.saveAttachment(DecayAttachment.class, new DecayAttachment(DEFAULT_FORCE_DECAY_TIMEOUT));
					}
				}
				return;
			}
			if (!st.is(BlockTags.OVERWORLD_NATURAL_LOGS)) {
				return;
			}
			if (st.getOptionalValue(RotatedPillarBlock.AXIS).map(axis -> axis != Direction.Axis.Y).orElse(true)) {
				return;
			}
		}
	}
}
