package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStats;
import com.monody.projectleveling.capability.PlayerStatsCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncStatsPacket {
    private final int level, strength, vitality, agility, intelligence, sense;
    private final int remainingPoints, currentMp, currentExp;
    private final String job, title;
    private final int questKills, questBlocksMined, questBlocksWalked;
    private final boolean questCompleted;
    private final boolean questRewardClaimed;
    private final long questDay;

    public S2CSyncStatsPacket(PlayerStats stats) {
        this.level = stats.getLevel();
        this.strength = stats.getStrength();
        this.vitality = stats.getVitality();
        this.agility = stats.getAgility();
        this.intelligence = stats.getIntelligence();
        this.sense = stats.getSense();
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
    }

    public S2CSyncStatsPacket(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        this.strength = buf.readInt();
        this.vitality = buf.readInt();
        this.agility = buf.readInt();
        this.intelligence = buf.readInt();
        this.sense = buf.readInt();
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
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(strength);
        buf.writeInt(vitality);
        buf.writeInt(agility);
        buf.writeInt(intelligence);
        buf.writeInt(sense);
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
        });

        ctx.get().setPacketHandled(true);
    }
}
