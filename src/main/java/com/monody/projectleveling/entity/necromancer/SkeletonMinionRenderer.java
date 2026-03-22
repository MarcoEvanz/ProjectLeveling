package com.monody.projectleveling.entity.necromancer;

import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class SkeletonMinionRenderer extends HumanoidMobRenderer<SkeletonMinionEntity, SkeletonModel<SkeletonMinionEntity>> {

    private static final ResourceLocation SKELETON_TEXTURE =
            new ResourceLocation("textures/entity/skeleton/skeleton.png");
    private static final ResourceLocation WITHER_SKELETON_TEXTURE =
            new ResourceLocation("textures/entity/skeleton/wither_skeleton.png");

    public SkeletonMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(SkeletonMinionEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        if (entity.isWitherVariant()) {
            poseStack.scale(1.2f, 1.2f, 1.2f);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonMinionEntity entity) {
        return entity.isWitherVariant() ? WITHER_SKELETON_TEXTURE : SKELETON_TEXTURE;
    }
}
