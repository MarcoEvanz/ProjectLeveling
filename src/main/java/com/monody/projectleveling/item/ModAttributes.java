package com.monody.projectleveling.item;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ProjectLeveling.MOD_ID);

    public static final RegistryObject<Attribute> MAGIC_ATTACK = ATTRIBUTES.register("magic_attack",
            () -> new RangedAttribute("attribute.projectleveling.magic_attack", 0.0, 0.0, 1024.0));

    public static final RegistryObject<Attribute> CRIT_RATE = ATTRIBUTES.register("crit_rate",
            () -> new RangedAttribute("attribute.projectleveling.crit_rate", 0.0, 0.0, 100.0));

    public static final RegistryObject<Attribute> CRIT_DAMAGE = ATTRIBUTES.register("crit_damage",
            () -> new RangedAttribute("attribute.projectleveling.crit_damage", 0.0, 0.0, 1000.0));

    public static final RegistryObject<Attribute> ATTACK_PERCENT = ATTRIBUTES.register("attack_percent",
            () -> new RangedAttribute("attribute.projectleveling.attack_percent", 0.0, 0.0, 1000.0));

    public static final RegistryObject<Attribute> MAGIC_ATTACK_PERCENT = ATTRIBUTES.register("magic_attack_percent",
            () -> new RangedAttribute("attribute.projectleveling.magic_attack_percent", 0.0, 0.0, 1000.0));

    public static final RegistryObject<Attribute> FINAL_DAMAGE = ATTRIBUTES.register("final_damage",
            () -> new RangedAttribute("attribute.projectleveling.final_damage", 0.0, 0.0, 1000.0));
}
