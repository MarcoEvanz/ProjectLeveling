package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.PlayerClass;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncStatsPacket {
    // Core stats
    private final int level, strength, vitality, agility, intelligence, sense, luck, dexterity, mind;
    private final int remainingPoints, currentMp, currentExp;
    private final String job, title;
    // Quest
    private final int questKills, questBlocksMined, questBlocksWalked;
    private final boolean questCompleted;
    private final boolean questRewardClaimed;
    private final long questDay;
    // Skills
    private final String selectedClassId;
    private final int[] tierSP;
    private final Map<SkillType, Integer> skillLevels;
    private final String[] equippedSlots;
    private final Map<SkillType, Integer> cooldowns;
    private final Map<SkillType, Boolean> toggleStates;
    private final boolean shadowStrikeActive;
    private final int domainTicks;
    private final int armyTicks;
    private final int deathMarkTicks;
    private final int deathMarkTargetId;
    private final int undyingWillCooldown;

    public S2CSyncStatsPacket(PlayerStats stats) {
        this.level = stats.getLevel();
        this.strength = stats.getStrength();
        this.vitality = stats.getVitality();
        this.agility = stats.getAgility();
        this.intelligence = stats.getIntelligence();
        this.sense = stats.getSense();
        this.luck = stats.getLuck();
        this.dexterity = stats.getDexterity();
        this.mind = stats.getMind();
        this.remainingPoints = stats.getRemainingPoints();
        this.currentMp = stats.getCurrentMp();
        this.currentExp = stats.getCurrentExp();
        this.job = stats.getJob();
        this.title = stats.getTitle();
        this.questKills = stats.getQuestKills();
        this.questBlocksMined = stats.getQuestBlocksMined();
        this.questBlocksWalked = stats.getQuestBlocksWalked();
        this.questCompleted = stats.isQuestCompleted();
        this.questRewardClaimed = stats.isQuestRewardClaimed();
        this.questDay = stats.getQuestDay();

        SkillData sd = stats.getSkillData();
        this.selectedClassId = sd.getSelectedClass().getId();
        this.tierSP = new int[]{sd.getTierSP(0), sd.getTierSP(1), sd.getTierSP(2), sd.getTierSP(3)};
        this.skillLevels = new EnumMap<>(sd.getSkillLevels());
        this.equippedSlots = new String[SkillData.MAX_SLOTS];
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            SkillType eq = sd.getEquipped(i);
            equippedSlots[i] = eq != null ? eq.getId() : "";
        }
        this.cooldowns = new EnumMap<>(sd.getCooldowns());
        this.toggleStates = new EnumMap<>(sd.getToggleStates());
        this.shadowStrikeActive = sd.isShadowStrikeActive();
        this.domainTicks = sd.getDomainTicks();
        this.armyTicks = sd.getArmyTicks();
        this.deathMarkTicks = sd.getDeathMarkTicks();
        this.deathMarkTargetId = sd.getDeathMarkTargetId();
        this.undyingWillCooldown = sd.getUndyingWillCooldown();
    }

    public S2CSyncStatsPacket(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        this.strength = buf.readInt();
        this.vitality = buf.readInt();
        this.agility = buf.readInt();
        this.intelligence = buf.readInt();
        this.sense = buf.readInt();
        this.luck = buf.readInt();
        this.dexterity = buf.readInt();
        this.mind = buf.readInt();
        this.remainingPoints = buf.readInt();
        this.currentMp = buf.readInt();
        this.currentExp = buf.readInt();
        this.job = buf.readUtf(64);
        this.title = buf.readUtf(64);
        this.questKills = buf.readInt();
        this.questBlocksMined = buf.readInt();
        this.questBlocksWalked = buf.readInt();
        this.questCompleted = buf.readBoolean();
        this.questRewardClaimed = buf.readBoolean();
        this.questDay = buf.readLong();

        // Skills
        this.selectedClassId = buf.readUtf(32);
        this.tierSP = new int[4];
        for (int i = 0; i < 4; i++) tierSP[i] = buf.readInt();
        int levelCount = buf.readInt();
        this.skillLevels = new EnumMap<>(SkillType.class);
        for (int i = 0; i < levelCount; i++) {
            SkillType type = SkillType.fromId(buf.readUtf(32));
            int lv = buf.readInt();
            if (type != null) skillLevels.put(type, lv);
        }
        this.equippedSlots = new String[SkillData.MAX_SLOTS];
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            equippedSlots[i] = buf.readUtf(32);
        }
        int cdCount = buf.readInt();
        this.cooldowns = new EnumMap<>(SkillType.class);
        for (int i = 0; i < cdCount; i++) {
            SkillType type = SkillType.fromId(buf.readUtf(32));
            int ticks = buf.readInt();
            if (type != null) cooldowns.put(type, ticks);
        }
        int toggleCount = buf.readInt();
        this.toggleStates = new EnumMap<>(SkillType.class);
        for (int i = 0; i < toggleCount; i++) {
            SkillType type = SkillType.fromId(buf.readUtf(32));
            boolean active = buf.readBoolean();
            if (type != null) toggleStates.put(type, active);
        }
        this.shadowStrikeActive = buf.readBoolean();
        this.domainTicks = buf.readInt();
        this.armyTicks = buf.readInt();
        this.deathMarkTicks = buf.readInt();
        this.deathMarkTargetId = buf.readInt();
        this.undyingWillCooldown = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(strength);
        buf.writeInt(vitality);
        buf.writeInt(agility);
        buf.writeInt(intelligence);
        buf.writeInt(sense);
        buf.writeInt(luck);
        buf.writeInt(dexterity);
        buf.writeInt(mind);
        buf.writeInt(remainingPoints);
        buf.writeInt(currentMp);
        buf.writeInt(currentExp);
        buf.writeUtf(job, 64);
        buf.writeUtf(title, 64);
        buf.writeInt(questKills);
        buf.writeInt(questBlocksMined);
        buf.writeInt(questBlocksWalked);
        buf.writeBoolean(questCompleted);
        buf.writeBoolean(questRewardClaimed);
        buf.writeLong(questDay);

        // Skills
        buf.writeUtf(selectedClassId, 32);
        for (int i = 0; i < 4; i++) buf.writeInt(tierSP[i]);
        buf.writeInt(skillLevels.size());
        for (Map.Entry<SkillType, Integer> e : skillLevels.entrySet()) {
            buf.writeUtf(e.getKey().getId(), 32);
            buf.writeInt(e.getValue());
        }
        for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
            buf.writeUtf(equippedSlots[i], 32);
        }
        buf.writeInt(cooldowns.size());
        for (Map.Entry<SkillType, Integer> e : cooldowns.entrySet()) {
            buf.writeUtf(e.getKey().getId(), 32);
            buf.writeInt(e.getValue());
        }
        buf.writeInt(toggleStates.size());
        for (Map.Entry<SkillType, Boolean> e : toggleStates.entrySet()) {
            buf.writeUtf(e.getKey().getId(), 32);
            buf.writeBoolean(e.getValue());
        }
        buf.writeBoolean(shadowStrikeActive);
        buf.writeInt(domainTicks);
        buf.writeInt(armyTicks);
        buf.writeInt(deathMarkTicks);
        buf.writeInt(deathMarkTargetId);
        buf.writeInt(undyingWillCooldown);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            stats.setLevel(level);
            stats.setStrength(strength);
            stats.setVitality(vitality);
            stats.setAgility(agility);
            stats.setIntelligence(intelligence);
            stats.setSense(sense);
            stats.setLuck(luck);
            stats.setDexterity(dexterity);
            stats.setMind(mind);
            stats.setRemainingPoints(remainingPoints);
            stats.setCurrentMp(currentMp);
            stats.setCurrentExp(currentExp);
            stats.setJob(job);
            stats.setTitle(title);
            stats.setQuestKills(questKills);
            stats.setQuestBlocksMined(questBlocksMined);
            stats.setQuestBlocksWalked(questBlocksWalked);
            stats.setQuestCompleted(questCompleted);
            stats.setQuestRewardClaimed(questRewardClaimed);
            stats.setQuestDay(questDay);

            // Skills
            SkillData sd = stats.getSkillData();
            sd.setSelectedClass(PlayerClass.fromId(selectedClassId));
            for (int i = 0; i < 4; i++) sd.setTierSP(i, tierSP[i]);
            sd.getSkillLevels().clear();
            sd.getSkillLevels().putAll(skillLevels);
            for (int i = 0; i < SkillData.MAX_SLOTS; i++) {
                sd.setEquipped(i, SkillType.fromId(equippedSlots[i]));
            }
            sd.getCooldowns().clear();
            sd.getCooldowns().putAll(cooldowns);
            sd.getToggleStates().clear();
            sd.getToggleStates().putAll(toggleStates);
            sd.setShadowStrikeActive(shadowStrikeActive);
            sd.setDomainTicks(domainTicks);
            sd.setArmyTicks(armyTicks);
            sd.setDeathMarkTicks(deathMarkTicks);
            sd.setDeathMarkTargetId(deathMarkTargetId);
            sd.setUndyingWillCooldown(undyingWillCooldown);
        });

        ctx.get().setPacketHandled(true);
    }
}
