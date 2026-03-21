package com.monody.projectleveling.skill;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.entity.ShadowPartnerEntity;
import com.monody.projectleveling.entity.SkeletonMinionEntity;
import com.monody.projectleveling.entity.SkillArrowEntity;
import com.monody.projectleveling.entity.SkillFireballEntity;
import com.monody.projectleveling.event.StatEventHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SkillExecutor {

    public static void activateSlot(ServerPlayer player, PlayerStats stats, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SkillData.MAX_SLOTS) return;
        SkillData sd = stats.getSkillData();
        SkillType skill = sd.getEquipped(slotIndex);
        if (skill == null || skill.isPassive()) return;

        // Toggle skills: press again to deactivate
        if (skill.isToggle() && sd.isToggleActive(skill)) {
            deactivateToggle(player, sd, skill);
            StatEventHandler.syncToClient(player);
            return;
        }

        if (sd.isOnCooldown(skill)) {
            int secs = (sd.getCooldownRemaining(skill) + 19) / 20;
            player.sendSystemMessage(Component.literal(
                    "\u00a7c" + skill.getDisplayName() + " is on cooldown (" + secs + "s)"));
            return;
        }

        int level = sd.getLevel(skill);
        if (level <= 0) return;

        int mpCost = skill.getMpCost(level);
        // Infinity: all skills cost 0 MP
        if (sd.getInfinityTicks() > 0 && skill != SkillType.INFINITY) {
            mpCost = 0;
        }
        // Element Amplification: +20% MP cost
        int eaLv = sd.getLevel(SkillType.ELEMENT_AMPLIFICATION);
        if (eaLv > 0 && mpCost > 0) {
            mpCost = (int) (mpCost * 1.2);
        }
        if (!skill.isToggle() && stats.getCurrentMp() < mpCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }

        switch (skill) {
            // T0
            case DASH -> executeDash(player, stats, sd, level);
            // Warrior
            case BLOODLUST -> executeBloodlust(player, stats, sd, level);
            case WAR_CRY -> executeWarCry(player, stats, sd, level);
            case IRON_WILL -> executeIronWill(player, stats, sd, level);
            case GROUND_SLAM -> executeGroundSlam(player, stats, sd, level);
            case DOMAIN_OF_MONARCH -> executeDomainOfMonarch(player, stats, sd, level);
            case UNBREAKABLE -> executeUnbreakable(player, stats, sd, level);
            // Assassin
            case SHADOW_STRIKE -> executeShadowStrike(player, stats, sd, level);
            case VENOM -> executeVenom(player, stats, sd, level);
            case STEALTH -> executeStealth(player, stats, sd, level);
            case BLADE_FURY -> executeBladeFury(player, stats, sd, level);
            case RULERS_AUTHORITY -> executeRulersAuthority(player, stats, sd, level);
            case SHADOW_PARTNER -> executeShadowPartner(player, stats, sd, level);
            // Archer
            case ARROW_RAIN -> executeArrowRain(player, stats, sd, level);
            case SOUL_ARROW -> executeSoulArrow(player, stats, sd, level);
            case ARROW_BOMB -> executeArrowBomb(player, stats, sd, level);
            case COVERING_FIRE -> executeCoveringFire(player, stats, sd, level);
            case PHOENIX -> executePhoenix(player, stats, sd, level);
            case HURRICANE -> executeHurricane(player, stats, sd, level);
            // Healer
            case HOLY_LIGHT -> executeHolyLight(player, stats, sd, level);
            case BLESS -> executeBless(player, stats, sd, level);
            case HOLY_SHELL -> executeHolyShell(player, stats, sd, level);
            case DISPEL -> executeDispel(player, stats, sd, level);
            case BENEDICTION -> executeBenediction(player, stats, sd, level);
            case ANGEL_RAY -> executeAngelRay(player, stats, sd, level);
            // Mage
            case FLAME_ORB -> executeFlameOrb(player, stats, sd, level);
            case MAGIC_GUARD -> executeMagicGuard(player, stats, sd, level);
            case FROST_BIND -> executeFrostBind(player, stats, sd, level);
            case POISON_MIST -> executePoisonMist(player, stats, sd, level);
            case MIST_ERUPTION -> executeMistEruption(player, stats, sd, level);
            case INFINITY -> executeInfinity(player, stats, sd, level);
            // Necromancer
            case LIFE_DRAIN -> executeLifeDrain(player, stats, sd, level);
            case RAISE_SKELETON -> executeRaiseSkeleton(player, stats, sd, level);
            case BONE_SHIELD -> executeBoneShield(player, stats, sd, level);
            case CORPSE_EXPLOSION -> executeCorpseExplosion(player, stats, sd, level);
            case ARMY_OF_THE_DEAD -> executeArmyOfTheDead(player, stats, sd, level);
            case DEATH_MARK -> executeDeathMark(player, stats, sd, level);
            // Legacy
            case VITAL_SURGE -> executeVitalSurge(player, stats, sd, level);
            default -> { /* passives handled elsewhere */ }
        }

        // Shadow Partner mirrors the skill
        if (sd.isToggleActive(SkillType.SHADOW_PARTNER) && skill != SkillType.SHADOW_PARTNER) {
            mirrorSkillFromPartner(player, stats, skill);
        }

        StatEventHandler.syncToClient(player);
    }

    // ================================================================
    // Tier 0 (Novice)
    // ================================================================

    private static void executeDash(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DASH.getMpCost(level));
        int speedAmp = Math.min((level - 1) / 2, 1);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, speedAmp, false, true));
        Vec3 look = player.getLookAngle();
        double force = 0.8 + level * 0.15 + stats.getAgility() * 0.01;
        // Particles at start position
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), 0.8, 8, ParticleTypes.CLOUD);
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.5, player.getZ(), 6, 0.4, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE);
            SkillSounds.playAt(player, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.5f);
        }
        player.push(look.x * force, 0.1, look.z * force);
        player.hurtMarked = true;
        sd.startCooldown(SkillType.DASH, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eDash!"));
    }

    // ================================================================
    // Warrior (STR)
    // ================================================================

    private static void executeBloodlust(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BLOODLUST.getMpCost(level));
        double range = 4 + level * 0.8 + stats.getStrength() * 0.3;
        int duration = (int) ((3 + level * 0.5) * 20);
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), range * 0.5, 20, ParticleTypes.DAMAGE_INDICATOR);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), range * 0.4, 12, ParticleTypes.SOUL_FIRE_FLAME);
            SkillSounds.playAt(player, SoundEvents.RAVAGER_ROAR, 0.7f, 0.8f);
        }
        sd.startCooldown(SkillType.BLOODLUST, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eBloodlust! " + mobs.size() + " enemies affected."));
    }

    private static void executeWarCry(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.WAR_CRY.getMpCost(level));
        int duration = (10 + level) * 20; // 11-20 seconds
        int strAmp = Math.min((level - 1) / 3, 2); // Str I at 1-3, II at 4-6, III at 7+
        // Buff self
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strAmp, false, true));
        // Buff nearby players
        double range = 6;
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strAmp, false, true));
        }
        // Weaken nearby mobs
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1.5, player.getZ(), 15, 0.5, ParticleTypes.CRIT);
            for (ServerPlayer p : nearby) {
                SkillParticles.burst(sl, p.getX(), p.getY() + 1.5, p.getZ(), 5, 0.3, ParticleTypes.ENCHANTED_HIT);
            }
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.2f);
        }
        sd.startCooldown(SkillType.WAR_CRY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eWar Cry! " + (nearby.size() + 1) + " allies buffed."));
    }

    private static void executeIronWill(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.IRON_WILL.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.IRON_WILL, true);
        applyIronWillEffects(player, stats, level);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.0, 15, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ANVIL_LAND, 0.4f, 1.5f);
        }
        sd.startCooldown(SkillType.IRON_WILL, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7eIron Will activated."));
    }

    public static void applyIronWillEffects(ServerPlayer player, PlayerStats stats, int level) {
        int amp = Math.min((level - 1) / 5, 2);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, amp, false, true));
    }

    private static void executeGroundSlam(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.GROUND_SLAM.getMpCost(level));
        double range = 3 + level * 0.15 + stats.getStrength() * 0.05;
        float damage = 2 + level * 0.5f + stats.getStrength() * 0.1f;
        int stunDuration = (1 + level / 5) * 20; // 1-3 seconds
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunDuration, 3, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.disc(sl, player.getX(), player.getY() + 0.1, player.getZ(), range, 40, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), range, 16, ParticleTypes.EXPLOSION);
            SkillSounds.playAt(player, SoundEvents.GENERIC_EXPLODE, 0.8f, 0.7f);
        }
        CombatLog.aoeSkill(player, "Ground Slam", damage, mobs);
        sd.startCooldown(SkillType.GROUND_SLAM, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eGround Slam! " + mobs.size() + " enemies stunned."));
    }

    private static void executeDomainOfMonarch(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DOMAIN_OF_MONARCH.getMpCost(level));
        int durationTicks = (4 + level / 2) * 20;
        sd.setDomainTicks(durationTicks);
        sd.setDomainPos(player.getX(), player.getY(), player.getZ());
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 0, false, true));
        if (player.level() instanceof ServerLevel sl) {
            double domainRadius = 2 + level * 0.3 + stats.getStrength() * 0.1;
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), domainRadius, 20, ParticleTypes.WITCH);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), domainRadius, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.DOMAIN_OF_MONARCH, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Domain of the Monarch activated!"));
    }

    public static void tickDomain(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.DOMAIN_OF_MONARCH);
        double radius = 2 + level * 0.3 + stats.getStrength() * 0.1;
        float damage = 0.5f + level * 0.15f + stats.getStrength() * 0.05f;
        AABB area = new AABB(
                sd.getDomainX() - radius, sd.getDomainY() - radius, sd.getDomainZ() - radius,
                sd.getDomainX() + radius, sd.getDomainY() + radius, sd.getDomainZ() + radius);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        }
        CombatLog.aoeSkill(player, "Domain", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, sd.getDomainX(), sd.getDomainY() + 0.1, sd.getDomainZ(), radius, 16, ParticleTypes.WITCH);
        }
    }

    private static void executeUnbreakable(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.UNBREAKABLE.getMpCost(level));
        int duration = (3 + level / 4) * 20; // 3-8 seconds invulnerability
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 4, false, true)); // Res V = invulnerable
        // Burst heal 20-40% max HP
        float healPct = 0.2f + level * 0.01f;
        float healAmt = player.getMaxHealth() * healPct;
        player.heal(healAmt);
        CombatLog.heal(player, "Unbreakable", healAmt);
        // Clear debuffs
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 1.0, ParticleTypes.TOTEM_OF_UNDYING);
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 15, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.TOTEM_USE, 0.8f, 1.0f);
        }
        sd.startCooldown(SkillType.UNBREAKABLE, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a76Unbreakable!"));
    }

    // ================================================================
    // Assassin (LUK)
    // ================================================================

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

    public static float getShadowStrikeDamage(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.SHADOW_STRIKE);
        return 1 + level * 0.8f + stats.getLuck() * 0.15f;
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

    /** Get venom poison duration in ticks based on level. */
    public static int getVenomPoisonDuration(int level) {
        return (3 + level / 3) * 20; // 3-6 seconds
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

    private static void executeBladeFury(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BLADE_FURY.getMpCost(level));
        double range = 3 + level * 0.1 + stats.getLuck() * 0.03;
        float damage = 2 + level * 0.6f + stats.getLuck() * 0.12f;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(player.damageSources().playerAttack(player), damage);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.8, player.getZ(), range * 0.6, 12, ParticleTypes.SWEEP_ATTACK);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, range * 0.4, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        }
        CombatLog.aoeSkill(player, "Blade Fury", damage, mobs);
        sd.startCooldown(SkillType.BLADE_FURY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eBlade Fury! " + mobs.size() + " enemies hit."));
    }

    private static void executeRulersAuthority(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.RULERS_AUTHORITY.getMpCost(level));
        double range = 4 + level * 0.4 + stats.getLuck() * 0.15;
        float damage = level * 0.4f + stats.getLuck() * 0.08f;
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

    /** Get Shadow Partner damage multiplier (30% at lv1, 40% at lv20). */
    public static float getShadowPartnerDamageMultiplier(int level) {
        return 0.3f + (level - 1) * (0.1f / 19.0f);
    }

    // ================================================================
    // Archer (DEX)
    // ================================================================

    private static void executeArrowRain(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ARROW_RAIN.getMpCost(level));
        float damage = 0.3f + level * 0.1f + stats.getDexterity() * 0.02f; // Low per-arrow damage
        float radius = 3.0f;
        int durationTicks = (int) (20 + (level - 1) * (40.0 / 9.0)); // 1s at lv1, 3s at lv10

        // Fire scout arrow where player is looking — rain triggers on impact
        SkillArrowEntity scout = new SkillArrowEntity(
                player.level(), player, SkillArrowEntity.ArrowType.RAIN_SCOUT, damage, radius, durationTicks);
        scout.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
        player.level().addFreshEntity(scout);

        SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.8f);
        sd.startCooldown(SkillType.ARROW_RAIN, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eArrow Rain fired!"));
    }

    /** Called every tick while arrow rain is active. Spawns many arrows at impact zone. */
    public static void tickArrowRain(ServerPlayer player, SkillData sd) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        // Follow target entity if one was hit by the scout arrow
        int targetId = sd.getArrowRainTargetId();
        if (targetId >= 0) {
            net.minecraft.world.entity.Entity target = sl.getEntity(targetId);
            if (target != null && target.isAlive()) {
                sd.setArrowRainPos(target.getX(), target.getY(), target.getZ());
            }
        }

        double x = sd.getArrowRainX();
        double y = sd.getArrowRainY();
        double z = sd.getArrowRainZ();
        float damage = sd.getArrowRainDamage();
        float radius = sd.getArrowRainRadius();

        // Spawn 2-3 arrows every tick for a dense rain effect
        int arrowsPerTick = 2 + (sd.getArrowRainTicks() % 2 == 0 ? 1 : 0);
        for (int i = 0; i < arrowsPerTick; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * radius * 2;
            double oz = (player.getRandom().nextDouble() - 0.5) * radius * 2;
            SkillArrowEntity arrow = new SkillArrowEntity(
                    player.level(), player, SkillArrowEntity.ArrowType.RAIN, damage, 0, 0);
            arrow.setPos(x + ox, y + 10, z + oz);
            arrow.shoot(0, -1, 0, 2.0f, 1.5f);
            sl.addFreshEntity(arrow);
        }
    }

    private static void executeSoulArrow(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.SOUL_ARROW.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SOUL_ARROW, true);
        SkillParticles.playerAura(player, 10, 0.8, ParticleTypes.END_ROD);
        SkillSounds.playAt(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eSoul Arrow activated. Infinite arrows!"));
    }

    private static void executeArrowBomb(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ARROW_BOMB.getMpCost(level));
        float aoeRadius = (float) (3 + level * 0.1);
        float damage = 2 + level * 0.6f + stats.getDexterity() * 0.1f;
        int stunTicks = (2 + level / 5) * 20;
        SkillArrowEntity arrow = new SkillArrowEntity(
                player.level(), player, SkillArrowEntity.ArrowType.BOMB, damage, aoeRadius, stunTicks);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
        player.level().addFreshEntity(arrow);
        SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.9f);
        sd.startCooldown(SkillType.ARROW_BOMB, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eArrow Bomb launched!"));
    }

    private static void executeCoveringFire(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.COVERING_FIRE.getMpCost(level));
        Vec3 look = player.getLookAngle();
        // Leap backward
        double force = 1.0 + level * 0.05;
        player.push(-look.x * force, 0.3, -look.z * force);
        player.hurtMarked = true;
        // Spawn arrows forward
        float damage = 1 + level * 0.4f + stats.getDexterity() * 0.08f;
        int arrowCount = 3 + level / 4;
        for (int i = 0; i < arrowCount; i++) {
            SkillArrowEntity arrow = new SkillArrowEntity(
                    player.level(), player, SkillArrowEntity.ArrowType.COVERING_FIRE, damage, 0, 0);
            float spreadAngle = (i - arrowCount / 2.0f) * 8.0f;
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + spreadAngle, 0.0f, 2.0f, 3.0f);
            player.level().addFreshEntity(arrow);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 0.5, player.getZ(), 8, 0.5, ParticleTypes.CLOUD);
            SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.7f, 1.0f);
            SkillSounds.playAt(player, SoundEvents.BAT_TAKEOFF, 0.5f, 1.0f);
        }
        sd.startCooldown(SkillType.COVERING_FIRE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eCovering Fire! " + arrowCount + " arrows fired."));
    }

    private static void executePhoenix(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.PHOENIX.getMpCost(level));
        int duration = (30 + level * 1.5f > 60 ? 60 : (int) (30 + level * 1.5f)) * 20;
        sd.setPhoenixTicks(duration);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        if (player.level() instanceof ServerLevel sl) {
            // Summon phoenix shape rising above player
            SkillParticles.phoenix(sl, player.getX(), player.getY() + 3, player.getZ(),
                    player.getYRot(), ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 4, 0.5, 15, ParticleTypes.FLAME);
            SkillSounds.playAt(player, SoundEvents.BLAZE_SHOOT, 0.8f, 0.5f);
        }
        sd.startCooldown(SkillType.PHOENIX, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Phoenix summoned!"));
    }

    /** Called every second while phoenix is active. */
    public static void tickPhoenix(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.PHOENIX);
        double range = 6 + level * 0.2 + stats.getDexterity() * 0.05;
        float damage = 1 + level * 0.3f + stats.getDexterity() * 0.06f;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        if (!mobs.isEmpty()) {
            Monster target = mobs.get(0);
            target.hurt(SkillDamageSource.get(player.level()), damage);
            if (player.getRandom().nextFloat() < 0.8f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
            }
            target.setSecondsOnFire(2);
            CombatLog.damageSkill(player, "Phoenix", damage, target);
            if (player.level() instanceof ServerLevel sl) {
                // Phoenix flies from above player toward target
                Vec3 from = player.position().add(0, 2.5, 0);
                Vec3 to = target.position().add(0, 1, 0);
                Vec3 mid = from.add(to).scale(0.5).add(0, 1, 0);
                double yaw = Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
                SkillParticles.phoenix(sl, mid.x, mid.y, mid.z, yaw,
                        ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
                SkillParticles.burst(sl, target.getX(), target.getY() + 1, target.getZ(), 8, 0.4, ParticleTypes.FLAME);
            }
        } else {
            // No target: phoenix circles above player
            if (player.level() instanceof ServerLevel sl) {
                double angle = (player.tickCount % 40) * (360.0 / 40.0);
                double orbX = player.getX() + Math.cos(Math.toRadians(angle)) * 2;
                double orbZ = player.getZ() + Math.sin(Math.toRadians(angle)) * 2;
                SkillParticles.phoenix(sl, orbX, player.getY() + 2.5, orbZ, angle + 90,
                        ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
            }
        }
    }

    private static void executeHurricane(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.HURRICANE.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.HURRICANE, true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.5, 2.5, 3, 10, ParticleTypes.CRIT);
            SkillSounds.playAt(player, SoundEvents.CROSSBOW_SHOOT, 0.3f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eHurricane activated!"));
    }

    /** Called every second while Hurricane is active. Shoots actual arrows at targets. */
    public static void tickHurricane(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.HURRICANE);
        double range = 8 + stats.getDexterity() * 0.1;
        float damage = 1.5f + level * 0.4f + stats.getDexterity() * 0.1f;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        if (!mobs.isEmpty()) {
            Monster target = mobs.get(0);
            // Shoot actual arrow projectile at target
            SkillArrowEntity arrow = new SkillArrowEntity(
                    player.level(), player, SkillArrowEntity.ArrowType.HURRICANE, damage, 0, 0);
            Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(player.getEyePosition());
            arrow.shoot(dir.x, dir.y, dir.z, 2.5f, 1.0f);
            player.level().addFreshEntity(arrow);
            if (player.level() instanceof ServerLevel sl) {
                SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, 0.4f, 1.2f);
            }
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.5, 2.5, 2, 8, ParticleTypes.CRIT);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false));
    }

    // ================================================================
    // Healer (INT)
    // ================================================================

    private static void executeHolyLight(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.HOLY_LIGHT.getMpCost(level));
        float healAmount = 2 + level * 0.8f + stats.getIntelligence() * 0.15f;
        double range = 6;
        // Heal self
        player.heal(healAmount);
        // Heal nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.heal(healAmount);
        }
        // Damage undead mobs
        float undeadDamage = 1 + level * 0.5f + stats.getIntelligence() * 0.1f;
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area,
                m -> m.getMobType() == net.minecraft.world.entity.MobType.UNDEAD);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), undeadDamage);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.6, ParticleTypes.HEART);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.5, player.getZ(), range * 0.5, 12, ParticleTypes.END_ROD);
            for (ServerPlayer p : nearby) {
                SkillParticles.burst(sl, p.getX(), p.getY() + 1.5, p.getZ(), 5, 0.3, ParticleTypes.HEART);
            }
            for (Monster mob : mobs) {
                SkillParticles.burst(sl, mob.getX(), mob.getY() + 1, mob.getZ(), 5, 0.3, ParticleTypes.ENCHANTED_HIT);
            }
            SkillSounds.playAt(player, SoundEvents.PLAYER_LEVELUP, 0.5f, 1.5f);
        }
        CombatLog.heal(player, "Holy Light", healAmount);
        CombatLog.aoeSkill(player, "Holy Light", undeadDamage, mobs);
        sd.startCooldown(SkillType.HOLY_LIGHT, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aHoly Light! Healed " + (nearby.size() + 1)
                        + " allies. " + mobs.size() + " undead damaged."));
    }

    private static void executeBless(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BLESS.getMpCost(level));
        int duration = (30 + level * 3) * 20; // 33-60 seconds
        int amp = Math.min(level / 4, 1);
        double range = 8;
        // Buff self
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
        // Buff nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 2, player.getZ(), 10, 0.4, ParticleTypes.TOTEM_OF_UNDYING);
            for (ServerPlayer p : nearby) {
                SkillParticles.column(sl, p.getX(), p.getZ(), p.getY(), p.getY() + 2.5, 0.3, 8, ParticleTypes.ENCHANTED_HIT);
            }
            SkillSounds.playAt(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1.0f);
        }
        sd.startCooldown(SkillType.BLESS, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aBless! " + (nearby.size() + 1) + " allies blessed."));
    }

    private static void executeHolyShell(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.HOLY_SHELL.getMpCost(level));
        float absorb = 2 + level * 0.4f + stats.getIntelligence() * 0.05f; // 2-12 absorption hearts
        double range = 6;
        // Shield self
        player.setAbsorptionAmount(player.getAbsorptionAmount() + absorb);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        // Shield nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.setAbsorptionAmount(p.getAbsorptionAmount() + absorb);
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.WITHER);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 15, ParticleTypes.END_ROD);
            for (ServerPlayer p : nearby) {
                SkillParticles.sphere(sl, p.getX(), p.getY() + 1, p.getZ(), 1.0, 10, ParticleTypes.END_ROD);
            }
            SkillSounds.playAt(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.HOLY_SHELL, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aHoly Shell! " + (nearby.size() + 1) + " allies shielded."));
    }

    private static void executeDispel(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DISPEL.getMpCost(level));
        double range = 8;
        // Cleanse self
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        // Cleanse nearby players
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.WITHER);
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 12, 0.8, ParticleTypes.HAPPY_VILLAGER);
            for (ServerPlayer p : nearby) {
                SkillParticles.burst(sl, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, ParticleTypes.HAPPY_VILLAGER);
            }
            SkillSounds.playAt(player, SoundEvents.BREWING_STAND_BREW, 0.5f, 1.5f);
        }
        sd.startCooldown(SkillType.DISPEL, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aDispel! Debuffs cleansed from " + (nearby.size() + 1) + " allies."));
    }

    private static void executeBenediction(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BENEDICTION.getMpCost(level));
        int duration = (15 + level) * 20; // 16-35 seconds
        sd.setBenedictionTicks(duration);
        sd.setBenedictionPos(player.getX(), player.getY(), player.getZ());
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.column(sl, player.getX(), player.getZ(), player.getY(), player.getY() + 3, 0.5, 20, ParticleTypes.END_ROD);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1.5, player.getZ(), 10, 0.5, ParticleTypes.HEART);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.0f);
        }
        sd.startCooldown(SkillType.BENEDICTION, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aBenediction zone created!"));
    }

    /** Called every second while benediction zone is active. */
    public static void tickBenediction(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.BENEDICTION);
        double radius = 4 + level * 0.2 + stats.getIntelligence() * 0.05;
        float damage = 0.5f + level * 0.1f + stats.getIntelligence() * 0.03f;
        AABB area = new AABB(
                sd.getBenedictionX() - radius, sd.getBenedictionY() - radius, sd.getBenedictionZ() - radius,
                sd.getBenedictionX() + radius, sd.getBenedictionY() + radius, sd.getBenedictionZ() + radius);
        // Heal allies inside
        List<ServerPlayer> allies = player.level().getEntitiesOfClass(ServerPlayer.class, area);
        for (ServerPlayer p : allies) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, false));
        }
        // Damage + slow enemies inside
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
        }
        CombatLog.aoeSkill(player, "Benediction", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, sd.getBenedictionX(), sd.getBenedictionY() + 0.1, sd.getBenedictionZ(), radius, 12, ParticleTypes.HEART);
            SkillParticles.ring(sl, sd.getBenedictionX(), sd.getBenedictionY() + 0.5, sd.getBenedictionZ(), radius * 0.7, 8, ParticleTypes.END_ROD);
        }
    }

    private static void executeAngelRay(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ANGEL_RAY.getMpCost(level));
        float damage = 1.5f + level * 0.4f + stats.getIntelligence() * 0.1f;
        float aoeRadius = 3.0f;
        Vec3 look = player.getLookAngle();
        SkillFireballEntity ray = new SkillFireballEntity(
                player.level(), player,
                look.x * 0.5, look.y * 0.5, look.z * 0.5,
                SkillFireballEntity.FireballType.ANGEL_RAY, damage, aoeRadius, level);
        ray.setPos(player.getX() + look.x, player.getEyeY(), player.getZ() + look.z);
        player.level().addFreshEntity(ray);
        SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.5f, 1.5f);
        sd.startCooldown(SkillType.ANGEL_RAY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7aAngel Ray launched!"));
    }

    // ================================================================
    // Mage (INT)
    // ================================================================

    private static void executeFlameOrb(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.FLAME_ORB.getMpCost(level));
        float damage = 2 + level * 0.6f + stats.getIntelligence() * 0.12f;
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
        if (stats.getCurrentMp() < SkillType.MAGIC_GUARD.getToggleMpPerSecond(level)) {
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
        float damage = 1 + level * 0.3f + stats.getIntelligence() * 0.08f;
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
        float damage = 0.5f + level * 0.1f + stats.getIntelligence() * 0.03f;
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
            float damage = 4 + level * 0.8f + stats.getIntelligence() * 0.2f;
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
            float damage = 2 + level * 0.4f + stats.getIntelligence() * 0.1f;
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
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.INFINITY.getMpCost(level));
        int duration = (20 + level) * 20; // 21-40 seconds
        sd.setInfinityTicks(duration);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.2, 3.0, 3, 12, ParticleTypes.DRAGON_BREATH);
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.5, 15, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        }
        sd.startCooldown(SkillType.INFINITY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Infinity activated! All skills cost 0 MP."));
    }

    /** Called every 4 seconds while Infinity is active to ramp damage buff. */
    public static void tickInfinity(ServerPlayer player, SkillData sd) {
        // Ramp damage: +5% per 4 seconds, shown as Strength buff
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, true));
        // Regen HP/MP
        float infinityHeal = player.getMaxHealth() * 0.03f;
        player.heal(infinityHeal);
        CombatLog.heal(player, "Infinity", infinityHeal);
        SkillParticles.playerAura(player, 8, 1.0, ParticleTypes.END_ROD);
        SkillParticles.playerFeet(player, 6, 0.8, ParticleTypes.WITCH);
    }

    // ================================================================
    // Necromancer (INT + Mind)
    // ================================================================

    private static void executeLifeDrain(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.LIFE_DRAIN.getMpCost(level));
        float range = 5 + level * 0.3f;
        float damage = 3 + level * 0.8f + stats.getIntelligence() * 0.15f;
        float healPct = 0.3f + level * 0.02f; // 30-50%
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        float totalDmg = 0;
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            totalDmg += damage;
        }
        float healAmount = totalDmg * healPct * (1 + stats.getMind() * 0.01f);
        if (healAmount > 0) {
            player.heal(healAmount);
            CombatLog.heal(player, "Life Drain", healAmount);
        }
        CombatLog.aoeSkill(player, "Life Drain", damage, mobs);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.disc(sl, player.getX(), player.getY() + 0.5, player.getZ(), range, 20, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, ParticleTypes.SOUL);
            SkillSounds.playAt(player, SoundEvents.WITHER_SHOOT, 0.5f, 1.2f);
        }
        sd.startCooldown(SkillType.LIFE_DRAIN, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Life Drain! Healed " + String.format("%.1f", healAmount) + " HP."));
    }

    private static void executeRaiseSkeleton(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.RAISE_SKELETON.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        int mindStat = stats.getMind();
        int dpLv = sd.getLevel(SkillType.DARK_PACT);
        int uwLv = sd.getLevel(SkillType.UNDYING_WILL);
        float minionDmg = 3 + mindStat * 0.15f;
        minionDmg *= (1 + dpLv * 0.02f);
        float minionHp = 20 + mindStat * 0.5f;
        minionHp *= (1 + uwLv * 0.03f);

        if (player.level() instanceof ServerLevel sl) {
            // Despawn only non-army minions (the raised one)
            despawnRaisedSkeleton(player, sl);
            SkeletonMinionEntity minion = new SkeletonMinionEntity(sl, player, minionHp, minionDmg, level);
            Vec3 behind = player.position().subtract(player.getLookAngle().scale(2.0));
            minion.setPos(behind.x, behind.y, behind.z);
            sl.addFreshEntity(minion);
            SkillParticles.column(sl, behind.x, behind.z, behind.y, behind.y + 2, 0.3, 12, ParticleTypes.SOUL_FIRE_FLAME);
            SkillSounds.playAt(sl, behind.x, behind.y, behind.z, SoundEvents.SKELETON_AMBIENT, 0.8f, 0.8f);
        }
        sd.setToggleActive(SkillType.RAISE_SKELETON, true);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Skeleton minion summoned!"));
    }

    private static void executeBoneShield(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.BONE_SHIELD.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.BONE_SHIELD, true);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ARMOR_EQUIP_CHAIN, 0.6f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Bone Shield activated. Damage reduced by " + getBoneShieldReduction(level) + "%."));
    }

    /** Get the damage reduction % for Bone Shield (10-25%). */
    public static int getBoneShieldReduction(int level) {
        return Math.min(10 + level, 25);
    }

    private static void executeCorpseExplosion(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        List<SkeletonMinionEntity> skeletons = findAllSkeletonMinions(player, sl);
        if (skeletons.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7cNo skeletons to detonate!"));
            return;
        }

        stats.setCurrentMp(stats.getCurrentMp() - SkillType.CORPSE_EXPLOSION.getMpCost(level));
        float damage = 8 + level * 1.5f + stats.getIntelligence() * 0.3f;

        int detonated = 0;
        for (SkeletonMinionEntity skeleton : skeletons) {
            if (skeleton.isExploding()) continue; // already detonating
            // Find nearest monster to this skeleton
            List<Monster> nearbyMobs = sl.getEntitiesOfClass(Monster.class,
                    skeleton.getBoundingBox().inflate(16));
            Monster closest = null;
            double minDist = Double.MAX_VALUE;
            for (Monster mob : nearbyMobs) {
                double dist = mob.distanceTo(skeleton);
                if (dist < minDist) {
                    minDist = dist;
                    closest = mob;
                }
            }
            if (closest != null) {
                skeleton.startExploding(closest.getId(), damage);
            } else {
                // No targets nearby — explode immediately
                skeleton.startExploding(-1, damage);
            }
            detonated++;
        }

        // Deactivate raise skeleton toggle since they're being detonated
        sd.setToggleActive(SkillType.RAISE_SKELETON, false);
        sd.startCooldown(SkillType.CORPSE_EXPLOSION, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Corpse Explosion! " + detonated + " skeleton(s) detonating!"));
    }

    private static void executeArmyOfTheDead(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.ARMY_OF_THE_DEAD.getMpCost(level));
        int duration = (int) ((10 + level * 0.5) * 20);
        sd.setArmyTicks(duration);

        if (!(player.level() instanceof ServerLevel sl)) return;

        // Despawn existing army minions
        despawnArmyMinions(player, sl);

        int mindStat = stats.getMind();
        int dpLv = sd.getLevel(SkillType.DARK_PACT);
        int uwLv = sd.getLevel(SkillType.UNDYING_WILL);
        float minionDmg = (3 + mindStat * 0.15f) * 0.7f; // 70% of raise skeleton
        minionDmg *= (1 + dpLv * 0.02f);
        float minionHp = (20 + mindStat * 0.5f) * 0.7f;
        minionHp *= (1 + uwLv * 0.03f);

        // Spawn 5 skeletons in a circle around the player
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI / 5) * i;
            double ox = Math.cos(angle) * 3.0;
            double oz = Math.sin(angle) * 3.0;

            SkeletonMinionEntity minion = new SkeletonMinionEntity(sl, player, minionHp, minionDmg, level);
            minion.setArmyMinion(true);
            minion.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
            sl.addFreshEntity(minion);

            SkillParticles.column(sl, player.getX() + ox, player.getZ() + oz,
                    player.getY(), player.getY() + 2, 0.3, 6, ParticleTypes.SOUL_FIRE_FLAME);
        }

        SkillSounds.playAt(player, SoundEvents.WITHER_SPAWN, 0.6f, 0.8f);
        sd.startCooldown(SkillType.ARMY_OF_THE_DEAD, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Army of the Dead! 5 skeletons summoned!"));
    }

    private static void executeDeathMark(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.DEATH_MARK.getMpCost(level));
        List<Monster> nearby = player.level().getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(8));
        Monster closest = null;
        double minDist = Double.MAX_VALUE;
        for (Monster mob : nearby) {
            double dist = mob.distanceTo(player);
            if (dist < minDist) {
                minDist = dist;
                closest = mob;
            }
        }
        if (closest == null) {
            stats.setCurrentMp(stats.getCurrentMp() + SkillType.DEATH_MARK.getMpCost(level));
            player.sendSystemMessage(Component.literal("\u00a7cNo target found!"));
            return;
        }
        int duration = (int) ((10 + level * 0.5) * 20);
        sd.setDeathMarkTicks(duration);
        sd.setDeathMarkTargetId(closest.getId());
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, closest.getX(), closest.getY() + 1, closest.getZ(), 15, 0.5, ParticleTypes.WITCH);
            SkillSounds.playAt(player, SoundEvents.WITHER_SHOOT, 0.5f, 1.5f);
        }
        sd.startCooldown(SkillType.DEATH_MARK, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Death Mark placed!"));
    }

    /** Called every second while Death Mark is active. Applies DoT to marked target. */
    public static void tickDeathMark(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.DEATH_MARK);
        if (level <= 0) return;
        int targetId = sd.getDeathMarkTargetId();
        if (targetId < 0) return;
        net.minecraft.world.entity.Entity target = player.level().getEntity(targetId);
        if (target == null || !target.isAlive() || !(target instanceof net.minecraft.world.entity.LivingEntity living)) {
            sd.setDeathMarkTicks(0);
            sd.setDeathMarkTargetId(-1);
            return;
        }
        float dotDmg = 2 + level * 0.5f + stats.getIntelligence() * 0.1f;
        living.hurt(SkillDamageSource.get(player.level()), dotDmg);
        CombatLog.damage(player, "Death Mark", dotDmg, living);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, living.getX(), living.getY() + 1.5, living.getZ(), 5, 0.3, ParticleTypes.WITCH);
        }
    }

    /** Called when a Death Mark target dies: AoE explosion + restore HP/MP. */
    public static void onDeathMarkTargetDeath(ServerPlayer player, PlayerStats stats, SkillData sd,
                                               net.minecraft.world.entity.LivingEntity deadEntity) {
        int level = sd.getLevel(SkillType.DEATH_MARK);
        if (level <= 0) return;
        float aoeRange = 6;
        float aoeDmg = 5 + level * 1.0f + stats.getIntelligence() * 0.2f;
        if (player.level() instanceof ServerLevel sl) {
            double dx = deadEntity.getX(), dy = deadEntity.getY(), dz = deadEntity.getZ();
            AABB area = deadEntity.getBoundingBox().inflate(aoeRange);
            List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area, e -> e != deadEntity);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(player.level()), aoeDmg);
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, true));
            }
            CombatLog.aoeSkill(player, "Death Mark Explosion", aoeDmg, mobs);
            // Clear, dramatic explosion effects
            SkillParticles.disc(sl, dx, dy + 0.3, dz, aoeRange, 30, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.ring(sl, dx, dy + 0.1, dz, aoeRange, 20, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.burst(sl, dx, dy + 1, dz, 25, 1.2, ParticleTypes.EXPLOSION);
            SkillParticles.burst(sl, dx, dy + 1, dz, 20, 0.8, ParticleTypes.WITCH);
            SkillParticles.column(sl, dx, dz, dy, dy + 3, 0.4, 12, ParticleTypes.SOUL_FIRE_FLAME);
            SkillSounds.playAt(sl, dx, dy, dz, SoundEvents.GENERIC_EXPLODE, 1.0f, 0.7f);
            SkillSounds.playAt(sl, dx, dy, dz, SoundEvents.WITHER_SPAWN, 0.6f, 1.5f);
        }
        float hpRestore = player.getMaxHealth() * (5 + level * 0.5f) / 100f;
        int mpRestore = (int) (stats.getMaxMp() * (5 + level * 0.5f) / 100f);
        player.heal(hpRestore);
        CombatLog.heal(player, "Death Mark", hpRestore);
        stats.setCurrentMp(Math.min(stats.getCurrentMp() + mpRestore, stats.getMaxMp()));
        sd.setDeathMarkTicks(0);
        sd.setDeathMarkTargetId(-1);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Death Mark detonated!"));
    }

    // ================================================================
    // Skeleton Minion utilities
    // ================================================================

    /** Find ALL skeleton minions owned by the player (both raised and army). */
    public static List<SkeletonMinionEntity> findAllSkeletonMinions(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(SkeletonMinionEntity.class,
                player.getBoundingBox().inflate(64), e -> player.getUUID().equals(e.getOwnerUUID()));
    }

    /** Find the player's raised (non-army) Skeleton Minion. */
    public static SkeletonMinionEntity findSkeletonMinion(ServerPlayer player, ServerLevel level) {
        List<SkeletonMinionEntity> minions = level.getEntitiesOfClass(SkeletonMinionEntity.class,
                player.getBoundingBox().inflate(32),
                e -> player.getUUID().equals(e.getOwnerUUID()) && !e.isArmyMinion());
        return minions.isEmpty() ? null : minions.get(0);
    }

    /** Despawn only the player's raised (non-army) skeleton. */
    public static void despawnRaisedSkeleton(ServerPlayer player, ServerLevel level) {
        List<SkeletonMinionEntity> minions = level.getEntitiesOfClass(SkeletonMinionEntity.class,
                player.getBoundingBox().inflate(64),
                e -> player.getUUID().equals(e.getOwnerUUID()) && !e.isArmyMinion());
        for (SkeletonMinionEntity m : minions) {
            m.despawnWithEffect();
        }
    }

    /** Despawn all army minions owned by the player. */
    public static void despawnArmyMinions(ServerPlayer player, ServerLevel level) {
        List<SkeletonMinionEntity> minions = level.getEntitiesOfClass(SkeletonMinionEntity.class,
                player.getBoundingBox().inflate(64),
                e -> player.getUUID().equals(e.getOwnerUUID()) && e.isArmyMinion());
        for (SkeletonMinionEntity m : minions) {
            m.despawnWithEffect();
        }
    }

    /** Despawn ALL skeleton minions owned by the player. */
    public static void despawnSkeletonMinion(ServerPlayer player, ServerLevel level) {
        List<SkeletonMinionEntity> minions = findAllSkeletonMinions(player, level);
        for (SkeletonMinionEntity m : minions) {
            m.despawnWithEffect();
        }
    }

    // ================================================================
    // Legacy
    // ================================================================

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
            SkillSounds.playAt(player, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f);
        }
        sd.startCooldown(SkillType.VITAL_SURGE, level);
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a7aVital Surge! HP restored."));
    }

    // ================================================================
    // Toggle deactivation
    // ================================================================

    private static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case IRON_WILL -> {
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Iron Will deactivated."));
            }
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
            case HURRICANE -> {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Hurricane deactivated."));
            }
            case MAGIC_GUARD ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Magic Guard deactivated."));
            case RAISE_SKELETON -> {
                if (player.level() instanceof ServerLevel sl) {
                    despawnSkeletonMinion(player, sl);
                }
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Raise Skeleton deactivated."));
            }
            case BONE_SHIELD ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Bone Shield deactivated."));
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ================================================================
    // Shadow Partner utilities
    // ================================================================

    /** Find the player's active Shadow Partner entity. */
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

    /** Mirror a skill from the Shadow Partner's position. All skills are mirrored. */
    public static void mirrorSkillFromPartner(ServerPlayer player, PlayerStats stats, SkillType skill) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        ShadowPartnerEntity partner = findShadowPartner(player, sl);
        if (partner == null) return;

        int level = stats.getSkillData().getLevel(skill);
        if (level <= 0) return;

        float multiplier = getShadowPartnerDamageMultiplier(
                stats.getSkillData().getLevel(SkillType.SHADOW_PARTNER));
        Vec3 pos = partner.position();
        Vec3 look = player.getLookAngle();

        switch (skill) {
            // === T0 ===
            case DASH -> {
                SkillParticles.ring(sl, pos.x, pos.y + 0.1, pos.z, 0.8, 8, ParticleTypes.CLOUD);
                SkillParticles.burst(sl, pos.x, pos.y + 0.5, pos.z, 6, 0.4, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE);
                double force = 0.8 + level * 0.15;
                partner.push(look.x * force, 0.1, look.z * force);
                partner.hurtMarked = true;
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.5f);
            }

            // === Warrior ===
            case BLOODLUST -> {
                float radius = 4 + level * 0.3f;
                float dmg = (2 + level * 0.5f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Bloodlust", dmg, mobs);
                SkillParticles.sphere(sl, pos.x, pos.y + 1, pos.z, radius, 15, ParticleTypes.DAMAGE_INDICATOR);
            }
            case WAR_CRY -> {
                float radius = 6;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
                }
                SkillParticles.burst(sl, pos.x, pos.y + 1.5, pos.z, 10, 0.5, ParticleTypes.CRIT);
            }
            case GROUND_SLAM -> {
                float radius = 5 + level * 0.2f;
                float dmg = (4 + level * 1.0f + stats.getStrength() * 0.3f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + level * 4, 2, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Ground Slam", dmg, mobs);
                SkillParticles.disc(sl, pos.x, pos.y + 0.1, pos.z, radius, 25, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            }
            case DOMAIN_OF_MONARCH -> {
                float radius = 3 + level * 0.2f;
                float dmg = (1 + level * 0.3f + stats.getStrength() * 0.1f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Domain", dmg, mobs);
                SkillParticles.ring(sl, pos.x, pos.y + 0.1, pos.z, radius, 12, ParticleTypes.WITCH);
            }
            case UNBREAKABLE -> {
                partner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, true));
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.5, ParticleTypes.TOTEM_OF_UNDYING);
            }

            // === Assassin ===
            case SHADOW_STRIKE -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.4, ParticleTypes.PORTAL);
                SkillParticles.spiral(sl, pos.x, pos.y, pos.z, 0.8, 2.0, 2, 8, ParticleTypes.SMOKE);
            }
            case VENOM -> {
                SkillParticles.burst(sl, pos.x, pos.y + 0.8, pos.z, 6, 0.3, ParticleTypes.ITEM_SLIME);
            }
            case BLADE_FURY -> {
                float radius = 3 + level * 0.2f;
                float dmg = (3 + level * 0.6f + stats.getLuck() * 0.1f) * multiplier;
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
                float dmg = (level * 0.3f + stats.getLuck() * 0.06f) * multiplier;
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

            // === Archer ===
            case ARROW_RAIN -> {
                float dmg = (0.3f + level * 0.1f + stats.getDexterity() * 0.02f) * multiplier;
                for (int i = 0; i < 5; i++) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 6;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 6;
                    SkillArrowEntity arrow = new SkillArrowEntity(sl, partner,
                            SkillArrowEntity.ArrowType.RAIN, dmg, 0, 0);
                    arrow.setPos(pos.x + ox, pos.y + 10, pos.z + oz);
                    arrow.shoot(0, -1, 0, 2.0f, 1.5f);
                    sl.addFreshEntity(arrow);
                }
            }
            case ARROW_BOMB -> {
                float dmg = (3 + level * 0.8f + stats.getDexterity() * 0.15f) * multiplier;
                SkillArrowEntity arrow = new SkillArrowEntity(sl, partner,
                        SkillArrowEntity.ArrowType.BOMB, dmg, 3 + level * 0.1f, 30 + level * 2);
                arrow.shootFromRotation(partner, partner.getXRot(), partner.getYRot(), 0, 2.5f, 1.0f);
                sl.addFreshEntity(arrow);
            }
            case COVERING_FIRE -> {
                float dmg = (2 + level * 0.5f + stats.getDexterity() * 0.1f) * multiplier;
                for (int i = 0; i < 3; i++) {
                    SkillArrowEntity arrow = new SkillArrowEntity(sl, partner,
                            SkillArrowEntity.ArrowType.COVERING_FIRE, dmg, 0, 0);
                    float spread = (i - 1) * 8.0f;
                    arrow.shootFromRotation(partner, partner.getXRot(), partner.getYRot() + spread, 0, 2.0f, 3.0f);
                    sl.addFreshEntity(arrow);
                }
            }
            case PHOENIX -> {
                float dmg = (1 + level * 0.2f + stats.getDexterity() * 0.04f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(4));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.setSecondsOnFire(2);
                }
                CombatLog.aoeSkill(player, "Shadow Phoenix", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.5, ParticleTypes.FLAME);
            }

            // === Healer ===
            case HOLY_LIGHT -> {
                float healAmt = (1 + level * 0.4f + stats.getIntelligence() * 0.08f) * multiplier;
                player.heal(healAmt);
                CombatLog.heal(player, "Shadow Holy Light", healAmt);
                float undeadDmg = (0.5f + level * 0.25f + stats.getIntelligence() * 0.05f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(6),
                        m -> m.getMobType() == net.minecraft.world.entity.MobType.UNDEAD);
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), undeadDmg);
                }
                CombatLog.aoeSkill(player, "Shadow Holy Light", undeadDmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.5, ParticleTypes.HEART);
            }
            case BENEDICTION -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.HEART);
                SkillParticles.column(sl, pos.x, pos.z, pos.y, pos.y + 2, 0.3, 8, ParticleTypes.END_ROD);
            }
            case ANGEL_RAY -> {
                float dmg = (2 + level * 0.8f + stats.getIntelligence() * 0.15f) * multiplier;
                SkillFireballEntity ray = new SkillFireballEntity(sl, partner,
                        look.x, look.y, look.z,
                        SkillFireballEntity.FireballType.ANGEL_RAY, dmg, 3.0f, level);
                sl.addFreshEntity(ray);
            }

            // === Mage ===
            case FLAME_ORB -> {
                float dmg = (3 + level * 1.2f + stats.getIntelligence() * 0.2f) * multiplier;
                SkillFireballEntity fb = new SkillFireballEntity(sl, partner,
                        look.x, look.y, look.z,
                        SkillFireballEntity.FireballType.FLAME_ORB, dmg, 3.0f, level);
                sl.addFreshEntity(fb);
            }
            case FROST_BIND -> {
                float radius = 4 + level * 0.2f;
                float dmg = (3 + level * 0.8f) * multiplier;
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
                float dmg = (0.5f + level * 0.1f + stats.getIntelligence() * 0.03f) * multiplier;
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
                float dmg = (2 + level * 0.5f + stats.getIntelligence() * 0.1f) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                }
                CombatLog.aoeSkill(player, "Shadow Eruption", dmg, mobs);
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 15, radius * 0.4, ParticleTypes.ENCHANTED_HIT);
            }

            // === All other skills: visual mirror (particles + swing) ===
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }
}
