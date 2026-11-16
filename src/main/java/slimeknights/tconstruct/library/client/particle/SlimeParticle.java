package slimeknights.tconstruct.library.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;

import javax.annotation.Nullable;

// not part of the tic particle system since it uses vanilla particles
public class SlimeParticle extends BreakingItemParticle {

  public SlimeParticle(ClientLevel worldIn, double posXIn, double posYIn, double posZIn, TextureAtlasSprite sprite) {
    super(worldIn, posXIn, posYIn, posZIn, sprite);
  }

  public SlimeParticle(ClientLevel worldIn, double posXIn, double posYIn, double posZIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, TextureAtlasSprite sprite) {
    super(worldIn, posXIn, posYIn, posZIn, xSpeedIn, ySpeedIn, zSpeedIn, sprite);
  }

  /** Factory creating slime particles from the given item's particle sprite */
  public static class Factory extends ItemParticleProvider<SimpleParticleType> {
    private final ItemLike slime;

    public Factory(ItemLike slime) {
      this.slime = slime;
    }

    public Factory(SlimeType type) {
      this(TinkerCommons.slimeball.get(type));
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
      // particle sprites now come from the item model, the item stack is no longer passed to the particle
      return new SlimeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, getSprite(new ItemStackTemplate(slime.asItem()), worldIn, random));
    }
  }
}
