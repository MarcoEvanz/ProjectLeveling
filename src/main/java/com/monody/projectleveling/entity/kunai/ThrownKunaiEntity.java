package com.monody.projectleveling.entity.kunai;

import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownKunaiEntity extends AbstractArrow implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownKunaiEntity.class, EntityDataSerializers.ITEM_STACK);

    private ItemStack kunaiStack = ItemStack.EMPTY;
    private boolean returning = false;
    private int stuckTimer = 0;
    private static final int STUCK_DURATION = 40; // 2 seconds stuck before returning
    private static final int MAX_LIFETIME = 200;  // 10 seconds total max

    // Client-side constructor (required for entity registration)
    public ThrownKunaiEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    // Server spawn constructor
    public ThrownKunaiEntity(Level level, LivingEntity owner, ItemStack kunaiStack) {
        super(ModEntities.THROWN_KUNAI.get(), owner, level);
        this.kunaiStack = kunaiStack.copy();
        this.entityData.set(DATA_ITEM, kunaiStack.copy());
        this.pickup = Pickup.DISALLOWED;
        // Use kunai's attack damage as base arrow damage
        this.setBaseDamage(2.0 + kunaiStack.getDamageValue() * 0.01);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ITEM, new ItemStack(ModItems.IRON_KUNAI.get()));
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    @Override
    public void tick() {
        if (returning) {
            Entity owner = getOwner();
            if (owner != null && owner.isAlive()) {
                Vec3 toOwner = owner.getEyePosition().subtract(position()).normalize();
                setDeltaMovement(toOwner.scale(1.5));
                // Close enough to return
                if (distanceTo(owner) < 2.0) {
                    returnToOwner();
                    return;
                }
                // Rotate blade to face the owner
                double dx = toOwner.x;
                double dy = toOwner.y;
                double dz = toOwner.z;
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                float newYRot = (float)(Mth.atan2(dx, dz) * Mth.RAD_TO_DEG);
                float newXRot = (float)(Mth.atan2(dy, horizDist) * Mth.RAD_TO_DEG);
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
                this.setYRot(newYRot);
                this.setXRot(newXRot);
            } else {
                // Owner gone — drop as item
                spawnAtLocation(kunaiStack);
                discard();
                return;
            }
            // Skip AbstractArrow tick when returning to avoid re-sticking
            noPhysics = true;
            super.tick();
            noPhysics = false;
            return;
        }

        super.tick();

        // If stuck in block, count down then start returning
        if (inGround) {
            stuckTimer++;
            if (stuckTimer >= STUCK_DURATION) {
                returning = true;
                inGround = false;
                setDeltaMovement(Vec3.ZERO);
            }
        }

        // Safety: max lifetime
        if (tickCount > MAX_LIFETIME && !returning) {
            spawnAtLocation(kunaiStack);
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide()) return;
        // Deal damage manually — don't call super which discards the arrow
        Entity target = result.getEntity();
        float damage = (float) getBaseDamage();
        target.hurt(damageSources().arrow(this, getOwner()), damage);
        // Start returning immediately after hitting an entity
        returning = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        // Will start returning after STUCK_DURATION ticks
        stuckTimer = 0;
    }

    private void returnToOwner() {
        Entity owner = getOwner();
        if (owner instanceof Player player && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(kunaiStack)) {
                spawnAtLocation(kunaiStack);
            }
        }
        discard();
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected ItemStack getPickupItem() {
        return kunaiStack.copy();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!kunaiStack.isEmpty()) {
            tag.put("Kunai", kunaiStack.save(new CompoundTag()));
        }
        tag.putBoolean("Returning", returning);
        tag.putInt("StuckTimer", stuckTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Kunai")) {
            kunaiStack = ItemStack.of(tag.getCompound("Kunai"));
            this.entityData.set(DATA_ITEM, kunaiStack.copy());
        }
        returning = tag.getBoolean("Returning");
        stuckTimer = tag.getInt("StuckTimer");
    }
}
