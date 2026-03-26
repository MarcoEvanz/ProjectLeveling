package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.mage.SkillFireballEntity;
import com.monody.projectleveling.skill.*;
import static com.monody.projectleveling.skill.StatContribRegistry.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class MageSkills {

    private static final java.util.Random RAND = new java.util.Random();

    public static final SkillType[] ALL = {
            SkillType.FLAME_ORB, SkillType.MAGIC_GUARD, SkillType.ELEMENTAL_DRAIN,
            SkillType.FROST_BIND, SkillType.POISON_MIST, SkillType.ELEMENT_AMPLIFICATION,
            SkillType.MIST_ERUPTION, SkillType.ARCANE_INFINITY, SkillType.ARCANE_OVERDRIVE,
            SkillType.STAR_FALL, SkillType.ARCANE_POWER,
    };

    private MageSkills() {}

    // ========== MATK MULTIPLIER HELPERS ==========

    /** Flame Orb: MATK × this value. (~35% reduced) */
    public static float getFlameOrbMultiplier(int level, int intel) { return 1.00f + level * 0.06f + intel * 0.010f; }
    /** Frost Bind: MATK × this value. (~35% reduced) */
    public static float getFrostBindMultiplier(int level, int intel) { return 0.65f + level * 0.04f + intel * 0.006f; }
    /** Poison Mist per-tick: MATK × this value. (~35% reduced) */
    public static float getPoisonMistMultiplier(int level, int intel) { return 0.26f + level * 0.02f + intel * 0.003f; }
    /** Mist Eruption (detonate): MATK × this value. (~35% reduced) */
    public static float getMistEruptionDetonateMultiplier(int level, int intel) { return 1.60f + level * 0.08f + intel * 0.013f; }
    /** Mist Eruption (arcane fallback): MATK × this value. (~35% reduced) */
    public static float getMistEruptionArcaneMultiplier(int level, int intel) { return 0.80f + level * 0.04f + intel * 0.006f; }
    /** Star Fall per-meteor: MATK × this value. */
    public static float getStarFallMultiplier(int level, int intel) { return 1.0f + level * 0.08f + intel * 0.012f; }

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case FLAME_ORB -> {
                float mult = getFlameOrbMultiplier(level, stats.getIntelligence()) * 100;
                double radius = 3 + level * 0.1;
                int fireDur = 3 + level / 3;
                texts.add("Damage: MATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Sets fire: " + fireDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile, explodes on impact");
                lines.add(new int[]{TEXT_DIM});
            }
            case MAGIC_GUARD -> {
                float redirect = 0.3f + level * 0.03f;
                texts.add("Dmg to MP: " + String.format("%.0f", redirect * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Remaining damage still hurts HP");
                lines.add(new int[]{TEXT_DIM});
            }
            case ELEMENTAL_DRAIN -> {
                texts.add("Bonus per debuff: +" + (level * 2) + "% dmg");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max bonus: +50%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case FROST_BIND -> {
                double range = 5 + level * 0.15 + stats.getIntelligence() * 0.05;
                float mult = getFrostBindMultiplier(level, stats.getIntelligence()) * 100;
                int freezeDur = 3 + level / 3;
                texts.add("Damage: MATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (INT scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Freeze: " + freezeDur + "s (Slowness IV + Weakness I)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Pulls enemies inward");
                lines.add(new int[]{TEXT_DIM});
            }
            case POISON_MIST -> {
                int mistDur = 8 + level / 3;
                float mult = getPoisonMistMultiplier(level, stats.getIntelligence()) * 100;
                double radius = 4 + level * 0.1;
                texts.add("Duration: " + mistDur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("DPS: MATK x " + String.format("%.0f", mult) + "%/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Radius: " + String.format("%.1f", radius) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Applies: Poison I, stationary zone");
                lines.add(new int[]{TEXT_DIM});
            }
            case ELEMENT_AMPLIFICATION -> {
                texts.add("Skill damage: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP cost: +20% for all skills");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T3 ===
            case MIST_ERUPTION -> {
                float detMult = getMistEruptionDetonateMultiplier(level, stats.getIntelligence()) * 100;
                double detRadius = 5 + level * 0.15;
                float arcMult = getMistEruptionArcaneMultiplier(level, stats.getIntelligence()) * 100;
                texts.add("With Mist: MATK x " + String.format("%.0f", detMult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Radius: " + String.format("%.1f", detRadius) + " blocks + fire");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("No Mist: MATK x " + String.format("%.0f", arcMult) + "%, 4 block range");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Detonates Poison Mist for bonus damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case ARCANE_INFINITY -> {
                int dur = 20 + level;
                texts.add("Duration: " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("All skills cost 0 MP while active");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Every 4s: +5% damage (stacking)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max stacks at " + dur + "s: +" + ((dur / 4) * 5) + "% damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case ARCANE_OVERDRIVE -> {
                texts.add("Crit rate: +" + String.format("%.1f", level * 1.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Armor pen: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T4 ===
            case STAR_FALL -> {
                float mult = getStarFallMultiplier(level, stats.getIntelligence()) * 100;
                texts.add("Per meteor: MATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Zone radius: 10 blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Channel: 10s (rooted)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("~30 meteors over duration");
                lines.add(new int[]{TEXT_DIM});
            }
            case ARCANE_POWER -> {
                texts.add("MATK bonus: +10%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: 2% max MP/sec");
                lines.add(new int[]{TEXT_VALUE});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case FLAME_ORB -> executeFlameOrb(player, stats, sd, level);
            case MAGIC_GUARD -> executeMagicGuard(player, stats, sd, level);
            case FROST_BIND -> executeFrostBind(player, stats, sd, level);
            case POISON_MIST -> executePoisonMist(player, stats, sd, level);
            case MIST_ERUPTION -> executeMistEruption(player, stats, sd, level);
            case ARCANE_INFINITY -> executeInfinity(player, stats, sd, level);
            case ARCANE_POWER -> executeArcanePower(player, stats, sd, level);
            // STAR_FALL uses hold-channel via handleHold(), not execute()
            default -> {}
        }
    }

    private static void executeFlameOrb(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.FLAME_ORB.getMpCost(level));
        float damage = stats.getMagicAttack(player) * getFlameOrbMultiplier(level, stats.getIntelligence());
        float aoeRadius = (float) (3 + level * 0.1);
        Vec3 look = player.getLookAngle();
        SkillFireballEntity fireball = new SkillFireballEntity(
                player.level(), player,
                look.x * 0.5, look.y * 0.5, look.z * 0.5,
                SkillFireballEntity.FireballType.FLAME_ORB, damage, aoeRadius, level);
        fireball.setPos(player.getX() + look.x, player.getEyeY(), player.getZ() + look.z);
        player.level().addFreshEntity(fireball);
        SkillSounds.playAt(player, SoundEvents.BLAZE_SHOOT, 0.8f, 1.0f);
        sd.startCooldown(SkillType.FLAME_ORB, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Flame Orb launched!"));
    }

    private static void executeMagicGuard(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.MAGIC_GUARD.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.MAGIC_GUARD, true);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 1.0, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.3f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a79Magic Guard activated. Damage redirected to MP."));
    }

    /** Get the damage-to-MP redirect ratio for Magic Guard (30-60%). */
    public static float getMagicGuardRedirectRatio(int level) {
        return 0.3f + level * 0.03f; // 33% at lv1, 60% at lv10
    }

    private static void executeFrostBind(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.FROST_BIND.getMpCost(level));
        double range = 5 + level * 0.15 + stats.getIntelligence() * 0.05;
        float damage = stats.getMagicAttack(player) * getFrostBindMultiplier(level, stats.getIntelligence());
        int freezeDuration = (3 + level / 3) * 20;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            // Pull inward
            Vec3 pullDir = player.position().subtract(mob.position()).normalize();
            mob.setDeltaMovement(pullDir.scale(0.4));
            mob.hurtMarked = true;
            // Freeze + damage
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeDuration, 3, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, freezeDuration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 25, range * 0.5, ParticleTypes.SNOWFLAKE);
            for (Monster mob : mobs) {
                SkillParticles.burst(sl, mob.getX(), mob.getY() + 1, mob.getZ(), 5, 0.3, ParticleTypes.ITEM_SNOWBALL);
            }
            SkillSounds.playAt(player, SoundEvents.GLASS_BREAK, 0.7f, 0.5f);
        }
        CombatLog.aoeSkill(player, "Frost Bind", damage, mobs);
        sd.startCooldown(SkillType.FROST_BIND, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7bFrost Bind! " + mobs.size() + " enemies frozen."));
    }

    private static void executePoisonMist(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.POISON_MIST.getMpCost(level));
        int duration = (8 + level / 3) * 20;
        sd.setMistActive(true);
        sd.setMistTicks(duration);
        sd.setMistPos(player.getX(), player.getY(), player.getZ());
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.5, player.getZ(), 20, 1.5, ParticleTypes.ITEM_SLIME);
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 2, 2.0, 15, ParticleTypes.WITCH);
            SkillSounds.playAt(player, SoundEvents.BREWING_STAND_BREW, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.POISON_MIST, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a72Poison Mist deployed!"));
    }

    /** Called every second while mist is active. */
    public static void tickPoisonMist(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.POISON_MIST);
        double radius = 4 + level * 0.1;
        float damage = stats.getMagicAttack(player) * getPoisonMistMultiplier(level, stats.getIntelligence());
        AABB area = new AABB(
                sd.getMistX() - radius, sd.getMistY() - radius, sd.getMistZ() - radius,
                sd.getMistX() + radius, sd.getMistY() + radius, sd.getMistZ() + radius);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
        }
        CombatLog.aoeSkill(player, "Poison Mist", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.column(sl, sd.getMistX(), sd.getMistZ(), sd.getMistY(), sd.getMistY() + 2.0, radius, 15, ParticleTypes.WITCH);
            SkillParticles.disc(sl, sd.getMistX(), sd.getMistY() + 0.3, sd.getMistZ(), radius, 10, ParticleTypes.ITEM_SLIME);
        }
    }

    private static void executeMistEruption(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.MIST_ERUPTION.getMpCost(level));
        if (sd.isMistActive()) {
            // Detonate the mist for massive damage
            double radius = 5 + level * 0.15;
            float damage = stats.getMagicAttack(player) * getMistEruptionDetonateMultiplier(level, stats.getIntelligence());
            AABB area = new AABB(
                    sd.getMistX() - radius, sd.getMistY() - radius, sd.getMistZ() - radius,
                    sd.getMistX() + radius, sd.getMistY() + radius, sd.getMistZ() + radius);
            List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(player.level()), damage);
                mob.setSecondsOnFire(3);
            }
            CombatLog.aoeSkill(player, "Mist Eruption", damage, mobs);
            sd.setMistActive(false);
            sd.setMistTicks(0);
            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.explosion(sl, sd.getMistX(), sd.getMistY() + 1, sd.getMistZ(), radius, ParticleTypes.FLAME, ParticleTypes.LAVA);
                SkillParticles.burst(sl, sd.getMistX(), sd.getMistY() + 1, sd.getMistZ(), 20, radius * 0.5, ParticleTypes.LARGE_SMOKE);
                SkillSounds.playAt(sl, sd.getMistX(), sd.getMistY(), sd.getMistZ(), SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
            }
            sd.startCooldown(SkillType.MIST_ERUPTION, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76Mist Eruption! " + mobs.size() + " enemies blasted!"));
        } else {
            // Weaker arcane blast without mist
            float damage = stats.getMagicAttack(player) * getMistEruptionArcaneMultiplier(level, stats.getIntelligence());
            double range = 4;
            AABB area = player.getBoundingBox().inflate(range);
            List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(player.level()), damage);
            }
            CombatLog.aoeSkill(player, "Arcane Blast", damage, mobs);
            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, range * 0.4, ParticleTypes.ENCHANTED_HIT);
                SkillSounds.playAt(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
            }
            sd.startCooldown(SkillType.MIST_ERUPTION, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7dArcane Blast! " + mobs.size() + " enemies hit."));
        }
    }

    private static void executeInfinity(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ARCANE_INFINITY.getMpCost(level));
        int duration = (20 + level) * 20; // 21-40 seconds
        sd.setInfinityTicks(duration);
        sd.setInfinityStacks(0);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.2, 3.0, 3, 12, ParticleTypes.DRAGON_BREATH);
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.5, 15, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        }
        sd.startCooldown(SkillType.ARCANE_INFINITY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Arcane Infinity activated! All skills cost 0 MP."));
    }

    /** Called every 4 seconds while Infinity is active to ramp damage buff. */
    public static void tickInfinity(ServerPlayer player, SkillData sd) {
        sd.setInfinityStacks(sd.getInfinityStacks() + 1);
        SkillParticles.playerAura(player, 8, 1.0, ParticleTypes.END_ROD);
        SkillParticles.playerFeet(player, 6, 0.8, ParticleTypes.WITCH);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Infinity +" + (sd.getInfinityStacks() * 5) + "% damage"));
    }

    // ========== ARCANE POWER (Toggle) ==========

    private static void executeArcanePower(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.ARCANE_POWER.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.ARCANE_POWER, true);
        SkillParticles.playerAura(player, 12, 1.2, ParticleTypes.ENCHANT);
        SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.5f, 1.2f);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7dArcane Power activated! +10% MATK."));
    }

    // ========== STAR FALL (Hold-to-channel) ==========

    public static final int STAR_FALL_MAX_TICKS = 200; // 10 seconds max

    public static void startStarFallChannel(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.STAR_FALL);
        if (level <= 0) return;
        if (sd.isOnCooldown(SkillType.STAR_FALL)) return;

        int mpCost = SkillType.STAR_FALL.getMpCost(level);
        if (stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - mpCost);

        sd.setStarFallChanneling(true);
        sd.setStarFallTicks(0);
        sd.setStarFallCastPos(player.getX(), player.getY(), player.getZ());
        sd.setStarFallAngle(RAND.nextDouble() * Math.PI * 2); // fixed direction for all meteors
        sd.setStarFallZoneLocked(false);
        sd.setStarFallZoneStableTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(STAR_FALL_MAX_TICKS);
        sd.setChannelSkillName("Star Fall");

        // Initial zone from raycast
        updateStarFallZone(player, sd);

        if (player.level() instanceof ServerLevel sl) {
            double zx = sd.getStarFallZoneX(), zy = sd.getStarFallZoneY(), zz = sd.getStarFallZoneZ();
            SkillParticles.burst(sl, zx, zy + 1, zz, 30, 5.0, ParticleTypes.LAVA);
            SkillParticles.ring(sl, zx, zy + 0.5, zz, 10.0, 24, ParticleTypes.FLAME);
            SkillSounds.playAt(sl, zx, zy, zz, SoundEvents.END_PORTAL_SPAWN, 0.5f, 0.5f);
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Star Fall! Channeling..."));
    }

    public static void endStarFallChannel(ServerPlayer player, SkillData sd) {
        if (!sd.isStarFallChanneling()) return;
        sd.setStarFallChanneling(false);
        sd.setStarFallTicks(0);
        sd.setChannelTicks(0);
        sd.setChannelMaxTicks(0);
        sd.setChannelSkillName("");
        sd.startCooldown(SkillType.STAR_FALL, sd.getLevel(SkillType.STAR_FALL));
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77Star Fall ended."));
    }

    /** Raycast from player eyes to update the zone center position. */
    private static void updateStarFallZone(ServerPlayer player, SkillData sd) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(50));
        net.minecraft.world.phys.BlockHitResult hitResult = player.level().clip(
                new net.minecraft.world.level.ClipContext(eye, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        sd.setStarFallZonePos(hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z);
    }

    /** Called every tick while Star Fall is channeling. */
    public static void tickStarFall(ServerPlayer player, PlayerStats stats, SkillData sd) {
        sd.setStarFallTicks(sd.getStarFallTicks() + 1);
        sd.setChannelTicks(sd.getStarFallTicks());

        // Max duration reached
        if (sd.getStarFallTicks() >= STAR_FALL_MAX_TICKS) {
            endStarFallChannel(player, sd);
            return;
        }

        // Root player at cast position but allow looking around
        player.teleportTo(sd.getStarFallCastX(), sd.getStarFallCastY(), sd.getStarFallCastZ());

        // Update zone center to where player is looking (unless locked)
        if (!sd.isStarFallZoneLocked()) {
            double oldX = sd.getStarFallZoneX();
            double oldZ = sd.getStarFallZoneZ();
            updateStarFallZone(player, sd);
            double dx = sd.getStarFallZoneX() - oldX;
            double dz = sd.getStarFallZoneZ() - oldZ;
            double distSq = dx * dx + dz * dz;
            if (distSq < 1.0) { // less than 1 block movement
                sd.setStarFallZoneStableTicks(sd.getStarFallZoneStableTicks() + 1);
                if (sd.getStarFallZoneStableTicks() >= 40) { // 2 seconds
                    sd.setStarFallZoneLocked(true);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a7b[System]\u00a7r \u00a7eZone locked!"));
                }
            } else {
                sd.setStarFallZoneStableTicks(0);
            }
        }

        if (!(player.level() instanceof ServerLevel sl)) return;

        // Spawn 1-2 meteors every 10 ticks (0.5s)
        if (sd.getStarFallTicks() % 10 == 0) {
            int level = sd.getLevel(SkillType.STAR_FALL);
            float damage = stats.getMagicAttack(player) * getStarFallMultiplier(level, stats.getIntelligence());
            double zoneX = sd.getStarFallZoneX();
            double zoneY = sd.getStarFallZoneY();
            double zoneZ = sd.getStarFallZoneZ();

            int meteorCount = 1 + RAND.nextInt(2);
            double baseAngle = sd.getStarFallAngle(); // all meteors from the same direction

            for (int i = 0; i < meteorCount; i++) {
                // Target: random point within 10-block radius
                double tAngle = RAND.nextDouble() * Math.PI * 2;
                double tDist = RAND.nextDouble() * 10.0;
                double tx = zoneX + Math.cos(tAngle) * tDist;
                double tz = zoneZ + Math.sin(tAngle) * tDist;

                // Spawn: all from same direction with slight spread (+/- 20 degrees)
                double spread = (RAND.nextDouble() - 0.5) * 0.7;
                double spawnDir = baseAngle + spread;
                double lateralOffset = 12 + RAND.nextDouble() * 6; // 12-18 blocks out
                double mx = tx + Math.cos(spawnDir) * lateralOffset;
                double mz = tz + Math.sin(spawnDir) * lateralOffset;
                double my = zoneY + 20 + RAND.nextDouble() * 5;

                // Direct velocity to reach target in ~20 ticks (1 second)
                double flightTicks = 18 + RAND.nextDouble() * 6;
                double vx = (tx - mx) / flightTicks;
                double vy = (zoneY - my) / flightTicks;
                double vz = (tz - mz) / flightTicks;

                // Pass (0,0,0) as power so no acceleration; use setDeltaMovement for constant velocity
                SkillFireballEntity meteor = new SkillFireballEntity(
                        sl, player,
                        0, 0, 0,
                        SkillFireballEntity.FireballType.METEOR, damage, 3.0f, level);
                meteor.setPos(mx, my, mz);
                meteor.setDeltaMovement(vx, vy, vz);
                sl.addFreshEntity(meteor);
            }

            // Zone border ring
            SkillParticles.ring(sl, zoneX, zoneY + 0.5, zoneZ, 10.0, 24, ParticleTypes.FLAME);
            SkillParticles.ring(sl, zoneX, zoneY + 0.2, zoneZ, 10.0, 24, ParticleTypes.SMOKE);
        }
    }

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case MAGIC_GUARD ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Magic Guard deactivated."));
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ========== MIRROR ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        Vec3 look = player.getLookAngle();
        switch (skill) {
            case FLAME_ORB -> {
                float dmg = stats.getMagicAttack(player) * getFlameOrbMultiplier(level, stats.getIntelligence()) * multiplier;
                SkillFireballEntity fb = new SkillFireballEntity(sl, partner,
                        look.x, look.y, look.z,
                        SkillFireballEntity.FireballType.FLAME_ORB, dmg, 3.0f, level);
                sl.addFreshEntity(fb);
            }
            case FROST_BIND -> {
                float radius = 4 + level * 0.2f;
                float dmg = stats.getMagicAttack(player) * getFrostBindMultiplier(level, stats.getIntelligence()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + level * 4, 4, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Frost Bind", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 20, radius * 0.5, ParticleTypes.SNOWFLAKE);
            }
            case POISON_MIST -> {
                float dmg = stats.getMagicAttack(player) * getPoisonMistMultiplier(level, stats.getIntelligence()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(4));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Poison Mist", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 0.5, pos.z, 12, 1.0, ParticleTypes.ITEM_SLIME);
            }
            case MIST_ERUPTION -> {
                float radius = 4 + level * 0.1f;
                float dmg = stats.getMagicAttack(player) * getMistEruptionArcaneMultiplier(level, stats.getIntelligence()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                }
                CombatLog.aoeSkill(player, "Shadow Eruption", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 15, radius * 0.4, ParticleTypes.ENCHANTED_HIT);
            }
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }

    // === Stat contributions ===
    public static void registerStats() {
        reg(StatLine.DMG_RED, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.MAGIC_GUARD);
            if (!sd.isToggleActive(SkillType.MAGIC_GUARD) || lv <= 0) return 0;
            float val = getMagicGuardRedirectRatio(lv) * 100;
            tags.add(SkillType.MAGIC_GUARD.getAbbreviation() + String.format("%.0f", val) + "%>MP");
            return val;
        });
        // Simple contributions (AO, EA) defined in SkillType enum
        reg(StatLine.DMG, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.ELEMENTAL_DRAIN);
            if (lv > 0) tags.add(SkillType.ELEMENTAL_DRAIN.getAbbreviation() + "+" + lv * 2 + "%/deb");
            return 0;
        });
    }
}
