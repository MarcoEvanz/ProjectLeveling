package com.monody.projectleveling.dimension;

import com.mojang.serialization.Codec;
import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ProjectLeveling.MOD_ID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> DUNGEON =
            CHUNK_GENERATORS.register("dungeon", () -> DungeonChunkGenerator.CODEC);
}
