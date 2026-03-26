package com.monody.projectleveling.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DungeonBiomeSource extends BiomeSource {

    public static final Codec<DungeonBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("dungeon_index").forGetter(s -> s.dungeonIndex),
                    Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.biomes)
            ).apply(instance, DungeonBiomeSource::new)
    );

    /** Biome tags/keys to exclude from dungeon selection. */
    private static boolean isExcluded(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_DEEP_OCEAN)
                || biome.is(BiomeTags.IS_RIVER)
                || biome.is(BiomeTags.IS_NETHER)
                || biome.is(BiomeTags.IS_END)
                || biome.is(Biomes.MUSHROOM_FIELDS)
                || biome.is(Biomes.THE_VOID);
    }

    private final int dungeonIndex;
    private final List<Holder<Biome>> biomes;
    private volatile Holder<Biome> selectedBiome;
    private volatile List<Holder<Biome>> runtimeBiomes;

    public DungeonBiomeSource(int dungeonIndex, List<Holder<Biome>> biomes) {
        super();
        this.dungeonIndex = dungeonIndex;
        this.biomes = biomes;
    }

    /** Build a biome list from the full registry, including modded biomes. */
    private List<Holder<Biome>> getRuntimeBiomes() {
        if (runtimeBiomes == null) {
            // Try to get all biomes from the registry (available at world load)
            try {
                var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    var registry = server.registryAccess().registryOrThrow(Registries.BIOME);
                    List<Holder<Biome>> all = new ArrayList<>();
                    for (var entry : registry.entrySet()) {
                        Holder<Biome> holder = registry.getHolderOrThrow(entry.getKey());
                        if (!isExcluded(holder)) {
                            all.add(holder);
                        }
                    }
                    if (!all.isEmpty()) {
                        runtimeBiomes = all;
                        return runtimeBiomes;
                    }
                }
            } catch (Exception ignored) {}
            // Fallback to JSON-defined list
            runtimeBiomes = biomes;
        }
        return runtimeBiomes;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        if (selectedBiome == null) {
            List<Holder<Biome>> pool = getRuntimeBiomes();
            Climate.TargetPoint tp = sampler.sample(0, 0, 0);
            long hash = tp.temperature() * 31L + tp.humidity() * 17L + dungeonIndex * 2654435761L;
            selectedBiome = pool.get((int) Math.floorMod(hash, pool.size()));
        }
        return selectedBiome;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }
}
