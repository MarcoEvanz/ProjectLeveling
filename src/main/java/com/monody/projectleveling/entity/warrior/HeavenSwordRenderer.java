package com.monody.projectleveling.entity.warrior;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.monody.projectleveling.item.ModItems;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class HeavenSwordRenderer extends EntityRenderer<HeavenSwordEntity> {
    private final ItemRenderer itemRenderer;
    private ItemStack swordStack;

    public HeavenSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private ItemStack getSwordStack() {
        if (swordStack == null) {
            swordStack = new ItemStack(ModItems.GOLD_SWORD.get());
        }
        return swordStack;
    }

    @Override
    public void render(HeavenSwordEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Y-axis billboard: flat face always toward the camera
        float cameraYRot = this.entityRenderDispatcher.camera.getYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - cameraYRot));

        // At 225° the blade is horizontal (right). -90° more to point straight down.
        poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));

        // Scale up for giant sword
        float scale = 5.0F;
        poseStack.scale(scale, scale, scale);

        ItemStack stack = getSwordStack();
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());
        itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, buffer,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HeavenSwordEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
