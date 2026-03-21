package com.monody.projectleveling.skill;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Custom damage source for skill damage that respects armor, armor toughness,
 * and Protection enchantments (unlike damageSources().magic() which bypasses armor).
 * Uses no entity reference to avoid triggering passive bonuses in onLivingHurt.
 */
public final class SkillDamageSource {

    private static final ResourceKey<DamageType> SKILL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ProjectLeveling.MOD_ID, "skill"));

    private SkillDamageSource() {}

    public static DamageSource get(Level level) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(SKILL));
    }
}
