package com.github.litermc.vsplit.command;

import com.github.litermc.vsplit.Constants;
import com.github.litermc.vsplit.api.clean.ShipCleaner;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Set;

public final class VSplitCommands {
	public static final String ROOT_LITERAL = Constants.MOD_ID;

	private VSplitCommands() {}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(ROOT_LITERAL)
			.requires((source) -> source.hasPermission(2))
			.then(Commands.literal("clean")
				.then(Commands.argument("forceRemoval", BoolArgumentType.bool())
					.executes((ctx) -> VSplitCommands.clean(ctx, BoolArgumentType.getBool(ctx, "forceRemoval")))
				)
				.executes((ctx) -> VSplitCommands.clean(ctx, false))
			)
		);
	}

	private static int clean(final CommandContext<CommandSourceStack> context, final boolean forceRemoval) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		source.sendSuccess(() -> Component.translatable("vsplit.command.clean.start"), false);
		final int count = ShipCleaner.clean(server, forceRemoval);
		if (count == 0) {
			source.sendSuccess(() -> Component.translatable("vsplit.command.clean.success.none"), true);
		} else {
			source.sendSuccess(() -> Component.translatable("vsplit.command.clean.success", count), true);
		}
		return count;
	}
}
