package com.monody.projectleveling.entity;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.entity.archer.SkillArrowEntity;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.mage.SkillFireballEntity;
import com.monody.projectleveling.entity.necromancer.SkeletonMinionEntity;
import com.monody.projectleveling.entity.kunai.ThrownKunaiEntity;
import com.monody.projectleveling.entity.kunai.ThrownShurikenEntity;
import com.monody.projectleveling.entity.ninja.FlyingRaijinKunaiEntity;
import com.monody.projectleveling.entity.ninja.ShadowCloneEntity;
import com.monody.projectleveling.entity.warrior.HeavenSwordEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ProjectLeveling.MOD_ID);

    public static final RegistryObject<EntityType<SkillFireballEntity>> SKILL_FIREBALL =
            ENTITIES.register("skill_fireball", () ->
                    EntityType.Builder.<SkillFireballEntity>of(SkillFireballEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("skill_fireball"));

    public static final RegistryObject<EntityType<SkillArrowEntity>> SKILL_ARROW =
            ENTITIES.register("skill_arrow", () ->
                    EntityType.Builder.<SkillArrowEntity>of(SkillArrowEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("skill_arrow"));

    public static final RegistryObject<EntityType<ShadowPartnerEntity>> SHADOW_PARTNER =
            ENTITIES.register("shadow_partner", () ->
                    EntityType.Builder.<ShadowPartnerEntity>of(ShadowPartnerEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("shadow_partner"));

    public static final RegistryObject<EntityType<SkeletonMinionEntity>> SKELETON_MINION =
            ENTITIES.register("skeleton_minion", () ->
                    EntityType.Builder.<SkeletonMinionEntity>of(SkeletonMinionEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.99f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("skeleton_minion"));

    public static final RegistryObject<EntityType<ShadowCloneEntity>> SHADOW_CLONE =
            ENTITIES.register("shadow_clone", () ->
                    EntityType.Builder.<ShadowCloneEntity>of(ShadowCloneEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("shadow_clone"));

    public static final RegistryObject<EntityType<FlyingRaijinKunaiEntity>> FLYING_RAIJIN_KUNAI =
            ENTITIES.register("flying_raijin_kunai", () ->
                    EntityType.Builder.<FlyingRaijinKunaiEntity>of(FlyingRaijinKunaiEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("flying_raijin_kunai"));

    public static final RegistryObject<EntityType<ThrownKunaiEntity>> THROWN_KUNAI =
            ENTITIES.register("thrown_kunai", () ->
                    EntityType.Builder.<ThrownKunaiEntity>of(ThrownKunaiEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("thrown_kunai"));

    public static final RegistryObject<EntityType<ThrownShurikenEntity>> THROWN_SHURIKEN =
            ENTITIES.register("thrown_shuriken", () ->
                    EntityType.Builder.<ThrownShurikenEntity>of(ThrownShurikenEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("thrown_shuriken"));

    public static final RegistryObject<EntityType<HeavenSwordEntity>> HEAVEN_SWORD =
            ENTITIES.register("heaven_sword", () ->
                    EntityType.Builder.<HeavenSwordEntity>of(HeavenSwordEntity::new, MobCategory.MISC)
                            .sized(1.0f, 3.0f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("heaven_sword"));
}
