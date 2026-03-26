package com.monody.projectleveling.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * END_ROD clone with 2-tick lifetime.
 * Uses PARTICLE_SHEET_TRANSLUCENT (additive blending) for the soft glow — same as vanilla END_ROD.
 */
@OnlyIn(Dist.CLIENT)
public class ShortEndRodParticle extends TextureSheetParticle {

    ShortEndRodParticle(ClientLevel level, double x, double y, double z,
                        double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize *= 0.75F;
        this.lifetime = 1;
        this.pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new ShortEndRodParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
