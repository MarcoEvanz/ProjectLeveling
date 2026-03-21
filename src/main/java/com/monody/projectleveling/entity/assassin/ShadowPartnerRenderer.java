package com.monody.projectleveling.entity.assassin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ShadowPartnerRenderer extends HumanoidMobRenderer<ShadowPartnerEntity, PlayerModel<ShadowPartnerEntity>> {

    public ShadowPartnerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowPartnerEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID != null) {
            PlayerInfo info = Minecraft.getInstance().getConnection() != null
                    ? Minecraft.getInstance().getConnection().getPlayerInfo(ownerUUID)
                    : null;
            if (info != null) {
                return info.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public void render(ShadowPartnerEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Completely hidden during stealth (skip model + armor + items)
        if (entity.isInvisible()) return;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected RenderType getRenderType(ShadowPartnerEntity entity, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        ResourceLocation texture = getTextureLocation(entity);
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected void scale(ShadowPartnerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
