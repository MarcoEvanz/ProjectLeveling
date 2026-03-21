package com.monody.projectleveling.network;

import com.monody.projectleveling.capability.PlayerStatsCapability;
import com.monody.projectleveling.event.StatEventHandler;
import com.monody.projectleveling.skill.PlayerClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSelectClassPacket {
    private final String classId;

    public C2SSelectClassPacket(String classId) {
        this.classId = classId;
    }

    public C2SSelectClassPacket(FriendlyByteBuf buf) {
        this.classId = buf.readUtf(32);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(classId, 32);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;

        player.getCapability(PlayerStatsCapability.PLAYER_STATS).ifPresent(stats -> {
            // Can only select class once, at level 10+
            if (stats.getSkillData().getSelectedClass() != PlayerClass.NONE) return;
            if (stats.getLevel() < 10) return;

            PlayerClass cls = PlayerClass.fromId(classId);
            if (cls == PlayerClass.NONE) return;

            stats.getSkillData().setSelectedClass(cls);
            StatEventHandler.syncToClient(player);
        });

        ctx.get().setPacketHandled(true);
    }
}
