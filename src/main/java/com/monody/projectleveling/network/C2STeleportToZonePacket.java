package com.monody.projectleveling.network;

import com.monody.projectleveling.dimension.DungeonHelper;
import com.monody.projectleveling.dimension.ModDimensions;
import com.monody.projectleveling.mob.MobLevelUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2STeleportToZonePacket {
    private final int zoneIndex;

    public C2STeleportToZonePacket(int zoneIndex) {
        this.zoneIndex = zoneIndex;
    }

    public C2STeleportToZonePacket(FriendlyByteBuf buf) {
        this.zoneIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(zoneIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (zoneIndex < 0 || zoneIndex >= ModDimensions.NUM_DUNGEONS) return;

            if (DungeonHelper.teleportToZone(player, zoneIndex)) {
                int minLv = MobLevelUtil.getDungeonMinLevel(zoneIndex);
                int maxLv = MobLevelUtil.getDungeonMaxLevel(zoneIndex);
                player.sendSystemMessage(Component.literal(
                        "\u00a76[Dungeon]\u00a7r Entered Dungeon " + (zoneIndex + 1)
                                + " (Lv." + minLv + "-" + maxLv + ")"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
