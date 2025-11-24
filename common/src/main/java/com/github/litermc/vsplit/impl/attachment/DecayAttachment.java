package com.github.litermc.vsplit.impl.attachment;

import com.github.litermc.vsplit.api.attachment.ISplitListener;
import com.github.litermc.vsplit.config.Config;
import com.github.litermc.vsplit.config.DecayMethod;
import com.github.litermc.vsplit.util.ShipRandomTickGenerator;
import com.github.litermc.vtil.api.attachment.IServerTickListener;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableMap;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerTickListener;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class DecayAttachment implements IServerTickListener, ISplitListener {
	public static final String DECAY_PREFIX = "+decay+";
	private static final ImmutableMap<Block, Block> STAGED = addStagedDecayBlocks(new ImmutableMap.Builder<Block, Block>()).build();

	@JsonProperty("forceDecayCounter")
	private int forceDecayCounter;
	private final ShipRandomTickGenerator rnd = new ShipRandomTickGenerator(5, DecayAttachment::onRandomTick);

	private DecayAttachment() {
		this(-1);
	}

	public DecayAttachment(final int forceDecayCounter) {
		this.forceDecayCounter = forceDecayCounter;
	}

	@JsonGetter("decayRate")
	private int getDecayRate() {
		return this.rnd.getRate();
	}

	@JsonSetter("decayRate")
	private void setDecayRate(final int rate) {
		this.rnd.setRate(rate);
	}

	@Override
	public void onServerTick(final ServerLevel level, final LoadedServerShip ship) {
		final String slug = ship.getSlug();
		if (slug == null || !slug.startsWith(DECAY_PREFIX)) {
			ship.saveAttachment(DecayAttachment.class, null);
			return;
		}
		this.forceDecayCounter--;
		if (this.forceDecayCounter < 0) {
			ship.saveAttachment(DecayAttachment.class, null);
			VSGameUtilsKt.getShipObjectWorld(level).deleteShip(ship);
			return;
		}
		this.rnd.tick(level, ship);
	}

	@Override
	public void onShipSplit(final Context context) {
		if (Config.decayMethod == DecayMethod.NO_SPLIT) {
			final ServerLevel level = context.getLevel();
			context.getBlocks().forEach((pos) -> level.destroyBlock(pos, true));
			return;
		}
		context.addAfterSplit((newShip) -> {
			newShip.saveAttachment(DecayAttachment.class, new DecayAttachment(this.forceDecayCounter));
		});
	}

	private static ImmutableMap.Builder<Block, Block> addStagedDecayBlocks(final ImmutableMap.Builder<Block, Block> builder) {
		return builder
			.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
			.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
			.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG)
			.put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM)
			.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
			.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
			.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG)
			.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
			.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
			.put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM);
	}

	private static void onRandomTick(final ServerLevel level, final LoadedServerShip ship, final BlockPos pos, final BlockState state) {
		if (!state.isAir()) {
			destroyBlock(level, ship, pos);
		}
	}

	private static void destroyBlock(final ServerLevel level, final LoadedServerShip ship, final BlockPos pos) {
		final BlockState state = level.getBlockState(pos);
		final Block decayedBlock = STAGED.get(state.getBlock());
		if (decayedBlock != null) {
			final BlockState[] newState = new BlockState[]{decayedBlock.defaultBlockState()};
			state.getValues().forEach((k, v) -> {
				newState[0] = trySetValue(newState[0], k, v);
			});
			level.setBlock(pos, newState[0], Block.UPDATE_ALL);
			return;
		}
		level.destroyBlock(pos, true);
	}

	private static <T extends Comparable<T>> BlockState trySetValue(
		final BlockState state,
		final Property<T> property,
		final Object value
	) {
		return state.trySetValue(property, (T) value);
	}
}
