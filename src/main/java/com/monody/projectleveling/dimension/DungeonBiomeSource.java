package com.monody.projectleveling.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/**
 * Delegates to the overworld's biome source at runtime, inheriting all modded biomes.
 * Falls back to a JSON-defined biome list during early loading.
 * Each dungeon naturally gets different biomes because the chunk generator
 * offsets terrain coordinates per dungeon index.
 */
public class DungeonBiomeSource extends BiomeSource {

    public static final Codec<DungeonBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.fallbackBiomes)
            ).apply(instance, DungeonBiomeSource::new)
    );

    private final List<Holder<Biome>> fallbackBiomes;
    private volatile BiomeSource overworldSource;

    public DungeonBiomeSource(List<Holder<Biome>> fallbackBiomes) {
        super();
        this.fallbackBiomes = fallbackBiomes;
    }

    private BiomeSource getOverworldSource() {
        if (overworldSource == null) {
            try {
                var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        overworldSource = overworld.getChunkSource().getGenerator().getBiomeSource();
                    }
                }
            } catch (Exception ignored) { /* server not ready yet, use fallback */ }
        }
        return overworldSource;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        BiomeSource source = getOverworldSource();
        if (source != null) {
            return source.possibleBiomes().stream();
        }
        return fallbackBiomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        BiomeSource source = getOverworldSource();
        if (source != null) {
            return source.getNoiseBiome(quartX, quartY, quartZ, sampler);
        }
        return fallbackBiomes.get(0);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }
}
