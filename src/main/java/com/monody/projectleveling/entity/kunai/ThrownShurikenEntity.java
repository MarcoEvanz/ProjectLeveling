package com.monody.projectleveling.entity.kunai;

import com.monody.projectleveling.entity.ModEntities;
import com.monody.projectleveling.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownShurikenEntity extends AbstractArrow implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownShurikenEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(ThrownShurikenEntity.class, EntityDataSerializers.BOOLEAN);

    private ItemStack shurikenStack = ItemStack.EMPTY;
    private boolean returning = false;
    private boolean skillProjectile = false;
    private int stuckTimer = 0;
    private static final int STUCK_DURATION = 40; // 2 seconds stuck before returning
    private static final int MAX_LIFETIME = 200;  // 10 seconds total max

    // Client-side constructor (required for entity registration)
    public ThrownShurikenEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    // Server spawn constructor
    public ThrownShurikenEntity(Level level, LivingEntity owner, ItemStack shurikenStack) {
        super(ModEntities.THROWN_SHURIKEN.get(), owner, level);
        this.shurikenStack = shurikenStack.copy();
        this.entityData.set(DATA_ITEM, shurikenStack.copy());
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(2.0 + shurikenStack.getDamageValue() * 0.01);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ITEM, new ItemStack(ModItems.IRON_SHURIKEN.get()));
        this.entityData.define(DATA_STUCK, false);
    }

    public boolean isStuck() {
        return this.entityData.get(DATA_STUCK);
    }

    public void setSkillProjectile(boolean value) {
        this.skillProjectile = value;
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
                if (distanceTo(owner) < 2.0) {
                    returnToOwner();
                    return;
                }
                // Rotate to face the owner
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
                spawnAtLocation(shurikenStack);
                discard();
                return;
            }
            noPhysics = true;
            super.tick();
            noPhysics = false;
            return;
        }

        super.tick();

        // If stuck in block
        if (inGround) {
            this.entityData.set(DATA_STUCK, true);
            stuckTimer++;
            if (skillProjectile) {
                if (stuckTimer >= STUCK_DURATION) {
                    discard();
                }
                return;
            }
            if (stuckTimer >= STUCK_DURATION) {
                returning = true;
                inGround = false;
                setDeltaMovement(Vec3.ZERO);
            }
        }

        // Safety: max lifetime
        if (tickCount > MAX_LIFETIME && !returning) {
            if (!skillProjectile) {
                spawnAtLocation(shurikenStack);
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide()) return;
        Entity target = result.getEntity();
        float damage = (float) getBaseDamage();
        target.hurt(damageSources().arrow(this, getOwner()), damage);
        if (skillProjectile) {
            discard();
        } else {
            returning = true;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        stuckTimer = 0;
    }

    private void returnToOwner() {
        Entity owner = getOwner();
        if (owner instanceof Player player && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(shurikenStack)) {
                spawnAtLocation(shurikenStack);
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
        return shurikenStack.copy();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!shurikenStack.isEmpty()) {
            tag.put("Shuriken", shurikenStack.save(new CompoundTag()));
        }
        tag.putBoolean("Returning", returning);
        tag.putBoolean("SkillProjectile", skillProjectile);
        tag.putInt("StuckTimer", stuckTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shuriken")) {
            shurikenStack = ItemStack.of(tag.getCompound("Shuriken"));
            this.entityData.set(DATA_ITEM, shurikenStack.copy());
        }
        returning = tag.getBoolean("Returning");
        skillProjectile = tag.getBoolean("SkillProjectile");
        stuckTimer = tag.getInt("StuckTimer");
    }
}
