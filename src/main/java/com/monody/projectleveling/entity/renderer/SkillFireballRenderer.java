package com.monody.projectleveling.entity.renderer;

import com.monody.projectleveling.entity.SkillFireballEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class SkillFireballRenderer extends ThrownItemRenderer<SkillFireballEntity> {
    public SkillFireballRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0f, true);
    }
}
