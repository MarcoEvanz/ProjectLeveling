package com.monody.projectleveling.dimension;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final int NUM_DUNGEONS = 20;

    public static ResourceKey<Level> dungeonKey(int index) {
        return ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(ProjectLeveling.MOD_ID, "dungeon_" + (index + 1)));
    }

    public static int getDungeonIndex(ResourceKey<Level> key) {
        if (!key.location().getNamespace().equals(ProjectLeveling.MOD_ID)) return -1;
        String path = key.location().getPath();
        if (!path.startsWith("dungeon_")) return -1;
        try {
            int num = Integer.parseInt(path.substring(8));
            if (num < 1 || num > NUM_DUNGEONS) return -1;
            return num - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean isDungeon(ResourceKey<Level> key) {
        return getDungeonIndex(key) >= 0;
    }
}
