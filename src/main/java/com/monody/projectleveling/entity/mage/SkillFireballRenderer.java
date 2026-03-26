package com.monody.projectleveling.entity.mage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class SkillFireballRenderer extends ThrownItemRenderer<SkillFireballEntity> {
    public SkillFireballRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0f, true);
    }

    @Override
    public void render(SkillFireballEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity.getFireballType() == SkillFireballEntity.FireballType.METEOR) {
            poseStack.pushPose();
            poseStack.scale(4.0f, 4.0f, 4.0f);
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
            poseStack.popPose();
        } else {
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
        }
    }
}
