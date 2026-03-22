package com.monody.projectleveling.entity.ninja;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FlyingRaijinKunaiRenderer extends ArrowRenderer<FlyingRaijinKunaiEntity> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/projectiles/spectral_arrow.png");

    public FlyingRaijinKunaiRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingRaijinKunaiEntity entity) {
        return TEXTURE;
    }
}
