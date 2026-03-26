package com.monody.projectleveling.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DungeonChunkGenerator extends NoiseBasedChunkGenerator {

    private static final int MAX_CHUNK_RADIUS = 7;

    public static final Codec<DungeonChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings)
            ).apply(instance, DungeonChunkGenerator::new)
    );

    public DungeonChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private static boolean isInBounds(ChunkPos pos) {
        return Math.abs(pos.x) <= MAX_CHUNK_RADIUS && Math.abs(pos.z) <= MAX_CHUNK_RADIUS;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                         RandomState randomState, StructureManager structureManager,
                                                         ChunkAccess chunk) {
        if (!isInBounds(chunk.getPos())) return CompletableFuture.completedFuture(chunk);
        return super.fillFromNoise(executor, blender, randomState, structureManager, chunk);
    }

    @Override
    public void buildSurface(net.minecraft.server.level.WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        if (!isInBounds(chunk.getPos())) return;
        super.buildSurface(region, structureManager, randomState, chunk);
    }

    @Override
    public void applyCarvers(net.minecraft.server.level.WorldGenRegion region, long seed,
                             RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk,
                             GenerationStep.Carving step) {
        if (!isInBounds(chunk.getPos())) return;
        super.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk, step);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
                                     StructureManager structureManager) {
        if (!isInBounds(chunk.getPos())) return;
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    @Override
    public void spawnOriginalMobs(net.minecraft.server.level.WorldGenRegion region) {
        if (!isInBounds(region.getCenter())) return;
        super.spawnOriginalMobs(region);
    }
}
