package com.monody.projectleveling.event;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.network.ModNetwork;
import com.monody.projectleveling.network.S2CSyncStatsPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ProjectLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatEventHandler {
    // Track last position per player for walk distance calculation
    private static final Map<UUID, double[]> lastPositions = new HashMap<>();

    private static final UUID STRENGTH_UUID = UUID.fromString("a3d5b8c1-1234-4a5b-9c6d-7e8f9a0b1c2d");
    private static final UUID VITALITY_UUID = UUID.fromString("b4e6c9d2-2345-4b6c-ad7e-8f9a0b1c2d3e");
    private static final UUID AGILITY_UUID = UUID.fromString("c5f7dae3-3456-4c7d-be8f-9a0b1c2d3e4f");

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastPositions.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                stats.checkNewDay(player.level().getDayTime() / 24000L);
                applyStatModifiers(player, stats);
                syncToClient(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                applyStatModifiers(player, stats);
                syncToClient(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                applyStatModifiers(player, stats);
                syncToClient(player);
            });
        }
    }

    // === Daily Quest Tracking ===

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (stats.isQuestCompleted()) return;
            stats.addQuestKill();
            checkQuestCompletion(player, stats);
            syncToClient(player);
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (stats.isQuestCompleted()) return;
            stats.addQuestBlockMined();
            checkQuestCompletion(player, stats);
            syncToClient(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Check for new day every 5 seconds
        if (player.tickCount % 100 == 0) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                long currentDay = player.level().getDayTime() / 24000L;
                if (stats.checkNewDay(currentDay)) {
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eA new Daily Quest has arrived."));
                    syncToClient(player);
                }
            });
        }

        // MP regen every second
        if (player.tickCount % 20 == 0) {
            player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                if (stats.regenMp()) {
                    syncToClient(player);
                }
            });
        }

        // Walk tracking every second
        if (player.tickCount % 20 == 0) {
            UUID pid = player.getUUID();
            double[] last = lastPositions.get(pid);
            double cx = player.getX();
            double cz = player.getZ();

            if (last != null) {
                double dx = cx - last[0];
                double dz = cz - last[1];
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                        if (stats.isQuestCompleted()) return;
                        stats.addQuestBlockWalked(dist);
                        checkQuestCompletion(player, stats);
                        syncToClient(player);
                    });
                }
            }
            lastPositions.put(pid, new double[]{cx, cz});
        }
    }

    private static void checkQuestCompletion(ServerPlayer player, PlayerStats stats) {
        if (stats.isQuestCompleted() || !stats.areQuestObjectivesMet()) return;
        stats.setQuestCompleted(true);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aDaily Quest Complete!\u00a7r Open the Quest screen to claim your reward."
        ));
    }

    public static void claimQuestReward(ServerPlayer player) {
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            if (!stats.isQuestCompleted() || stats.isQuestRewardClaimed()) return;
            stats.setQuestRewardClaimed(true);

            // EXP reward
            int expReward = stats.getQuestExpReward();
            int levelsGained = stats.addExp(expReward);

            // Status Recovery: full HP, MP, hunger, clear debuffs
            player.setHealth(player.getMaxHealth());
            stats.setCurrentMp(stats.getMaxMp());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
            player.removeAllEffects();

            if (levelsGained > 0) {
                applyStatModifiers(player, stats);
            }

            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7aReward Claimed!\u00a7r +" + expReward + " EXP. Status Recovery applied."
            ));
            if (levelsGained > 0) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76Level Up!\u00a7r You are now Level " + stats.getLevel() + ". (+" + (levelsGained * 4) + " stat points)"
                ));
            }

            syncToClient(player);
        });
    }

    // === Stat Modifiers ===

    public static void applyStatModifiers(ServerPlayer player, PlayerStats stats) {
        applyModifier(player, Attributes.ATTACK_DAMAGE, STRENGTH_UUID, "Project Leveling Strength",
                (stats.getStrength() - 1) * 0.25);
        applyModifier(player, Attributes.MAX_HEALTH, VITALITY_UUID, "Project Leveling Vitality",
                (stats.getVitality() - 1) * 0.5);
        applyModifier(player, Attributes.ATTACK_SPEED, AGILITY_UUID, "Project Leveling Agility",
                (stats.getAgility() - 1) * 0.01);

        float maxHealth = player.getMaxHealth();
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    private static void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                       UUID uuid, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) {
            instance.removeModifier(uuid);
        }

        if (amount > 0) {
            instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            ModNetwork.sendToPlayer(new S2CSyncStatsPacket(stats), player);
        });
    }
}
