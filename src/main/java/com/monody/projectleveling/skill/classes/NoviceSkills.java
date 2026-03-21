package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class NoviceSkills {

    public static final SkillType[] ALL = {
            SkillType.DASH, SkillType.ENDURANCE,
    };

    private NoviceSkills() {}

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            case DASH -> {
                int speedAmp = Math.min((level - 1) / 2, 1);
                double force = 0.8 + level * 0.15 + stats.getAgility() * 0.01;
                texts.add("Speed: " + SkillTooltips.toRoman(speedAmp + 1) + " (3s)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Impulse: " + String.format("%.2f", force) + " (AGI scales)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case ENDURANCE -> {
                texts.add("Max HP: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("HP regen: " + String.format("%.1f", level * 0.5) + "% max HP every 3s");
                lines.add(new int[]{TEXT_VALUE});
            }
            // Legacy
            case VITAL_SURGE -> {
                float heal = 2 + level * 0.9f + stats.getVitality() * 0.1f;
                int regenDur = 2 + level / 3;
                texts.add("Heal: " + String.format("%.1f", heal) + " HP (VIT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Regen I: " + regenDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither");
                lines.add(new int[]{TEXT_DIM});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case DASH -> executeDash(player, stats, sd, level);
            case VITAL_SURGE -> executeVitalSurge(player, stats, sd, level);
            default -> {}
        }
    }

    private static void executeDash(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DASH.getMpCost(level));
        int speedAmp = Math.min((level - 1) / 2, 1);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, speedAmp, false, true));
        Vec3 look = player.getLookAngle();
        double force = 0.8 + level * 0.15 + stats.getAgility() * 0.01;
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), 0.8, 8, ParticleTypes.CLOUD);
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.5, player.getZ(), 6, 0.4, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE);
            SkillSounds.playAt(player, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.5f);
        }
        player.push(look.x * force, 0.1, look.z * force);
        player.hurtMarked = true;
        sd.startCooldown(SkillType.DASH, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eDash!"));
    }

    private static void executeVitalSurge(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.VITAL_SURGE.getMpCost(level));
        float healAmount = 2 + level * 0.9f + stats.getVitality() * 0.1f;
        player.heal(healAmount);
        CombatLog.heal(player, "Vital Surge", healAmount);
        int regenDuration = (2 + level / 3) * 20;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, 0, false, true));
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, ParticleTypes.HEART);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.6, ParticleTypes.HAPPY_VILLAGER);
            SkillSounds.playAt(player, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f);
        }
        sd.startCooldown(SkillType.VITAL_SURGE, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7aVital Surge! HP restored."));
    }

    // ========== MIRROR ==========

    public static void mirrorSkill(ServerLevel sl, com.monody.projectleveling.entity.assassin.ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        switch (skill) {
            case DASH -> {
                SkillParticles.ring(sl, pos.x, pos.y + 0.1, pos.z, 0.8, 8, ParticleTypes.CLOUD);
                SkillParticles.burst(sl, pos.x, pos.y + 0.5, pos.z, 6, 0.4, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE);
                double force = 0.8 + level * 0.15;
                Vec3 look = player.getLookAngle();
                partner.push(look.x * force, 0.1, look.z * force);
                partner.hurtMarked = true;
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.5f);
            }
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }
}
