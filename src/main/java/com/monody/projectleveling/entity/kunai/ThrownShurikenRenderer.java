package com.monody.projectleveling.entity.kunai;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ThrownShurikenRenderer extends EntityRenderer<ThrownShurikenEntity> {
    private final ItemRenderer itemRenderer;

    public ThrownShurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownShurikenEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        ItemStack stack = entity.getItem();

        // Orient along flight direction
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));

        // Flat disc spin around the face normal
        if (!entity.isStuck()) {
            float spin = (entity.tickCount + partialTick) * 40.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        }

        float scale = 0.65F;
        poseStack.scale(scale, scale, scale);

        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());
        itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, buffer,
                packedLight, OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownShurikenEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
