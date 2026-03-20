package com.monody.projectleveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.event.StatEventHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("plvl")
                .then(Commands.literal("reset")
                        .then(Commands.literal("stats")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        stats.resetStats();
                                        StatEventHandler.applyStatModifiers(player, stats);
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Stats have been reset. Remaining points: " + stats.getRemainingPoints()), false);
                                    });
                                    return 1;
                                })
                        )
                        .then(Commands.literal("level")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        stats.resetLevel();
                                        StatEventHandler.applyStatModifiers(player, stats);
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Level reset to 1. All stats and points have been reset."), false);
                                    });
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("quest")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        long day = player.level().getDayTime() / 24000L;
                                        stats.resetQuest(day);
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Daily quest reset."), false);
                                    });
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("addexp")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        int oldLevel = stats.getLevel();
                                        int levelsGained = stats.addExp(amount);
                                        if (levelsGained > 0) {
                                            StatEventHandler.applyStatModifiers(player, stats);
                                        }
                                        StatEventHandler.syncToClient(player);
                                        String msg = "+" + amount + " EXP (" + stats.getCurrentExp() + "/" + stats.getMaxExp() + ")";
                                        if (levelsGained > 0) {
                                            msg += " Level " + oldLevel + " -> " + stats.getLevel() + "! (+" + (levelsGained * 4) + " stat points)";
                                        }
                                        String finalMsg = msg;
                                        ctx.getSource().sendSuccess(() -> Component.literal(finalMsg), false);
                                    });
                                    return 1;
                                })
                        )
                )
        );
    }
}
