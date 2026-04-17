package com.github.litermc.vsplit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants {
	public static final String MOD_ID = "vsplit";
	public static final String MOD_NAME = "VSplit";
	public static final String MOD_VERSION = "0.4.0";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
	public static final Component MESSAGE_PREFIX = Component.literal("[" + MOD_NAME + "] ").withStyle(ChatFormatting.DARK_PURPLE);

	private Constants() {}
}
