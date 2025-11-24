package com.github.litermc.vsplit.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import org.valkyrienskies.core.api.ships.LoadedServerShip;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

public class ShipRandomTickGenerator {
	private final Random rnd = new Random();
	private final Ticker blockTicker;
	private int rate;

	public ShipRandomTickGenerator(final int rate, final Ticker blockTicker) {
		this.rate = rate;
		this.blockTicker = blockTicker;
	}

	public int getRate() {
		return this.rate;
	}

	public void setRate(final int rate) {
		this.rate = rate;
	}

	public void tick(final ServerLevel level, final LoadedServerShip ship) {
		ship.getActiveChunksSet().forEach((chunkX, chunkZ) -> {
			final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
			final LevelChunkSection[] sections = chunk.getSections();
			for (int i = 0; i < sections.length; i++) {
				final LevelChunkSection section = sections[i];
				if (section.hasOnlyAir()) {
					continue;
				}
				final SectionPos spos = SectionPos.of(chunk.getPos(), chunk.getSectionYFromSectionIndex(i));
				for (int j = 0; j < this.rate; j++) {
					// section size is 16*16*16
					final int n = this.rnd.nextInt();
					final int x = n & 0xf, z = (n >> 8) & 0xf, y = (n >> 16) & 0xf;
					final BlockState state = section.getBlockState(x, y, z);
					this.blockTicker.tick(level, ship, spos.origin().offset(x, y, z), state);
				}
			}
		});
	}

	@FunctionalInterface
	public static interface Ticker {
		void tick(ServerLevel level, LoadedServerShip ship, BlockPos pos, BlockState state);
	}
}
