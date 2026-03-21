package com.monody.projectleveling.skill;

public enum PlayerClass {
    NONE("none", "None"),
    WARRIOR("warrior", "Warrior"),
    ASSASSIN("assassin", "Assassin"),
    ARCHER("archer", "Archer"),
    HEALER("healer", "Healer"),
    MAGE("mage", "Mage"),
    NECROMANCER("necromancer", "Necromancer");

    private final String id;
    private final String displayName;

    PlayerClass(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public static PlayerClass fromId(String id) {
        for (PlayerClass pc : values()) {
            if (pc.id.equals(id)) return pc;
        }
        return NONE;
    }
}
