package com.monody.projectleveling.entity.necromancer;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillDamageSource;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.SkillSounds;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SkeletonMinionEntity extends PathfinderMob implements RangedAttackMob {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(SkeletonMinionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private boolean armyMinion = false;
    private boolean exploding = false;
    private int explodeTargetId = -1;
    private int explodeTicks = 0;
    private float explodeDamage = 0;

    public SkeletonMinionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.setPersistenceRequired();
    }

    public SkeletonMinionEntity(Level level, ServerPlayer owner, float maxHp, float damage, int skillLevel) {
        super(ModEntities.SKELETON_MINION.get(), level);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        this.setNoGravity(false);
        this.setPersistenceRequired();

        // Set scaled HP and damage
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
        this.setHealth(maxHp);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);

        // Level-based weapon
        this.setItemSlot(EquipmentSlot.MAINHAND, getWeaponForLevel(skillLevel));
    }

    public static ItemStack getWeaponForLevel(int level) {
        if (level >= 9) return new ItemStack(Items.DIAMOND_SWORD);
        if (level >= 7) return new ItemStack(Items.IRON_SWORD);
        if (level >= 5) return new ItemStack(Items.IRON_SWORD);
        if (level >= 3) return new ItemStack(Items.STONE_SWORD);
        return new ItemStack(Items.WOODEN_SWORD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // Melee-only minion, no ranged attack
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    @Nullable
    public ServerPlayer getOwnerPlayer() {
        UUID uuid = getOwnerUUID();
        if (uuid == null) return null;
        if (level() instanceof ServerLevel sl) {
            Player p = sl.getPlayerByUUID(uuid);
            if (p instanceof ServerPlayer sp) return sp;
        }
        return null;
    }

    // === Army minion flag ===

    public boolean isArmyMinion() { return armyMinion; }
    public void setArmyMinion(boolean army) { this.armyMinion = army; }
    public boolean isExploding() { return exploding; }

    // === Corpse Explosion: rush-then-explode ===

    public void startExploding(int targetEntityId, float damage) {
        this.exploding = true;
        this.explodeTargetId = targetEntityId;
        this.explodeTicks = 60; // 3 seconds max
        this.explodeDamage = damage;
        this.setTarget(null);
        // Speed boost for rushing
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.5);
    }

    public void doExplosion() {
        if (level().isClientSide()) return;
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            despawnWithEffect();
            return;
        }

        if (level() instanceof ServerLevel sl) {
            float range = 4.0f;
            AABB area = this.getBoundingBox().inflate(range);
            List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area);
            for (Monster mob : mobs) {
                mob.hurt(SkillDamageSource.get(this.level()), explodeDamage);
            }
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                CombatLog.aoeSkill(owner, "Corpse Explosion", explodeDamage, mobs);
            });

            // Big explosion effects
            SkillParticles.disc(sl, getX(), getY() + 0.5, getZ(), range, 20, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 15, 1.0, ParticleTypes.EXPLOSION);
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 10, 0.6, ParticleTypes.SMOKE);
            SkillSounds.playAt(sl, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, 0.8f, 0.9f);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        ServerPlayer owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            despawnWithEffect();
            return;
        }

        // Exploding mode: rush to target then explode
        if (exploding) {
            explodeTicks--;
            Entity target = level().getEntity(explodeTargetId);
            if (target != null && target.isAlive()) {
                this.getNavigation().moveTo(target, 1.8);
                if (this.distanceTo(target) < 2.5) {
                    doExplosion();
                    return;
                }
            }
            if (explodeTicks <= 0) {
                doExplosion();
            }
            return; // skip normal logic while exploding
        }

        // Army minions: check duration
        if (armyMinion) {
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                if (sd.getArmyTicks() <= 0) {
                    despawnWithEffect();
                }
            });
            if (isRemoved()) return;
        } else {
            // Raise Skeleton: check if toggle is still active
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                if (!sd.isToggleActive(SkillType.RAISE_SKELETON)) {
                    despawnWithEffect();
                }
            });
        }

        if (isRemoved()) return;

        // Unholy Fervor: speed boost while active
        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (sd.getFervorTicks() > 0) {
                if (this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() < 0.4) {
                    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
                }
                if (tickCount % 40 == 0 && level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 3, 0.3, ParticleTypes.ENCHANT);
                }
            } else {
                if (this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() > 0.3) {
                    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
                }
            }
        });

        // Follow owner if no target and far away
        if (getTarget() == null || !getTarget().isAlive()) {
            double dist = distanceTo(owner);
            if (dist > 16.0) {
                Vec3 behind = owner.position().subtract(owner.getLookAngle().scale(2.0));
                this.teleportTo(behind.x, behind.y, behind.z);
            } else if (dist > 6.0) {
                this.getNavigation().moveTo(owner, 1.2);
            }
        }

        // Ambient particles every 2 seconds
        if (tickCount % 40 == 0 && level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 2, 0.3, ParticleTypes.SOUL_FIRE_FLAME);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Immune to owner's damage
        if (source.getEntity() instanceof Player p) {
            UUID ownerUUID = getOwnerUUID();
            if (ownerUUID != null && ownerUUID.equals(p.getUUID())) {
                return false;
            }
        }

        boolean result = super.hurt(source, amount);

        if (this.isDeadOrDying() && !level().isClientSide()) {
            ServerPlayer owner = getOwnerPlayer();
            if (owner != null && !armyMinion) {
                // Only raise skeleton (not army minions) triggers cooldown
                owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                    SkillData sd = stats.getSkillData();
                    sd.setToggleActive(SkillType.RAISE_SKELETON, false);
                    sd.setCooldown(SkillType.RAISE_SKELETON, 200); // 10 seconds
                    owner.sendSystemMessage(Component.literal(
                            "\u00a7b[System]\u00a7r \u00a7cSkeleton minion was killed! 10s cooldown."));
                    com.monody.projectleveling.event.StatEventHandler.syncToClient(owner);
                });
            }
            despawnWithEffect();
        }

        return result;
    }

    public void despawnWithEffect() {
        if (level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 15, 0.6, ParticleTypes.SOUL_FIRE_FLAME);
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 10, 0.4, ParticleTypes.SMOKE);
            SkillSounds.playAt(sl, getX(), getY(), getZ(),
                    SoundEvents.SKELETON_DEATH, 0.6f, 0.8f);
        }
        this.discard();
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        if (other instanceof Player p) {
            UUID ownerUUID = getOwnerUUID();
            return ownerUUID != null && ownerUUID.equals(p.getUUID());
        }
        // Allied to other skeleton minions of the same owner
        if (other instanceof SkeletonMinionEntity otherMinion) {
            UUID myOwner = getOwnerUUID();
            return myOwner != null && myOwner.equals(otherMinion.getOwnerUUID());
        }
        return super.isAlliedTo(other);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean removeWhenFarAway(double distToPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public int getExperienceReward() {
        return 0;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putBoolean("ArmyMinion", armyMinion);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        this.armyMinion = tag.getBoolean("ArmyMinion");
    }
}
