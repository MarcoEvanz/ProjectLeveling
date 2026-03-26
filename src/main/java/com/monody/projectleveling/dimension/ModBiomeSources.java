package com.monody.projectleveling.dimension;

import com.mojang.serialization.Codec;
import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModBiomeSources {
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, ProjectLeveling.MOD_ID);

    public static final RegistryObject<Codec<? extends BiomeSource>> DUNGEON_BIOME =
            BIOME_SOURCES.register("dungeon_biome", () -> DungeonBiomeSource.CODEC);
}
