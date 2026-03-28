package com.monody.projectleveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.dimension.DungeonHelper;
import com.monody.projectleveling.dimension.ModDimensions;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.mob.MobLevelUtil;
import com.monody.projectleveling.party.Party;
import com.monody.projectleveling.party.PartyManager;
import com.monody.projectleveling.skill.SkillData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class ModCommands {

    private static final String[] STAT_NAMES = {
            "strength", "vitality", "agility", "intelligence",
            "sight", "luck", "dexterity", "mind", "faith"
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("plvl")

                // ======== /plvl reset ========
                .then(Commands.literal("reset")

                        // /plvl reset — reset everything (level, stats, skills, quest)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                stats.resetLevel();
                                long day = player.level().getDayTime() / 24000L;
                                stats.resetQuest(day);
                                StatEventHandler.applyStatModifiers(player, stats);
                                StatEventHandler.syncToClient(player);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Everything has been reset."), false);
                            });
                            return 1;
                        })

                        // /plvl reset stats
                        .then(Commands.literal("stats")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        stats.resetStats();
                                        StatEventHandler.applyStatModifiers(player, stats);
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Stats reset. Remaining points: " + stats.getRemainingPoints()), false);
                                    });
                                    return 1;
                                })
                        )

                        // /plvl reset level (OP)
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

                        // /plvl reset skill (OP) — refund skill SP, keep class
                        .then(Commands.literal("skill")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        stats.getSkillData().resetSkills();
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Skills reset. SP refunded."), false);
                                    });
                                    return 1;
                                })
                        )

                        // /plvl reset class (OP)
                        .then(Commands.literal("class")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        SkillData sd = stats.getSkillData();
                                        String oldClass = sd.getSelectedClass().getDisplayName();
                                        sd.reset(); // resets class, skill levels, SP, cooldowns
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Class reset (was " + oldClass + "). All skills and SP cleared."), false);
                                    });
                                    return 1;
                                })
                        )

                        // /plvl reset quest (OP)
                        .then(Commands.literal("quest")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                        long day = player.level().getDayTime() / 24000L;
                                        stats.resetQuest(day);
                                        StatEventHandler.syncToClient(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Daily quest reset."), false);
                                    });
                                    return 1;
                                })
                        )
                )

                // ======== /plvl add ========
                .then(Commands.literal("add")
                        .requires(src -> src.hasPermission(2))

                        // /plvl add level <amount>
                        .then(Commands.literal("level")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                int oldLevel = stats.getLevel();
                                                // Add enough EXP to gain the requested levels
                                                for (int i = 0; i < amount; i++) {
                                                    int needed = stats.getMaxExp() - stats.getCurrentExp();
                                                    stats.addExp(needed);
                                                }
                                                StatEventHandler.applyStatModifiers(player, stats);
                                                StatEventHandler.syncToClient(player);
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("Level " + oldLevel + " -> " + stats.getLevel()
                                                                + " (+" + (stats.getLevel() - oldLevel) + " levels)"), false);
                                            });
                                            return 1;
                                        })
                                )
                        )

                        // /plvl add exp <amount>
                        .then(Commands.literal("exp")
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
                                                    msg += " Level " + oldLevel + " -> " + stats.getLevel()
                                                            + "! (+" + (levelsGained * 4) + " stat points)";
                                                }
                                                String finalMsg = msg;
                                                ctx.getSource().sendSuccess(() -> Component.literal(finalMsg), false);
                                            });
                                            return 1;
                                        })
                                )
                        )

                        // /plvl add point <amount> — add allocation points
                        // /plvl add point <stat> <amount> — add points directly to a stat
                        .then(Commands.literal("point")
                                // /plvl add point <amount>
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                stats.setRemainingPoints(stats.getRemainingPoints() + amount);
                                                StatEventHandler.syncToClient(player);
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("+" + amount + " stat points. Total: " + stats.getRemainingPoints()), false);
                                            });
                                            return 1;
                                        })
                                )
                                // /plvl add point <stat> <amount>
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String s : STAT_NAMES) builder.suggest(s);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    String stat = StringArgumentType.getString(ctx, "stat").toLowerCase();
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                        boolean valid = addStatDirect(stats, stat, amount);
                                                        if (valid) {
                                                            StatEventHandler.applyStatModifiers(player, stats);
                                                            StatEventHandler.syncToClient(player);
                                                            ctx.getSource().sendSuccess(() ->
                                                                    Component.literal("+" + amount + " " + stat + " (free, no point cost)"), false);
                                                        } else {
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("Unknown stat: " + stat
                                                                            + ". Valid: strength, vitality, agility, intelligence, sight, luck, dexterity, mind, faith"));
                                                        }
                                                    });
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /plvl add sp <tier> <amount>
                        .then(Commands.literal("sp")
                                .then(Commands.argument("tier", IntegerArgumentType.integer(0, 4))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                        SkillData sd = stats.getSkillData();
                                                        sd.addTierSP(tier, amount);
                                                        StatEventHandler.syncToClient(player);
                                                        ctx.getSource().sendSuccess(() ->
                                                                Component.literal("+" + amount + " T" + tier + " SP. [T0:" + sd.getTierSP(0)
                                                                        + " T1:" + sd.getTierSP(1) + " T2:" + sd.getTierSP(2)
                                                                        + " T3:" + sd.getTierSP(3) + " T4:" + sd.getTierSP(4) + "]"), false);
                                                    });
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )

                // ======== /plvl set ========
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))

                        // /plvl set sp <tier> <amount>
                        .then(Commands.literal("sp")
                                .then(Commands.argument("tier", IntegerArgumentType.integer(0, 4))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                        SkillData sd = stats.getSkillData();
                                                        sd.setTierSP(tier, amount);
                                                        StatEventHandler.syncToClient(player);
                                                        ctx.getSource().sendSuccess(() ->
                                                                Component.literal("T" + tier + " SP set to " + amount + "."), false);
                                                    });
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /plvl set level <amount>
                        .then(Commands.literal("level")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                int oldLevel = stats.getLevel();
                                                stats.setLevel(amount);
                                                stats.setCurrentExp(0);
                                                StatEventHandler.applyStatModifiers(player, stats);
                                                StatEventHandler.syncToClient(player);
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("Level " + oldLevel + " -> " + amount + "."), false);
                                            });
                                            return 1;
                                        })
                                )
                        )

                        // /plvl set point <stat> <amount>
                        .then(Commands.literal("point")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String s : STAT_NAMES) builder.suggest(s);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    String stat = StringArgumentType.getString(ctx, "stat").toLowerCase();
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                                        boolean valid = setStatDirect(stats, stat, amount);
                                                        if (valid) {
                                                            StatEventHandler.applyStatModifiers(player, stats);
                                                            StatEventHandler.syncToClient(player);
                                                            ctx.getSource().sendSuccess(() ->
                                                                    Component.literal(stat + " set to " + amount + "."), false);
                                                        } else {
                                                            ctx.getSource().sendFailure(
                                                                    Component.literal("Unknown stat: " + stat
                                                                            + ". Valid: str, vit, agi, int, sight, luk, dex, mind, faith"));
                                                        }
                                                    });
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )

                // ======== /plvl hunt <zone> ========
                .then(Commands.literal("hunt")
                        .then(Commands.argument("zone", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int zone = IntegerArgumentType.getInteger(ctx, "zone") - 1;
                                    if (DungeonHelper.teleportToZone(player, zone)) {
                                        int minLv = MobLevelUtil.getDungeonMinLevel(zone);
                                        int maxLv = MobLevelUtil.getDungeonMaxLevel(zone);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Teleported to Dungeon " + (zone + 1)
                                                        + " (Lv." + minLv + "-" + maxLv + ")"), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Failed to teleport."));
                                    }
                                    return 1;
                                })
                        )
                )

                // ======== /plvl return ========
                .then(Commands.literal("return")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (ModDimensions.isDungeon(player.level().dimension())) {
                                DungeonHelper.returnToOverworld(player);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Returned to the Overworld."), false);
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal("You are not in a dungeon."));
                            }
                            return 1;
                        })
                )

                // ======== /plvl cd ========
                .then(Commands.literal("cd")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                                stats.getSkillData().clearAllCooldowns();
                                StatEventHandler.syncToClient(player);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("All skill cooldowns cleared."), false);
                            });
                            return 1;
                        })
                )

                // ======== /plvl party ========
                .then(Commands.literal("party")

                        // /plvl party create
                        .then(Commands.literal("create")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Party party = PartyManager.createParty(player.getUUID());
                                    if (party != null) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("\u00a7aParty created! Invite players with /plvl party invite <player>"), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("You are already in a party."));
                                    }
                                    return 1;
                                })
                        )

                        // /plvl party invite <player>
                        .then(Commands.literal("invite")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            if (player.getUUID().equals(target.getUUID())) {
                                                ctx.getSource().sendFailure(Component.literal("You can't invite yourself."));
                                                return 1;
                                            }
                                            int result = PartyManager.invite(player.getUUID(), target.getUUID());
                                            switch (result) {
                                                case 0 -> {
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("\u00a7aInvited " + target.getScoreboardName() + " to the party."), false);
                                                    target.sendSystemMessage(Component.literal(
                                                            "\u00a7e" + player.getScoreboardName() + " invited you to a party! Use /plvl party accept or /plvl party decline"));
                                                }
                                                case 1 -> ctx.getSource().sendFailure(Component.literal("Only the party leader can invite."));
                                                case 2 -> ctx.getSource().sendFailure(Component.literal(target.getScoreboardName() + " is already in a party."));
                                                case 3 -> ctx.getSource().sendFailure(Component.literal("Party is full (max " + Party.MAX_MEMBERS + ")."));
                                                case 4 -> ctx.getSource().sendFailure(Component.literal(target.getScoreboardName() + " already has a pending invite."));
                                                case 5 -> ctx.getSource().sendFailure(Component.literal("You are not in a party. Create one with /plvl party create"));
                                            }
                                            return 1;
                                        })
                                )
                        )

                        // /plvl party accept
                        .then(Commands.literal("accept")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Party party = PartyManager.acceptInvite(player.getUUID());
                                    if (party != null) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("\u00a7aYou joined the party!"), false);
                                        MinecraftServer server = ctx.getSource().getServer();
                                        for (ServerPlayer member : PartyManager.getOnlineMembers(party, server)) {
                                            if (!member.getUUID().equals(player.getUUID())) {
                                                member.sendSystemMessage(Component.literal(
                                                        "\u00a7e" + player.getScoreboardName() + " joined the party."));
                                            }
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(
                                                Component.literal("No pending invite, party is full, or you are already in a party."));
                                    }
                                    return 1;
                                })
                        )

                        // /plvl party decline
                        .then(Commands.literal("decline")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    if (PartyManager.declineInvite(player.getUUID())) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("\u00a7cInvite declined."), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("No pending invite."));
                                    }
                                    return 1;
                                })
                        )

                        // /plvl party leave
                        .then(Commands.literal("leave")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Party party = PartyManager.getParty(player.getUUID());
                                    if (party == null) {
                                        ctx.getSource().sendFailure(Component.literal("You are not in a party."));
                                        return 1;
                                    }
                                    MinecraftServer server = ctx.getSource().getServer();
                                    List<ServerPlayer> members = PartyManager.getOnlineMembers(party, server);
                                    PartyManager.leave(player.getUUID());
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("\u00a7cYou left the party."), false);
                                    for (ServerPlayer member : members) {
                                        if (!member.getUUID().equals(player.getUUID())) {
                                            member.sendSystemMessage(Component.literal(
                                                    "\u00a7e" + player.getScoreboardName() + " left the party."));
                                            Party updated = PartyManager.getParty(member.getUUID());
                                            if (updated != null && updated.isLeader(member.getUUID())) {
                                                member.sendSystemMessage(Component.literal(
                                                        "\u00a76You are now the party leader."));
                                            }
                                        }
                                    }
                                    return 1;
                                })
                        )

                        // /plvl party kick <player>
                        .then(Commands.literal("kick")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            int result = PartyManager.kick(player.getUUID(), target.getUUID());
                                            switch (result) {
                                                case 0 -> {
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("\u00a7c" + target.getScoreboardName() + " has been kicked."), false);
                                                    target.sendSystemMessage(Component.literal(
                                                            "\u00a7cYou have been kicked from the party."));
                                                }
                                                case 1 -> ctx.getSource().sendFailure(Component.literal("Only the party leader can kick."));
                                                case 2 -> ctx.getSource().sendFailure(Component.literal(target.getScoreboardName() + " is not in your party."));
                                                case 3 -> ctx.getSource().sendFailure(Component.literal("You can't kick yourself. Use /plvl party leave"));
                                                case 4 -> ctx.getSource().sendFailure(Component.literal("You are not in a party."));
                                            }
                                            return 1;
                                        })
                                )
                        )

                        // /plvl party list
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Party party = PartyManager.getParty(player.getUUID());
                                    if (party == null) {
                                        ctx.getSource().sendFailure(Component.literal("You are not in a party."));
                                        return 1;
                                    }
                                    MinecraftServer server = ctx.getSource().getServer();
                                    StringBuilder sb = new StringBuilder("\u00a76=== Party Members ===\n");
                                    for (UUID memberUUID : party.getMembers()) {
                                        ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
                                        String name = member != null ? member.getScoreboardName() : memberUUID.toString().substring(0, 8) + "...";
                                        String status = member != null ? "\u00a7a(online)" : "\u00a77(offline)";
                                        String leader = party.isLeader(memberUUID) ? " \u00a7e[Leader]" : "";
                                        sb.append("\u00a7f- ").append(name).append(" ").append(status).append(leader).append("\n");
                                    }
                                    sb.append("\u00a76Members: ").append(party.size()).append("/").append(Party.MAX_MEMBERS);
                                    ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                                    return 1;
                                })
                        )

                        // /plvl party disband
                        .then(Commands.literal("disband")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Party party = PartyManager.getParty(player.getUUID());
                                    if (party == null) {
                                        ctx.getSource().sendFailure(Component.literal("You are not in a party."));
                                        return 1;
                                    }
                                    MinecraftServer server = ctx.getSource().getServer();
                                    List<ServerPlayer> members = PartyManager.getOnlineMembers(party, server);
                                    if (PartyManager.disband(player.getUUID())) {
                                        for (ServerPlayer member : members) {
                                            member.sendSystemMessage(Component.literal("\u00a7cThe party has been disbanded."));
                                        }
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Only the party leader can disband."));
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }

    /**
     * Directly add stat points to a specific stat (bypasses allocation point cost).
     * Returns false if stat name is invalid.
     */
    private static boolean addStatDirect(com.monody.projectleveling.capability.PlayerStats stats, String stat, int amount) {
        switch (stat) {
            case "str", "strength" -> stats.setStrength(stats.getStrength() + amount);
            case "vit", "vitality" -> stats.setVitality(stats.getVitality() + amount);
            case "agi", "agility" -> stats.setAgility(stats.getAgility() + amount);
            case "int", "intelligence" -> stats.setIntelligence(stats.getIntelligence() + amount);
            case "sight" -> stats.setSight(stats.getSight() + amount);
            case "luk", "luck" -> stats.setLuck(stats.getLuck() + amount);
            case "dex", "dexterity" -> stats.setDexterity(stats.getDexterity() + amount);
            case "mind" -> stats.setMind(stats.getMind() + amount);
            case "faith" -> stats.setFaith(stats.getFaith() + amount);
            default -> { return false; }
        }
        return true;
    }

    private static boolean setStatDirect(com.monody.projectleveling.capability.PlayerStats stats, String stat, int value) {
        switch (stat) {
            case "str", "strength" -> stats.setStrength(value);
            case "vit", "vitality" -> stats.setVitality(value);
            case "agi", "agility" -> stats.setAgility(value);
            case "int", "intelligence" -> stats.setIntelligence(value);
            case "sight" -> stats.setSight(value);
            case "luk", "luck" -> stats.setLuck(value);
            case "dex", "dexterity" -> stats.setDexterity(value);
            case "mind" -> stats.setMind(value);
            case "faith" -> stats.setFaith(value);
            default -> { return false; }
        }
        return true;
    }
}
