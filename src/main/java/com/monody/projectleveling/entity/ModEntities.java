package com.monody.projectleveling.entity;

import com.monody.projectleveling.ProjectLeveling;
import com.monody.projectleveling.entity.archer.SkillArrowEntity;
import com.monody.projectleveling.entity.assassin.ShadowPartnerEntity;
import com.monody.projectleveling.entity.mage.SkillFireballEntity;
import com.monody.projectleveling.entity.necromancer.SkeletonMinionEntity;
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
}
