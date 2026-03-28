package com.monody.projectleveling.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DungeonChunkGenerator extends NoiseBasedChunkGenerator {

    private static final int MAX_CHUNK_RADIUS = 32;
    private static final int LAND_SEARCH_STEP = 48;
    private static final int LAND_SEARCH_MAX = 3000;

    /** Block offset between dungeon base positions (10000 blocks). */
    public static final int BLOCKS_OFFSET_PER_DUNGEON = 10000;

    public static final Codec<DungeonChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
                    Codec.INT.fieldOf("dungeon_index").forGetter(g -> g.dungeonIndex)
            ).apply(instance, DungeonChunkGenerator::new)
    );

    private final int dungeonIndex;
    private volatile int centerBlockX;
    private volatile int centerBlockZ;
    private volatile boolean centerResolved;

    public DungeonChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, int dungeonIndex) {
        super(biomeSource, settings);
        this.dungeonIndex = dungeonIndex;
        this.centerBlockX = dungeonIndex * BLOCKS_OFFSET_PER_DUNGEON;
        this.centerBlockZ = dungeonIndex * BLOCKS_OFFSET_PER_DUNGEON;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ---- Center resolution (shifts away from ocean) ----

    public int getCenterBlockX() { return centerBlockX; }
    public int getCenterBlockZ() { return centerBlockZ; }

    /**
     * Resolves the actual center, shifting away from ocean if needed.
     * Called from DungeonHelper before teleport, and from generation methods as safety net.
     */
    public void resolveCenter(ServerLevel level) {
        if (!centerResolved) {
            resolveCenter(level.getChunkSource().randomState().sampler());
        }
    }

    private void resolveCenter(Climate.Sampler sampler) {
        if (centerResolved) return;
        synchronized (this) {
            if (centerResolved) return;

            int baseX = dungeonIndex * BLOCKS_OFFSET_PER_DUNGEON;
            int baseZ = dungeonIndex * BLOCKS_OFFSET_PER_DUNGEON;
            BiomeSource source = getBiomeSource();

            if (isDryLand(source, sampler, baseX, baseZ)) {
                centerResolved = true;
                return;
            }

            for (int r = LAND_SEARCH_STEP; r <= LAND_SEARCH_MAX; r += LAND_SEARCH_STEP) {
                BlockPos found = checkRing(source, sampler, baseX, baseZ, r);
                if (found != null) {
                    centerBlockX = found.getX();
                    centerBlockZ = found.getZ();
                    centerResolved = true;
                    return;
                }
            }
            // No land found within search range, keep default center
            centerResolved = true;
        }
    }

    private static BlockPos checkRing(BiomeSource source, Climate.Sampler sampler, int cx, int cz, int r) {
        int[][] offsets = {
                {r, 0}, {-r, 0}, {0, r}, {0, -r},
                {r, r}, {-r, r}, {r, -r}, {-r, -r}
        };
        for (int[] off : offsets) {
            if (isDryLand(source, sampler, cx + off[0], cz + off[1])) {
                return new BlockPos(cx + off[0], 0, cz + off[1]);
            }
        }
        return null;
    }

    private static boolean isDryLand(BiomeSource source, Climate.Sampler sampler, int blockX, int blockZ) {
        Holder<Biome> biome = source.getNoiseBiome(blockX >> 2, 0, blockZ >> 2, sampler);
        return !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_DEEP_OCEAN);
    }

    // ---- Bounded generation ----

    private boolean isInBounds(ChunkPos pos) {
        return Math.abs(pos.x - (centerBlockX >> 4)) <= MAX_CHUNK_RADIUS
                && Math.abs(pos.z - (centerBlockZ >> 4)) <= MAX_CHUNK_RADIUS;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                         RandomState randomState, StructureManager structureManager,
                                                         ChunkAccess chunk) {
        resolveCenter(randomState.sampler());
        if (!isInBounds(chunk.getPos())) return CompletableFuture.completedFuture(chunk);
        return super.fillFromNoise(executor, blender, randomState, structureManager, chunk);
    }

    @Override
    public void buildSurface(net.minecraft.server.level.WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        resolveCenter(randomState.sampler());
        if (!isInBounds(chunk.getPos())) return;
        super.buildSurface(region, structureManager, randomState, chunk);
    }

    @Override
    public void applyCarvers(net.minecraft.server.level.WorldGenRegion region, long seed,
                             RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk,
                             GenerationStep.Carving step) {
        resolveCenter(randomState.sampler());
        if (!isInBounds(chunk.getPos())) return;
        super.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk, step);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
                                     StructureManager structureManager) {
        if (!isInBounds(chunk.getPos())) return;
        super.applyBiomeDecoration(level, chunk, structureManager);
        placeBarrierWalls(chunk);
    }

    // ---- Barrier walls ----

    private void placeBarrierWalls(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int relX = pos.x - (centerBlockX >> 4);
        int relZ = pos.z - (centerBlockZ >> 4);
        if (Math.abs(relX) != MAX_CHUNK_RADIUS && Math.abs(relZ) != MAX_CHUNK_RADIUS) return;

        int minY = chunk.getMinBuildHeight();
        int maxY = minY + chunk.getHeight();
        int minBX = pos.getMinBlockX();
        int minBZ = pos.getMinBlockZ();

        if (relX == MAX_CHUNK_RADIUS)  fillWallX(chunk, minBX + 15, minBZ, minY, maxY);
        if (relX == -MAX_CHUNK_RADIUS) fillWallX(chunk, minBX, minBZ, minY, maxY);
        if (relZ == MAX_CHUNK_RADIUS)  fillWallZ(chunk, minBX, minBZ + 15, minY, maxY);
        if (relZ == -MAX_CHUNK_RADIUS) fillWallZ(chunk, minBX, minBZ, minY, maxY);
    }

    private static void fillWallX(ChunkAccess chunk, int bx, int minBZ, int minY, int maxY) {
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        for (int y = minY; y < maxY; y++)
            for (int z = 0; z < 16; z++)
                chunk.setBlockState(new BlockPos(bx, y, minBZ + z), barrier, false);
    }

    private static void fillWallZ(ChunkAccess chunk, int minBX, int bz, int minY, int maxY) {
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        for (int y = minY; y < maxY; y++)
            for (int x = 0; x < 16; x++)
                chunk.setBlockState(new BlockPos(minBX + x, y, bz), barrier, false);
    }

    @Override
    public void spawnOriginalMobs(net.minecraft.server.level.WorldGenRegion region) {
        if (!isInBounds(region.getCenter())) return;
        super.spawnOriginalMobs(region);
    }
}
