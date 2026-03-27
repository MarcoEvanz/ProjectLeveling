package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.skill.PlayerClass;
import com.monody.projectleveling.skill.SkillData;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncStatsPacket {
    // Core stats
    private final int level, strength, vitality, agility, intelligence, sight, luck, dexterity, mind, faith;
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
    private final int fervorTicks;
    // Ninja state
    private final int substitutionTicks;
    private final boolean rasenganBuffActive;
    private final int rasenganBuffTicks;
    private final int flyingRaijinPhase;
    private final int frgPhase;
    private final int frgTicks;
    // Beast Master state
    private final String bmActiveBuffId;
    private final int bmBuffTicks;
    private final boolean bmEnhanced;
    private final boolean powerOfNatureActive;
    private final int turtleShellTicks;
    private final int phoenixLifestealHits;
    private final float phoenixLifestealPct;
    private final int tigerClawHitsLeft;
    private final int tigerClawTargetId;
    private final float tigerClawDmg;
    private final int tigerClawTimer;
    // Warrior state
    private final boolean slashBlastActive;
    private final int slashBlastTicks;
    private final float slashBlastPct;
    private final int warCryTicks;
    private final float warCryAtkBonus;
    private final int spiritBladeTicks;
    private final float spiritBladeAtk;
    private final boolean spiritBladeDefActive;
    private final int unbreakableCooldown;
    // Limitless state
    private final boolean blueChanneling;
    private final int blueChannelTicks;
    // Limitless: Red state
    private final boolean redChanneling;
    private final int redChannelTicks;
    // Limitless: Purple state
    private final boolean purpleChanneling;
    private final int purpleChannelTicks;
    // Healer state
    private final int blessTicks;
    private final int benedictionTicks;
    // Generic channeling bar
    private final int channelTicks;
    private final int channelMaxTicks;
    private final String channelSkillName;

    public S2CSyncStatsPacket(PlayerStats stats) {
        this.level = stats.getLevel();
        this.strength = stats.getStrength();
        this.vitality = stats.getVitality();
        this.agility = stats.getAgility();
        this.intelligence = stats.getIntelligence();
        this.sight = stats.getSight();
        this.luck = stats.getLuck();
        this.dexterity = stats.getDexterity();
        this.mind = stats.getMind();
        this.faith = stats.getFaith();
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
        this.tierSP = new int[]{sd.getTierSP(0), sd.getTierSP(1), sd.getTierSP(2), sd.getTierSP(3), sd.getTierSP(4)};
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
        this.fervorTicks = sd.getFervorTicks();
        this.substitutionTicks = sd.getSubstitutionTicks();
        this.rasenganBuffActive = sd.isRasenganBuffActive();
        this.rasenganBuffTicks = sd.getRasenganBuffTicks();
        this.flyingRaijinPhase = sd.getFlyingRaijinPhase();
        this.frgPhase = sd.getFrgPhase();
        this.frgTicks = sd.getFrgTicks();
        this.bmActiveBuffId = sd.getBmActiveBuff() != null ? sd.getBmActiveBuff().getId() : "";
        this.bmBuffTicks = sd.getBmBuffTicks();
        this.bmEnhanced = sd.isBmEnhanced();
        this.powerOfNatureActive = sd.isPowerOfNatureActive();
        this.turtleShellTicks = sd.getTurtleShellTicks();
        this.phoenixLifestealHits = sd.getPhoenixLifestealHits();
        this.phoenixLifestealPct = sd.getPhoenixLifestealPct();
        this.tigerClawHitsLeft = sd.getTigerClawHitsLeft();
        this.tigerClawTargetId = sd.getTigerClawTargetId();
        this.tigerClawDmg = sd.getTigerClawDmg();
        this.tigerClawTimer = sd.getTigerClawTimer();
        this.slashBlastActive = sd.isSlashBlastActive();
        this.slashBlastTicks = sd.getSlashBlastTicks();
        this.slashBlastPct = sd.getSlashBlastPct();
        this.warCryTicks = sd.getWarCryTicks();
        this.warCryAtkBonus = sd.getWarCryAtkBonus();
        this.spiritBladeTicks = sd.getSpiritBladeTicks();
        this.spiritBladeAtk = sd.getSpiritBladeAtk();
        this.spiritBladeDefActive = sd.isSpiritBladeDefActive();
        this.unbreakableCooldown = sd.getUnbreakableCooldown();
        this.blueChanneling = sd.isBlueChanneling();
        this.blueChannelTicks = sd.getBlueChannelTicks();
        this.redChanneling = sd.isRedChanneling();
        this.redChannelTicks = sd.getRedChannelTicks();
        this.purpleChanneling = sd.isPurpleChanneling();
        this.purpleChannelTicks = sd.getPurpleChannelTicks();
        this.blessTicks = sd.getBlessTicks();
        this.benedictionTicks = sd.getBenedictionTicks();
        this.channelTicks = sd.getChannelTicks();
        this.channelMaxTicks = sd.getChannelMaxTicks();
        this.channelSkillName = sd.getChannelSkillName();
    }

    public S2CSyncStatsPacket(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        this.strength = buf.readInt();
        this.vitality = buf.readInt();
        this.agility = buf.readInt();
        this.intelligence = buf.readInt();
        this.sight = buf.readInt();
        this.luck = buf.readInt();
        this.dexterity = buf.readInt();
        this.mind = buf.readInt();
        this.faith = buf.readInt();
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
        this.tierSP = new int[5];
        for (int i = 0; i < 5; i++) tierSP[i] = buf.readInt();
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
        this.fervorTicks = buf.readInt();
        this.substitutionTicks = buf.readInt();
        this.rasenganBuffActive = buf.readBoolean();
        this.rasenganBuffTicks = buf.readInt();
        this.flyingRaijinPhase = buf.readInt();
        this.frgPhase = buf.readInt();
        this.frgTicks = buf.readInt();
        this.bmActiveBuffId = buf.readUtf(32);
        this.bmBuffTicks = buf.readInt();
        this.bmEnhanced = buf.readBoolean();
        this.powerOfNatureActive = buf.readBoolean();
        this.turtleShellTicks = buf.readInt();
        this.phoenixLifestealHits = buf.readInt();
        this.phoenixLifestealPct = buf.readFloat();
        this.tigerClawHitsLeft = buf.readInt();
        this.tigerClawTargetId = buf.readInt();
        this.tigerClawDmg = buf.readFloat();
        this.tigerClawTimer = buf.readInt();
        this.slashBlastActive = buf.readBoolean();
        this.slashBlastTicks = buf.readInt();
        this.slashBlastPct = buf.readFloat();
        this.warCryTicks = buf.readInt();
        this.warCryAtkBonus = buf.readFloat();
        this.spiritBladeTicks = buf.readInt();
        this.spiritBladeAtk = buf.readFloat();
        this.spiritBladeDefActive = buf.readBoolean();
        this.unbreakableCooldown = buf.readInt();
        this.blueChanneling = buf.readBoolean();
        this.blueChannelTicks = buf.readInt();
        this.redChanneling = buf.readBoolean();
        this.redChannelTicks = buf.readInt();
        this.purpleChanneling = buf.readBoolean();
        this.purpleChannelTicks = buf.readInt();
        this.blessTicks = buf.readInt();
        this.benedictionTicks = buf.readInt();
        this.channelTicks = buf.readInt();
        this.channelMaxTicks = buf.readInt();
        this.channelSkillName = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(strength);
        buf.writeInt(vitality);
        buf.writeInt(agility);
        buf.writeInt(intelligence);
        buf.writeInt(sight);
        buf.writeInt(luck);
        buf.writeInt(dexterity);
        buf.writeInt(mind);
        buf.writeInt(faith);
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
        for (int i = 0; i < 5; i++) buf.writeInt(tierSP[i]);
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
        buf.writeInt(fervorTicks);
        buf.writeInt(substitutionTicks);
        buf.writeBoolean(rasenganBuffActive);
        buf.writeInt(rasenganBuffTicks);
        buf.writeInt(flyingRaijinPhase);
        buf.writeInt(frgPhase);
        buf.writeInt(frgTicks);
        buf.writeUtf(bmActiveBuffId, 32);
        buf.writeInt(bmBuffTicks);
        buf.writeBoolean(bmEnhanced);
        buf.writeBoolean(powerOfNatureActive);
        buf.writeInt(turtleShellTicks);
        buf.writeInt(phoenixLifestealHits);
        buf.writeFloat(phoenixLifestealPct);
        buf.writeInt(tigerClawHitsLeft);
        buf.writeInt(tigerClawTargetId);
        buf.writeFloat(tigerClawDmg);
        buf.writeInt(tigerClawTimer);
        buf.writeBoolean(slashBlastActive);
        buf.writeInt(slashBlastTicks);
        buf.writeFloat(slashBlastPct);
        buf.writeInt(warCryTicks);
        buf.writeFloat(warCryAtkBonus);
        buf.writeInt(spiritBladeTicks);
        buf.writeFloat(spiritBladeAtk);
        buf.writeBoolean(spiritBladeDefActive);
        buf.writeInt(unbreakableCooldown);
        buf.writeBoolean(blueChanneling);
        buf.writeInt(blueChannelTicks);
        buf.writeBoolean(redChanneling);
        buf.writeInt(redChannelTicks);
        buf.writeBoolean(purpleChanneling);
        buf.writeInt(purpleChannelTicks);
        buf.writeInt(blessTicks);
        buf.writeInt(benedictionTicks);
        buf.writeInt(channelTicks);
        buf.writeInt(channelMaxTicks);
        buf.writeUtf(channelSkillName, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::applyOnClient);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void applyOnClient() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            stats.setLevel(level);
            stats.setStrength(strength);
            stats.setVitality(vitality);
            stats.setAgility(agility);
            stats.setIntelligence(intelligence);
            stats.setSight(sight);
            stats.setLuck(luck);
            stats.setDexterity(dexterity);
            stats.setMind(mind);
            stats.setFaith(faith);
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
            for (int i = 0; i < 5; i++) sd.setTierSP(i, tierSP[i]);
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
            sd.setFervorTicks(fervorTicks);
            sd.setSubstitutionTicks(substitutionTicks);
            sd.setRasenganBuffActive(rasenganBuffActive);
            sd.setRasenganBuffTicks(rasenganBuffTicks);
            sd.setFlyingRaijinPhase(flyingRaijinPhase);
            sd.setFrgPhase(frgPhase);
            sd.setFrgTicks(frgTicks);
            sd.setBmActiveBuff(bmActiveBuffId.isEmpty() ? null : SkillType.fromId(bmActiveBuffId));
            sd.setBmBuffTicks(bmBuffTicks);
            sd.setBmEnhanced(bmEnhanced);
            sd.setPowerOfNatureActive(powerOfNatureActive);
            sd.setTurtleShellTicks(turtleShellTicks);
            sd.setPhoenixLifestealHits(phoenixLifestealHits);
            sd.setPhoenixLifestealPct(phoenixLifestealPct);
            sd.setTigerClawHitsLeft(tigerClawHitsLeft);
            sd.setTigerClawTargetId(tigerClawTargetId);
            sd.setTigerClawDmg(tigerClawDmg);
            sd.setTigerClawTimer(tigerClawTimer);
            sd.setSlashBlastActive(slashBlastActive);
            sd.setSlashBlastTicks(slashBlastTicks);
            sd.setSlashBlastPct(slashBlastPct);
            sd.setWarCryTicks(warCryTicks);
            sd.setWarCryAtkBonus(warCryAtkBonus);
            sd.setSpiritBladeTicks(spiritBladeTicks);
            sd.setSpiritBladeAtk(spiritBladeAtk);
            sd.setSpiritBladeDefActive(spiritBladeDefActive);
            sd.setUnbreakableCooldown(unbreakableCooldown);
            sd.setBlueChanneling(blueChanneling);
            sd.setBlueChannelTicks(blueChannelTicks);
            sd.setRedChanneling(redChanneling);
            sd.setRedChannelTicks(redChannelTicks);
            sd.setPurpleChanneling(purpleChanneling);
            sd.setPurpleChannelTicks(purpleChannelTicks);
            sd.setBlessTicks(blessTicks);
            sd.setBenedictionTicks(benedictionTicks);
            sd.setChannelTicks(channelTicks);
            sd.setChannelMaxTicks(channelMaxTicks);
            sd.setChannelSkillName(channelSkillName);
        });
    }
}
