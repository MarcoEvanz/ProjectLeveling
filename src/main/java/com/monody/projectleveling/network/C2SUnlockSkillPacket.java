package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.SkillType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SUnlockSkillPacket {
    private final String skillId;
    private final int count;

    public C2SUnlockSkillPacket(String skillId) {
        this(skillId, 1);
    }

    public C2SUnlockSkillPacket(String skillId, int count) {
        this.skillId = skillId;
        this.count = Math.max(1, count);
    }

    public C2SUnlockSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf(32);
        this.count = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId, 32);
        buf.writeInt(count);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            SkillType skill = SkillType.fromId(skillId);
            if (skill == null) return;
            boolean changed = false;
            for (int i = 0; i < count; i++) {
                if (!stats.getSkillData().unlockOrLevel(skill, stats.getLevel())) break;
                changed = true;
            }
            if (changed) StatEventHandler.syncToClient(player);
        });

        ctx.get().setPacketHandled(true);
    }
}
