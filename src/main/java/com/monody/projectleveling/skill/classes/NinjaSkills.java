package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.ninja.FlyingRaijinKunaiEntity;
import com.monody.projectleveling.entity.ninja.ShadowCloneEntity;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class NinjaSkills {

    public static final SkillType[] ALL = {
            SkillType.SHURIKEN_JUTSU, SkillType.SUBSTITUTION_JUTSU, SkillType.KUNAI_MASTERY,
            SkillType.SHADOW_CLONE, SkillType.FLYING_RAIJIN, SkillType.FLYING_RAIJIN_GROUND, SkillType.CHAKRA_CONTROL,
            SkillType.RASENGAN, SkillType.SAGE_MODE, SkillType.EIGHT_INNER_GATES,
            SkillType.MULTI_SHADOW_CLONE,
    };

    private NinjaSkills() {}

    // ── Tooltips ─────────────────────────────────────────────────────────

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case SHURIKEN_JUTSU -> {
                float dmg = 2 + level * 0.5f + stats.getAgility() * 0.1f + stats.getLuck() * 0.05f;
                float range = 5 + level * 0.3f;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (AGI+LUK scale)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (cone)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Hits all enemies in front");
                lines.add(new int[]{TEXT_DIM});
            }
            case SUBSTITUTION_JUTSU -> {
                float dur = 3 + level / 2.0f;
                texts.add("Buffer duration: " + String.format("%.1f", dur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Negates first hit, teleport behind attacker");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Face attacker + 1s Speed II after dodge");
                lines.add(new int[]{TEXT_DIM});
            }
            case KUNAI_MASTERY -> {
                float meleeBonus = stats.getAgility() * 0.08f * level / 10.0f;
                float projBonus = level * 1.5f;
                float critBonus = stats.getLuck() * 0.05f * level / 10.0f;
                texts.add("Melee dmg: +" + String.format("%.2f", meleeBonus) + " (AGI scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Projectile dmg: +" + String.format("%.1f", projBonus) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit rate: +" + String.format("%.1f", critBonus) + "% (LUK scales)");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T2 ===
            case SHADOW_CLONE -> {
                int baseClones = getBaseMaxClones(level);
                int mscBonus = getMultiShadowCloneBonus(stats.getSkillData().getLevel(SkillType.MULTI_SHADOW_CLONE));
                int totalClones = baseClones + mscBonus;
                float statPct = getCloneStatMultiplier(level) * 100;
                texts.add("Clones: " + totalClones + (mscBonus > 0 ? " (" + baseClones + "+" + mscBonus + " MSC)" : ""));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Clone stats: " + String.format("%.0f", statPct) + "% of owner");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + skill.getToggleMpPerSecond(level) + "/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Clones use Shuriken & Rasengan on their own");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Clones also copy your active skills");
                lines.add(new int[]{TEXT_DIM});
                texts.add("10s cooldown when all clones destroyed");
                lines.add(new int[]{TEXT_DIM});
            }
            case FLYING_RAIJIN -> {
                float dmg = 3 + level * 0.5f + stats.getAgility() * 0.08f;
                texts.add("Kunai damage: " + String.format("%.1f", dmg) + " (on hit)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("1st use: throw kunai, marks hit target");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("2nd use: teleport to kunai/target");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Kunai lasts 10s, cooldown if not reused");
                lines.add(new int[]{TEXT_DIM});
                int rsgLv = stats.getSkillData().getLevel(SkillType.RASENGAN);
                if (rsgLv > 0) {
                    float comboDmg = 5 + getRasenganBonusDamage(stats, rsgLv);
                    texts.add("\u00a76Lv2 Combo: \u00a7fRasengan buff + marked target");
                    lines.add(new int[]{TEXT_VALUE});
                    texts.add("  Deals " + String.format("%.1f", comboDmg) + " + 30% AoE splash");
                    lines.add(new int[]{TEXT_VALUE});
                }
            }
            case FLYING_RAIJIN_GROUND -> {
                texts.add("1st use: place kunai at your feet");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("2nd use: teleport back to kunai");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Kunai lasts 10s, cooldown if expired");
                lines.add(new int[]{TEXT_DIM});
            }
            case CHAKRA_CONTROL -> {
                texts.add("MP cost reduction: -" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP regen: +" + String.format("%.1f", level * 0.15) + "%/s");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T3 ===
            case RASENGAN -> {
                float bonusDmg = getRasenganBonusDamage(stats, level);
                texts.add("Empowers next melee attack");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bonus damage: +" + String.format("%.1f", bonusDmg) + " (AGI+LUK scale)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE explosion: 2-block radius");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Splash damage: 30% of total hit damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Buff lasts 10s or until next attack");
                lines.add(new int[]{TEXT_DIM});
            }
            case SAGE_MODE -> {
                float dmgBoost = 20 + level;
                texts.add("Damage boost: +" + String.format("%.0f", dmgBoost) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Speed: +15%  |  Knockback resist");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: 3% of max MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Enhanced particle aura while active");
                lines.add(new int[]{TEXT_DIM});
            }
            case EIGHT_INNER_GATES -> {
                texts.add("Below 30% HP:");
                lines.add(new int[]{TEXT_DIM});
                texts.add("  ATK boost: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("  Speed boost: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                if (level >= 20) {
                    texts.add("Max level: Brief invuln at 10% HP (60s CD)");
                    lines.add(new int[]{TEXT_VALUE});
                }
            }
            case MULTI_SHADOW_CLONE -> {
                int bonus = getMultiShadowCloneBonus(level);
                int mpPenalty = level * 4;
                texts.add("Bonus clones: +" + bonus + (level >= 10 ? " (max)" : ""));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max total clones: " + (getBaseMaxClones(15) + bonus));
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP penalty: -" + mpPenalty + "% max MP when clones active");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Requires Shadow Clone to be active");
                lines.add(new int[]{TEXT_DIM});
            }
            default -> {}
        }
    }

    // ── Execute dispatcher ───────────────────────────────────────────────

    public static void execute(ServerPlayer player, PlayerStats stats, SkillData sd,
                               SkillType skill, int level) {
        switch (skill) {
            case SHURIKEN_JUTSU -> executeShurikenJutsu(player, stats, sd, level);
            case SUBSTITUTION_JUTSU -> executeSubstitutionJutsu(player, stats, sd, level);
            case SHADOW_CLONE -> executeShadowClone(player, stats, sd, level);
            case FLYING_RAIJIN -> executeFlyingRaijin(player, stats, sd, level);
            case FLYING_RAIJIN_GROUND -> executeFlyingRaijinGround(player, stats, sd, level);
            case RASENGAN -> executeRasengan(player, stats, sd, level);
            case SAGE_MODE -> executeSageMode(player, stats, sd, level);
            default -> {}
        }
    }

    // ── Individual execute methods ───────────────────────────────────────

    private static void executeShurikenJutsu(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.SHURIKEN_JUTSU.getMpCost(level));
        float damage = 2 + level * 0.5f + stats.getAgility() * 0.1f + stats.getLuck() * 0.05f + SkillExecutor.getWeaponDamage(player);
        float range = 5 + level * 0.3f;
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        List<Monster> hitMobs = new java.util.ArrayList<>();
        CombatLog.suppressDamageLog = true;
        CombatLog.pendingAoeSplashDmg = 0;
        CombatLog.pendingAoeSplashCount = 0;
        for (Monster mob : mobs) {
            Vec3 toMob = mob.position().add(0, mob.getBbHeight() / 2, 0).subtract(eye).normalize();
            if (look.dot(toMob) < 0.5) continue; // ~60 degree cone
            mob.hurt(player.damageSources().playerAttack(player), damage);
            hitMobs.add(mob);
        }
        CombatLog.suppressDamageLog = false;
        CombatLog.flushAoe(player, "Shuriken Jutsu");
        if (player.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 12; i++) {
                double spread = (player.getRandom().nextDouble() - 0.5) * 0.6;
                Vec3 dir = look.add(look.yRot((float) spread)).normalize();
                Vec3 end = eye.add(dir.scale(range));
                SkillParticles.line(sl, eye.add(dir.scale(1.5)), end, 0.8, ParticleTypes.CRIT);
            }
            SkillSounds.playAt(player, SoundEvents.ARROW_SHOOT, 0.8f, 1.5f);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.2f);
        }
        sd.startCooldown(SkillType.SHURIKEN_JUTSU, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Shuriken Jutsu! " + hitMobs.size() + " enemies hit."));
    }

    private static void executeSubstitutionJutsu(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.SUBSTITUTION_JUTSU.getMpCost(level));
        int ticks = (int) ((3 + level / 2.0f) * 20);
        sd.setSubstitutionTicks(ticks);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.4, ParticleTypes.CLOUD);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 8, 0.3, ParticleTypes.SMOKE);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.5f, 1.2f);
        }
        sd.startCooldown(SkillType.SUBSTITUTION_JUTSU, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eSubstitution ready! Next hit will be dodged."));
    }

    private static void executeShadowClone(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.SHADOW_CLONE.getToggleMpPerSecond(level)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SHADOW_CLONE, true);
        if (player.level() instanceof ServerLevel sl) {
            // Despawn any existing clones
            despawnAllShadowClones(player, sl);

            int maxClones = getMaxClones(sd);
            float statMult = getCloneStatMultiplier(level);

            for (int i = 0; i < maxClones; i++) {
                double angle = (2 * Math.PI / maxClones) * i + Math.PI;
                Vec3 offset = new Vec3(Math.sin(angle) * 2.0, 0, Math.cos(angle) * 2.0);
                Vec3 spawnPos = player.position().add(offset);

                ShadowCloneEntity clone = new ShadowCloneEntity(sl, player, stats, statMult);
                clone.moveTo(spawnPos.x, player.getY(), spawnPos.z, player.getYRot(), 0);
                sl.addFreshEntity(clone);

                SkillParticles.burst(sl, spawnPos.x, spawnPos.y + 1, spawnPos.z, 20, 0.6, ParticleTypes.CLOUD);
            }
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.4, ParticleTypes.SMOKE);
            SkillSounds.playAt(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.6f, 1.0f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Shadow Clone Jutsu!"));
    }

    private static void executeFlyingRaijin(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (sd.getFlyingRaijinPhase() == 0) {
            // Phase 1: Throw kunai
            stats.setCurrentMp(stats.getCurrentMp() - SkillType.FLYING_RAIJIN.getMpCost(level));
            sd.setFlyingRaijinPhase(1);
            sd.setFlyingRaijinTargetId(-1);
            sd.setFlyingRaijinKunaiId(-1);

            if (player.level() instanceof ServerLevel sl) {
                float dmg = 3 + level * 0.5f + stats.getAgility() * 0.08f + SkillExecutor.getWeaponDamage(player);
                FlyingRaijinKunaiEntity kunai = new FlyingRaijinKunaiEntity(sl, player, dmg);
                kunai.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 0.5f);
                sl.addFreshEntity(kunai);
                sd.setFlyingRaijinKunaiId(kunai.getId());
                SkillSounds.playAt(player, SoundEvents.ARROW_SHOOT, 0.8f, 1.2f);
            }
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76Flying Raijin kunai thrown!"));
        } else {
            // Phase 2: Teleport to kunai/target
            double tx, ty, tz;
            int targetId = sd.getFlyingRaijinTargetId();
            int kunaiId = sd.getFlyingRaijinKunaiId();
            net.minecraft.world.entity.Entity kunaiEntity = kunaiId > 0 ? player.level().getEntity(kunaiId) : null;

            if (targetId > 0) {
                // Marked entity: teleport to its current position
                net.minecraft.world.entity.Entity target = player.level().getEntity(targetId);
                if (target != null && target.isAlive()) {
                    tx = target.getX();
                    ty = target.getY();
                    tz = target.getZ();
                } else {
                    tx = sd.getFlyingRaijinX();
                    ty = sd.getFlyingRaijinY();
                    tz = sd.getFlyingRaijinZ();
                }
            } else if (kunaiEntity != null && kunaiEntity.isAlive()) {
                // Kunai still in flight or stuck: teleport to its current position
                tx = kunaiEntity.getX();
                ty = kunaiEntity.getY();
                tz = kunaiEntity.getZ();
            } else {
                tx = sd.getFlyingRaijinX();
                ty = sd.getFlyingRaijinY();
                tz = sd.getFlyingRaijinZ();
            }

            // Despawn kunai entity if still exists
            if (kunaiEntity != null) kunaiEntity.discard();

            if (player.level() instanceof ServerLevel sl) {
                // Departure particles
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, ParticleTypes.END_ROD);
                SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.8f);
            }

            player.teleportTo(tx, ty, tz);

            // Flying Raijin, level 2: if marked an entity AND Rasengan buff is active,
            // consume Rasengan and deal its damage at the teleport destination
            boolean isLevel2 = false;
            net.minecraft.world.entity.Entity markedTarget = null;
            if (targetId > 0) {
                markedTarget = player.level().getEntity(targetId);
            }
            if (markedTarget instanceof net.minecraft.world.entity.LivingEntity livingTarget
                    && markedTarget.isAlive() && sd.isRasenganBuffActive()) {
                isLevel2 = true;
                sd.setRasenganBuffActive(false);
                sd.setRasenganBuffTicks(0);

                int rsgLv = sd.getLevel(SkillType.RASENGAN);
                float rasenganBonus = getRasenganBonusDamage(stats, rsgLv);
                // Deal Rasengan damage to the marked target
                float rasenganDmg = 5 + rasenganBonus + SkillExecutor.getWeaponDamage(player);
                CombatLog.nextSource = "Flying Raijin: Rasengan";
                livingTarget.hurt(player.damageSources().playerAttack(player), rasenganDmg);

                // AoE splash around teleport destination (2-block radius)
                float splashDmg = rasenganDmg * 0.3f;
                if (player.level() instanceof ServerLevel sl) {
                    AABB splashBox = new AABB(tx - 2, ty - 1, tz - 2, tx + 2, ty + 3, tz + 2);
                    List<net.minecraft.world.entity.LivingEntity> nearby = sl.getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class, splashBox,
                            e -> e != player && e != livingTarget && e.isAlive()
                                    && !(e instanceof ShadowCloneEntity sc && player.getUUID().equals(sc.getOwnerUUID())));
                    for (net.minecraft.world.entity.LivingEntity e : nearby) {
                        e.hurt(SkillDamageSource.get(player.level()), splashDmg);
                    }
                    if (!nearby.isEmpty()) {
                        CombatLog.aoeSkill(player, "Flying Raijin: Rasengan", splashDmg, nearby);
                    }
                }
            }

            if (player.level() instanceof ServerLevel sl) {
                if (isLevel2) {
                    // Level 2: enhanced arrival — Rasengan explosion effect
                    SkillParticles.burst(sl, tx, ty + 1, tz, 50, 1.2, ParticleTypes.END_ROD);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 30, 0.8, ParticleTypes.FIREWORK);
                    SkillParticles.spiral(sl, tx, ty, tz, 2.0, 3.0, 3, 20, ParticleTypes.END_ROD);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 20, 1.0, ParticleTypes.EXPLOSION);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.5f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 0.8f);
                } else {
                    // Normal arrival: yellow flash
                    SkillParticles.burst(sl, tx, ty + 1, tz, 30, 0.8, ParticleTypes.END_ROD);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 15, 0.5, ParticleTypes.FIREWORK);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.5f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.2f);
                }
            }

            // Reset state and start cooldown
            sd.setFlyingRaijinPhase(0);
            sd.setFlyingRaijinTargetId(-1);
            sd.setFlyingRaijinKunaiId(-1);
            sd.startCooldown(SkillType.FLYING_RAIJIN, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76" + (isLevel2 ? "Flying Raijin, level 2!" : "Flying Raijin!")));
        }
    }

    private static void executeFlyingRaijinGround(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (sd.getFrgPhase() == 0) {
            // Phase 1: Place kunai at feet
            stats.setCurrentMp(stats.getCurrentMp() - SkillType.FLYING_RAIJIN_GROUND.getMpCost(level));
            sd.setFrgPhase(1);
            sd.setFrgPos(player.getX(), player.getY(), player.getZ());
            sd.setFrgTicks(200); // 10 seconds

            if (player.level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, player.getX(), player.getY() + 0.2, player.getZ(),
                        10, 0.3, ParticleTypes.END_ROD);
                SkillSounds.playAt(player, SoundEvents.ITEM_PICKUP, 0.8f, 0.6f);
            }
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76Flying Raijin: Ground set! Reuse to return."));
        } else {
            // Phase 2: Teleport back to placed kunai
            double tx = sd.getFrgX();
            double ty = sd.getFrgY();
            double tz = sd.getFrgZ();

            if (player.level() instanceof ServerLevel sl) {
                // Departure particles
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(),
                        20, 0.5, ParticleTypes.END_ROD);
                SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.8f);
            }

            player.teleportTo(tx, ty, tz);

            if (player.level() instanceof ServerLevel sl) {
                // Arrival: yellow flash
                SkillParticles.burst(sl, tx, ty + 1, tz, 30, 0.8, ParticleTypes.END_ROD);
                SkillParticles.burst(sl, tx, ty + 1, tz, 15, 0.5, ParticleTypes.FIREWORK);
                SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.5f);
            }

            sd.setFrgPhase(0);
            sd.setFrgTicks(0);
            sd.startCooldown(SkillType.FLYING_RAIJIN_GROUND, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76Flying Raijin: Ground!"));
        }
    }

    private static void executeRasengan(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.RASENGAN.getMpCost(level));
        sd.setRasenganBuffActive(true);
        sd.setRasenganBuffTicks(200); // 10 seconds to use

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.spiral(sl, player.getX(), player.getY() + 0.8, player.getZ(),
                    0.5, 1.5, 3, 12, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 0.8f);
        }
        sd.startCooldown(SkillType.RASENGAN, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Rasengan charged! Next attack will explode."));
    }

    private static void executeSageMode(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        int drain = Math.max(1, (int) (stats.getMaxMp() * 0.03));
        if (stats.getCurrentMp() < drain) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SAGE_MODE, true);
        sd.setSageModeDrainTimer(0);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 30, 0.8, ParticleTypes.END_ROD);
            SkillParticles.spiral(sl, player.getX(), player.getY(), player.getZ(), 1.5, 3.0, 3, 15, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Sage Mode activated!"));
    }

    // ── Clone skill sync ─────────────────────────────────────────────────

    /** Called from SkillExecutor after owner uses a skill; clones mirror it. */
    public static void syncSkillToClones(ServerPlayer player, PlayerStats stats,
                                          SkillType skill, int level) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        SkillData sd = stats.getSkillData();
        if (!sd.isToggleActive(SkillType.SHADOW_CLONE)) return;

        List<ShadowCloneEntity> clones = findAllShadowClones(player, sl);
        float statMult = getCloneStatMultiplier(sd.getLevel(SkillType.SHADOW_CLONE));

        for (ShadowCloneEntity clone : clones) {
            executeCloneSkill(sl, clone, player, stats, skill, level, statMult);
        }
    }

    private static void executeCloneSkill(ServerLevel sl, ShadowCloneEntity clone,
                                           ServerPlayer player, PlayerStats stats,
                                           SkillType skill, int level, float statMult) {
        Vec3 pos = clone.position();
        switch (skill) {
            case SHURIKEN_JUTSU -> {
                float dmg = (2 + level * 0.5f + stats.getAgility() * 0.1f + SkillExecutor.getWeaponDamage(player)) * statMult;
                float range = 5 + level * 0.3f;
                Vec3 look = clone.getLookAngle();
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        clone.getBoundingBox().inflate(range));
                for (Monster mob : mobs) {
                    Vec3 toMob = mob.position().subtract(pos).normalize();
                    if (look.dot(toMob) < 0.3) continue;
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                }
                for (int i = 0; i < 8; i++) {
                    SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 3, range * 0.3, ParticleTypes.CRIT);
                }
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z, SoundEvents.ARROW_SHOOT, 0.5f, 1.5f);
            }
            case RASENGAN -> {
                // Clones also get Rasengan buff — AoE on next melee hit
                clone.setRasenganReady(true);
                SkillParticles.spiral(sl, pos.x, pos.y + 0.8, pos.z,
                        0.5, 1.5, 3, 8, ParticleTypes.END_ROD);
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z,
                        SoundEvents.FIREWORK_ROCKET_BLAST, 0.4f, 0.8f);
            }
            case FLYING_RAIJIN -> {
                // Phase check: after execution, phase 1 = kunai thrown, phase 0 = teleport done
                if (stats.getSkillData().getFlyingRaijinPhase() == 0) {
                    // Phase 2 completed: clones teleport with player
                    teleportCloneToPlayer(sl, clone, player, stats, statMult);
                } else {
                    SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 5, 0.3, ParticleTypes.END_ROD);
                }
            }
            case FLYING_RAIJIN_GROUND -> {
                if (stats.getSkillData().getFrgPhase() == 0) {
                    // Phase 2 completed: clones teleport with player
                    teleportCloneToPlayer(sl, clone, player, stats, statMult);
                }
            }
            default -> {
                // Default: clone swings for visual feedback
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.SMOKE);
                clone.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    /** Teleport a clone near the player. If clone has Rasengan ready, perform Lv2 AoE at destination. */
    private static void teleportCloneToPlayer(ServerLevel sl, ShadowCloneEntity clone,
                                               ServerPlayer player, PlayerStats stats, float statMult) {
        Vec3 oldPos = clone.position();
        // Departure particles
        SkillParticles.burst(sl, oldPos.x, oldPos.y + 1, oldPos.z, 10, 0.4, ParticleTypes.END_ROD);

        // Teleport near player with random offset
        double angle = clone.getRandom().nextDouble() * 2 * Math.PI;
        double dist = 1.5 + clone.getRandom().nextDouble() * 1.5;
        double cx = player.getX() + Math.sin(angle) * dist;
        double cz = player.getZ() + Math.cos(angle) * dist;
        clone.teleportTo(cx, player.getY(), cz);

        // Arrival particles
        SkillParticles.burst(sl, cx, player.getY() + 1, cz, 10, 0.4, ParticleTypes.END_ROD);
        SkillSounds.playAt(sl, cx, player.getY(), cz, SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.8f);

        // Flying Raijin Lv2: if clone has Rasengan ready, deal AoE at destination
        if (clone.isRasenganReady()) {
            clone.setRasenganReady(false);
            SkillData sd = stats.getSkillData();
            int rsgLv = sd.getLevel(SkillType.RASENGAN);
            float rasenganDmg = (5 + getRasenganBonusDamage(stats, rsgLv) + SkillExecutor.getWeaponDamage(player)) * statMult;

            // AoE around clone's arrival position
            AABB splashBox = new AABB(cx - 2, player.getY() - 1, cz - 2, cx + 2, player.getY() + 3, cz + 2);
            List<net.minecraft.world.entity.LivingEntity> nearby = sl.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class, splashBox,
                    e -> e != player && e.isAlive() && e instanceof Monster);
            for (net.minecraft.world.entity.LivingEntity e : nearby) {
                e.hurt(SkillDamageSource.get(player.level()), rasenganDmg);
            }
            if (!nearby.isEmpty()) {
                CombatLog.aoeSkill(player, "Clone Flying Raijin: Rasengan", rasenganDmg, nearby);
            }

            // Enhanced explosion particles for clone Lv2
            SkillParticles.burst(sl, cx, player.getY() + 1, cz, 25, 0.8, ParticleTypes.END_ROD);
            SkillParticles.burst(sl, cx, player.getY() + 1, cz, 10, 0.6, ParticleTypes.EXPLOSION);
            SkillSounds.playAt(sl, cx, player.getY(), cz, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.3f);
        }
    }

    // ── Public helper methods ────────────────────────────────────────────

    /** Base max clones from Shadow Clone skill level alone. */
    public static int getBaseMaxClones(int level) {
        return level >= 15 ? 2 : 1;
    }

    /** Bonus clones from Multi Shadow Clone passive. Lv1=+1, Lv5=+2, Lv10=+3. */
    public static int getMultiShadowCloneBonus(int mscLevel) {
        if (mscLevel >= 10) return 3;
        if (mscLevel >= 5) return 2;
        if (mscLevel >= 1) return 1;
        return 0;
    }

    /** Total max clones (base + Multi Shadow Clone bonus). */
    public static int getMaxClones(SkillData sd) {
        int base = getBaseMaxClones(sd.getLevel(SkillType.SHADOW_CLONE));
        int bonus = getMultiShadowCloneBonus(sd.getLevel(SkillType.MULTI_SHADOW_CLONE));
        return base + bonus;
    }

    /** Stat multiplier for shadow clones. Scales from 20% at lv1 to 50% at lv15. */
    public static float getCloneStatMultiplier(int level) {
        if (level <= 1) return 0.2f;
        return 0.2f + 0.3f * (level - 1) / 14.0f;
    }

    /** Rasengan bonus damage scaling with AGI + LUK. */
    public static float getRasenganBonusDamage(PlayerStats stats, int level) {
        if (level <= 0) return 0;
        return level * 0.5f + stats.getAgility() * 0.15f + stats.getLuck() * 0.1f;
    }

    /** Kunai Mastery melee damage bonus. */
    public static float getKunaiMasteryMeleeBonus(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.KUNAI_MASTERY);
        if (level <= 0) return 0;
        return stats.getAgility() * 0.08f * level / 10.0f;
    }

    /** Kunai Mastery projectile damage multiplier (1.0 = no bonus). */
    public static float getKunaiMasteryProjectileMultiplier(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.KUNAI_MASTERY);
        if (level <= 0) return 1.0f;
        return 1.0f + level * 0.015f;
    }

    /** Kunai Mastery crit rate bonus. */
    public static float getKunaiMasteryCritBonus(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.KUNAI_MASTERY);
        if (level <= 0) return 0;
        return stats.getSight() * 0.05f * level / 10.0f;
    }

    /** Chakra Control MP cost reduction multiplier (e.g., 0.85 = 15% reduction). */
    public static float getChakraControlCostMultiplier(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.CHAKRA_CONTROL);
        if (level <= 0) return 1.0f;
        return 1.0f - level * 0.01f;
    }

    /** Chakra Control MP regen bonus (percentage). */
    public static float getChakraControlRegenBonus(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.CHAKRA_CONTROL);
        if (level <= 0) return 0;
        return level * 0.15f;
    }

    /** Sage Mode damage multiplier (1.0 = no boost). */
    public static float getSageModeDamageMultiplier(SkillData sd) {
        if (!sd.isToggleActive(SkillType.SAGE_MODE)) return 1.0f;
        int level = sd.getLevel(SkillType.SAGE_MODE);
        return 1.0f + (20 + level) / 100.0f;
    }

    /** Eight Inner Gates damage multiplier. Only applies below 30% HP. */
    public static float getGatesDamageMultiplier(PlayerStats stats, float healthPct) {
        int level = stats.getSkillData().getLevel(SkillType.EIGHT_INNER_GATES);
        if (level <= 0 || healthPct > 0.3f) return 1.0f;
        return 1.0f + level * 0.02f;
    }

    /** Eight Inner Gates speed boost (0 if not active). */
    public static float getGatesSpeedBoost(PlayerStats stats, float healthPct) {
        int level = stats.getSkillData().getLevel(SkillType.EIGHT_INNER_GATES);
        if (level <= 0 || healthPct > 0.3f) return 0;
        return level * 0.01f;
    }

    // ── Entity utilities ─────────────────────────────────────────────────

    public static ShadowCloneEntity findShadowClone(ServerPlayer player, ServerLevel level) {
        List<ShadowCloneEntity> clones = findAllShadowClones(player, level);
        return clones.isEmpty() ? null : clones.get(0);
    }

    public static List<ShadowCloneEntity> findAllShadowClones(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(ShadowCloneEntity.class,
                player.getBoundingBox().inflate(64), e -> player.getUUID().equals(e.getOwnerUUID()));
    }

    public static void despawnShadowClone(ServerPlayer player, ServerLevel level) {
        despawnAllShadowClones(player, level);
    }

    public static void despawnAllShadowClones(ServerPlayer player, ServerLevel level) {
        List<ShadowCloneEntity> clones = level.getEntitiesOfClass(ShadowCloneEntity.class,
                player.getBoundingBox().inflate(64), e -> player.getUUID().equals(e.getOwnerUUID()));
        for (ShadowCloneEntity c : clones) {
            c.despawnWithSmoke();
        }
    }

    // ── Toggle deactivation ──────────────────────────────────────────────

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case SHADOW_CLONE -> {
                if (player.level() instanceof ServerLevel sl) {
                    despawnAllShadowClones(player, sl);
                }
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77Shadow Clones dismissed."));
            }
            case SAGE_MODE -> {
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77Sage Mode deactivated."));
            }
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
            case SHURIKEN_JUTSU -> {
                float dmg = (2 + level * 0.5f + stats.getAgility() * 0.1f + SkillExecutor.getWeaponDamage(player)) * multiplier;
                float range = 5 + level * 0.3f;
                Vec3 look = partner.getLookAngle();
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(range));
                for (Monster mob : mobs) {
                    Vec3 toMob = mob.position().subtract(pos).normalize();
                    if (look.dot(toMob) < 0.3) continue;
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                }
                for (int i = 0; i < 8; i++) {
                    SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 3, range * 0.3, ParticleTypes.CRIT);
                }
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z, SoundEvents.ARROW_SHOOT, 0.5f, 1.5f);
            }
            case FLYING_RAIJIN -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 10, 0.4, ParticleTypes.END_ROD);
            }
            case RASENGAN -> {
                // Mirror just shows visual; Rasengan is now a buff
                SkillParticles.spiral(sl, pos.x, pos.y + 0.8, pos.z,
                        0.5, 1.5, 3, 8, ParticleTypes.END_ROD);
            }
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.SMOKE);
                partner.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
}
