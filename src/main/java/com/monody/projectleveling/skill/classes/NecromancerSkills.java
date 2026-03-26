package com.monody.projectleveling.skill.classes;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.necromancer.SkeletonMinionEntity;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.monody.projectleveling.skill.SkillTooltips.TEXT_DIM;
import static com.monody.projectleveling.skill.SkillTooltips.TEXT_VALUE;

public final class NecromancerSkills {

    public static final SkillType[] ALL = {
            SkillType.LIFE_DRAIN, SkillType.RAISE_SKELETON, SkillType.DARK_PACT, SkillType.UNHOLY_FERVOR,
            SkillType.BONE_SHIELD, SkillType.CORPSE_EXPLOSION, SkillType.SOUL_SIPHON, SkillType.SKELETAL_MASTERY,
            SkillType.ARMY_OF_THE_DEAD, SkillType.DEATH_MARK, SkillType.UNDYING_WILL, SkillType.SOUL_LINK,
            SkillType.ENHANCE_UNDEAD, SkillType.DARK_RESONANCE,
            SkillType.NIGHT_OF_THE_LIVING_DEAD, SkillType.UNDEAD_ARMAMENT,
    };

    private NecromancerSkills() {}

    // ========== MATK MULTIPLIER HELPERS ==========

    /** Life Drain: MATK × this value. (~35% reduced) */
    public static float getLifeDrainMultiplier(int level, int intel) { return 1.00f + level * 0.05f + intel * 0.008f; }
    /** Corpse Explosion: MATK × this value. (~35% reduced) */
    public static float getCorpseExplosionMultiplier(int level, int intel) { return 2.00f + level * 0.10f + intel * 0.013f; }
    /** Death Mark DoT: MATK × this value per tick. (~35% reduced) */
    public static float getDeathMarkDotMultiplier(int level, int intel) { return 0.40f + level * 0.025f + intel * 0.004f; }
    /** Death Mark explode: MATK × this value. (~35% reduced) */
    public static float getDeathMarkExplodeMultiplier(int level, int intel) { return 1.30f + level * 0.065f + intel * 0.010f; }

    // ========== TOOLTIPS ==========

    public static void addDetailLines(List<String> texts, List<int[]> lines,
                                       SkillType skill, int level, PlayerStats stats) {
        switch (skill) {
            // === T1 ===
            case LIFE_DRAIN -> {
                float ldRange = 5 + level * 0.3f;
                float ldMult = getLifeDrainMultiplier(level, stats.getIntelligence()) * 100;
                float ldHeal = 20 + level * 1.6f;
                texts.add("Damage: MATK x " + String.format("%.0f", ldMult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Range: " + String.format("%.1f", ldRange) + " blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Heal: " + String.format("%.0f", ldHeal) + "% of damage (Mind scales)");
                lines.add(new int[]{TEXT_VALUE});
            }
            case RAISE_SKELETON -> {
                float mDmg = 3 + stats.getMind() * 0.2f + stats.getIntelligence() * 0.1f + stats.getMagicAttack();
                float mHp = 20 + stats.getMind() * 0.5f;
                String weapon = level >= 9 ? "Diamond" : level >= 7 ? "Iron" : level >= 5 ? "Iron" : level >= 3 ? "Stone" : "Wooden";
                texts.add("Minion HP: " + String.format("%.0f", mHp) + " (Mind scales)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion DMG: " + String.format("%.1f", mDmg) + " (Mind+INT+MATK)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Weapon: " + weapon + " Sword");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", SkillType.RAISE_SKELETON.getToggleDrainPercent(level)) + "% MP/s");
                lines.add(new int[]{TEXT_VALUE});
            }
            case DARK_PACT -> {
                texts.add("Max MP: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Summon damage: +" + String.format("%.1f", level * 1.5f) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case UNHOLY_FERVOR -> {
                float ufDmgPct = 15 + level * 1.5f;
                float ufDur = 8 + level * 0.4f;
                texts.add("Minion speed: 0.3 \u2192 0.4");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion damage: +" + String.format("%.1f", ufDmgPct) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", ufDur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Affects ALL skeletons (Raise + Army)");
                lines.add(new int[]{TEXT_DIM});
            }

            // === T2 ===
            case BONE_SHIELD -> {
                int reduction = Math.min(8 + level, 20);
                texts.add("Damage reduction: " + reduction + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", SkillType.BONE_SHIELD.getToggleDrainPercent(level)) + "% MP/s");
                lines.add(new int[]{TEXT_VALUE});
            }
            case CORPSE_EXPLOSION -> {
                float ceMult = getCorpseExplosionMultiplier(level, stats.getIntelligence()) * 100;
                texts.add("Dmg/skeleton: MATK x " + String.format("%.0f", ceMult) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Blast radius: 4 blocks per skeleton");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Detonates ALL skeletons (Raise + Army)");
                lines.add(new int[]{TEXT_DIM});
                texts.add("Skeletons rush to target, then explode");
                lines.add(new int[]{TEXT_DIM});
            }
            case SOUL_SIPHON -> {
                texts.add("HP restore: " + String.format("%.1f", 0.5f + level * 0.15f) + "% on kill");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP restore: " + String.format("%.1f", 1 + level * 0.2f) + "% on kill");
                lines.add(new int[]{TEXT_VALUE});
            }
            case SKELETAL_MASTERY -> {
                float smLifesteal = 0.5f + level * 0.2f;
                float smReduction = 3 + level * 0.3f;
                texts.add("Minion lifesteal: " + String.format("%.1f", smLifesteal) + "% of damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion damage reduction: " + String.format("%.1f", smReduction) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }

            // === T3 ===
            case ARMY_OF_THE_DEAD -> {
                float mDmg = (3 + stats.getMind() * 0.2f + stats.getIntelligence() * 0.1f + stats.getMagicAttack()) * 0.5f;
                float mHp = (20 + stats.getMind() * 0.5f) * 0.5f;
                float adDur = 10 + level * 0.5f;
                texts.add("Summons: 5 skeleton minions");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion HP: " + String.format("%.0f", mHp) + " (50% of Raise)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion DMG: " + String.format("%.1f", mDmg) + " (50% of Raise)");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", adDur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Corpse Explosion detonates these too");
                lines.add(new int[]{TEXT_DIM});
            }
            case DEATH_MARK -> {
                float dotMult = getDeathMarkDotMultiplier(level, stats.getIntelligence()) * 100;
                float dmDur = 10 + level * 0.5f;
                texts.add("DoT: MATK x " + String.format("%.0f", dotMult) + "%/s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: " + String.format("%.1f", dmDur) + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("On target death: AoE + restore " + String.format("%.1f", 3 + level * 0.3f) + "% HP/MP");
                lines.add(new int[]{TEXT_DIM});
            }
            case UNDYING_WILL -> {
                int revPct = 10 + level;
                int cdSec = Math.max(3600 - level * 90, 1800) / 20;
                texts.add("Revive at: " + revPct + "% HP on fatal damage");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Cooldown: " + cdSec + "s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion max HP: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
            }
            case SOUL_LINK -> {
                float slRedirect = 10 + level * 0.5f;
                float slBonusDmg = 8 + level * 0.5f;
                texts.add("Damage redirect: " + String.format("%.1f", slRedirect) + "% to nearest minion");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion bonus damage: +" + String.format("%.1f", slBonusDmg) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("MP drain: " + String.format("%.1f", SkillType.SOUL_LINK.getToggleDrainPercent(level)) + "% MP/s");
                lines.add(new int[]{TEXT_VALUE});
            }
            case ENHANCE_UNDEAD -> {
                texts.add("Summon damage: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                if (level >= 20) {
                    texts.add("Summons Wither Skeletons (Wither on hit)");
                    lines.add(new int[]{TEXT_VALUE});
                } else {
                    texts.add("At max level: summon Wither Skeletons");
                    lines.add(new int[]{TEXT_DIM});
                }
            }
            case DARK_RESONANCE -> {
                texts.add("Crit rate: +" + level + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crit damage: +" + (level * 2) + "%");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Crits apply Wither I for 2s");
                lines.add(new int[]{TEXT_VALUE});
            }
            case NIGHT_OF_THE_LIVING_DEAD -> {
                texts.add("Domain radius: 20 blocks");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Duration: 30s");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Minion speed: x2 inside domain");
                lines.add(new int[]{TEXT_VALUE});
                texts.add("You and minions cannot die inside domain");
                lines.add(new int[]{TEXT_VALUE});
            }
            case UNDEAD_ARMAMENT -> {
                String material = level >= 20 ? "Diamond" : level >= 13 ? "Iron" : level >= 9 ? "Chainmail" : "Leather";
                String pieces = level >= 13 ? "Full set" : level >= 9 ? "Boots + Legs + Chest" : level >= 5 ? "Boots + Legs" : "Boots";
                texts.add("Armor: " + material);
                lines.add(new int[]{TEXT_VALUE});
                texts.add("Pieces: " + pieces);
                lines.add(new int[]{TEXT_VALUE});
                if (level < 20) {
                    texts.add("Lv5: +Legs | Lv9: +Chest (Chain) | Lv13: +Helm (Iron) | Lv20: Diamond");
                    lines.add(new int[]{TEXT_DIM});
                }
            }
            default -> {}
        }
    }

    // ========== EXECUTION ==========

    public static void execute(ServerPlayer player, PlayerStats stats,
                               SkillData sd, SkillType skill, int level) {
        switch (skill) {
            case LIFE_DRAIN -> executeLifeDrain(player, stats, sd, level);
            case RAISE_SKELETON -> executeRaiseSkeleton(player, stats, sd, level);
            case BONE_SHIELD -> executeBoneShield(player, stats, sd, level);
            case CORPSE_EXPLOSION -> executeCorpseExplosion(player, stats, sd, level);
            case ARMY_OF_THE_DEAD -> executeArmyOfTheDead(player, stats, sd, level);
            case DEATH_MARK -> executeDeathMark(player, stats, sd, level);
            case UNHOLY_FERVOR -> executeUnholyFervor(player, stats, sd, level);
            case SOUL_LINK -> executeSoulLink(player, stats, sd, level);
            case NIGHT_OF_THE_LIVING_DEAD -> executeNightDomain(player, stats, sd, level);
            default -> {}
        }
    }

    private static void executeLifeDrain(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.LIFE_DRAIN.getMpCost(level));
        float range = 5 + level * 0.3f;
        float damage = stats.getMagicAttack(player) * getLifeDrainMultiplier(level, stats.getIntelligence());
        float healPct = 0.2f + level * 0.016f; // 20-36%
        AABB area = player.getBoundingBox().inflate(range);
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class, area);
        float totalDmg = 0;
        for (Monster mob : mobs) {
            mob.hurt(SkillDamageSource.get(player.level(), player), damage);
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
        if (stats.getCurrentMp() < SkillType.RAISE_SKELETON.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        int mindStat = stats.getMind();
        int dpLv = sd.getLevel(SkillType.DARK_PACT);
        int uwLv = sd.getLevel(SkillType.UNDYING_WILL);
        int euLv = sd.getLevel(SkillType.ENHANCE_UNDEAD);
        float minionDmg = 3 + mindStat * 0.2f + stats.getIntelligence() * 0.1f + stats.getMagicAttack(player);
        minionDmg *= (1 + dpLv * 0.015f);
        if (euLv > 0) minionDmg *= (1 + euLv * 0.01f);
        float minionHp = 20 + mindStat * 0.5f;
        minionHp *= (1 + uwLv * 0.02f);

        if (player.level() instanceof ServerLevel sl) {
            // Despawn only non-army minions (the raised one)
            despawnRaisedSkeleton(player, sl);
            SkeletonMinionEntity minion = new SkeletonMinionEntity(sl, player, minionHp, minionDmg, level);
            if (euLv >= 20) minion.setWitherVariant(true);
            int uaLv = sd.getLevel(SkillType.UNDEAD_ARMAMENT);
            if (uaLv > 0) equipMinionArmor(minion, uaLv);
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
        if (stats.getCurrentMp() < SkillType.BONE_SHIELD.getToggleMpPerSecond(level, stats.getMaxMp())) {
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

    /** Get the damage reduction % for Bone Shield (8-20%). */
    public static int getBoneShieldReduction(int level) {
        return Math.min(8 + level, 20);
    }

    private static void executeCorpseExplosion(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        List<SkeletonMinionEntity> skeletons = findAllSkeletonMinions(player, sl);
        if (skeletons.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7cNo skeletons to detonate!"));
            return;
        }

        stats.setCurrentMp(stats.getCurrentMp() - SkillType.CORPSE_EXPLOSION.getMpCost(level));
        float damage = stats.getMagicAttack(player) * getCorpseExplosionMultiplier(level, stats.getIntelligence());

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
        int euLv = sd.getLevel(SkillType.ENHANCE_UNDEAD);
        float minionDmg = (3 + mindStat * 0.2f + stats.getIntelligence() * 0.1f + stats.getMagicAttack(player)) * 0.5f; // 50% of raise skeleton
        minionDmg *= (1 + dpLv * 0.015f);
        if (euLv > 0) minionDmg *= (1 + euLv * 0.01f);
        float minionHp = (20 + mindStat * 0.5f) * 0.5f;
        minionHp *= (1 + uwLv * 0.02f);
        boolean wither = euLv >= 20;
        int uaLv = sd.getLevel(SkillType.UNDEAD_ARMAMENT);

        // Spawn 5 skeletons in a circle around the player
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI / 5) * i;
            double ox = Math.cos(angle) * 3.0;
            double oz = Math.sin(angle) * 3.0;

            SkeletonMinionEntity minion = new SkeletonMinionEntity(sl, player, minionHp, minionDmg, level);
            minion.setArmyMinion(true);
            if (wither) minion.setWitherVariant(true);
            if (uaLv > 0) equipMinionArmor(minion, uaLv);
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
        float dotDmg = stats.getMagicAttack(player) * getDeathMarkDotMultiplier(level, stats.getIntelligence());
        living.hurt(SkillDamageSource.get(player.level(), player), dotDmg);
        CombatLog.damageSkill(player, "Death Mark", dotDmg, living);
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
        float aoeDmg = stats.getMagicAttack(player) * getDeathMarkExplodeMultiplier(level, stats.getIntelligence());
        if (player.level() instanceof ServerLevel sl) {
            double dx = deadEntity.getX(), dy = deadEntity.getY(), dz = deadEntity.getZ();
            AABB area = deadEntity.getBoundingBox().inflate(aoeRange);
            List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area, e -> e != deadEntity);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(player.level(), player), aoeDmg);
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
        float hpRestore = player.getMaxHealth() * (3 + level * 0.3f) / 100f;
        int mpRestore = (int) (stats.getMaxMp() * (3 + level * 0.3f) / 100f);
        player.heal(hpRestore);
        CombatLog.heal(player, "Death Mark", hpRestore);
        stats.setCurrentMp(Math.min(stats.getCurrentMp() + mpRestore, stats.getMaxMp()));
        sd.setDeathMarkTicks(0);
        sd.setDeathMarkTargetId(-1);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Death Mark detonated!"));
    }

    private static void executeUnholyFervor(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.UNHOLY_FERVOR.getMpCost(level));
        int duration = (int) ((8 + level * 0.4) * 20);
        sd.setFervorTicks(duration);

        if (player.level() instanceof ServerLevel sl) {
            List<SkeletonMinionEntity> minions = findAllSkeletonMinions(player, sl);
            for (SkeletonMinionEntity minion : minions) {
                minion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
            }
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(), 15, 0.6, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(), 3.0, 12, ParticleTypes.ENCHANT);
            SkillSounds.playAt(player, SoundEvents.EVOKER_PREPARE_ATTACK, 0.7f, 1.2f);
        }

        sd.startCooldown(SkillType.UNHOLY_FERVOR, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Unholy Fervor! Minions empowered!"));
    }

    private static void executeSoulLink(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        if (stats.getCurrentMp() < SkillType.SOUL_LINK.getToggleMpPerSecond(level, stats.getMaxMp())) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough MP!"));
            return;
        }
        sd.setToggleActive(SkillType.SOUL_LINK, true);
        if (player.level() instanceof ServerLevel sl) {
            SkillParticles.ring(sl, player.getX(), player.getY() + 1, player.getZ(), 1.5, 10, ParticleTypes.SOUL);
            SkillSounds.playAt(player, SoundEvents.BEACON_ACTIVATE, 0.5f, 1.5f);
        }
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a75Soul Link activated. Damage shared with minions."));
    }

    // ========== NIGHT OF THE LIVING DEAD (Domain) ==========

    public static final int NIGHT_DOMAIN_RADIUS = 20;
    public static final int NIGHT_DOMAIN_TICKS = 600; // 30 seconds

    private static void executeNightDomain(ServerPlayer player, PlayerStats stats, SkillData sd, int level) {
        stats.setCurrentMp(stats.getCurrentMp() - SkillType.NIGHT_OF_THE_LIVING_DEAD.getMpCost(level));
        sd.setNightDomainTicks(NIGHT_DOMAIN_TICKS);
        sd.setNightDomainPos(player.getX(), player.getY(), player.getZ());

        if (player.level() instanceof ServerLevel sl) {
            // Domain activation visuals
            SkillParticles.disc(sl, player.getX(), player.getY() + 0.2, player.getZ(),
                    NIGHT_DOMAIN_RADIUS, 60, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.ring(sl, player.getX(), player.getY() + 0.1, player.getZ(),
                    NIGHT_DOMAIN_RADIUS, 40, ParticleTypes.SOUL);
            SkillParticles.burst(sl, player.getX(), player.getY() + 1, player.getZ(),
                    30, 1.5, ParticleTypes.SOUL_FIRE_FLAME);
            SkillSounds.playAt(player, SoundEvents.WITHER_SPAWN, 1.0f, 0.6f);
            SkillSounds.playAt(player, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 0.5f);
        }

        sd.startCooldown(SkillType.NIGHT_OF_THE_LIVING_DEAD, level);
        player.sendSystemMessage(Component.literal(
                "\u00a7b[System]\u00a7r \u00a74Night of The Living Dead! Domain deployed."));
    }

    /** Called every tick while the domain is active. Handles visuals. */
    public static void tickNightDomain(ServerPlayer player, SkillData sd) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        // Ambient particles along domain edge every 10 ticks
        if (player.tickCount % 10 == 0) {
            double cx = sd.getNightDomainX();
            double cy = sd.getNightDomainY();
            double cz = sd.getNightDomainZ();
            SkillParticles.ring(sl, cx, cy + 0.1, cz, NIGHT_DOMAIN_RADIUS, 16, ParticleTypes.SOUL);
        }

        // Periodic ground particles every 20 ticks
        if (player.tickCount % 20 == 0) {
            double cx = sd.getNightDomainX();
            double cy = sd.getNightDomainY();
            double cz = sd.getNightDomainZ();
            SkillParticles.disc(sl, cx, cy + 0.1, cz, NIGHT_DOMAIN_RADIUS, 12, ParticleTypes.SOUL_FIRE_FLAME);
        }
    }

    /** Check if a position is inside the active Night domain. */
    public static boolean isInsideNightDomain(SkillData sd, double x, double y, double z) {
        if (sd.getNightDomainTicks() <= 0) return false;
        double dx = x - sd.getNightDomainX();
        double dy = y - sd.getNightDomainY();
        double dz = z - sd.getNightDomainZ();
        return (dx * dx + dz * dz) <= NIGHT_DOMAIN_RADIUS * NIGHT_DOMAIN_RADIUS
                && Math.abs(dy) <= 10; // vertical tolerance
    }

    // ========== HELPER METHODS ==========

    public static float getUnholyFervorDamageBonus(int level) {
        return (15 + level * 1.5f) / 100.0f;
    }

    public static float getSoulLinkRedirectRatio(int level) {
        return (10 + level * 0.5f) / 100.0f;
    }

    public static float getSoulLinkMinionDamageBonus(int level) {
        return (8 + level * 0.5f) / 100.0f;
    }

    public static float getSkeletalMasteryLifesteal(int level) {
        return (0.5f + level * 0.2f) / 100.0f;
    }

    public static float getSkeletalMasteryDamageReduction(int level) {
        return (3 + level * 0.3f) / 100.0f;
    }

    /** Equip armor pieces on a skeleton minion based on Undead Armament level. */
    public static void equipMinionArmor(SkeletonMinionEntity minion, int level) {
        if (level <= 0) return;

        // Determine material tier
        boolean diamond = level >= 20;
        boolean iron = level >= 13;
        boolean chain = level >= 9;

        // Boots (level 1+)
        minion.setItemSlot(EquipmentSlot.FEET, new ItemStack(
                diamond ? Items.DIAMOND_BOOTS : iron ? Items.IRON_BOOTS :
                chain ? Items.CHAINMAIL_BOOTS : Items.LEATHER_BOOTS));

        // Leggings (level 5+)
        if (level >= 5) {
            minion.setItemSlot(EquipmentSlot.LEGS, new ItemStack(
                    diamond ? Items.DIAMOND_LEGGINGS : iron ? Items.IRON_LEGGINGS :
                    chain ? Items.CHAINMAIL_LEGGINGS : Items.LEATHER_LEGGINGS));
        }

        // Chestplate (level 9+)
        if (level >= 9) {
            minion.setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                    diamond ? Items.DIAMOND_CHESTPLATE : iron ? Items.IRON_CHESTPLATE :
                    Items.CHAINMAIL_CHESTPLATE));
        }

        // Helmet (level 13+)
        if (level >= 13) {
            minion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                    diamond ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
        }
    }

    public static SkeletonMinionEntity findNearestMinion(ServerPlayer player, ServerLevel level) {
        List<SkeletonMinionEntity> minions = findAllSkeletonMinions(player, level);
        SkeletonMinionEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (SkeletonMinionEntity m : minions) {
            double dist = m.distanceTo(player);
            if (dist < minDist) {
                minDist = dist;
                nearest = m;
            }
        }
        return nearest;
    }

    // ========== ENTITY UTILITIES ==========

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

    // ========== TOGGLE DEACTIVATION ==========

    public static void deactivateToggle(ServerPlayer player, SkillData sd, SkillType skill) {
        sd.setToggleActive(skill, false);
        switch (skill) {
            case RAISE_SKELETON -> {
                if (player.level() instanceof ServerLevel sl) {
                    despawnSkeletonMinion(player, sl);
                }
                player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Raise Skeleton deactivated."));
            }
            case BONE_SHIELD ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Bone Shield deactivated."));
            case SOUL_LINK ->
                    player.sendSystemMessage(Component.literal("\u00a7b[System]\u00a7r \u00a77Soul Link deactivated."));
            default -> player.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a77" + skill.getDisplayName() + " deactivated."));
        }
    }

    // ========== MIRROR ==========

    public static void mirrorSkill(ServerLevel sl, ShadowPartnerEntity partner,
                                   ServerPlayer player, PlayerStats stats,
                                   SkillType skill, int level, float multiplier) {
        Vec3 pos = partner.position();
        switch (skill) {
            default -> {
                SkillParticles.burst(sl, pos.x, pos.y + 1, pos.z, 8, 0.4, ParticleTypes.PORTAL);
                partner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }

    // === Stat contributions ===
    public static void registerStats() {
        reg(StatLine.DMG, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.UNHOLY_FERVOR);
            if (lv <= 0) return 0;
            double val = 15.0 + lv * 1.5;
            tags.add(SkillType.UNHOLY_FERVOR.getAbbreviation() + "+" + String.format("%.1f", val) + "%");
            return val;
        });
        reg(StatLine.DMG_RED, (sd, p, s, tags) -> {
            int lv = sd.getLevel(SkillType.BONE_SHIELD);
            if (!sd.isToggleActive(SkillType.BONE_SHIELD) || lv <= 0) return 0;
            int val = getBoneShieldReduction(lv);
            tags.pct(SkillType.BONE_SHIELD.getAbbreviation(), val);
            return val;
        });
    }
}
