package com.github.litermc.vsplit.impl.attachment;

import com.github.litermc.vtil.api.storage.IShipAdditionalData;
import com.github.litermc.vtil.api.storage.ShipDataStorage;
import net.minecraft.nbt.CompoundTag;
import org.valkyrienskies.core.api.ships.ServerShip;

public final class ShipCleanAttachment implements IShipAdditionalData {
	private boolean protecting = false;
	private boolean marked = false;

	public static ShipCleanAttachment get(final ServerShip ship) {
		return ShipDataStorage.get(ship).getOrCreate(ShipCleanAttachment.class);
	}

	public boolean isProtected() {
		return this.protecting;
	}

	public void setProtected(final boolean protecting) {
		this.protecting = protecting;
	}

	public boolean isMarked() {
		return this.marked;
	}

	public void setMarked(final boolean marked) {
		this.marked = marked;
	}

	@Override
	public void load(final CompoundTag data) {
		data.putBoolean("protected", this.protecting);
		data.putBoolean("marked", this.marked);
	}

	@Override
	public void save(final CompoundTag data) {
		this.protecting = data.getBoolean("protected");
		this.marked = data.getBoolean("marked");
	}
}
