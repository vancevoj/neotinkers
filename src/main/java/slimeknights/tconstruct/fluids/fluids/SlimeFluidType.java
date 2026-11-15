package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.tconstruct.common.TinkerTags;

/** Fluid type that does not drown slimes, so slimes and magma cubes can live in their own liquid. */
public class SlimeFluidType extends TextureFluidType {
  public SlimeFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public boolean canDrownIn(LivingEntity entity) {
    return !entity.is(TinkerTags.EntityTypes.SLIMES);
  }

  /**
   * Slime fluid type with the flipped in-world texture, used by ichor.
   * The flipped texture comes from the inverted fluid model (ichor is an {@link slimeknights.mantle.fluid.InvertedFluid}),
   * and the inverted client extension is registered in {@link slimeknights.tconstruct.fluids.FluidClientEvents}, as {@code initializeClient} no longer exists.
   */
  public static class Inverted extends SlimeFluidType {
    public Inverted(Properties properties) {
      super(properties);
    }
  }
}
