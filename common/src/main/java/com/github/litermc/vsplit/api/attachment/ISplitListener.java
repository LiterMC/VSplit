package com.github.litermc.vsplit.api.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.Set;
import java.util.function.Consumer;

/**
 * ISplitListener is an interface for ship attachments to define special logic on ship splitting.
 */
public interface ISplitListener {
	/**
	 * onShipSplit is invoked before each part of a ship is going to split out.
	 *
	 * @param context The ship split context. Should only be used inside the method's lifecycle.
	 */
	void onShipSplit(Context context);

	interface Context {
		/**
		 * Get the level where split happens
		 *
		 * @return the level the splitting ship is in
		 */
		ServerLevel getLevel();

		/**
		 * Get the instance of the ship where the splitting is happening.
		 *
		 * @return the splitting ship.
		 */
		ServerShip getShip();

		/**
		 * Get the blocks that is going to split out.
		 *
		 * @return immutable block set that will form a new ship. 
		 */
		Set<BlockPos> getBlocks();

		/**
		 * Add a callback to be invoked after ship splitting.
		 * The callback will be provided with the instance of splitted ship.
		 *
		 * @param callback The after split callback.
		 */
		void addAfterSplit(Consumer<ServerShip> callback);
	}
}
