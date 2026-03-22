package com.monody.projectleveling.skill;

public enum PlayerClass {
    // bonusSPPerEvenLevel: extra SP awarded on even-numbered levels for tiers [T0, T1, T2, T3]
    // Base SP totals (all classes): T0=10, T1=30, T2=45, T3=60
    // Each +1 bonus on even levels adds: T1=+10, T2=+15, T3=+20 to the total
    NONE("none", "None", new int[]{0, 0, 0, 0}),
    WARRIOR("warrior", "Warrior", new int[]{0, 0, 0, 0}),
    ASSASSIN("assassin", "Assassin", new int[]{0, 0, 0, 0}),
    ARCHER("archer", "Archer", new int[]{0, 0, 0, 0}),
    HEALER("healer", "Healer", new int[]{0, 0, 0, 0}),
    MAGE("mage", "Mage", new int[]{0, 0, 0, 0}),
    NINJA("ninja", "Ninja", new int[]{0, 0, 0, 0}),
    NECROMANCER("necromancer", "Necromancer", new int[]{0, 1, 1, 1}), // T1=40, T2=60, T3=80
    BEAST_MASTER("beast_master", "Beast Master", new int[]{0, 2, 0, 2}); // T1=50, T2=45, T3=100

    private final String id;
    private final String displayName;
    private final int[] bonusSPPerEvenLevel; // indexed by tier (0-3)

    PlayerClass(String id, String displayName, int[] bonusSPPerEvenLevel) {
        this.id = id;
        this.displayName = displayName;
        this.bonusSPPerEvenLevel = bonusSPPerEvenLevel;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    /** Extra SP awarded on even-numbered levels for the given tier (0-3). */
    public int getBonusSPPerEvenLevel(int tier) {
        if (tier < 0 || tier >= bonusSPPerEvenLevel.length) return 0;
        return bonusSPPerEvenLevel[tier];
    }

    public static PlayerClass fromId(String id) {
        for (PlayerClass pc : values()) {
            if (pc.id.equals(id)) return pc;
        }
        return NONE;
    }
}
