package slimeknights.tconstruct.shared.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.tconstruct.shared.particle.FluidParticleData;

/** Particle type that renders a fluid still texture */
public class FluidParticle extends SingleQuadParticle {
  private final FluidStack fluid;
  private final float uCoord;
  private final float vCoord;
  protected FluidParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ, FluidStack fluid) {
    this(world, x, y, z, motionX, motionY, motionZ, fluid, FluidSprites.of(fluid));
  }

  private FluidParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ, FluidStack fluid, FluidSprites sprites) {
    // fluid textures and tints come from the fluid model since 26.3, sprite is a constructor argument now
    super(world, x, y, z, motionX, motionY, motionZ, sprites.still());
    this.fluid = fluid;
    this.gravity = 1.0F;
    int color = sprites.color();
    this.alpha = ((color >> 24) & 0xFF) / 255f;
    this.rCol   = ((color >> 16) & 0xFF) / 255f;
    this.gCol = ((color >>  8) & 0xFF) / 255f;
    this.bCol  = ( color        & 0xFF) / 255f;
    this.quadSize /= 2.0F;
    this.uCoord = this.random.nextFloat() * 3.0F;
    this.vCoord = this.random.nextFloat() * 3.0F;
  }

  @Override
  protected Layer getLayer() {
    // was TERRAIN_SHEET, which blended. Keep translucency for tints with alpha, else let the sprite decide
    if (this.alpha < 1.0F) {
      return Layer.TRANSLUCENT_TERRAIN;
    }
    return Layer.bySprite(this.sprite);
  }

  @Override
  protected float getU0() {
    return this.sprite.getU((this.uCoord + 1.0F) / 4.0F);
  }

  @Override
  protected float getU1() {
    return this.sprite.getU(this.uCoord / 4.0F);
  }

  @Override
  protected float getV0() {
    return this.sprite.getV(this.vCoord / 4.0F);
  }

  @Override
  protected float getV1() {
    return this.sprite.getV((this.vCoord + 1.0F) / 4.0F);
  }

  @Override
  public int getLightCoords(float partialTick) {
    return FluidRenderer.withBlockLight(super.getLightCoords(partialTick), fluid.getFluid().getFluidType().getLightLevel(fluid));
  }

  /** Factory to create a fluid particle */
  public static class Factory implements ParticleProvider<FluidParticleData> {
    @Override
    public Particle createParticle(FluidParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
      FluidStack fluid = data.getFluid();
      return !fluid.isEmpty() ? new FluidParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, fluid) : null;
    }
  }
}
