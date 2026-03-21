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

    public SkeletonMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonMinionEntity entity) {
        return SKELETON_TEXTURE;
    }
}
