package com.monody.projectleveling.sound;

import com.monody.projectleveling.ProjectLeveling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ProjectLeveling.MOD_ID);

    public static final RegistryObject<SoundEvent> FLYING_RAIJIN_TELEPORT = SOUNDS.register(
            "flying_raijin_teleport",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ProjectLeveling.MOD_ID, "flying_raijin_teleport")));
}
