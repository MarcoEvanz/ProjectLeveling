package com.monody.projectleveling.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class DungeonHelper {

    public static final double BORDER_SIZE = 194.0;
    public static final String KEY_DIED_IN_DUNGEON = "pl_died_in_dungeon";

    public static boolean teleportToZone(ServerPlayer player, int dungeonIndex) {
        if (dungeonIndex < 0 || dungeonIndex >= ModDimensions.NUM_DUNGEONS) return false;

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        ResourceKey<Level> dimKey = ModDimensions.dungeonKey(dungeonIndex);
        ServerLevel dungeon = server.getLevel(dimKey);
        if (dungeon == null) return false;

        // Set up WorldBorder
        WorldBorder border = dungeon.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(BORDER_SIZE);

        // Find safe spawn, avoiding ocean and mushroom biomes
        double spawnX = 0.5;
        double spawnZ = 0.5;
        for (int attempt = 0; attempt < 5; attempt++) {
            int checkX = (int) spawnX;
            int checkZ = (int) spawnZ;

            // Force chunk generation so heightmap is available
            dungeon.getChunk(checkX >> 4, checkZ >> 4);

            int surfaceY = dungeon.getHeight(Heightmap.Types.MOTION_BLOCKING, checkX, checkZ);
            Holder<Biome> biome = dungeon.getBiome(new BlockPos(checkX, surfaceY, checkZ));

            boolean isOcean = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
            boolean isMushroom = biome.is(Biomes.MUSHROOM_FIELDS);

            if (!isOcean && !isMushroom && surfaceY > dungeon.getMinBuildHeight()) {
                saveReturnPosition(player);
                player.teleportTo(dungeon, spawnX, surfaceY + 1.0, spawnZ,
                        player.getYRot(), player.getXRot());
                return true;
            }
            spawnZ += 200;
        }

        // Fallback: force chunk + use last attempted position
        dungeon.getChunk((int) spawnX >> 4, (int) spawnZ >> 4);
        int surfaceY = dungeon.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) spawnX, (int) spawnZ);
        if (surfaceY <= dungeon.getMinBuildHeight()) surfaceY = 70;
        saveReturnPosition(player);
        player.teleportTo(dungeon, spawnX, surfaceY + 1.0, spawnZ,
                player.getYRot(), player.getXRot());
        return true;
    }

    public static void returnToOverworld(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        CompoundTag data = player.getPersistentData();
        double x, y, z;
        if (data.contains("pl_return_x")) {
            x = data.getDouble("pl_return_x");
            y = data.getDouble("pl_return_y");
            z = data.getDouble("pl_return_z");
        } else {
            BlockPos spawn = overworld.getSharedSpawnPos();
            x = spawn.getX() + 0.5;
            y = spawn.getY() + 1.0;
            z = spawn.getZ() + 0.5;
        }

        player.teleportTo(overworld, x, y, z, player.getYRot(), player.getXRot());
        data.remove("pl_return_x");
        data.remove("pl_return_y");
        data.remove("pl_return_z");
    }

    private static void saveReturnPosition(ServerPlayer player) {
        if (ModDimensions.isDungeon(player.level().dimension())) return;

        CompoundTag data = player.getPersistentData();
        data.putDouble("pl_return_x", player.getX());
        data.putDouble("pl_return_y", player.getY());
        data.putDouble("pl_return_z", player.getZ());
    }
}
