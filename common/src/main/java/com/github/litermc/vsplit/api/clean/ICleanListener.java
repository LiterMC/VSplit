package com.github.litermc.vsplit.api.clean;

import com.github.litermc.vsplit.api.clean.ShipCleaner;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.function.Consumer;

/**
 * ICleanListener is an interface to define special logic on ship cleaning.
 * It may be implemented on ship attachments to have ship specific checks.
 *
 * @see ShipCleaner
 */
public interface ICleanListener {
	/**
	 * Get the priority order of {@link onShipClean} or {@link checkShipCleanStatus}.
	 * Higher priority means it will be invoked at last.
	 * The prority should NOT change at any given time.
	 *
	 * Implemention Note:
	 *   VSplit will use and only use priority between [{@code 0}, {@code 99}] and [{@code 10000}, {@code 10099}].
	 *
	 * @return a constant priority
	 */
	int getShipCleanPriority();

	/**
	 * onShipClean will be invoked before a ship is marked as cleanable and before it is actually removed.
	 *
	 * @param context The ship clean context. Should only be used inside the method's lifecycle.
	 */
	void onShipClean(Context context);

	/**
	 * Predict if a ship maybe removed or not and show possible instructions to player.
	 *
	 * @param context         The ship clean context. Should only be used inside the method's lifecycle.
	 * @param messageConsumer Provides messages that may be present to players.
	 */
	default void checkShipCleanStatus(final Context context, final Consumer<Component> messageConsumer) {
		this.onShipClean(context);
	}

	interface Context {
		/**
		 * Get the level the ship is at.
		 *
		 * @return the level the ship is at.
		 */
		ServerLevel getLevel();

		/**
		 * Get the instance of the ship where the cleaning is happening.
		 *
		 * @return the cleaning ship.
		 */
		ServerShip getShip();

		/**
		 * Check if the ship had already been marked or not.
		 * If a ship had been marked, it will immediately be removed after the cycle.
		 * Otherwise, it will be cleaned in the next cycle if it is still suggested to be removed.
		 * 
		 * @return whether or not the ship has been marked in the last clean cycle.
		 */
		boolean wasMarked();

		/**
		 * Get the suggestion of whether the ship is going to be marked for remove or not.
		 *
		 * @return {@code true} if the ship will be removed, {@code false} otherwise.
		 * @see setCleanSuggestion
		 */
		boolean getCleanSuggestion();

		/**
		 * Set the suggestion of whether the ship is going to be marked for remove or not.
		 * If the clean suggestion is {@code true} at the end of the clean cycle, it will be marked for removal
		 * and be deleted right away if it had already been marked in the last cycle.
		 *
		 * @param suggestion {@code true} if the ship will be removed, {@code false} otherwise.
		 * @see getCleanSuggestion
		 * @see skipCheck
		 */
		void setCleanSuggestion(boolean suggestion);

		/**
		 * Skip the rest of clean check and use current {@link getCleanSuggestion clean suggestion} value
		 * @see setCleanSuggestion
		 */
		void skipCheck();
	}
}
