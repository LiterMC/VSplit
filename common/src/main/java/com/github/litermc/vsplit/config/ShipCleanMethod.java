package com.github.litermc.vsplit.config;

public enum ShipCleanMethod {
	DESTROY, REMOVE;

	public boolean isDestroy() {
		return this == DESTROY;
	}
}
