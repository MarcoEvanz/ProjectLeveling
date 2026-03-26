package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.*;
import static com.monody.projectleveling.skill.StatContribRegistry.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class AssassinSkills {

    public static final SkillType[] ALL = {
            SkillType.SHADOW_STRIKE, SkillType.VENOM, SkillType.CRITICAL_EDGE,
            SkillType.STEALTH, SkillType.BLADE_FURY, SkillType.EVASION,
            SkillType.SHADOW_SNEAK, SkillType.SHADOW_PARTNER, SkillType.FATAL_BLOW,
            SkillType.SHADOW_LEGION,
            SkillType.FINAL_BLOW, SkillType.LETHAL_MASTERY,
    };

    private AssassinSkills() {}

    // ── Tooltips ─────────────────────────────────────────────────────────

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case SHADOW_STRIKE -> {
                float mult = (0.40f + level * 0.06f + stats.getLuck() * 0.005f) * 100;
                texts.add("Bonus dmg: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Window: 5s to land a hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Consumes buff on hit");
                lines.add(new int[]{TEXT_DIM});
            }
            case VENOM -> {
                float chance = getBleedChance(level);
                texts.add("Bleed chance: " + String.format("%.0f", chance) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bleed: 30% hit dmg/s +10% per stack (true dmg)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max stacks: 10 | Duration: 10s (refreshes)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case CRITICAL_EDGE -> {
                texts.add("Crit rate: +" + String.format("%.1f", level * 1.5f) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit lifesteal: " + String.format("%.1f", level * 0.5f) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case STEALTH -> {
                double detect = Math.max(5.0 - level * 0.27, 1.0);
                texts.add("Detection range: " + String.format("%.1f", detect) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Grants: Invisibility, clears mob aggro");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Breaks on attack or taking damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLADE_FURY -> {
                double range = 3 + level * 0.1 + stats.getLuck() * 0.03;
                float mult = getBladeFuryMultiplier(level, stats.getLuck()) * 100;
                texts.add("Damage: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Hits all enemies in radius");
                lines.add(new int[]{TEXT_DIM});
            }
            case EVASION -> {
                texts.add("Dodge chance: " + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Dodge guarantees next crit");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T3 ===
            case SHADOW_SNEAK -> {
                float mult = getShadowSneakMultiplier(level, stats.getLuck()) * 100;
                double range = 20 + stats.getLuck() * 0.1;
                texts.add("Burst damage: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Mark range: " + String.format("%.0f", range) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Mark duration: 10s");
                lines.add(new int[]{TEXT_DIM});
                texts.add("1st cast: mark target | 2nd cast: teleport + strike");
                lines.add(new int[]{TEXT_DIM});
            }
            case SHADOW_PARTNER -> {
                float mult = 0.3f + (level - 1) * (0.1f / 19.0f);
                int slLv = stats.getSkillData().getLevel(SkillType.SHADOW_LEGION);
                int mpPenalty = slLv > 0 ? getShadowPartnerMpPenalty(slLv) : 50;
                texts.add("Mirror damage: " + String.format("%.0f", mult * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max MP -" + mpPenalty + "% while active");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Mirrors all skills + melee + ranged");
                lines.add(new int[]{TEXT_DIM});
            }
            case FATAL_BLOW -> {
                texts.add("Below 30% HP: +" + (level * 2) + "% damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Execute chance: " + String.format("%.1f", level * 0.5) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case FINAL_BLOW -> {
                float fbMult = getFinalBlowMultiplier(level, stats.getLuck()) * 100;
                texts.add("Bonus dmg: ATK x " + String.format("%.0f", fbMult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bleed: +100% per stack consumed (max 10)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Window: 5s to land a hit");
                lines.add(new int[]{TEXT_DIM});
            }
            case SHADOW_LEGION -> {
                int mpPenalty = getShadowPartnerMpPenalty(level);
                texts.add("Unlocks 2nd Shadow Partner");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Partners auto-attack nearby mobs (2s, 20% ATK)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("On dodge: partners counter-attack (50% ATK)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Shadow Partner MP penalty: -" + mpPenalty + "% (was -50%)");
                lines.add(new int[]{TEXT_DIM});
            }
            case LETHAL_MASTERY -> {
                float atkPct = getLethalMasteryAtkPct(level);
                float bleedBonus = getLethalMasteryBleedBonus(level);
                texts.add("ATK +" + String.format("%.1f", atkPct) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bleed chance +" + String.format("%.1f", bleedBonus) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            default -> {}
        }
    }

    // ── Execute dispatcher ───────────────────────────────────────────────

    public static void execute(ServerPlayer player, PlayerStats stats, SkillData sd,
                               SkillType skill, int level) {
        switch (skill) {
            case SHADOW_STRIKE -> executeShadowStrike(player, stats, sd, level);
            case STEALTH -> executeStealth(player, stats, sd, level);
            case BLADE_FURY -> executeBladeFury(player, stats, sd, level);
            case SHADOW_SNEAK -> executeShadowSneak(player, stats, sd, level);
            case SHADOW_PARTNER -> executeShadowPartner(player, stats, sd, level);
            case FINAL_BLOW -> executeFinalBlow(player, stats, sd, level);
            default -> {}
        }
    }

    // ── Individual execute methods ───────────────────────────────────────

    private static void executeShadowStrike(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.SHADOW_STRIKE.getMpCost(level));
        sd.setShadowStrikeActive(true);
        sd.setShadowStrikeTicks(100);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.6, ParticleTypes.PORTAL);
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 0.8, 2.0, 2, 8, ParticleTypes.SMOKE);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.SHADOW_STRIKE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eShadow Strike ready. Next attack empowered!"));
    }


    private static void executeStealth(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.STEALTH.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.STEALTH, true);
        applyStealthEffects(player, level);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, ParticleTypes.PORTAL);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.4f, 0.7f);
        }
        double range = 32;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
        sd.startCooldown(SkillType.STEALTH, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eStealth activated."));
    }

    private static void executeBladeFury(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BLADE_FURY.getMpCost(level));
        double range = 3 + level * 0.1 + stats.getLuck() * 0.03;
        float damage = stats.getAttack(player) * getBladeFuryMultiplier(level, stats.getLuck());
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        CombatLog.suppressDamageLog = true;
        CombatLog.pendingAoeSplashDmg = 0;
        CombatLog.pendingAoeSplashCount = 0;
        for (Monster mob : mobs) {
            mob.hurt(player.damageSources().playerAttack(player), damage);
        }
        CombatLog.suppressDamageLog = false;
        CombatLog.flushAoe(player, "Blade Fury");
        if (sd.getLevel(SkillType.VENOM) > 0) {
            for (Monster mob : mobs) {
                StatEventHandler.applyBleedStacks(player, stats, sd, mob, 3);
            }
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.8, player.getZ(), range * 0.6, 12, ParticleTypes.SWEEP_ATTACK);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, range * 0.4, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        }
        sd.startCooldown(SkillType.BLADE_FURY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eBlade Fury! " + mobs.size() + " enemies hit."));
    }

    private static void executeShadowSneak(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (sd.getShadowSneakPhase() == 0) {
            // Phase 0: Mark target via raycast
            stats.setCurrentMp(stats.getCurrentMp() - SkillType.SHADOW_SNEAK.getMpCost(level));
            double range = 20 + stats.getLuck() * 0.1;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = eye.add(look.scale(range));

            // Raycast for entities along the line
            AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
            net.minecraft.world.entity.LivingEntity closest = null;
            double closestDist = range;
            for (net.minecraft.world.entity.LivingEntity entity :
                    player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, searchBox,
                            e -> e != player && e.isAlive())) {
                AABB entityBox = entity.getBoundingBox().inflate(0.3);
                var hitResult = entityBox.clip(eye, end);
                if (hitResult.isPresent()) {
                    double dist = eye.distanceTo(hitResult.get());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = entity;
                    }
                }
            }

            if (closest != null) {
                // Mark target
                sd.setShadowSneakPhase(1);
                sd.setShadowSneakTargetId(closest.getId());
                sd.setShadowSneakMarkTicks(200); // 10 seconds
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.6, ParticleTypes.PORTAL);
                    SkillParticles.line(sl, eye, closest.position().add(0, 1, 0), 0.5, ParticleTypes.SMOKE);
                    SkillParticles.burst(sl, closest.getX(), closest.getY() + 1, closest.getZ(), 10, 0.4, ParticleTypes.PORTAL);
                    SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6f, 1.0f);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a7eTarget marked! Cast again to Shadow Sneak."));
            } else {
                // Miss — half cooldown
                sd.startCooldownRaw(SkillType.SHADOW_SNEAK,
                        SkillType.SHADOW_SNEAK.getCooldownTicks(level) / 2);
                if (player.level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 5, 0.3, ParticleTypes.SMOKE);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77No target found. Half cooldown."));
            }
        } else {
            // Phase 1: Teleport behind target + burst damage
            net.minecraft.world.entity.Entity targetEnt =
                    player.level().getEntity(sd.getShadowSneakTargetId());

            if (!(targetEnt instanceof net.minecraft.world.entity.LivingEntity target)
                    || !target.isAlive()) {
                // Target gone — half cooldown
                sd.setShadowSneakPhase(0);
                sd.setShadowSneakTargetId(-1);
                sd.setShadowSneakMarkTicks(0);
                sd.startCooldownRaw(SkillType.SHADOW_SNEAK,
                        SkillType.SHADOW_SNEAK.getCooldownTicks(level) / 2);
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77Target lost. Half cooldown."));
                return;
            }

            // Calculate position behind target (opposite of target's facing, 1 block back)
            float yaw = target.getYRot();
            double behindX = target.getX() + Math.sin(Math.toRadians(yaw)) * 1.0;
            double behindZ = target.getZ() - Math.cos(Math.toRadians(yaw)) * 1.0;
            double behindY = target.getY();

            // Departure particles
            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, ParticleTypes.PORTAL);
            }

            // Teleport
            player.teleportTo(behindX, behindY, behindZ);
            // Face toward the target
            player.setYRot(yaw + 180);
            player.setXRot(0);

            // Deal burst damage
            float damage = stats.getAttack(player) * getShadowSneakMultiplier(level, stats.getLuck());
            CombatLog.nextSource = "Shadow Sneak";
            target.hurt(SkillDamageSource.get(player.level(), player), damage);

            // Apply 5 bleed stacks
            if (sd.getLevel(SkillType.VENOM) > 0 && target instanceof Monster mob) {
                StatEventHandler.applyBleedStacks(player, stats, sd, mob, 5);
            }

            // Arrival particles + sound
            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, behindX, behindY + 1, behindZ, 20, 0.5, ParticleTypes.PORTAL);
                SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(),
                        10, 0.4, ParticleTypes.CRIT);
                SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(),
                        8, 0.3, ParticleTypes.ENCHANTED_HIT);
                SkillSounds.playAt(sl, behindX, behindY, behindZ,
                        SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.2f);
                SkillSounds.playAt(sl, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            }

            // Reset state, full cooldown
            sd.setShadowSneakPhase(0);
            sd.setShadowSneakTargetId(-1);
            sd.setShadowSneakMarkTicks(0);
            sd.startCooldown(SkillType.SHADOW_SNEAK, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7eShadow Sneak! " + String.format("%.1f", damage) + " damage!"));
        }
    }

    private static void executeFinalBlow(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.FINAL_BLOW.getMpCost(level));
        sd.setFinalBlowActive(true);
        sd.setFinalBlowTicks(100); // 5 seconds
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.6, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.4, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7f, 0.6f);
        }
        sd.startCooldown(SkillType.FINAL_BLOW, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7cFinal Blow ready. Next attack empowered!"));
    }

    private static void executeShadowPartner(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.SHADOW_PARTNER.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SHADOW_PARTNER, true);
        // Clamp MP to new reduced max
        stats.setCurrentMp(Math.min(stats.getCurrentMp(), stats.getMaxMp()));
        int slLv = sd.getLevel(SkillType.SHADOW_LEGION);
        int maxPartners = getMaxShadowPartners(slLv);
        int mpPenalty = getShadowPartnerMpPenalty(slLv);
        if (player.level() instanceof ServerLevel sl) {
            // Despawn any existing shadow partners first
            despawnShadowPartner(player, sl);
            // Spawn Shadow Partners
            for (int i = 0; i < maxPartners; i++) {
                double offset = (i == 0) ? -2.0 : -3.5; // stagger positions
                Vec3 behind = player.position().subtract(player.getLookAngle().scale(-offset));
                if (i == 1) {
                    // Second partner offset to the side
                    Vec3 right = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(1.5);
                    behind = player.position().subtract(player.getLookAngle().scale(2.0)).add(right);
                }
                ShadowPartnerEntity partner = new ShadowPartnerEntity(sl, player);
                partner.moveTo(behind.x, player.getY(), behind.z, player.getYRot(), 0);
                sl.addFreshEntity(partner);
                SkillParticles.burst(sl, behind.x, behind.y + 1, behind.z, 15, 0.5, ParticleTypes.SMOKE);
                SkillParticles.burst(sl, behind.x, behind.y + 1, behind.z, 10, 0.4, ParticleTypes.PORTAL);
            }
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.4f, 1.0f);
        }
        String msg = maxPartners > 1
                ? "\u00a7b[System]\u00a7r \u00a78Shadow Legion activated! " + maxPartners + " partners. Max MP -" + mpPenalty + "%."
                : "\u00a7b[System]\u00a7r \u00a78Shadow Partner activated. Max MP -" + mpPenalty + "%.";
        player.sendSystemMessage(Component.literal(msg));
    }

    // ── Public helper methods ────────────────────────────────────────────

    /** Shadow Strike ATK multiplier: ATK × this = bonus damage on next hit. (+50% buffed) */
    public static float getShadowStrikeMultiplier(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.SHADOW_STRIKE);
        if (level <= 0) return 0;
        return 0.60f + level * 0.09f + stats.getLuck() * 0.008f;
    }

    /** Blade Fury ATK multiplier: ATK × this value. (+50% buffed) */
    public static float getBladeFuryMultiplier(int level, int luk) {
        return 1.05f + level * 0.06f + luk * 0.008f;
    }

    /** Final Blow ATK multiplier: ATK × this value. Bleed stacks add +1.0 each. */
    public static float getFinalBlowMultiplier(int level, int luk) {
        return 4.0f + level * 0.16f + luk * 0.015f;
    }

    /** Lethal Mastery: ATK% bonus. 5% at max lv25. */
    public static float getLethalMasteryAtkPct(int level) {
        return level * (5.0f / 25.0f);
    }

    /** Lethal Mastery: bleed chance bonus. 10% at max lv25. */
    public static float getLethalMasteryBleedBonus(int level) {
        return level * (10.0f / 25.0f);
    }

    /** Shadow Sneak burst ATK multiplier: ATK × this value. */
    public static float getShadowSneakMultiplier(int level, int luk) {
        return 1.2f + level * 0.10f + luk * 0.010f;
    }

    /** Venom per-hit ATK multiplier: ATK × this = element damage per hit. (+50% buffed) */
    /** Bleeding Edge: chance to proc bleed. 20% at lv1, 60% at lv10 */
    public static float getBleedChance(int level) {
        return 20 + (level - 1) * (40.0f / 9.0f);
    }

    /** Bleeding Edge: ATK multiplier for bleed. 30% base + 10% per stack. */
    public static float getBleedMultiplier(int stacks) {
        return 0.20f + stacks * 0.10f;
    }

    public static void applyStealthEffects(ServerPlayer player, int level) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
    }

    public static void breakStealth(ServerPlayer player, SkillData sd) {
        if (!sd.isToggleActive(SkillType.STEALTH)) return;
        sd.setToggleActive(SkillType.STEALTH, false);
        player.removeEffect(MobEffects.INVISIBILITY);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, ParticleTypes.POOF);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.4, ParticleTypes.SMOKE);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.5f, 1.0f);
        }
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Stealth broken!"));
    }

    public static double getStealthDetectionRange(int level) {
        return Math.max(5.0 - level * 0.27, 1.0);
    }

    /** Get Shadow Partner damage multiplier (30% at lv1, 40% at lv20). */
    public static float getShadowPartnerDamageMultiplier(int level) {
        return 0.3f + (level - 1) * (0.1f / 19.0f);
    }

    /** Shadow Partner MP penalty with Shadow Legion: 50% default, scales to 40% at max Shadow Legion. */
    public static int getShadowPartnerMpPenalty(int shadowLegionLevel) {
        if (shadowLegionLevel <= 0) return 50;
        return Math.max(40, 50 - shadowLegionLevel / 2);
    }

    /** Get number of Shadow Partners allowed (1 default, 2 with Shadow Legion). */
    public static int getMaxShadowPartners(int shadowLegionLevel) {
        return shadowLegionLevel > 0 ? 2 : 1;
    }

    // ── Entity utilities ─────────────────────────────────────────────────

    public static ShadowPartnerEntity findShadowPartner(ServerPlayer player, ServerLevel level) {
        List<ShadowPartnerEntity> partners = level.getEntitiesOfClass(ShadowPartnerEntity.class,
                player.getBoundingBox().inflate(32), e -> player.getUUID().equals(e.getOwnerUUID()));
        return partners.isEmpty() ? null : partners.get(0);
    }

    /** Find ALL Shadow Partners owned by the player. */
    public static List<ShadowPartnerEntity> findAllShadowPartners(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(ShadowPartnerEntity.class,
                player.getBoundingBox().inflate(32), e -> player.getUUID().equals(e.getOwnerUUID()));
    }

    /** Despawn the player's Shadow Partner with smoke effect. */
    public static void despawnShadowPartner(ServerPlayer player, ServerLevel level) {
        List<ShadowPartnerEntity> partners = level.getEntitiesOfClass(ShadowPartnerEntity.class,
                player.getBoundingBox().inflate(64), e -> player.getUUID().equals(e.getOwnerUUID()));
        for (ShadowPartnerEntity p : partners) {
            p.despawnWithSmoke();
        }
    }

    // ── Toggle deactivation ──────────────────────────────────────────────

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case STEALTH -> {
                player.removeEffect(MobEffects.INVISIBILITY);
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Stealth deactivated."));
            }
            case SHADOW_PARTNER -> {
                if (player.level() instanceof ServerLevel sl) {
                    despawnShadowPartner(player, sl);
                }
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Shadow Partner deactivated."));
            }
            case SOUL_ARROW ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Soul Arrow deactivated."));
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ── Mirror skill from Shadow Partner ─────────────────────────────────

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        switch (skill) {
            case SHADOW_STRIKE -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.4, ParticleTypes.PORTAL);
                SkillParticles.spiral(sl, pos.x, pos.y, pos.z, 0.8, 2.0, 2, 8, ParticleTypes.SMOKE);
            }
            case VENOM -> { /* now passive, no mirror */ }
            case BLADE_FURY -> {
                float radius = 3 + level * 0.2f;
                float dmg = stats.getAttack(player) * getBladeFuryMultiplier(level, stats.getLuck()) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level(), player), dmg);
                }
                CombatLog.aoeSkill(player, "Shadow Blade Fury", dmg, mobs);
                SkillParticles.ring(sl, pos.x, pos.y + 0.5, pos.z, radius, 20, ParticleTypes.SWEEP_ATTACK);
            }
            case SHADOW_SNEAK -> {
                // Shadow Partner teleports to nearest enemy and deals burst damage
                float range = 8.0f;
                List<Monster> nearby = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(range));
                if (!nearby.isEmpty()) {
                    Monster target = nearby.get(0);
                    double minDist = partner.distanceToSqr(target);
                    for (int i = 1; i < nearby.size(); i++) {
                        double d = partner.distanceToSqr(nearby.get(i));
                        if (d < minDist) { minDist = d; target = nearby.get(i); }
                    }
                    // Teleport behind target
                    float yaw = target.getYRot();
                    double bx = target.getX() + Math.sin(Math.toRadians(yaw)) * 1.0;
                    double bz = target.getZ() - Math.cos(Math.toRadians(yaw)) * 1.0;
                    SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 15, 0.5, ParticleTypes.PORTAL);
                    partner.moveTo(bx, target.getY(), bz, yaw + 180, 0);
                    // Deal damage
                    float dmg = stats.getAttack(player) * getShadowSneakMultiplier(level, stats.getLuck()) * multiplier;
                    target.hurt(SkillDamageSource.get(player.level(), player), dmg);
                    CombatLog.damageSkill(player, "Shadow Sneak", dmg, target);
                    SkillParticles.burst(sl, bx, target.getY() + 1, bz, 15, 0.5, ParticleTypes.PORTAL);
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(),
                            10, 0.4, ParticleTypes.CRIT);
                    SkillSounds.playAt(sl, bx, target.getY(), bz,
                            SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.2f);
                }
            }

            case FINAL_BLOW -> {
                // Shadow Partner deals Final Blow damage to nearest mob
                float range = 8.0f;
                List<Monster> nearby = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(range));
                if (!nearby.isEmpty()) {
                    Monster target = nearby.get(0);
                    double minDist = partner.distanceToSqr(target);
                    for (int i = 1; i < nearby.size(); i++) {
                        double d = partner.distanceToSqr(nearby.get(i));
                        if (d < minDist) { minDist = d; target = nearby.get(i); }
                    }
                    float dmg = stats.getAttack(player) * getFinalBlowMultiplier(level, stats.getLuck()) * multiplier;
                    target.hurt(SkillDamageSource.get(player.level(), player), dmg);
                    CombatLog.damageSkill(player, "Shadow Final Blow", dmg, target);
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(),
                            12, 0.5, ParticleTypes.SOUL_FIRE_FLAME);
                    SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(),
                            8, 0.4, ParticleTypes.CRIT);
                }
            }

            // === All other skills: visual mirror (particles + swing) ===
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    // === Stat contributions ===
    public static void registerStats() {
        // Simple contributions (CE, EV, FB) defined in SkillType enum
        // Lethal Mastery: ATK% passive
        reg(StatLine.ATK_PCT, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.LETHAL_MASTERY);
            if (lv <= 0) return 0;
            double val = getLethalMasteryAtkPct(lv);
            tags.pct(SkillType.LETHAL_MASTERY.getAbbreviation() + "+", val);
            return val;
        });
    }
}
