package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.skill.*;
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
            SkillType.RULERS_AUTHORITY, SkillType.SHADOW_PARTNER, SkillType.FATAL_BLOW,
    };

    private AssassinSkills() {}

    // ── Tooltips ─────────────────────────────────────────────────────────

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case SHADOW_STRIKE -> {
                float dmg = 1 + level * 0.8f + stats.getLuck() * 0.15f;
                texts.add("Bonus dmg: +" + String.format("%.1f", dmg) + " (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Window: 5s to land a hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Consumes buff on hit");
                lines.add(new int[]{TEXT_DIM});
            }
            case VENOM -> {
                int venomDur = 10 + level;
                int poisonDur = 3 + level / 3;
                int poisonAmp = Math.min(level / 4, 2);
                float directDmg = 0.5f + level * 0.15f;
                texts.add("Active: " + venomDur + "s (all melee attacks poison)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Poison " + SkillTooltips.toRoman(poisonAmp + 1) + ": " + poisonDur + "s per hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Magic dmg: " + String.format("%.1f", directDmg) + " per hit");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Wither on undead (immune to Poison)");
                lines.add(new int[]{TEXT_DIM});
            }
            case CRITICAL_EDGE -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case STEALTH -> {
                double detect = Math.max(5.0 - level * 0.27, 1.0);
                texts.add("Detection range: " + String.format("%.1f", detect) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Grants: Invisibility, clears mob aggro");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Breaks on attack or taking damage");
                lines.add(new int[]{TEXT_DIM});
            }
            case BLADE_FURY -> {
                double range = 3 + level * 0.1 + stats.getLuck() * 0.03;
                float dmg = 2 + level * 0.6f + stats.getLuck() * 0.12f;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (LUK scales)");
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
            case RULERS_AUTHORITY -> {
                double range = 4 + level * 0.4 + stats.getLuck() * 0.15;
                float dmg = level * 0.4f + stats.getLuck() * 0.08f;
                double pullForce = 0.3 + level * 0.1;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Pull force: " + String.format("%.1f", pullForce));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cone: ~60 degrees in front");
                lines.add(new int[]{TEXT_DIM});
            }
            case SHADOW_PARTNER -> {
                float mult = 0.3f + (level - 1) * (0.1f / 19.0f);
                texts.add("Mirror damage: " + String.format("%.0f", mult * 100) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max MP halved while active");
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
            default -> {}
        }
    }

    // ── Execute dispatcher ───────────────────────────────────────────────

    public static void execute(ServerPlayer player, PlayerStats stats, SkillData sd,
                               SkillType skill, int level) {
        switch (skill) {
            case SHADOW_STRIKE -> executeShadowStrike(player, stats, sd, level);
            case VENOM -> executeVenom(player, stats, sd, level);
            case STEALTH -> executeStealth(player, stats, sd, level);
            case BLADE_FURY -> executeBladeFury(player, stats, sd, level);
            case RULERS_AUTHORITY -> executeRulersAuthority(player, stats, sd, level);
            case SHADOW_PARTNER -> executeShadowPartner(player, stats, sd, level);
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

    private static void executeVenom(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.VENOM.getMpCost(level));
        sd.setVenomActive(true);
        sd.setVenomTicks((10 + level) * 20); // 11-20 seconds
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.8, player.getZ(), 10, 0.4, ParticleTypes.ITEM_SLIME);
            SkillParticles.playerAura(player, 8, 0.6, ParticleTypes.ENTITY_EFFECT);
            SkillSounds.playAt(player, SoundEvents.BREWING_STAND_BREW, 0.5f, 1.0f);
        }
        sd.startCooldown(SkillType.VENOM, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a72Venom applied! Attacks now poison enemies."));
    }

    private static void executeStealth(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.STEALTH.getToggleMpPerSecond(level)) {
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
        float damage = 2 + level * 0.6f + stats.getLuck() * 0.12f + SkillExecutor.getWeaponDamage(player);
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
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.8, player.getZ(), range * 0.6, 12, ParticleTypes.SWEEP_ATTACK);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, range * 0.4, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        }
        sd.startCooldown(SkillType.BLADE_FURY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eBlade Fury! " + mobs.size() + " enemies hit."));
    }

    private static void executeRulersAuthority(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.RULERS_AUTHORITY.getMpCost(level));
        double range = 4 + level * 0.4 + stats.getLuck() * 0.15;
        float damage = level * 0.4f + stats.getLuck() * 0.08f + SkillExecutor.getWeaponDamage(player);
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(range);
        List<net.minecraft.world.entity.LivingEntity> entities =
                player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
                        e -> e != player);
        int pulled = 0;
        List<Monster> hitMobs = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.LivingEntity entity : entities) {
            Vec3 toEntity = entity.position().subtract(eye).normalize();
            if (look.dot(toEntity) < 0.5) continue;
            Vec3 pullDir = player.position().subtract(entity.position()).normalize();
            double pullForce = 0.3 + level * 0.1;
            entity.setDeltaMovement(pullDir.scale(pullForce));
            entity.hurtMarked = true;
            if (entity instanceof Monster m) {
                m.hurt(SkillDamageSource.get(player.level()), damage);
                hitMobs.add(m);
            }
            pulled++;
        }
        if (player.level() instanceof ServerLevel sl) {
            for (net.minecraft.world.entity.LivingEntity entity : entities) {
                Vec3 toEnt = entity.position().subtract(eye).normalize();
                if (look.dot(toEnt) >= 0.5) {
                    SkillParticles.line(sl, player.getEyePosition(), entity.position().add(0, 1, 0), 0.5, ParticleTypes.PORTAL);
                }
            }
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 0.5f);
        }
        CombatLog.aoeSkill(player, "Ruler's Authority", damage, hitMobs);
        sd.startCooldown(SkillType.RULERS_AUTHORITY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7dRuler's Authority! " + pulled + " entities pulled."));
    }

    private static void executeShadowPartner(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.SHADOW_PARTNER.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SHADOW_PARTNER, true);
        // Clamp MP to new halved max
        stats.setCurrentMp(Math.min(stats.getCurrentMp(), stats.getMaxMp()));
        if (player.level() instanceof ServerLevel sl) {
            // Despawn any existing shadow partner first
            despawnShadowPartner(player, sl);
            // Spawn new Shadow Partner entity behind player
            Vec3 behind = player.position().subtract(player.getLookAngle().scale(2.0));
            ShadowPartnerEntity partner = new ShadowPartnerEntity(sl, player);
            partner.moveTo(behind.x, player.getY(), behind.z, player.getYRot(), 0);
            sl.addFreshEntity(partner);
            SkillParticles.burst(sl, behind.x, behind.y + 1, behind.z, 15, 0.5, ParticleTypes.SMOKE);
            SkillParticles.burst(sl, behind.x, behind.y + 1, behind.z, 10, 0.4, ParticleTypes.PORTAL);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.4f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a78Shadow Partner activated. Max MP halved."));
    }

    // ── Public helper methods ────────────────────────────────────────────

    public static float getShadowStrikeDamage(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.SHADOW_STRIKE);
        return 1 + level * 0.8f + stats.getLuck() * 0.15f;
    }

    /** Get venom poison duration in ticks based on level. */
    public static int getVenomPoisonDuration(int level) {
        return (3 + level / 3) * 20; // 3-6 seconds
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

    // ── Entity utilities ─────────────────────────────────────────────────

    public static ShadowPartnerEntity findShadowPartner(ServerPlayer player, ServerLevel level) {
        List<ShadowPartnerEntity> partners = level.getEntitiesOfClass(ShadowPartnerEntity.class,
                player.getBoundingBox().inflate(32), e -> player.getUUID().equals(e.getOwnerUUID()));
        return partners.isEmpty() ? null : partners.get(0);
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
            case VENOM -> {
                SkillParticles.burst(sl, pos.x, pos.y + 0.8, pos.z, 6, 0.3, ParticleTypes.ITEM_SLIME);
            }
            case BLADE_FURY -> {
                float radius = 3 + level * 0.2f;
                float dmg = (3 + level * 0.6f + stats.getLuck() * 0.1f + SkillExecutor.getWeaponDamage(player)) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                }
                CombatLog.aoeSkill(player, "Shadow Blade Fury", dmg, mobs);
                SkillParticles.ring(sl, pos.x, pos.y + 0.5, pos.z, radius, 20, ParticleTypes.SWEEP_ATTACK);
            }
            case RULERS_AUTHORITY -> {
                float radius = 4 + level * 0.3f;
                float dmg = (level * 0.3f + stats.getLuck() * 0.06f + SkillExecutor.getWeaponDamage(player)) * multiplier;
                List<net.minecraft.world.entity.LivingEntity> entities = sl.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        partner.getBoundingBox().inflate(radius), e -> e != partner && e != player);
                List<Monster> hitMobs = new java.util.ArrayList<>();
                for (var entity : entities) {
                    Vec3 pullDir = partner.position().subtract(entity.position()).normalize();
                    entity.setDeltaMovement(pullDir.scale(0.3));
                    entity.hurtMarked = true;
                    if (entity instanceof Monster mob) {
                        mob.hurt(SkillDamageSource.get(player.level()), dmg);
                        hitMobs.add(mob);
                    }
                }
                CombatLog.aoeSkill(player, "Shadow Authority", dmg, hitMobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.5, ParticleTypes.ENCHANTED_HIT);
            }

            // === All other skills: visual mirror (particles + swing) ===
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
}
