package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.kunai.ThrownShurikenEntity;
import com.monody.projectleveling.entity.ninja.FlyingRaijinKunaiEntity;
import com.monody.projectleveling.entity.ninja.ShadowCloneEntity;
import com.monody.projectleveling.item.ModItems;
import com.monody.projectleveling.skill.*;
import static com.monody.projectleveling.skill.StatContribRegistry.*;
import com.monody.projectleveling.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
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
            SkillType.FLYING_RAIJIN_SSRZ, SkillType.MASTERED_SAGE_MODE,
    };

    private NinjaSkills() {}

    // ── Tooltips ─────────────────────────────────────────────────────────

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case SHURIKEN_JUTSU -> {
                float mult = getShurikenJutsuMultiplier(level, stats.getAgility()) * 100;
                int count = 5 + level / 3;
                texts.add("Damage per shuriken: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Shurikens: " + count);
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Throws shurikens in a wide cone");
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
                float meleeMult = (level * 0.006f + stats.getAgility() * 0.001f) * 100;
                float projBonus = level * 1.5f;
                float critBonus = stats.getLuck() * 0.05f * level / 10.0f;
                texts.add("Melee dmg: ATK x " + String.format("%.1f", meleeMult) + "%");
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
                texts.add("Cast cost: 20% max MP");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Clones use Shuriken & Rasengan on their own");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Clones also copy your active skills");
                lines.add(new int[]{TEXT_DIM});
                texts.add("10s cooldown when all clones destroyed");
                lines.add(new int[]{TEXT_DIM});
            }
            case FLYING_RAIJIN -> {
                float mult = getFlyingRaijinMultiplier(level, stats.getAgility()) * 100;
                texts.add("Kunai damage: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("1st use: throw kunai, marks hit target");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("2nd use: teleport to kunai/target");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Kunai lasts 10s, cooldown if not reused");
                lines.add(new int[]{TEXT_DIM});
                int rsgLv = stats.getSkillData().getLevel(SkillType.RASENGAN);
                if (rsgLv > 0) {
                    float comboMult = getRasenganMultiplier(rsgLv, stats.getAgility()) * 100;
                    texts.add("\u00a76Lv2 Combo: \u00a7fRasengan buff + marked target");
                    lines.add(new int[]{TEXT_VALUE});
                    texts.add("  Deals ATK x " + String.format("%.0f", comboMult) + "% + 30% AoE splash");
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
                float mult = getRasenganMultiplier(level, stats.getAgility()) * 100;
                texts.add("Empowers next melee attack");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Bonus damage: ATK x " + String.format("%.0f", mult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("AoE explosion: 2-block radius");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Splash damage: 30% of total hit damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Buff lasts 8s or until next attack");
                lines.add(new int[]{TEXT_DIM});
            }
            case SAGE_MODE -> {
                float dmgBoost = level * 2.0f;
                int msmLv = stats.getSkillData().getLevel(SkillType.MASTERED_SAGE_MODE);
                if (msmLv > 0) dmgBoost += msmLv * 0.4f;
                texts.add("Damage boost: +" + String.format("%.0f", dmgBoost) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Speed: +15%  |  Knockback resist");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", skill.getToggleDrainPercent(level)) + "% MP/sec");
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

            // === T4 ===
            case FLYING_RAIJIN_SSRZ -> {
                float strikeMult = getSSRZMultiplier(level, stats.getAgility()) * 100;
                float finisherMult = strikeMult * 3;
                texts.add("Strike 1-8 damage: ATK x " + String.format("%.0f", strikeMult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("9th strike (finisher): ATK x " + String.format("%.0f", finisherMult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Throw kunai \u2192 mark on hit (10s)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Miss = no cooldown. Mark/target lost = full CD");
                lines.add(new int[]{TEXT_DIM});
                texts.add("0.5s invincibility per teleport strike");
                lines.add(new int[]{TEXT_DIM});
                int rsgLv = stats.getSkillData().getLevel(SkillType.RASENGAN);
                if (rsgLv > 0) {
                    float rsgMult = getRasenganMultiplier(rsgLv, stats.getAgility()) * 100;
                    texts.add("\u00a76Rasengan Combo:\u00a7f 9th strike + Rasengan x " + String.format("%.0f", rsgMult * 2) + "%");
                    lines.add(new int[]{TEXT_VALUE});
                }
            }
            case MASTERED_SAGE_MODE -> {
                texts.add("Sage Mode damage: +" + String.format("%.1f", level * 0.4f) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("ATK: +" + String.format("%.1f", level * 0.4f) + "%");
                lines.add(new int[]{TEXT_VALUE});
                int sgmLv = stats.getSkillData().getLevel(SkillType.SAGE_MODE);
                float total = sgmLv * 2.0f + level * 0.4f;
                texts.add("Total Sage Mode boost: +" + String.format("%.1f", total) + "%");
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
            case FLYING_RAIJIN_SSRZ -> executeSSRZ(player, stats, sd, level);
            default -> {}
        }
    }

    // ── Individual execute methods ───────────────────────────────────────

    private static void executeShurikenJutsu(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.SHURIKEN_JUTSU.getMpCost(level));
        float damage = stats.getAttack(player) * getShurikenJutsuMultiplier(level, stats.getAgility());
        int shurikenCount = 5 + level / 3;

        if (player.level() instanceof ServerLevel sl) {
            ItemStack visualStack = new ItemStack(ModItems.IRON_SHURIKEN.get());
            for (int i = 0; i < shurikenCount; i++) {
                ThrownShurikenEntity shuriken = new ThrownShurikenEntity(sl, player, visualStack);
                shuriken.setBaseDamage(damage);
                shuriken.setSkillProjectile(true);
                shuriken.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 20.0f);
                sl.addFreshEntity(shuriken);
            }
            SkillSounds.playAt(player, SoundEvents.TRIDENT_THROW, 0.8f, 1.5f);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.2f);
        }
        sd.startCooldown(SkillType.SHURIKEN_JUTSU, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a76Shuriken Jutsu!"));
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
        int castCost = (int) (stats.getMaxMp() * 0.20f);
        if (stats.getCurrentMp() < castCost) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP! (Need 20% max MP)"));
            return;
        }
        stats.setCurrentMp(stats.getCurrentMp() - castCost);
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
                float dmg = stats.getAttack(player) * getFlyingRaijinMultiplier(level, stats.getAgility());
                FlyingRaijinKunaiEntity kunai = new FlyingRaijinKunaiEntity(sl, player, dmg, player.getMainHandItem());
                kunai.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 0.5f);
                sl.addFreshEntity(kunai);
                sd.setFlyingRaijinKunaiId(kunai.getId());
                SkillSounds.playAt(player, SoundEvents.TRIDENT_THROW, 1.0f, 1.5f);
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
                        ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.03f, 1.0f);
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
                // Deal Rasengan damage to the marked target (×1.5 combo bonus)
                float rasenganDmg = stats.getAttack(player) * getRasenganMultiplier(rsgLv, stats.getAgility()) * 1.5f;
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
                    SkillSounds.playAt(sl, tx, ty, tz, ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.05f, 1.0f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 0.8f);
                } else {
                    // Normal arrival: yellow flash
                    SkillParticles.burst(sl, tx, ty + 1, tz, 30, 0.8, ParticleTypes.END_ROD);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 15, 0.5, ParticleTypes.FIREWORK);
                    SkillSounds.playAt(sl, tx, ty, tz, ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.05f, 1.0f);
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
                // Spawn a visible kunai stuck in the ground
                FlyingRaijinKunaiEntity kunai = FlyingRaijinKunaiEntity.placeOnGround(
                        sl, player, player.getX(), player.getY(), player.getZ(),
                        player.getMainHandItem());
                sl.addFreshEntity(kunai);
                sd.setFrgKunaiId(kunai.getId());

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
                // Remove the ground kunai entity
                removeFrgKunai(sl, sd);

                // Departure particles
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(),
                        20, 0.5, ParticleTypes.END_ROD);
                SkillSounds.playAt(sl, player.getX(), player.getY(), player.getZ(),
                        ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.03f, 1.0f);
            }

            player.teleportTo(tx, ty, tz);

            if (player.level() instanceof ServerLevel sl) {
                // Arrival: yellow flash
                SkillParticles.burst(sl, tx, ty + 1, tz, 30, 0.8, ParticleTypes.END_ROD);
                SkillParticles.burst(sl, tx, ty + 1, tz, 15, 0.5, ParticleTypes.FIREWORK);
                SkillSounds.playAt(sl, tx, ty, tz, ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.02f, 1.0f);
            }

            sd.setFrgPhase(0);
            sd.setFrgTicks(0);
            sd.setFrgKunaiId(-1);
            sd.startCooldown(SkillType.FLYING_RAIJIN_GROUND, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76Flying Raijin: Ground!"));
        }
    }

    /** Remove the FRG ground kunai entity if it exists. */
    public static void removeFrgKunai(ServerLevel level, SkillData sd) {
        int kunaiId = sd.getFrgKunaiId();
        if (kunaiId != -1) {
            net.minecraft.world.entity.Entity entity = level.getEntity(kunaiId);
            if (entity instanceof FlyingRaijinKunaiEntity) {
                entity.discard();
            }
            sd.setFrgKunaiId(-1);
        }
    }

    private static void executeRasengan(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.RASENGAN.getMpCost(level));
        sd.setRasenganBuffActive(true);
        sd.setRasenganBuffTicks(160); // 8 seconds to use

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
        int drain = SkillType.SAGE_MODE.getToggleMpPerSecond(level, stats.getMaxMp());
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

    // ── SSRZ (Flying Raijin: Shippu Senko Rennodan Zeroshiki) ───────────

    private static void executeSSRZ(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        int phase = sd.getSsrzPhase();

        if (phase == 0) {
            // Phase 0: Throw kunai
            stats.setCurrentMp(stats.getCurrentMp() - SkillType.FLYING_RAIJIN_SSRZ.getMpCost(level));

            if (player.level() instanceof ServerLevel sl) {
                float dmg = stats.getAttack(player) * getSSRZMultiplier(level, stats.getAgility()) * 0.5f;
                FlyingRaijinKunaiEntity kunai = new FlyingRaijinKunaiEntity(sl, player, dmg, player.getMainHandItem());
                kunai.setSSRZMode(true);
                kunai.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 0.5f);
                sl.addFreshEntity(kunai);
                SkillSounds.playAt(player, SoundEvents.TRIDENT_THROW, 1.0f, 1.5f);
            }
            // Cooldown starts now; if kunai misses, it resets to 0 via the entity callback
            sd.startCooldown(SkillType.FLYING_RAIJIN_SSRZ, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a76FR: Zeroshiki kunai thrown!"));

        } else if (phase >= 1 && phase <= 9) {
            // Phase 1-9: Teleport strikes
            int targetId = sd.getSsrzTargetId();
            net.minecraft.world.entity.Entity targetEntity = targetId > 0 ? player.level().getEntity(targetId) : null;

            // Check if target is still valid
            if (targetEntity == null || !targetEntity.isAlive() || !(targetEntity instanceof net.minecraft.world.entity.LivingEntity livingTarget)) {
                // Target dead/gone — end combo, enter full cooldown
                sd.setSsrzPhase(0);
                sd.setSsrzTargetId(-1);
                sd.setSsrzMarkTicks(0);
                sd.startCooldown(SkillType.FLYING_RAIJIN_SSRZ, level);
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77FR: Zeroshiki target lost. Combo ended."));
                return;
            }

            // Check if mark expired
            if (sd.getSsrzMarkTicks() <= 0) {
                sd.setSsrzPhase(0);
                sd.setSsrzTargetId(-1);
                sd.startCooldown(SkillType.FLYING_RAIJIN_SSRZ, level);
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a77FR: Zeroshiki mark expired. Combo ended."));
                return;
            }

            // Teleport to random position 0.5 blocks from target
            double angle = player.getRandom().nextDouble() * 2 * Math.PI;
            double tx = livingTarget.getX() + Math.sin(angle) * 0.5;
            double ty = livingTarget.getY();
            double tz = livingTarget.getZ() + Math.cos(angle) * 0.5;

            if (player.level() instanceof ServerLevel sl) {
                // Departure particles
                SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, ParticleTypes.END_ROD);
            }

            player.teleportTo(tx, ty, tz);
            // Grant 0.5s invincibility — must exceed invulnerableDuration/2 (default 10)
            player.invulnerableTime = 20;

            // Calculate damage
            float atk = stats.getAttack(player);
            float mult = getSSRZMultiplier(level, stats.getAgility());
            float damage;

            if (phase == 9) {
                // Finisher: 3x damage
                damage = atk * mult * 3.0f;

                // Rasengan combo: consume buff for extra damage
                if (sd.isRasenganBuffActive()) {
                    sd.setRasenganBuffActive(false);
                    sd.setRasenganBuffTicks(0);
                    int rsgLv = sd.getLevel(SkillType.RASENGAN);
                    float rasenganBonus = atk * getRasenganMultiplier(rsgLv, stats.getAgility()) * 2.0f;
                    damage += rasenganBonus;
                    CombatLog.nextSource = "FR: Zeroshiki (Rasengan Finisher)";
                } else {
                    CombatLog.nextSource = "FR: Zeroshiki (Finisher)";
                }
            } else {
                // Normal strike
                damage = atk * mult;
                CombatLog.nextSource = "FR: Zeroshiki (Strike " + phase + ")";
            }

            livingTarget.invulnerableTime = 0; // bypass target iframe for rapid strikes
            livingTarget.hurt(player.damageSources().playerAttack(player), damage);

            if (player.level() instanceof ServerLevel sl) {
                if (phase == 9) {
                    // Finisher: big explosion effect
                    SkillParticles.burst(sl, tx, ty + 1, tz, 50, 1.5, ParticleTypes.END_ROD);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 30, 1.0, ParticleTypes.FIREWORK);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 20, 0.8, ParticleTypes.EXPLOSION);
                    SkillParticles.spiral(sl, tx, ty, tz, 2.0, 3.0, 3, 20, ParticleTypes.END_ROD);
                    SkillSounds.playAt(sl, tx, ty, tz, ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.05f, 1.0f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
                    SkillSounds.playAt(sl, tx, ty, tz, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 0.8f);
                } else {
                    // Normal strike: yellow flash
                    SkillParticles.burst(sl, tx, ty + 1, tz, 15, 0.5, ParticleTypes.END_ROD);
                    SkillParticles.burst(sl, tx, ty + 1, tz, 5, 0.3, ParticleTypes.FIREWORK);
                    SkillSounds.playAt(sl, tx, ty, tz, ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.03f, 1.0f);
                }
            }

            if (phase == 9) {
                // Combo complete — reset and start cooldown
                sd.setSsrzPhase(0);
                sd.setSsrzTargetId(-1);
                sd.setSsrzMarkTicks(0);
                sd.startCooldown(SkillType.FLYING_RAIJIN_SSRZ, level);
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76FR: Zeroshiki \u2014 FINISHER!"));
            } else {
                // Advance to next strike
                sd.setSsrzPhase(phase + 1);
                player.sendSystemMessage(Component.literal(
                        "\u00a7b[System]\u00a7r \u00a76FR: Zeroshiki \u2014 Strike " + phase + "/9"));
            }
        }
    }

    /** SSRZ ATK multiplier per strike. Scales with level and AGI. */
    public static float getSSRZMultiplier(int level, int agi) {
        if (level <= 0) return 0;
        return 1.00f + level * 0.06f + agi * 0.010f;
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
                float dmg = stats.getAttack(player) * getShurikenJutsuMultiplier(level, stats.getAgility()) * statMult;
                ItemStack visualStack = new ItemStack(ModItems.IRON_SHURIKEN.get());
                for (int i = 0; i < 3; i++) {
                    ThrownShurikenEntity shuriken = new ThrownShurikenEntity(sl, clone, visualStack);
                    shuriken.setBaseDamage(dmg);
                    shuriken.setSkillProjectile(true);
                    shuriken.shootFromRotation(clone, clone.getXRot(), clone.getYRot(), 0.0f, 2.5f, 20.0f);
                    sl.addFreshEntity(shuriken);
                }
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THROW, 0.5f, 1.5f);
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
        SkillSounds.playAt(sl, cx, player.getY(), cz, ModSounds.FLYING_RAIJIN_TELEPORT.get(), 0.02f, 1.0f);

        // Flying Raijin Lv2: if clone has Rasengan ready, deal AoE at destination
        if (clone.isRasenganReady()) {
            clone.setRasenganReady(false);
            SkillData sd = stats.getSkillData();
            int rsgLv = sd.getLevel(SkillType.RASENGAN);
            float rasenganDmg = stats.getAttack(player) * getRasenganMultiplier(rsgLv, stats.getAgility()) * statMult;

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

    /** Rasengan ATK multiplier: ATK × this value. (buffed, ~400% at max) */
    public static float getRasenganMultiplier(int level, int agi) {
        if (level <= 0) return 0;
        return 1.10f + level * 0.07f + agi * 0.012f;
    }

    /** Shuriken Jutsu ATK multiplier per shuriken: ATK × this value. (buffed, ~155% at max) */
    public static float getShurikenJutsuMultiplier(int level, int agi) {
        return 0.40f + level * 0.04f + agi * 0.006f;
    }

    /** Flying Raijin kunai ATK multiplier: ATK × this value. (buffed, ~280% at max) */
    public static float getFlyingRaijinMultiplier(int level, int agi) {
        return 1.00f + level * 0.05f + agi * 0.008f;
    }

    /** Kunai Mastery melee ATK multiplier. ATK × this = flat melee bonus. */
    public static float getKunaiMasteryMeleeMultiplier(PlayerStats stats) {
        int level = stats.getSkillData().getLevel(SkillType.KUNAI_MASTERY);
        if (level <= 0) return 0;
        return level * 0.006f + stats.getAgility() * 0.001f;
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
        return stats.getLuck() * 0.05f * level / 10.0f;
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

    /** Sage Mode damage multiplier (1.0 = no boost). Base: 2% per level (max 40% at lv20).
     *  Mastered Sage Mode: +0.4% per level (max +10% at lv25). Total cap: 50%. */
    public static float getSageModeDamageMultiplier(SkillData sd) {
        if (!sd.isToggleActive(SkillType.SAGE_MODE)) return 1.0f;
        int level = sd.getLevel(SkillType.SAGE_MODE);
        float bonus = level * 2.0f;
        int msmLv = sd.getLevel(SkillType.MASTERED_SAGE_MODE);
        if (msmLv > 0) bonus += msmLv * 0.4f;
        return 1.0f + bonus / 100.0f;
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
                float dmg = stats.getAttack(player) * getShurikenJutsuMultiplier(level, stats.getAgility()) * multiplier;
                ItemStack visualStack = new ItemStack(ModItems.IRON_SHURIKEN.get());
                for (int i = 0; i < 3; i++) {
                    ThrownShurikenEntity shuriken = new ThrownShurikenEntity(sl, partner, visualStack);
                    shuriken.setBaseDamage(dmg);
                    shuriken.setSkillProjectile(true);
                    shuriken.shootFromRotation(partner, partner.getXRot(), partner.getYRot(), 0.0f, 2.5f, 20.0f);
                    sl.addFreshEntity(shuriken);
                }
                SkillSounds.playAt(sl, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THROW, 0.5f, 1.5f);
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

    // === Stat contributions ===
    public static void registerStats() {
        // Simple contribution (KM PROJ) defined in SkillType enum
        reg(StatLine.CRIT, (sd, p, s, tags) -> {
            int km = sd.getLevel(SkillType.KUNAI_MASTERY);
            if (km <= 0) return 0;
            double val = s.getLuck() * 0.05 * km / 10.0;
            if (val > 0) tags.add(SkillType.KUNAI_MASTERY.getAbbreviation() + "+" + String.format("%.1f", val) + "%");
            return val;
        });
        reg(StatLine.DMG, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.SAGE_MODE);
            if (lv <= 0 || !sd.isToggleActive(SkillType.SAGE_MODE)) return 0;
            double val = 20 + lv;
            tags.pct(SkillType.SAGE_MODE.getAbbreviation() + "+", val);
            return val;
        });
        reg(StatLine.MP_REGEN, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.CHAKRA_CONTROL);
            if (lv <= 0) return 0;
            double val = lv * 0.15;
            tags.add(SkillType.CHAKRA_CONTROL.getAbbreviation() + "+" + String.format("%.1f", val) + "%");
            return val;
        });
    }
}
