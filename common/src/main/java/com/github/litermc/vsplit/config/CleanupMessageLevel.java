package com.github.litermc.vsplit.config;

public enum CleanupMessageLevel {
	FULL, NO_WARN, MINIMUM, NONE;

	public boolean showSchedule() {
		return this == FULL;
	}

	public boolean showGeneral() {
		return this == FULL || this == NO_WARN;
	}

	public boolean showCleaned() {
		return this != NONE;
	}
}
