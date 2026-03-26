package com.monody.projectleveling.skill;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Custom damage source for skill damage that respects armor, armor toughness,
 * and Protection enchantments (unlike damageSources().magic() which bypasses armor).
 * The entity-less overload avoids triggering passive bonuses in onLivingHurt,
 * but the entity overload sets kill credit so dungeon EXP is awarded.
 */
public final class SkillDamageSource {

    public static final ResourceKey<DamageType> SKILL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ProjectLeveling.MOD_ID, "skill"));

    private SkillDamageSource() {}

    /** Skill damage with no entity — use get(level, player) when kill credit matters. */
    public static DamageSource get(Level level) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(SKILL));
    }

    /** Skill damage attributed to an entity — sets kill credit for EXP rewards. */
    public static DamageSource get(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(SKILL),
                attacker);
    }
}
