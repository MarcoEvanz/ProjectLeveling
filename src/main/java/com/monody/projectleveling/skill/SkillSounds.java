package com.monody.projectleveling.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class SkillSounds {

    /** Play a sound at the player's position, audible to all nearby players */
    public static void playAt(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    /** Play a sound at an arbitrary position */
    public static void playAt(ServerLevel level, double x, double y, double z,
                              SoundEvent sound, float volume, float pitch) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
