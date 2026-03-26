package com.monody.projectleveling.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.projectleveling";

    public static final KeyMapping STATUS_SCREEN = new KeyMapping(
            "key.projectleveling.status_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    public static final KeyMapping QUEST_SCREEN = new KeyMapping(
            "key.projectleveling.quest_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping SKILL_TREE_SCREEN = new KeyMapping(
            "key.projectleveling.skill_tree_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_1 = new KeyMapping(
            "key.projectleveling.skill_slot_1",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_2 = new KeyMapping(
            "key.projectleveling.skill_slot_2",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_3 = new KeyMapping(
            "key.projectleveling.skill_slot_3",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_4 = new KeyMapping(
            "key.projectleveling.skill_slot_4",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_5 = new KeyMapping(
            "key.projectleveling.skill_slot_5",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_6 = new KeyMapping(
            "key.projectleveling.skill_slot_6",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_7 = new KeyMapping(
            "key.projectleveling.skill_slot_7",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            CATEGORY
    );

    public static final KeyMapping DUNGEON_SCREEN = new KeyMapping(
            "key.projectleveling.dungeon_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );
}
