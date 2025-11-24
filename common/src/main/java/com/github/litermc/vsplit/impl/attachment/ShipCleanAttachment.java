package com.github.litermc.vsplit.impl.attachment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.valkyrienskies.core.api.ships.ServerShip;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ShipCleanAttachment {
	private boolean protecting = false;
	private boolean marked = false;

	@JsonGetter("protected")
	public boolean isProtected() {
		return this.protecting;
	}

	@JsonSetter("protected")
	public void setProtected(final boolean protecting) {
		this.protecting = protecting;
	}

	@JsonGetter("marked")
	public boolean isMarked() {
		return this.marked;
	}

	@JsonSetter("marked")
	public void setMarked(final boolean marked) {
		this.marked = marked;
	}

	public static ShipCleanAttachment get(final ServerShip ship) {
		final ShipCleanAttachment attachment = ship.getAttachment(ShipCleanAttachment.class);
		if (attachment != null) {
			return attachment;
		}
		final ShipCleanAttachment newAttachment = new ShipCleanAttachment();
		ship.saveAttachment(ShipCleanAttachment.class, newAttachment);
		return newAttachment;
	}
}
