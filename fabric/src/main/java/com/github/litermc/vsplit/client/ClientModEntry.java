package com.github.litermc.vsplit.client;

import com.github.litermc.vsplit.VSplitRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class ClientModEntry implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VSplitRegistry.Blocks.onRegisterRenderType(BlockRenderLayerMap.INSTANCE::putBlock);
	}
}
