package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class WarriorSkills {

    public static final SkillType[] ALL = {
            SkillType.SLASH_BLAST, SkillType.WAR_CRY, SkillType.WARRIOR_MASTERY,
            SkillType.SPIRIT_BLADE, SkillType.GROUND_SLAM, SkillType.FINAL_ATTACK,
            SkillType.BEAM_BLADE, SkillType.UNBREAKABLE, SkillType.BERSERKER_SPIRIT,
    };

    private WarriorSkills() {}

    // ========== SCALING HELPERS ==========

    /** Slash Blast bonus damage %: 20% at lv1, 50% at lv10 */
    public static float getSlashBlastPct(int level) {
        return 20 + (level - 1) * (30.0f / 9.0f);
    }

    /** War Cry ATK buff %: 10% at lv1, 20% at lv10 */
    public static float getWarCryAtkPct(int level) {
        return 10 + (level - 1) * (10.0f / 9.0f);
    }

    /** War Cry aggro range: 5 + VIT scaling */
    public static double getWarCryRange(int vit) {
        return 5 + vit * 0.1;
    }

    /** Warrior Mastery: +2% max HP per level */
    public static float getWarriorMasteryHpPct(int level) { return level * 0.02f; }
    /** Warrior Mastery: -1% damage taken per level */
    public static float getWarriorMasteryDmgReduction(int level) { return level * 0.01f; }
    /** Warrior Mastery: +2% knockback resist per level */
    public static float getWarriorMasteryKbResist(int level) { return level * 0.02f; }

    /** Spirit Blade flat ATK: 1 at lv1, 5 at lv15 */
    public static float getSpiritBladeAtk(int level) {
        return 1 + (level - 1) * (4.0f / 14.0f);
    }

    /** Spirit Blade self damage reduction: 5% at lv1, 20% at lv15 */
    public static float getSpiritBladeDefPct(int level) {
        return 0.05f + (level - 1) * (0.15f / 14.0f);
    }

    /** Final Attack trigger chance: 10% at lv1, ~40% at lv15 */
    public static float getFinalAttackChance(int level) {
        return 10 + (level - 1) * (30.0f / 14.0f);
    }

    /** Beam Blade damage multiplier: 100% at lv1, 160% at lv20 */
    public static float getBeamBladeMultiplier(int level) {
        return 1.0f + (level - 1) * (0.6f / 19.0f);
    }

    /** Unbreakable revive HP %: 20% + 1% per level */
    public static float getUnbreakableReviveHpPct(int level) {
        return 0.2f + level * 0.01f;
    }

    /** Berserker Spirit: +1% Final Attack chance per level */
    public static float getBerserkerFinalAttackBonus(int level) { return level * 1.0f; }

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            case SLASH_BLAST -> {
                float pct = getSlashBlastPct(level);
                texts.add("Next melee: +" + String.format("%.0f", pct) + "% damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Hits up to 3 nearby enemies");
                lines.add(new int[]{TEXT_DIM});
            }
            case WAR_CRY -> {
                float atkPct = getWarCryAtkPct(level);
                double range = getWarCryRange(stats.getVitality());
                int dur = 10 + level;
                texts.add("ATK buff: +" + String.format("%.0f", atkPct) + "% for " + dur + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Includes weapon ATK in calculation");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Aggro range: " + String.format("%.1f", range) + " blocks (VIT scales)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case WARRIOR_MASTERY -> {
                texts.add("Max HP: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Damage taken: -" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Knockback resist: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case SPIRIT_BLADE -> {
                float atk = getSpiritBladeAtk(level);
                float defPct = getSpiritBladeDefPct(level) * 100;
                texts.add("Flat ATK: +" + String.format("%.1f", atk) + " for 30s (party)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Self: -" + String.format("%.0f", defPct) + "% damage taken");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Flat ATK ignores % buffs");
                lines.add(new int[]{TEXT_DIM});
            }
            case GROUND_SLAM -> {
                double range = 3 + level * 0.15 + stats.getStrength() * 0.05;
                float dmg = 2 + level * 0.5f + stats.getStrength() * 0.1f;
                int stunDur = 1 + level / 5;
                texts.add("Damage: " + String.format("%.1f", dmg) + " (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", range) + " blocks (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Stun: " + stunDur + "s (Slowness IV)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case FINAL_ATTACK -> {
                float chance = getFinalAttackChance(level);
                int bsLv = stats.getSkillData().getLevel(SkillType.BERSERKER_SPIRIT);
                float bsBonus = getBerserkerFinalAttackBonus(bsLv);
                String total = String.format("%.0f", chance + bsBonus);
                texts.add("Trigger chance: " + total + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Repeat hit at 50% damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Works with skills and melee");
                lines.add(new int[]{TEXT_DIM});
            }
            case BEAM_BLADE -> {
                float mult = getBeamBladeMultiplier(level) * 100;
                texts.add("Damage: " + String.format("%.0f", mult) + "% ATK (STR scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Max range: 7 blocks, pierces enemies");
                lines.add(new int[]{TEXT_VALUE});
            }
            case UNBREAKABLE -> {
                float revPct = getUnbreakableReviveHpPct(level) * 100;
                texts.add("Revive at " + String.format("%.0f", revPct) + "% HP on fatal damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cleanses: Poison, Wither, Weakness, Slow");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Cooldown: 5 minutes");
                lines.add(new int[]{TEXT_VALUE});
            }
            case BERSERKER_SPIRIT -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Lifesteal: " + String.format("%.1f", level * 0.3) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Final Attack bonus: +" + String.format("%.0f", getBerserkerFinalAttackBonus(level)) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case SLASH_BLAST -> executeSlashBlast(player, stats, sd, level);
            case WAR_CRY -> executeWarCry(player, stats, sd, level);
            case SPIRIT_BLADE -> executeSpiritBlade(player, stats, sd, level);
            case GROUND_SLAM -> executeGroundSlam(player, stats, sd, level);
            case BEAM_BLADE -> executeBeamBlade(player, stats, sd, level);
            default -> {}
        }
    }

    private static void executeSlashBlast(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.SLASH_BLAST.getMpCost(level));
        float pct = getSlashBlastPct(level);
        sd.setSlashBlastActive(true);
        sd.setSlashBlastPct(pct / 100.0f);
        sd.setSlashBlastTicks(80); // 4 seconds
        SkillParticles.playerAura(player, 6, 0.5, ParticleTypes.ENCHANTED_HIT);
        sd.startCooldown(SkillType.SLASH_BLAST, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eSlash Blast ready! +" + String.format("%.0f", pct) + "% next melee."));
    }

    private static void executeWarCry(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.WAR_CRY.getMpCost(level));
        int durationTicks = (10 + level) * 20;
        float atkPct = getWarCryAtkPct(level) / 100.0f;

        // Calculate flat ATK bonus from % of current attack (base + weapon)
        float baseAtk = (stats.getStrength() - 1) * 0.1f;
        float weaponAtk = SkillExecutor.getWeaponDamage(player);
        float atkBonus = (baseAtk + weaponAtk) * atkPct;

        sd.setWarCryTicks(durationTicks);
        sd.setWarCryAtkBonus(atkBonus);

        // Pull aggro from nearby mobs
        double range = getWarCryRange(stats.getVitality());
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.setTarget(player);
        }

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1.5, player.getZ(), 15, 0.5, ParticleTypes.CRIT);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), range, 12, ParticleTypes.ENCHANTED_HIT);
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.2f);
        }
        sd.startCooldown(SkillType.WAR_CRY, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eWar Cry! ATK +" + String.format("%.1f", atkBonus) + ". " + mobs.size() + " mobs aggro'd."));
    }

    private static void executeSpiritBlade(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.SPIRIT_BLADE.getMpCost(level));
        float flatAtk = getSpiritBladeAtk(level);
        int durationTicks = 600; // 30 seconds

        // Self: flat ATK + damage reduction
        sd.setSpiritBladeTicks(durationTicks);
        sd.setSpiritBladeAtk(flatAtk);
        sd.setSpiritBladeDefActive(true);

        // Party: flat ATK only (no damage reduction)
        double range = 8;
        AABB area = player.getBoundingBox().inflate(range);
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);
        for (ServerPlayer p : nearby) {
            p.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(pStats -> {
                SkillData pSd = pStats.getSkillData();
                pSd.setSpiritBladeTicks(durationTicks);
                pSd.setSpiritBladeAtk(flatAtk);
                // No def for party members
                StatEventHandler.syncToClient(p);
            });
        }

        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 2.0, 16, ParticleTypes.ENCHANTED_HIT);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 12, 0.6, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.ANVIL_LAND, 0.4f, 1.5f);
        }
        sd.startCooldown(SkillType.SPIRIT_BLADE, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a7eSpirit Blade! +" + String.format("%.1f", flatAtk) + " ATK for 30s. " + (nearby.size() + 1) + " allies buffed."));
    }

    private static void executeGroundSlam(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.GROUND_SLAM.getMpCost(level));
        double range = 3 + level * 0.15 + stats.getStrength() * 0.05;
        float damage = 2 + level * 0.5f + stats.getStrength() * 0.1f + SkillExecutor.getWeaponDamage(player);
        int stunDuration = (1 + level / 5) * 20;
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level()), damage);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunDuration, 3, false, true));
            StatEventHandler.tryFinalAttack(player, mob, damage);
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

    private static void executeBeamBlade(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.BEAM_BLADE.getMpCost(level));
        float baseAtk = (stats.getStrength() - 1) * 0.1f + SkillExecutor.getWeaponDamage(player);
        float damage = baseAtk * getBeamBladeMultiplier(level) + stats.getStrength() * 0.15f;
        float maxDist = 7.0f;

        if (player.level() instanceof ServerLevel sl) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            double hitWidth = 0.8;

            // Find all entities along the beam
            int hits = 0;
            for (float d = 0.5f; d <= maxDist; d += 0.5f) {
                Vec3 point = eye.add(look.scale(d));
                AABB box = new AABB(point.x - hitWidth, point.y - hitWidth, point.z - hitWidth,
                        point.x + hitWidth, point.y + hitWidth, point.z + hitWidth);
                List<LivingEntity> entities = sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player));
                for (LivingEntity target : entities) {
                    if (target.invulnerableTime <= 0) {
                        target.invulnerableTime = 0;
                        target.hurt(SkillDamageSource.get(player.level()), damage);
                        target.invulnerableTime = 4; // Short iframe to prevent double-hit from same beam
                        CombatLog.damageSkill(player, "Beam Blade", damage, target);
                        StatEventHandler.tryFinalAttack(player, target, damage);
                        // Warden sonic boom effect on hit
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);
                        hits++;
                    }
                }
                // Beam trail particles
                sl.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 0, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0);
            }
            SkillSounds.playAt(player, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.2f);
            SkillSounds.playAt(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);

            sd.startCooldown(SkillType.BEAM_BLADE, level);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7eBeam Blade! " + hits + " enemies hit."));
        }
    }

    /** Called when Unbreakable passive triggers on fatal damage. */
    public static void triggerUnbreakable(ServerPlayer player, PlayerStats stats, SkillData sd) {
        int level = sd.getLevel(SkillType.UNBREAKABLE);
        float healPct = getUnbreakableReviveHpPct(level);
        float healAmt = player.getMaxHealth() * healPct;
        player.setHealth(healAmt);
        CombatLog.heal(player, "Unbreakable", healAmt);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        // Brief invulnerability (3s)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, true));
        sd.setUnbreakableCooldown(6000); // 5 minutes
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 20, 1.0, ParticleTypes.TOTEM_OF_UNDYING);
            SkillParticles.sphere(sl, player.getX(), player.getY() + 1, player.getZ(), 1.2, 15, ParticleTypes.END_ROD);
            SkillSounds.playAt(player, SoundEvents.TOTEM_USE, 0.8f, 1.0f);
        }
        player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a76Unbreakable triggered!"));
    }

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
    }

    // ========== MIRROR ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        switch (skill) {
            case GROUND_SLAM -> {
                float radius = 5 + level * 0.2f;
                float dmg = (4 + level * 1.0f + stats.getStrength() * 0.3f + SkillExecutor.getWeaponDamage(player)) * multiplier;
                List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                        partner.getBoundingBox().inflate(radius));
                for (Monster mob : mobs) {
                    mob.hurt(SkillDamageSource.get(player.level()), dmg);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + level * 4, 2, false, true));
                }
                CombatLog.aoeSkill(player, "Shadow Ground Slam", dmg, mobs);
                SkillParticles.disc(sl, pos.x, pos.y + 0.1, pos.z, radius, 25, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            }
            case BEAM_BLADE -> {
                float baseAtk = (stats.getStrength() - 1) * 0.1f + SkillExecutor.getWeaponDamage(player);
                float dmg = baseAtk * getBeamBladeMultiplier(level) * multiplier;
                Vec3 look = player.getLookAngle();
                for (float d = 0.5f; d <= 7.0f; d += 0.5f) {
                    Vec3 point = pos.add(look.scale(d));
                    List<Monster> mobs = sl.getEntitiesOfClass(Monster.class,
                            new AABB(point.x - 0.8, point.y - 0.8, point.z - 0.8,
                                    point.x + 0.8, point.y + 0.8, point.z + 0.8));
                    for (Monster mob : mobs) {
                        if (mob.invulnerableTime <= 0) {
                            mob.hurt(SkillDamageSource.get(player.level()), dmg);
                            mob.invulnerableTime = 4;
                        }
                    }
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
                CombatLog.damageSkill(player, "Shadow Beam Blade", dmg, partner);
            }
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }
}
