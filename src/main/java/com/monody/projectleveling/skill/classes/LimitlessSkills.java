package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class LimitlessSkills {

    public static final SkillType[] ALL = {
            SkillType.BLACK_FLASH, SkillType.SIX_EYES_NEWBORN, SkillType.INFINITY,
            SkillType.CURSED_TECHNIQUE_BLUE, SkillType.SIX_EYES_JUNIOR, SkillType.SIX_EYES_SEE_THROUGH,
    };

    private LimitlessSkills() {}

    // ========== SCALING HELPERS ==========

    /** Black Flash MATK multiplier. */
    public static float getBlackFlashMultiplier(int level, int intel) {
        return 1.5f + level * 0.15f + intel * 0.008f;
    }

    /** Blue push/pull MATK multiplier. */
    public static float getBlueMultiplier(int level, int intel) {
        return 0.8f + level * 0.08f + intel * 0.006f;
    }

    /** Combined Six Eyes MP cost multiplier (New Born + Junior). */
    public static float getSixEyesCostMultiplier(PlayerStats stats) {
        SkillData sd = stats.getSkillData();
        int senLv = sd.getLevel(SkillType.SIX_EYES_NEWBORN);
        int sejLv = sd.getLevel(SkillType.SIX_EYES_JUNIOR);
        if (senLv <= 0 && sejLv <= 0) return 1.0f;
        // New Born: 5% per level (max 50%). Junior: adds up to 10% more (max 60% total).
        double reduction = senLv * 5.0 + sejLv * (10.0 / 15.0);
        return (float) Math.max(0.40, 1.0 - reduction / 100.0);
    }

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            case BLACK_FLASH -> {
                float mult = getBlackFlashMultiplier(level, stats.getIntelligence());
                texts.add("MATK x" + String.format("%.2f", mult) + " next melee");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP cost: 5% max MP");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_NEWBORN -> {
                texts.add("MP Regen: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP Cost: -" + (level * 5) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
            }
            case INFINITY -> {
                double drain = SkillType.INFINITY.getToggleDrainPercent(level);
                texts.add("All damage redirected to MP (2 MP/1 dmg)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Deflects projectiles");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP drain: " + String.format("%.1f", drain) + "%/s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case CURSED_TECHNIQUE_BLUE -> {
                float mult = getBlueMultiplier(level, stats.getIntelligence());
                texts.add("Tap: push MATK x" + String.format("%.2f", mult));
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Hold: pull to cursor (up to 3s)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP cost: 10% tap / 10%/s hold");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_JUNIOR -> {
                float totalRed = (float)(stats.getSkillData().getLevel(SkillType.SIX_EYES_NEWBORN) * 5.0
                        + level * (10.0 / 15.0));
                texts.add("Total MP cost reduction: " + String.format("%.0f", totalRed) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Flight during Infinity (5% MP/s)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
            }
            case SIX_EYES_SEE_THROUGH -> {
                texts.add("Crit Rate: +" + String.format("%.1f", level * 1.33) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Crit Damage: +" + String.format("%.1f", level * 3.33) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case BLACK_FLASH -> executeBlackFlash(player, stats, sd, level);
            case INFINITY -> executeInfinity(player, stats, sd, level);
            case CURSED_TECHNIQUE_BLUE -> executeBlueTap(player, stats, sd, level);
            default -> {} // passives don't execute
        }
    }

    // ---------- Black Flash ----------

    private static void executeBlackFlash(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        // 5% max MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.05f * getSixEyesCostMultiplier(stats)));
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        float damage = stats.getMagicAttack(player) * getBlackFlashMultiplier(level, stats.getIntelligence());
        sd.setBlackFlashActive(true);
        sd.setBlackFlashMultiplier(damage);
        sd.setBlackFlashTicks(60); // 3 seconds

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.playerAura(player, 10, 0.6, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.4, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1.8f);
        }

        sd.startCooldown(SkillType.BLACK_FLASH, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7dBlack Flash ready! +" + String.format("%.0f", damage) + " next melee."));
        StatEventHandler.syncToClient(player);
    }

    // ---------- Infinity (Toggle) ----------

    private static void executeInfinity(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        int drain = SkillType.INFINITY.getToggleMpPerSecond(level, stats.getMaxMp());
        if (stats.getCurrentMp() < drain) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP to sustain Infinity!"));
            return;
        }

        sd.setToggleActive(SkillType.INFINITY, true);

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 2.0, 20, ParticleTypes.END_ROD);
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.5, 3.0, 3, 10, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.5f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Infinity activated. All damage redirected to MP."));
        StatEventHandler.syncToClient(player);
    }

    // ---------- Cursed Technique: Blue (Tap = Push) ----------

    private static void executeBlueTap(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        // 10% max MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.10f * getSixEyesCostMultiplier(stats)));
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        double range = 6 + level * 0.3;
        float damage = stats.getMagicAttack(player) * getBlueMultiplier(level, stats.getIntelligence());
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(range);

        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player);
        List<Monster> hitMobs = new ArrayList<>();
        int pushed = 0;

        for (LivingEntity entity : entities) {
            Vec3 toEntity = entity.position().subtract(eye).normalize();
            if (look.dot(toEntity) < 0.5) continue; // ~60 degree cone
            double pushForce = 0.8 + level * 0.06;
            entity.setDeltaMovement(look.scale(pushForce).add(0, 0.2, 0));
            entity.hurtMarked = true;
            if (entity instanceof Monster m) {
                m.hurt(SkillDamageSource.get(player.level()), damage);
                hitMobs.add(m);
            }
            pushed++;
        }

        if (player.level() instanceof ServerLevel sl) {
            Vec3 fwd = eye.add(look.scale(range * 0.5));
            SkillParticles.burst(sl, fwd.x, fwd.y, fwd.z, 15, 1.5, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.line(sl, eye, eye.add(look.scale(range)), 0.5, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 1.5f);
        }

        CombatLog.aoeSkill(player, "Cursed Technique: Blue", damage, hitMobs);
        sd.startCooldown(SkillType.CURSED_TECHNIQUE_BLUE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a79Blue (Push)! " + pushed + " entities hit."));
        StatEventHandler.syncToClient(player);
    }

    // ---------- Cursed Technique: Blue (Hold = Pull Channel) ----------

    /** Called from SkillExecutor.handleHold when hold starts. */
    public static void startBlueChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.CURSED_TECHNIQUE_BLUE);
        if (level <= 0) return;
        if (sd.isOnCooldown(SkillType.CURSED_TECHNIQUE_BLUE)) return;

        // Initial 10% MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.10f * getSixEyesCostMultiplier(stats)));
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        sd.setBlueChanneling(true);
        sd.setBlueChannelTicks(0);
        sd.setBlueDrainTimer(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(BLUE_MAX_TICKS);
        sd.setChannelSkillName("C.T. Blue");

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.0, 10, ParticleTypes.SOUL_FIRE_FLAME);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.4f, 2.0f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a79Blue (Pull) channeling..."));
        StatEventHandler.syncToClient(player);
    }

    public static final int BLUE_MAX_TICKS = 60; // 3 seconds max hold

    /** Called every server tick while Blue is channeling. */
    public static void tickBlueChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        sd.setBlueChannelTicks(sd.getBlueChannelTicks() + 1);
        sd.setChannelTicks(sd.getBlueChannelTicks());

        // Max 3 seconds
        if (sd.getBlueChannelTicks() >= BLUE_MAX_TICKS) {
            endBlueChannel(player, stats, sd);
            return;
        }

        // Drain 10% MP every second (with Six Eyes reduction)
        sd.setBlueDrainTimer(sd.getBlueDrainTimer() + 1);
        if (sd.getBlueDrainTimer() >= 20) {
            sd.setBlueDrainTimer(0);
            int drain = Math.max(1, (int) (stats.getMaxMp() * 0.10f * getSixEyesCostMultiplier(stats)));
            if (stats.getCurrentMp() < drain) {
                endBlueChannel(player, stats, sd);
                return;
            }
            stats.setCurrentMp(stats.getCurrentMp() - drain);
        }

        int level = sd.getLevel(SkillType.CURSED_TECHNIQUE_BLUE);
        float damage = stats.getMagicAttack(player) * getBlueMultiplier(level, stats.getIntelligence()) * 0.1f; // 10% per tick

        // Pull point: 10 blocks in front of player's look direction
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 pullPoint = eye.add(look.scale(10));

        double range = 8 + level * 0.2;
        AABB area = new AABB(
                pullPoint.x - range, pullPoint.y - range, pullPoint.z - range,
                pullPoint.x + range, pullPoint.y + range, pullPoint.z + range);

        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player);

        for (LivingEntity entity : entities) {
            Vec3 pullDir = pullPoint.subtract(entity.position()).normalize();
            double dist = entity.position().distanceTo(pullPoint);
            double force = Math.min(0.5, 1.5 / Math.max(1, dist));
            entity.setDeltaMovement(entity.getDeltaMovement().add(pullDir.scale(force)));
            entity.hurtMarked = true;
            // Damage every 10 ticks (0.5s)
            if (sd.getBlueChannelTicks() % 10 == 0 && entity instanceof Monster m) {
                m.hurt(SkillDamageSource.get(player.level()), damage);
            }
        }

        // Particles at pull point
        if (player.level() instanceof ServerLevel sl && sd.getBlueChannelTicks() % 4 == 0) {
            SkillParticles.sphere(sl, pullPoint.x, pullPoint.y, pullPoint.z, 1.5, 8, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.spiral(sl, pullPoint.x, pullPoint.y, pullPoint.z, 1.0, 2.0, 2, 6, ParticleTypes.ENCHANTED_HIT);
        }
    }

    /** End Blue channel (called on release, timeout, or no MP). */
    public static void endBlueChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        if (!sd.isBlueChanneling()) return; // not channeling, nothing to end
        sd.setBlueChanneling(false);
        sd.setBlueChannelTicks(0);
        sd.setBlueDrainTimer(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(0);
        sd.setChannelSkillName("");

        int level = sd.getLevel(SkillType.CURSED_TECHNIQUE_BLUE);
        sd.startCooldown(SkillType.CURSED_TECHNIQUE_BLUE, level);

        if (player.level() instanceof ServerLevel sl) {
            SkillSounds.playAt(player, SoundEvents.BEACON_DEACTIVATE, 0.4f, 1.5f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77Blue channel ended."));
        StatEventHandler.syncToClient(player);
    }

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        if (skill == SkillType.INFINITY) {
            // Remove flight granted by Six Eyes Junior
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77Infinity deactivated."));
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
        StatEventHandler.syncToClient(player);
    }

    // ========== MIRROR (Shadow Partner support) ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        // Limitless skills are not mirrored by Shadow Partner
    }

    // ========== STAT CONTRIBUTIONS ==========

    public static void registerStats() {
        // SIX_EYES_NEWBORN MP_REGEN auto-registered via StatDef
        // SIX_EYES_SEE_THROUGH CRIT/CDMG auto-registered via StatDef
        // No complex contributions needed
    }
}
