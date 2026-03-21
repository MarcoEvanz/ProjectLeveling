package com.monody.projectleveling.entity;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.SkillSounds;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ShadowPartnerEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ShadowPartnerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int syncEquipmentTimer = 0;
    private Vec3 returnPos = null;
    private int returnTicks = 0;

    // Pending attack (delayed 2 ticks after teleport so clients see position update)
    private LivingEntity pendingTarget = null;
    private float pendingDamage = 0;
    private int attackDelayTicks = 0;

    public ShadowPartnerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.setPersistenceRequired();
    }

    public ShadowPartnerEntity(Level level, ServerPlayer owner) {
        super(ModEntities.SHADOW_PARTNER.get(), level);
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        this.setNoGravity(false);
        this.setPersistenceRequired();
        copyEquipmentFrom(owner);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
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

    public void copyEquipmentFrom(Player owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack ownerItem = owner.getItemBySlot(slot);
            this.setItemSlot(slot, ownerItem.copy());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        ServerPlayer owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            despawnWithSmoke();
            return;
        }

        // Check if Shadow Partner toggle is still active + sync stealth visibility
        owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillData sd = stats.getSkillData();
            if (!sd.isToggleActive(SkillType.SHADOW_PARTNER)) {
                despawnWithSmoke();
                return;
            }
            // Hide Shadow Partner when owner is stealthed
            this.setInvisible(sd.isToggleActive(SkillType.STEALTH));
        });

        if (isRemoved()) return;

        // Handle pending attack (delayed after teleport so client sees position)
        if (attackDelayTicks > 0) {
            attackDelayTicks--;
            if (attackDelayTicks <= 0 && pendingTarget != null && pendingTarget.isAlive()) {
                // Face target
                double dx = pendingTarget.getX() - getX();
                double dz = pendingTarget.getZ() - getZ();
                float yRot = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                this.setYRot(yRot);
                this.yHeadRot = yRot;
                // Swing + deal damage
                this.swing(InteractionHand.MAIN_HAND);
                pendingTarget.invulnerableTime = 0;
                pendingTarget.hurt(this.damageSources().mobAttack(this), pendingDamage);
                if (level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, pendingTarget.getX(), pendingTarget.getY() + 1, pendingTarget.getZ(), 6, 0.3, ParticleTypes.PORTAL);
                    SkillSounds.playAt(sl, pendingTarget.getX(), pendingTarget.getY(), pendingTarget.getZ(),
                            SoundEvents.PLAYER_ATTACK_STRONG, 0.6f, 0.8f);
                }
                // Combat log (post-armor)
                if (owner != null) {
                    float finalDmg = CombatLog.afterArmor(pendingTarget, this.damageSources().mobAttack(this), pendingDamage);
                    CombatLog.pet(owner, finalDmg, pendingTarget);
                }
                pendingTarget = null;
            }
            return; // Skip follow logic while attacking
        }

        // Handle teleport-return after mirror attack
        if (returnTicks > 0) {
            returnTicks--;
            if (returnTicks <= 0 && returnPos != null) {
                if (level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 5, 0.3, ParticleTypes.SMOKE);
                }
                this.teleportTo(returnPos.x, returnPos.y, returnPos.z);
                if (level() instanceof ServerLevel sl) {
                    SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 5, 0.3, ParticleTypes.SMOKE);
                }
                returnPos = null;
            }
            return; // Skip follow logic while returning
        }

        // Follow owner
        double dist = distanceTo(owner);
        if (dist > 16.0) {
            // Teleport if too far
            Vec3 behind = owner.position().subtract(owner.getLookAngle().scale(2.0));
            this.teleportTo(behind.x, behind.y, behind.z);
        } else if (dist > 3.0) {
            // Navigate toward owner
            this.getNavigation().moveTo(owner, 1.2);
        } else {
            this.getNavigation().stop();
            // Face same direction as owner
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.yHeadRot = owner.yHeadRot;
        }

        // Sync equipment every 2 seconds
        syncEquipmentTimer++;
        if (syncEquipmentTimer >= 40) {
            syncEquipmentTimer = 0;
            copyEquipmentFrom(owner);
        }

        // Ambient particles every 2 seconds (suppressed when invisible/stealthed)
        if (!this.isInvisible() && tickCount % 40 == 0 && level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 3, 0.3, ParticleTypes.SMOKE);
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
            // Shadow Partner was killed — apply 10s cooldown to owner
            ServerPlayer owner = getOwnerPlayer();
            if (owner != null) {
                owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                    SkillData sd = stats.getSkillData();
                    sd.setToggleActive(SkillType.SHADOW_PARTNER, false);
                    sd.setCooldown(SkillType.SHADOW_PARTNER, 200); // 10 seconds
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00a7b[System]\u00a7r \u00a7cShadow Partner was killed! 10s cooldown."));
                    com.monody.projectleveling.event.StatEventHandler.syncToClient(owner);
                });
            }
            despawnWithSmoke();
        }

        return result;
    }

    /** Teleport to target, then swing + deal damage after a short delay (so clients see the teleport). */
    public void performMirrorAttack(LivingEntity target, float damage) {
        if (level().isClientSide()) return;

        // Compute return position (behind owner)
        ServerPlayer owner = getOwnerPlayer();
        this.returnPos = owner != null
                ? owner.position().subtract(owner.getLookAngle().scale(2.0))
                : this.position();
        this.returnTicks = 7; // Return after attack delay (2) + linger (5)

        // Smoke at departure
        if (level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 5, 0.3, ParticleTypes.SMOKE);
        }

        // Teleport behind target
        Vec3 dir = target.position().subtract(this.position());
        if (dir.lengthSqr() > 0.01) {
            dir = dir.normalize();
        } else {
            dir = target.getLookAngle().scale(-1);
        }
        Vec3 attackPos = target.position().subtract(dir.scale(1.5));
        this.teleportTo(attackPos.x, target.getY(), attackPos.z);

        // Queue the actual swing + damage for 2 ticks later
        this.pendingTarget = target;
        this.pendingDamage = damage;
        this.attackDelayTicks = 2;
    }

    public void despawnWithSmoke() {
        if (level() instanceof ServerLevel sl) {
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 20, 0.6, ParticleTypes.SMOKE);
            SkillParticles.burst(sl, getX(), getY() + 1, getZ(), 10, 0.4, ParticleTypes.POOF);
            SkillSounds.playAt(sl, getX(), getY(), getZ(),
                    SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.5f, 0.7f);
        }
        this.discard();
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
    }
}
