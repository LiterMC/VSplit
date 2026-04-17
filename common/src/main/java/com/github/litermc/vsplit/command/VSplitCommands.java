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

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.arguments.ShipArgument;

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
			.then(Commands.literal("protect")
				.then(Commands.argument("ships", ShipArgument.ships())
					.then(Commands.argument("shouldProtect", BoolArgumentType.bool())
						.executes((ctx) -> VSplitCommands.protect(ctx, BoolArgumentType.getBool(ctx, "shouldProtect")))
					)
					.executes((ctx) -> VSplitCommands.protect(ctx, true))
				)
			)
		);
	}

	private static int clean(final CommandContext<CommandSourceStack> context, final boolean forceRemoval) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		ShipCleaner.clean(server, forceRemoval);
		return 1;
	}

	private static int protect(final CommandContext<CommandSourceStack> context, final boolean shouldProtect) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.getShips(context, "ships");
		int count = 0;
		for (final Ship ship : ships) {
			if (!(ship instanceof ServerShip serverShip)) {
				continue;
			}
			ShipCleaner.setShipProtected(serverShip, shouldProtect);
			count++;
		}
		final int finalCount = count;
		source.sendSuccess(() -> Component.translatable("vsplit.command.protect.success", finalCount), false);
		return count;
	}
}
