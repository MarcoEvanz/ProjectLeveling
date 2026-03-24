package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class LimitlessSkills {

    public static final SkillType[] ALL = {
            SkillType.BLACK_FLASH, SkillType.SIX_EYES_NEWBORN, SkillType.INFINITY,
            SkillType.CURSED_TECHNIQUE_BLUE, SkillType.SIX_EYES_JUNIOR, SkillType.SIX_EYES_SEE_THROUGH,
            SkillType.CURSED_TECHNIQUE_RED, SkillType.SIX_EYES_MASTERED, SkillType.REVERSE_CURSED_TECHNIQUE,
            SkillType.HOLLOW_TECHNIQUE_PURPLE, SkillType.SIX_EYES_AWAKENED,
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

    /** Red beam base MATK multiplier. Charged T3 nuke — comparable to other T3s; charge bonus is the reward. */
    public static float getRedMultiplier(int level, int intel) {
        return 2.0f + level * 0.15f + intel * 0.01f;
    }

    /** Purple beam MATK multiplier. T4 ultimate — strongest skill in the game.
     *  Scales with level, INT (0.15), and current MP (0.2%). */
    public static float getPurpleMultiplier(int level, int intel, int currentMp) {
        return 5.0f + level * 0.30f + intel * 0.15f + currentMp * 0.002f;
    }

    /** Combined Six Eyes MP cost multiplier (New Born + Junior + Mastered + Awakened). */
    public static float getSixEyesCostMultiplier(PlayerStats stats) {
        SkillData sd = stats.getSkillData();
        int senLv = sd.getLevel(SkillType.SIX_EYES_NEWBORN);
        int sejLv = sd.getLevel(SkillType.SIX_EYES_JUNIOR);
        int semLv = sd.getLevel(SkillType.SIX_EYES_MASTERED);
        int seaLv = sd.getLevel(SkillType.SIX_EYES_AWAKENED);
        if (senLv <= 0 && sejLv <= 0 && semLv <= 0 && seaLv <= 0) return 1.0f;
        // New Born: 5% per level (max 50%). Junior: adds up to 10% (max 60%).
        // Mastered: adds up to 10% more (max 70%). Awakened: adds up to 10% more (max 80%).
        double reduction = senLv * 5.0 + sejLv * (10.0 / 15.0) + semLv * (10.0 / 20.0) + seaLv * (10.0 / 25.0);
        double floor = seaLv > 0 ? 0.20 : (semLv > 0 ? 0.30 : 0.40);
        return (float) Math.max(floor, 1.0 - reduction / 100.0);
    }

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            case BLACK_FLASH -> {
                float mult = getBlackFlashMultiplier(level, stats.getIntelligence());
                texts.add("Empower next melee: MATK x" + String.format("%.2f", mult));
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Buff lasts 3s. Single-target.");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP: 15% max | CD: " + String.format("%.1f", skill.getCooldownTicks(level) / 20.0) + "s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_NEWBORN -> {
                texts.add("All skill MP cost: -" + (level * 5) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Stacks with Junior and Mastered.");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case INFINITY -> {
                double drain = SkillType.INFINITY.getToggleDrainPercent(level);
                texts.add("All damage redirected to MP (2 MP per 1 dmg)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Deflects all projectiles.");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP drain: " + String.format("%.1f", drain) + "%%/s (Six Eyes reduces)");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case CURSED_TECHNIQUE_BLUE -> {
                float mult = getBlueMultiplier(level, stats.getIntelligence());
                double tapRange = 6 + level * 0.3;
                double holdRange = 8 + level * 0.2;
                texts.add("Tap: push in 60\u00b0 cone, MATK x" + String.format("%.2f", mult));
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Tap range: " + String.format("%.1f", tapRange) + " blocks");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Hold: pull enemies to cursor (3s max)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Pull range: " + String.format("%.1f", holdRange) + " blocks, dmg every 0.5s");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP: 25% initial + 15%/s hold");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
                texts.add("CD: " + String.format("%.1f", skill.getCooldownTicks(level) / 20.0) + "s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_JUNIOR -> {
                float totalRed = (float)(stats.getSkillData().getLevel(SkillType.SIX_EYES_NEWBORN) * 5.0
                        + level * (10.0 / 15.0)
                        + stats.getSkillData().getLevel(SkillType.SIX_EYES_MASTERED) * (10.0 / 20.0)
                        + stats.getSkillData().getLevel(SkillType.SIX_EYES_AWAKENED) * (10.0 / 25.0));
                texts.add("Total MP cost reduction: " + String.format("%.1f", totalRed) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Grants flight during Infinity.");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Flight drain: 5% MP/s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_SEE_THROUGH -> {
                texts.add("Crit Rate: +" + String.format("%.1f", level * 1.33) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Crit Damage: +" + String.format("%.1f", level * 3.33) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
            }
            case CURSED_TECHNIQUE_RED -> {
                float mult = getRedMultiplier(level, stats.getIntelligence());
                float maxCharge = 1.0f + (RED_MAX_TICKS - RED_MIN_TICKS) * 0.015f;
                float aoe = 3.0f + level * 0.1f;
                texts.add("Beam: MATK x" + String.format("%.2f", mult) + ", range 40 blocks");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Beam width: 1.5 blocks, pierces mobs");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Charge 1.5-3s: up to " + String.format("%.0f", maxCharge * 100) + "% bonus dmg");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Explosion on impact: " + String.format("%.1f", aoe) + " block radius");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP: 40% initial + 10%/0.5s after 1.5s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
                texts.add("CD: " + String.format("%.1f", skill.getCooldownTicks(level) / 20.0) + "s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_MASTERED -> {
                texts.add("MATK: +" + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                float totalRed = (float)(stats.getSkillData().getLevel(SkillType.SIX_EYES_NEWBORN) * 5.0
                        + stats.getSkillData().getLevel(SkillType.SIX_EYES_JUNIOR) * (10.0 / 15.0)
                        + level * (10.0 / 20.0)
                        + stats.getSkillData().getLevel(SkillType.SIX_EYES_AWAKENED) * (10.0 / 25.0));
                texts.add("Total MP cost reduction: " + String.format("%.1f", totalRed) + "% (cap 70%)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
            }
            case REVERSE_CURSED_TECHNIQUE -> {
                texts.add("Instant heal to full HP.");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("MP cost = missing HP% of current MP");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
                texts.add("Six Eyes reduces MP cost.");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
                texts.add("CD: " + String.format("%.1f", skill.getCooldownTicks(level) / 20.0) + "s");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
            }
            case SIX_EYES_AWAKENED -> {
                texts.add("MATK: +" + String.format("%.1f", level * 0.2) + "%");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                float totalRed = (float)(stats.getSkillData().getLevel(SkillType.SIX_EYES_NEWBORN) * 5.0
                        + stats.getSkillData().getLevel(SkillType.SIX_EYES_JUNIOR) * (10.0 / 15.0)
                        + stats.getSkillData().getLevel(SkillType.SIX_EYES_MASTERED) * (10.0 / 20.0)
                        + level * (10.0 / 25.0));
                texts.add("Total MP cost reduction: " + String.format("%.1f", totalRed) + "% (cap 80%)");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
            }
            case HOLLOW_TECHNIQUE_PURPLE -> {
                float mult = getPurpleMultiplier(level, stats.getIntelligence(), stats.getCurrentMp());
                texts.add("Use Red+Blue together to fuse into Purple.");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Hold both keys during 3.5s charge. Release = cancel.");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Beam: MATK x" + String.format("%.2f", mult) + ", 128 blocks, 10 block radius");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Pierces all targets. Explosion on each hit (3 block radius).");
                lines.add(new int[]{TEXT_VALUE, TEXT_DIM});
                texts.add("Requires 50% MP. Consumes ALL MP on fire (+0.2%MP to mult).");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
                texts.add("CD: 60.0s (also triggers Red+Blue CD)");
                lines.add(new int[]{TEXT_DIM, TEXT_DIM});
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
            case CURSED_TECHNIQUE_RED -> {} // Red is hold-only, handled via handleHold
            case REVERSE_CURSED_TECHNIQUE -> executeRCT(player, stats, sd, level);
            default -> {} // passives don't execute
        }
    }

    // ---------- Black Flash ----------

    private static void executeBlackFlash(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        // 15% max MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.15f * getSixEyesCostMultiplier(stats)));
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
        // Purple trigger: if Red is channeling and Purple is learned, fuse into Purple
        int purpleLv = sd.getLevel(SkillType.HOLLOW_TECHNIQUE_PURPLE);
        if (purpleLv > 0 && sd.isRedChanneling() && !sd.isOnCooldown(SkillType.HOLLOW_TECHNIQUE_PURPLE)
                && stats.getCurrentMp() >= stats.getMaxMp() / 2) {
            startPurpleChannel(player, stats, sd);
            return;
        }
        // 25% max MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.25f * getSixEyesCostMultiplier(stats)));
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
            DustParticleOptions blueDust = new DustParticleOptions(new Vector3f(0.0f, 0.3f, 1.0f), 3.0f);
            DustParticleOptions blueDustSmall = new DustParticleOptions(new Vector3f(0.2f, 0.5f, 1.0f), 1.5f);
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(look).normalize();
            double fwdSpeed = 3.0;

            // Push wave: expanding rings moving forward along the look direction
            for (double d = 1.0; d < range; d += 2.0) {
                Vec3 center = eye.add(look.scale(d));
                double ringRadius = 0.5 + (d / range) * 2.5; // expands as it goes
                int ringCount = 12 + (int) (d / range * 8);
                for (int i = 0; i < ringCount; i++) {
                    double angle = i * Math.PI * 2.0 / ringCount;
                    Vec3 offset = right.scale(Math.cos(angle) * ringRadius).add(up.scale(Math.sin(angle) * ringRadius));
                    Vec3 pos = center.add(offset);
                    sl.sendParticles(blueDust, pos.x, pos.y, pos.z, 0,
                            look.x * fwdSpeed, look.y * fwdSpeed, look.z * fwdSpeed, 1.0);
                }
                // Inner fill particles rushing forward
                for (int i = 0; i < 6; i++) {
                    double ox = (sl.random.nextDouble() - 0.5) * ringRadius;
                    double oy = (sl.random.nextDouble() - 0.5) * ringRadius;
                    double oz = (sl.random.nextDouble() - 0.5) * ringRadius;
                    sl.sendParticles(blueDustSmall, center.x + ox, center.y + oy, center.z + oz, 0,
                            look.x * fwdSpeed * 1.5, look.y * fwdSpeed * 1.5, look.z * fwdSpeed * 1.5, 1.0);
                }
            }
            // Soul fire trail along beam core
            for (double d = 0.5; d < range; d += 1.0) {
                Vec3 pos = eye.add(look.scale(d));
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 0,
                        look.x * fwdSpeed * 0.8, look.y * fwdSpeed * 0.8, look.z * fwdSpeed * 0.8, 1.0);
            }
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

        // Purple trigger: if Red is channeling and Purple is learned, fuse into Purple
        int purpleLv = sd.getLevel(SkillType.HOLLOW_TECHNIQUE_PURPLE);
        if (purpleLv > 0 && sd.isRedChanneling() && !sd.isOnCooldown(SkillType.HOLLOW_TECHNIQUE_PURPLE)
                && stats.getCurrentMp() >= stats.getMaxMp() / 2) {
            startPurpleChannel(player, stats, sd);
            return;
        }

        // Initial 25% MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.25f * getSixEyesCostMultiplier(stats)));
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

        // Drain 15% MP every second (with Six Eyes reduction)
        sd.setBlueDrainTimer(sd.getBlueDrainTimer() + 1);
        if (sd.getBlueDrainTimer() >= 20) {
            sd.setBlueDrainTimer(0);
            int drain = Math.max(1, (int) (stats.getMaxMp() * 0.15f * getSixEyesCostMultiplier(stats)));
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

        // Particles at pull point: blue orb constantly pulling particles inward
        if (player.level() instanceof ServerLevel sl) {
            DustParticleOptions blueDust = new DustParticleOptions(new Vector3f(0.0f, 0.3f, 1.0f), 3.0f);
            float chargeRatio = (float) sd.getBlueChannelTicks() / BLUE_MAX_TICKS;
            double orbRadius = 2.0 + chargeRatio * 2.0;
            int particleCount = 6 + (int) (chargeRatio * 10);

            // Inward-pulling particles: spawn at outer radius with velocity toward center
            for (int i = 0; i < particleCount; i++) {
                double theta = sl.random.nextDouble() * Math.PI * 2;
                double phi = sl.random.nextDouble() * Math.PI;
                double r = orbRadius;
                double px = pullPoint.x + r * Math.sin(phi) * Math.cos(theta);
                double py = pullPoint.y + r * Math.cos(phi);
                double pz = pullPoint.z + r * Math.sin(phi) * Math.sin(theta);
                // Velocity points inward toward center
                double speed = 0.15 + chargeRatio * 0.1;
                double vx = (pullPoint.x - px) * speed;
                double vy = (pullPoint.y - py) * speed;
                double vz = (pullPoint.z - pz) * speed;
                sl.sendParticles(blueDust, px, py, pz, 0, vx, vy, vz, 1.0);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0, vx, vy, vz, 1.0);
            }
            // Core orb glow
            sl.sendParticles(blueDust, pullPoint.x, pullPoint.y, pullPoint.z,
                    3 + (int) (chargeRatio * 5), 0.3, 0.3, 0.3, 0);
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

    // ---------- Cursed Technique Reversal: Red (Hold = Charge Beam) ----------

    public static final int RED_MIN_TICKS = 30;  // 1.5s minimum charge
    public static final int RED_MAX_TICKS = 60;  // 3.0s maximum charge
    private static final double RED_BEAM_RANGE = 40.0;

    /** Called from SkillExecutor.handleHold when hold starts. */
    public static void startRedChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.CURSED_TECHNIQUE_RED);
        if (level <= 0) return;
        if (sd.isOnCooldown(SkillType.CURSED_TECHNIQUE_RED)) return;

        // Purple trigger: if Blue is channeling and Purple is learned, fuse into Purple
        int purpleLv = sd.getLevel(SkillType.HOLLOW_TECHNIQUE_PURPLE);
        if (purpleLv > 0 && sd.isBlueChanneling() && !sd.isOnCooldown(SkillType.HOLLOW_TECHNIQUE_PURPLE)
                && stats.getCurrentMp() >= stats.getMaxMp() / 2) {
            startPurpleChannel(player, stats, sd);
            return;
        }

        // Initial 40% MP cost (reduced by Six Eyes)
        int mpCost = Math.max(1, (int) (stats.getMaxMp() * 0.40f * getSixEyesCostMultiplier(stats)));
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        sd.setRedChanneling(true);
        sd.setRedChannelTicks(0);
        sd.setRedDrainTimer(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(RED_MAX_TICKS);
        sd.setChannelSkillName("C.T. Red");

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 6, 0.3, ParticleTypes.FLAME);
            SkillSounds.playAt(player, SoundEvents.BLAZE_AMBIENT, 0.5f, 0.8f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7cRed charging..."));
        StatEventHandler.syncToClient(player);
    }

    /** Called every server tick while Red is channeling. */
    public static void tickRedChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        sd.setRedChannelTicks(sd.getRedChannelTicks() + 1);
        sd.setChannelTicks(sd.getRedChannelTicks());

        // Auto-fire at max charge
        if (sd.getRedChannelTicks() >= RED_MAX_TICKS) {
            fireRedBeam(player, stats, sd);
            return;
        }

        // Every 0.5s (10 ticks) after 1.5s (30 ticks): drain +10% MP
        if (sd.getRedChannelTicks() > RED_MIN_TICKS) {
            sd.setRedDrainTimer(sd.getRedDrainTimer() + 1);
            if (sd.getRedDrainTimer() >= 10) {
                sd.setRedDrainTimer(0);
                int drain = Math.max(1, (int) (stats.getMaxMp() * 0.10f * getSixEyesCostMultiplier(stats)));
                if (stats.getCurrentMp() < drain) {
                    // Not enough MP — force fire if past minimum, else cancel
                    if (sd.getRedChannelTicks() >= RED_MIN_TICKS) {
                        fireRedBeam(player, stats, sd);
                    } else {
                        cancelRedChannel(player, stats, sd);
                    }
                    return;
                }
                stats.setCurrentMp(stats.getCurrentMp() - drain);
            }
        }

        // Charging particles: mass compressing to a single tight point
        if (player.level() instanceof ServerLevel sl && sd.getRedChannelTicks() % 2 == 0) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 orbPos = eye.add(look.scale(1.5));
            float chargeRatio = (float) sd.getRedChannelTicks() / RED_MAX_TICKS;

            // Dense stream of small flames converging to the point
            int count = 5 + (int) (chargeRatio * 8);
            double radius = 1.0 - chargeRatio * 0.5;
            for (int i = 0; i < count; i++) {
                double theta = sl.random.nextDouble() * Math.PI * 2;
                double phi = sl.random.nextDouble() * Math.PI;
                double r = radius * (0.4 + sl.random.nextDouble() * 0.6);
                double px = orbPos.x + r * Math.sin(phi) * Math.cos(theta);
                double py = orbPos.y + r * Math.cos(phi);
                double pz = orbPos.z + r * Math.sin(phi) * Math.sin(theta);
                sl.sendParticles(ParticleTypes.SMALL_FLAME, px, py, pz, 0,
                        (orbPos.x - px) * 0.35, (orbPos.y - py) * 0.35, (orbPos.z - pz) * 0.35, 1.0);
            }
            // Tiny dense core dot
            DustParticleOptions redDot = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.0f), 0.5f + chargeRatio * 0.5f);
            sl.sendParticles(redDot, orbPos.x, orbPos.y, orbPos.z, 2, 0.03, 0.03, 0.03, 0);
        }
    }

    /** Called when player releases the key. */
    public static void endRedChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        if (!sd.isRedChanneling()) return;

        if (sd.getRedChannelTicks() >= RED_MIN_TICKS) {
            // Charged enough — fire beam
            fireRedBeam(player, stats, sd);
        } else {
            // Released too early — cancel, MP already spent
            cancelRedChannel(player, stats, sd);
        }
    }

    /** Fire the Red beam: raycast forward, damage entities along path, explode on impact. */
    private static void fireRedBeam(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.CURSED_TECHNIQUE_RED);
        int chargeTicks = sd.getRedChannelTicks();

        // Calculate damage with charge bonus: +3.33% per tick beyond minimum (up to +100% at full charge)
        float chargeBonus = 1.0f + Math.max(0, chargeTicks - RED_MIN_TICKS) * 0.0333f;
        float damage = stats.getMagicAttack(player) * getRedMultiplier(level, stats.getIntelligence()) * chargeBonus;
        float aoeRadius = 3.0f + level * 0.1f;
        float beamWidth = 1.5f; // radius around beam line that hits entities

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RED_BEAM_RANGE));

        // Block raycast
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 blockHitPos = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        double blockDist = eye.distanceTo(blockHitPos);

        // Entity raycast: find the closest entity along the FULL beam range
        AABB searchArea = player.getBoundingBox().expandTowards(look.scale(RED_BEAM_RANGE)).inflate(beamWidth);
        List<LivingEntity> allEntities = player.level().getEntitiesOfClass(
                LivingEntity.class, searchArea, e -> e != player && e.isAlive());

        // Find first entity hit (closest to eye along the beam)
        LivingEntity firstHit = null;
        double firstHitDist = blockDist; // don't look past the block hit
        for (LivingEntity entity : allEntities) {
            AABB bb = entity.getBoundingBox().inflate(0.3); // slight inflation for easier targeting
            java.util.Optional<Vec3> intersect = bb.clip(eye, end);
            if (intersect.isPresent()) {
                double d = eye.distanceTo(intersect.get());
                if (d < firstHitDist) {
                    firstHitDist = d;
                    firstHit = entity;
                }
            }
        }

        // Explosion center: first entity hit or block hit (whichever is closer)
        Vec3 hitPos = firstHit != null
                ? firstHit.position().add(0, firstHit.getBbHeight() * 0.5, 0)
                : blockHitPos;

        // Beam end extends to the explosion point
        Vec3 beamEnd = hitPos;
        Vec3 beamDir = beamEnd.subtract(eye);
        double beamLenSq = beamDir.lengthSqr();

        List<Monster> hitMobs = new ArrayList<>();
        java.util.Set<Integer> alreadyHit = new java.util.HashSet<>();
        DamageSource dmgSrc = SkillDamageSource.get(player.level());

        // 1) Direct hit on first entity in beam path: full damage
        if (firstHit != null) {
            firstHit.hurt(dmgSrc, damage);
            Vec3 knockDir = look.add(0, 0.15, 0);
            firstHit.setDeltaMovement(firstHit.getDeltaMovement().add(knockDir.scale(1.2)));
            firstHit.hurtMarked = true;
            alreadyHit.add(firstHit.getId());
            if (firstHit instanceof Monster m) hitMobs.add(m);
        }

        // 2) Beam path: damage entities within beamWidth of the beam line
        if (beamLenSq > 0.01) {
            for (LivingEntity entity : allEntities) {
                if (alreadyHit.contains(entity.getId())) continue;
                Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                Vec3 toEntity = entityCenter.subtract(eye);
                double t = toEntity.dot(beamDir) / beamLenSq;
                if (t < 0 || t > 1) continue;
                Vec3 closest = eye.add(beamDir.scale(t));
                double distToBeam = entityCenter.distanceTo(closest);
                if (distToBeam <= beamWidth) {
                    entity.hurt(dmgSrc, damage);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(look.scale(1.0)));
                    entity.hurtMarked = true;
                    alreadyHit.add(entity.getId());
                    if (entity instanceof Monster m) hitMobs.add(m);
                }
            }
        }

        // 3) AOE explosion at impact: entities within aoeRadius take falloff damage
        for (LivingEntity entity : allEntities) {
            if (alreadyHit.contains(entity.getId())) continue;
            Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            double dist = entityCenter.distanceTo(hitPos);
            if (dist > aoeRadius) continue;
            float falloff = 1.0f - (float) (dist / aoeRadius) * 0.5f;
            entity.hurt(dmgSrc, damage * falloff);
            Vec3 knockDir = entityCenter.subtract(hitPos).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockDir.scale(0.8)));
            entity.hurtMarked = true;
            if (entity instanceof Monster m) hitMobs.add(m);
        }

        // Visuals: beam line + explosion at impact + red impact ring
        if (player.level() instanceof ServerLevel sl) {
            DustParticleOptions redDust = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.0f), 4.0f);
            SkillParticles.line(sl, eye.add(look.scale(1.5)), hitPos, 0.4, ParticleTypes.FLAME);
            SkillParticles.line(sl, eye.add(look.scale(1.5)), hitPos, 0.8, ParticleTypes.SMALL_FLAME);
            SkillParticles.explosion(sl, hitPos.x, hitPos.y, hitPos.z, aoeRadius, ParticleTypes.FLAME, ParticleTypes.LAVA);
            // Red impact ring at explosion point
            SkillParticles.ring(sl, hitPos.x, hitPos.y, hitPos.z, aoeRadius, 24, redDust);
            sl.sendParticles(redDust, hitPos.x, hitPos.y, hitPos.z, 15, aoeRadius * 0.4, aoeRadius * 0.4, aoeRadius * 0.4, 0);
            SkillSounds.playAt(player, SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 0.6f);
            SkillSounds.playAt(sl, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE, 1.0f, 0.9f);
        }

        CombatLog.aoeSkill(player, "C.T. Reversal: Red", damage, hitMobs);
        clearRedState(sd);
        sd.startCooldown(SkillType.CURSED_TECHNIQUE_RED, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7c\u00a7lRed fired! " + String.format("%.0f", damage)
                        + " dmg, " + hitMobs.size() + " hit ("
                        + String.format("%.0f", chargeBonus * 100) + "% charge)."));
        StatEventHandler.syncToClient(player);
    }

    /** Cancel Red without firing (released before minimum charge). */
    private static void cancelRedChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        clearRedState(sd);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77Red cancelled (charge too short)."));
        StatEventHandler.syncToClient(player);
    }

    private static void clearRedState(SkillData sd) {
        sd.setRedChanneling(false);
        sd.setRedChannelTicks(0);
        sd.setRedDrainTimer(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(0);
        sd.setChannelSkillName("");
    }

    // ---------- Reverse Cursed Technique ----------

    private static void executeRCT(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        float maxHp = player.getMaxHealth();
        float currentHp = player.getHealth();
        if (currentHp >= maxHp) {
            player.sendSystemMessage(Component.literal("\u00a7cAlready at full HP!"));
            return;
        }

        // Missing HP % determines both heal and MP cost
        float missingPercent = (maxHp - currentHp) / maxHp;
        int mpCost = Math.max(1, (int) (stats.getCurrentMp() * missingPercent * getSixEyesCostMultiplier(stats)));
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        // Heal to full
        float healAmount = maxHp - currentHp;
        player.heal(healAmount);

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.playerAura(player, 15, 0.8, ParticleTypes.HEART);
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.0, 2.5, 3, 8, ParticleTypes.COMPOSTER);
            SkillSounds.playAt(player, SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);
        }

        sd.startCooldown(SkillType.REVERSE_CURSED_TECHNIQUE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aReverse Cursed Technique! Healed " + String.format("%.0f", healAmount)
                        + " HP (" + mpCost + " MP spent)."));
        StatEventHandler.syncToClient(player);
    }

    // ---------- Hollow Technique: Purple (Passive T4 Combo) ----------

    private static final int PURPLE_CONVERGE_TICKS = 40;  // 2.0s — red & blue converge
    private static final int PURPLE_CHARGE_TICKS = 70;    // +1.5s fuse phase, total 3.5s
    private static final int PURPLE_COOLDOWN_TICKS = 1200; // 60s fixed cooldown
    private static final double PURPLE_BEAM_RANGE = 128.0;
    private static final double PURPLE_BEAM_RADIUS = 10.0;
    private static final double PURPLE_EXPLOSION_RADIUS = 3.0;

    /** Cancel active Red/Blue channels and begin Purple charge. */
    public static void startPurpleChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        // Cancel whatever Red/Blue state is active (MP already spent on them)
        sd.setRedChanneling(false);
        sd.setRedChannelTicks(0);
        sd.setRedDrainTimer(0);
        sd.setBlueChanneling(false);
        sd.setBlueChannelTicks(0);
        sd.setBlueDrainTimer(0);

        // Init Purple channel
        sd.setPurpleChanneling(true);
        sd.setPurpleChannelTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(PURPLE_CHARGE_TICKS);
        sd.setChannelSkillName("H.T. Purple");

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 12, 0.5, ParticleTypes.WITCH);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.7f, 0.5f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75\u00a7lHollow Technique: Purple charging..."));
        StatEventHandler.syncToClient(player);
    }

    /** Called every server tick while Purple is channeling. */
    public static void tickPurpleChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int ticks = sd.getPurpleChannelTicks() + 1;
        sd.setPurpleChannelTicks(ticks);
        sd.setChannelTicks(ticks);

        // Auto-fire at full charge
        if (ticks >= PURPLE_CHARGE_TICKS) {
            firePurpleBeam(player, stats, sd);
            return;
        }

        if (!(player.level() instanceof ServerLevel sl)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Merge point: 2 blocks in front of eyes
        Vec3 mergePoint = eye.add(look.scale(2.0));
        // Perpendicular offset (right vector)
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

        if (ticks <= PURPLE_CONVERGE_TICKS) {
            // Phase 1: Red and Blue masses rushing inward to their points
            float progress = (float) ticks / PURPLE_CONVERGE_TICKS; // 0.0 → 1.0
            double offset = 1.5 * (1.0 - progress);

            Vec3 redPos = mergePoint.add(right.scale(offset));
            Vec3 bluePos = mergePoint.add(right.scale(-offset));

            // Many particles spawning from a wide area, all sucked to the point
            int count = 6 + (int) (progress * 10);
            double spawnRadius = 2.5 - progress * 1.5; // wide start, tightens

            // Red side
            for (int i = 0; i < count; i++) {
                double theta = sl.random.nextDouble() * Math.PI * 2;
                double phi = sl.random.nextDouble() * Math.PI;
                double r = spawnRadius * (0.3 + sl.random.nextDouble() * 0.7);
                double px = redPos.x + r * Math.sin(phi) * Math.cos(theta);
                double py = redPos.y + r * Math.cos(phi);
                double pz = redPos.z + r * Math.sin(phi) * Math.sin(theta);
                sl.sendParticles(ParticleTypes.SMALL_FLAME, px, py, pz, 0,
                        (redPos.x - px) * 0.4, (redPos.y - py) * 0.4, (redPos.z - pz) * 0.4, 1.0);
            }
            // Red core
            DustParticleOptions redDot = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.0f), 0.5f + progress * 0.5f);
            sl.sendParticles(redDot, redPos.x, redPos.y, redPos.z, 1, 0.02, 0.02, 0.02, 0);

            // Blue side
            for (int i = 0; i < count; i++) {
                double theta = sl.random.nextDouble() * Math.PI * 2;
                double phi = sl.random.nextDouble() * Math.PI;
                double r = spawnRadius * (0.3 + sl.random.nextDouble() * 0.7);
                double px = bluePos.x + r * Math.sin(phi) * Math.cos(theta);
                double py = bluePos.y + r * Math.cos(phi);
                double pz = bluePos.z + r * Math.sin(phi) * Math.sin(theta);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0,
                        (bluePos.x - px) * 0.4, (bluePos.y - py) * 0.4, (bluePos.z - pz) * 0.4, 1.0);
            }
            // Blue core
            DustParticleOptions blueDot = new DustParticleOptions(new Vector3f(0.0f, 0.3f, 1.0f), 0.5f + progress * 0.5f);
            sl.sendParticles(blueDot, bluePos.x, bluePos.y, bluePos.z, 1, 0.02, 0.02, 0.02, 0);

            if (ticks % 10 == 0) {
                float pitch = 0.5f + progress * 1.0f;
                SkillSounds.playAt(player, SoundEvents.BEACON_AMBIENT, 0.3f + progress * 0.3f, pitch);
            }
        } else if (ticks <= PURPLE_CHARGE_TICKS - 10) {
            // Phase 2a: Fusing — many particles sucked into one dense purple point
            float fuseProgress = (float) (ticks - PURPLE_CONVERGE_TICKS) / (PURPLE_CHARGE_TICKS - PURPLE_CONVERGE_TICKS - 10);

            DustParticleOptions purpleDust = new DustParticleOptions(new Vector3f(0.5f, 0.0f, 1.0f), 0.8f);
            int count = 8 + (int) (fuseProgress * 14);
            double spawnRadius = 1.5 - fuseProgress * 0.8;
            for (int i = 0; i < count; i++) {
                double theta = sl.random.nextDouble() * Math.PI * 2;
                double phi = sl.random.nextDouble() * Math.PI;
                double r = spawnRadius * (0.3 + sl.random.nextDouble() * 0.7);
                double px = mergePoint.x + r * Math.sin(phi) * Math.cos(theta);
                double py = mergePoint.y + r * Math.cos(phi);
                double pz = mergePoint.z + r * Math.sin(phi) * Math.sin(theta);
                double vx = (mergePoint.x - px) * 0.4;
                double vy = (mergePoint.y - py) * 0.4;
                double vz = (mergePoint.z - pz) * 0.4;
                sl.sendParticles(ParticleTypes.WITCH, px, py, pz, 0, vx, vy, vz, 1.0);
                sl.sendParticles(purpleDust, px, py, pz, 0, vx, vy, vz, 1.0);
            }
            DustParticleOptions purpleDot = new DustParticleOptions(new Vector3f(0.5f, 0.0f, 1.0f), 0.6f + fuseProgress * 0.6f);
            sl.sendParticles(purpleDot, mergePoint.x, mergePoint.y, mergePoint.z, 2, 0.03, 0.03, 0.03, 0);

            if (ticks % 5 == 0) {
                SkillSounds.playAt(player, SoundEvents.WARDEN_SONIC_CHARGE, 0.5f + fuseProgress * 0.3f, 0.8f);
            }
        } else {
            // Phase 2b: Fuse complete — stable orb sitting still at center
            float readyProgress = (float) (ticks - (PURPLE_CHARGE_TICKS - 10)) / 10.0f;
            DustParticleOptions purpleOrb = new DustParticleOptions(new Vector3f(0.5f, 0.0f, 1.0f), 1.0f + readyProgress * 0.5f);
            sl.sendParticles(purpleOrb, mergePoint.x, mergePoint.y, mergePoint.z, 8, 0.08, 0.08, 0.08, 0);
            sl.sendParticles(ParticleTypes.WITCH, mergePoint.x, mergePoint.y, mergePoint.z, 4, 0.1, 0.1, 0.1, 0.01);

            if (ticks % 4 == 0) {
                SkillSounds.playAt(player, SoundEvents.BEACON_AMBIENT, 0.6f, 1.5f + readyProgress * 0.5f);
            }
        }
    }

    /** Fire the Purple beam: piercing cylinder, explosion on each hit. */
    private static void firePurpleBeam(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.HOLLOW_TECHNIQUE_PURPLE);
        // Calculate multiplier BEFORE consuming MP (MP bonus uses current MP)
        float damage = stats.getMagicAttack(player) * getPurpleMultiplier(level, stats.getIntelligence(), stats.getCurrentMp());
        // Consume ALL remaining MP (no cost reduction applies)
        stats.setCurrentMp(0);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Purple pierces through ALL blocks — no block raycast
        Vec3 beamEnd = eye.add(look.scale(PURPLE_BEAM_RANGE));
        Vec3 beamDir = beamEnd.subtract(eye);
        double beamLenSq = beamDir.lengthSqr();

        // Collect ALL entities in beam cylinder — AABB from eye to beamEnd inflated by radius
        AABB searchArea = new AABB(eye, beamEnd).inflate(PURPLE_BEAM_RADIUS);
        List<LivingEntity> allEntities = player.level().getEntitiesOfClass(
                LivingEntity.class, searchArea, e -> e != player && e.isAlive());

        DamageSource dmgSrc = SkillDamageSource.get(player.level());
        List<Monster> hitMobs = new ArrayList<>();
        java.util.Set<Integer> beamHitIds = new java.util.HashSet<>();
        List<Vec3> explosionPoints = new ArrayList<>();

        // 1) Beam path: damage ALL entities within PURPLE_BEAM_RADIUS of beam line
        if (beamLenSq > 0.01) {
            for (LivingEntity entity : allEntities) {
                Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                Vec3 toEntity = entityCenter.subtract(eye);
                double t = toEntity.dot(beamDir) / beamLenSq;
                if (t < 0 || t > 1) continue;
                Vec3 closest = eye.add(beamDir.scale(t));
                double distToBeam = entityCenter.distanceTo(closest);
                if (distToBeam <= PURPLE_BEAM_RADIUS) {
                    entity.hurt(dmgSrc, damage);
                    Vec3 knockDir = look.add(0, 0.2, 0);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockDir.scale(1.5)));
                    entity.hurtMarked = true;
                    beamHitIds.add(entity.getId());
                    explosionPoints.add(entityCenter);
                    if (entity instanceof Monster m) hitMobs.add(m);
                }
            }
        }

        // 2) Explosion at each beam-hit entity: AoE falloff damage to nearby entities
        for (Vec3 explosionCenter : explosionPoints) {
            for (LivingEntity entity : allEntities) {
                if (beamHitIds.contains(entity.getId())) continue; // already took full beam damage
                Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                double dist = entityCenter.distanceTo(explosionCenter);
                if (dist > PURPLE_EXPLOSION_RADIUS) continue;
                float falloff = 1.0f - (float) (dist / PURPLE_EXPLOSION_RADIUS) * 0.5f; // 50-100%
                entity.hurt(dmgSrc, damage * falloff);
                Vec3 knockDir = entityCenter.subtract(explosionCenter).normalize();
                entity.setDeltaMovement(entity.getDeltaMovement().add(knockDir.scale(1.0)));
                entity.hurtMarked = true;
                if (entity instanceof Monster m && !hitMobs.contains(m)) hitMobs.add(m);
            }
        }

        // Visuals: massive beam cylinder with sonic boom effect
        if (player.level() instanceof ServerLevel sl) {
            Vec3 beamStart = eye.add(look.scale(1.5));
            Vec3 beamVec = beamEnd.subtract(beamStart);
            double beamLen = beamVec.length();
            Vec3 beamNorm = beamVec.normalize();
            // Perpendicular vectors for cylinder spread
            Vec3 right = beamNorm.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = beamNorm.cross(right).normalize();

            // Large purple dust particle (max size 4.0, purple RGB)
            DustParticleOptions purpleDust = new DustParticleOptions(new Vector3f(0.5f, 0.0f, 1.0f), 4.0f);

            // Core beam lines (force=true so visible at 64 blocks)
            for (double d = 0; d < beamLen; d += 0.3) {
                Vec3 p = beamStart.add(beamNorm.scale(d));
                sl.sendParticles(player, ParticleTypes.WITCH, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            for (double d = 0; d < beamLen; d += 0.4) {
                Vec3 p = beamStart.add(beamNorm.scale(d));
                sl.sendParticles(player, ParticleTypes.DRAGON_BREATH, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            // Massive cylinder: rings every 3 blocks, particles move forward (force=true)
            double fwd = 4.0;
            for (double d = 0; d < beamLen; d += 3.0) {
                Vec3 center = beamStart.add(beamNorm.scale(d));
                // Purple shockwave moving forward
                for (int i = 0; i < 15; i++) {
                    double ox = (sl.random.nextDouble() - 0.5) * PURPLE_BEAM_RADIUS * 2;
                    double oy = (sl.random.nextDouble() - 0.5) * PURPLE_BEAM_RADIUS * 2;
                    double oz = (sl.random.nextDouble() - 0.5) * PURPLE_BEAM_RADIUS * 2;
                    sl.sendParticles(player, purpleDust, true, center.x + ox, center.y + oy, center.z + oz, 0,
                            beamNorm.x * fwd, beamNorm.y * fwd, beamNorm.z * fwd, 1.0);
                }
                // Outer ring moving forward
                for (int ring = 0; ring < 20; ring++) {
                    double angle = ring * Math.PI * 2.0 / 20;
                    double r = PURPLE_BEAM_RADIUS * 0.85;
                    Vec3 offset = right.scale(Math.cos(angle) * r).add(up.scale(Math.sin(angle) * r));
                    Vec3 pos = center.add(offset);
                    sl.sendParticles(player, purpleDust, true, pos.x, pos.y, pos.z, 0,
                            beamNorm.x * fwd, beamNorm.y * fwd, beamNorm.z * fwd, 1.0);
                    sl.sendParticles(player, ParticleTypes.WITCH, true, pos.x, pos.y, pos.z, 0,
                            beamNorm.x * fwd, beamNorm.y * fwd, beamNorm.z * fwd, 1.0);
                }
                // Mid ring moving forward
                for (int ring = 0; ring < 12; ring++) {
                    double angle = ring * Math.PI * 2.0 / 12;
                    double r = PURPLE_BEAM_RADIUS * 0.45;
                    Vec3 offset = right.scale(Math.cos(angle) * r).add(up.scale(Math.sin(angle) * r));
                    Vec3 pos = center.add(offset);
                    sl.sendParticles(player, purpleDust, true, pos.x, pos.y, pos.z, 0,
                            beamNorm.x * fwd, beamNorm.y * fwd, beamNorm.z * fwd, 1.0);
                }
                // Inner fill moving forward
                for (int i = 0; i < 12; i++) {
                    double ox = (sl.random.nextDouble() - 0.5) * PURPLE_BEAM_RADIUS;
                    double oy = (sl.random.nextDouble() - 0.5) * PURPLE_BEAM_RADIUS;
                    double oz = (sl.random.nextDouble() - 0.5) * PURPLE_BEAM_RADIUS;
                    sl.sendParticles(player, ParticleTypes.WITCH, true, center.x + ox, center.y + oy, center.z + oz, 0,
                            beamNorm.x * fwd, beamNorm.y * fwd, beamNorm.z * fwd, 1.0);
                }
            }
            // Explosion particles at each hit entity (force=true for distant hits)
            for (Vec3 pt : explosionPoints) {
                sl.sendParticles(player, purpleDust, true, pt.x, pt.y, pt.z, 20, PURPLE_EXPLOSION_RADIUS, PURPLE_EXPLOSION_RADIUS, PURPLE_EXPLOSION_RADIUS, 0);
                sl.sendParticles(player, ParticleTypes.WITCH, true, pt.x, pt.y, pt.z, 15, PURPLE_EXPLOSION_RADIUS * 0.5, PURPLE_EXPLOSION_RADIUS * 0.5, PURPLE_EXPLOSION_RADIUS * 0.5, 0.05);
                sl.sendParticles(player, ParticleTypes.DRAGON_BREATH, true, pt.x, pt.y, pt.z, 10, PURPLE_EXPLOSION_RADIUS * 0.3, PURPLE_EXPLOSION_RADIUS * 0.3, PURPLE_EXPLOSION_RADIUS * 0.3, 0.05);
            }
            // Final massive explosion at beam end
            sl.sendParticles(player, purpleDust, true, beamEnd.x, beamEnd.y, beamEnd.z, 30, PURPLE_BEAM_RADIUS, PURPLE_BEAM_RADIUS, PURPLE_BEAM_RADIUS, 0);
            sl.sendParticles(player, ParticleTypes.WITCH, true, beamEnd.x, beamEnd.y, beamEnd.z, 20, PURPLE_BEAM_RADIUS * 0.5, PURPLE_BEAM_RADIUS * 0.5, PURPLE_BEAM_RADIUS * 0.5, 0.05);
            sl.sendParticles(player, ParticleTypes.EXPLOSION, true, beamEnd.x, beamEnd.y, beamEnd.z, 1, 0, 0, 0, 0);
            // Sound: layered sonic boom at player, mid-beam, and beam end
            SkillSounds.playAt(player, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.3f);
            Vec3 midBeam = beamStart.add(beamNorm.scale(beamLen * 0.5));
            SkillSounds.playAt(sl, midBeam.x, midBeam.y, midBeam.z, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.4f);
            SkillSounds.playAt(sl, beamEnd.x, beamEnd.y, beamEnd.z, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.5f);
            SkillSounds.playAt(sl, beamEnd.x, beamEnd.y, beamEnd.z, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f);
        }

        CombatLog.aoeSkill(player, "Hollow Technique: Purple", damage, hitMobs);

        // Clear Purple state
        clearPurpleState(sd);

        // Start cooldowns for Purple (fixed 30s), Red, AND Blue
        sd.setCooldown(SkillType.HOLLOW_TECHNIQUE_PURPLE, PURPLE_COOLDOWN_TICKS);
        int redLv = sd.getLevel(SkillType.CURSED_TECHNIQUE_RED);
        if (redLv > 0) sd.startCooldown(SkillType.CURSED_TECHNIQUE_RED, redLv);
        int blueLv = sd.getLevel(SkillType.CURSED_TECHNIQUE_BLUE);
        if (blueLv > 0) sd.startCooldown(SkillType.CURSED_TECHNIQUE_BLUE, blueLv);

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75\u00a7lPurple fired! " + String.format("%.0f", damage)
                        + " dmg, " + hitMobs.size() + " hit."));
        StatEventHandler.syncToClient(player);
    }

    /** End Purple channel early (e.g., player dies or disconnects). */
    public static void endPurpleChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        if (!sd.isPurpleChanneling()) return;
        clearPurpleState(sd);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77Purple cancelled."));
        StatEventHandler.syncToClient(player);
    }

    private static void clearPurpleState(SkillData sd) {
        sd.setPurpleChanneling(false);
        sd.setPurpleChannelTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(0);
        sd.setChannelSkillName("");
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
        // SIX_EYES_SEE_THROUGH CRIT/CDMG auto-registered via StatDef
        // No complex contributions needed
    }
}
