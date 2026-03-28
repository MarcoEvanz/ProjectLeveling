package com.monody.projectleveling.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public class DungeonHelper {

    public static final String KEY_DIED_IN_DUNGEON = "pl_died_in_dungeon";

    public static boolean teleportToZone(ServerPlayer player, int dungeonIndex) {
        if (dungeonIndex < 0 || dungeonIndex >= ModDimensions.NUM_DUNGEONS) return false;

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        ResourceKey<Level> dimKey = ModDimensions.dungeonKey(dungeonIndex);
        ServerLevel dungeon = server.getLevel(dimKey);
        if (dungeon == null) return false;

        // Resolve the zone center (shifts away from ocean if needed)
        DungeonChunkGenerator gen = (DungeonChunkGenerator) dungeon.getChunkSource().getGenerator();
        gen.resolveCenter(dungeon);
        int spawnX = gen.getCenterBlockX();
        int spawnZ = gen.getCenterBlockZ();

        // Generate the target chunk and get surface height
        dungeon.getChunk(spawnX >> 4, spawnZ >> 4);
        int surfaceY = dungeon.getHeight(Heightmap.Types.MOTION_BLOCKING, spawnX, spawnZ);
        if (surfaceY <= dungeon.getMinBuildHeight()) surfaceY = 63;

        saveReturnPosition(player);
        player.teleportTo(dungeon, spawnX + 0.5, surfaceY + 1.0, spawnZ + 0.5,
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
