package com.monody.projectleveling.entity.ninja;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.item.ModItems;
import com.monody.projectleveling.skill.CombatLog;
import com.monody.projectleveling.skill.SkillDamageSource;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillParticles;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class FlyingRaijinKunaiEntity extends AbstractArrow implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(FlyingRaijinKunaiEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(FlyingRaijinKunaiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GROUND_PLACED =
            SynchedEntityData.defineId(FlyingRaijinKunaiEntity.class, EntityDataSerializers.BOOLEAN);

    private int groundLife = 0;
    private float kunaiDamage = 3.0f;

    // Required for entity registration (client-side spawn)
    public FlyingRaijinKunaiEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    // Server spawn constructor
    public FlyingRaijinKunaiEntity(Level level, LivingEntity owner, float damage, ItemStack heldKunai) {
        super(ModEntities.FLYING_RAIJIN_KUNAI.get(), owner, level);
        this.pickup = Pickup.DISALLOWED;
        this.kunaiDamage = damage;
        if (!heldKunai.isEmpty()) {
            this.entityData.set(DATA_ITEM, heldKunai.copy());
        }
    }

    /** Spawn a kunai already stuck in the ground at the given position. */
    public static FlyingRaijinKunaiEntity placeOnGround(Level level, LivingEntity owner, double x, double y, double z, ItemStack heldKunai) {
        FlyingRaijinKunaiEntity kunai = new FlyingRaijinKunaiEntity(level, owner, 0, heldKunai);
        kunai.setPos(x, y, z);
        kunai.setNoGravity(true);
        kunai.setDeltaMovement(0, 0, 0);
        kunai.entityData.set(DATA_STUCK, true);
        kunai.entityData.set(DATA_GROUND_PLACED, true);
        kunai.inGround = true; // prevent AbstractArrow from resetting rotation
        kunai.setYRot(owner.getYRot());
        kunai.setXRot(60.0F); // angled into the ground like a thrown kunai
        return kunai;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ITEM, new ItemStack(ModItems.IRON_KUNAI.get()));
        this.entityData.define(DATA_STUCK, false);
        this.entityData.define(DATA_GROUND_PLACED, false);
    }

    public boolean isStuck() {
        return this.entityData.get(DATA_STUCK);
    }

    public boolean isGroundPlaced() {
        return this.entityData.get(DATA_GROUND_PLACED);
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    @Override
    public void tick() {
        // Ground-placed kunai: skip all arrow physics, just sit still
        if (isGroundPlaced()) {
            // Only tick the base Entity (age, remove if discarded, etc.)
            this.baseTick();
            return;
        }

        super.tick();

        // Golden particle trail while flying
        if (!isStuck() && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        }

        // Stuck in block: show marker particles and check lifetime
        if (inGround) {
            this.entityData.set(DATA_STUCK, true);
            groundLife++;
            if (level() instanceof ServerLevel sl && groundLife % 5 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(),
                        2, 0.1, 0.2, 0.1, 0.01);
            }
            if (groundLife > 200) { // 10 seconds
                notifyOwnerKunaiExpired();
                discard();
            }
        }

        // Safety: discard if flying too long (5 seconds without hitting anything)
        if (!isStuck() && tickCount > 100) {
            notifyOwnerKunaiExpired();
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide()) return;

        // Deal damage
        if (getOwner() instanceof ServerPlayer owner) {
            result.getEntity().hurt(SkillDamageSource.get(owner.level()), kunaiDamage);
            if (result.getEntity() instanceof LivingEntity target) {
                CombatLog.damageSkill(owner, "Flying Raijin", kunaiDamage, target);
            }

            // Mark the target in owner's SkillData
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                sd.setFlyingRaijinTargetId(result.getEntity().getId());
                sd.setFlyingRaijinPos(result.getEntity().getX(),
                        result.getEntity().getY(), result.getEntity().getZ());
                sd.setFlyingRaijinKunaiId(-1); // No kunai to track, target is marked
            });

            if (level() instanceof ServerLevel sl) {
                SkillParticles.burst(sl, result.getEntity().getX(),
                        result.getEntity().getY() + 1, result.getEntity().getZ(),
                        10, 0.3, ParticleTypes.END_ROD);
            }

            owner.sendSystemMessage(Component.literal(
                    "\u00a7b[System]\u00a7r \u00a7eTarget marked by Flying Raijin!"));
            StatEventHandler.syncToClient(owner);
        }

        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result); // stick in block
        this.entityData.set(DATA_STUCK, true);

        if (getOwner() instanceof ServerPlayer owner) {
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                sd.setFlyingRaijinTargetId(-1); // No entity target
                sd.setFlyingRaijinPos(getX(), getY(), getZ());
                sd.setFlyingRaijinKunaiId(getId());
            });
        }
    }

    private void notifyOwnerKunaiExpired() {
        if (getOwner() instanceof ServerPlayer owner) {
            owner.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
                SkillData sd = stats.getSkillData();
                if (sd.getFlyingRaijinPhase() == 1) {
                    sd.setFlyingRaijinPhase(0);
                    sd.setFlyingRaijinTargetId(-1);
                    sd.setFlyingRaijinKunaiId(-1);
                    int level = sd.getLevel(SkillType.FLYING_RAIJIN);
                    sd.startCooldown(SkillType.FLYING_RAIJIN, level);
                    owner.sendSystemMessage(Component.literal(
                            "\u00a7b[System]\u00a7r \u00a77Flying Raijin kunai expired."));
                    StatEventHandler.syncToClient(owner);
                }
            });
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
