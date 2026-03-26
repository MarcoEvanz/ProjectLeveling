package com.monody.projectleveling.particle;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ProjectLeveling.MOD_ID);

    /** END_ROD look-alike with ~2 tick (0.1s) lifetime. */
    public static final RegistryObject<SimpleParticleType> SHORT_END_ROD =
            PARTICLES.register("short_end_rod", () -> new SimpleParticleType(false));
}
