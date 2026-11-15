package slimeknights.tconstruct.fluids.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @deprecated use {@link slimeknights.mantle.block.fluid.BurningLiquidBlock}. Now just an alias of it, the entity logic lives in Mantle.
 */
@Deprecated(forRemoval = true)
public class BurningLiquidBlock extends slimeknights.mantle.block.fluid.BurningLiquidBlock {
  public BurningLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, int burnTime, float damage) {
    super(supplier, properties, burnTime, damage);
  }

  /**
   * Creates a new block constructor for {@link slimeknights.mantle.registration.deferred.FluidDeferredRegister.Builder#block(BiFunction, Supplier)}.
   * Block properties need the block ID since 1.21.2, so they come from the register; this sets the map color and light level on them.
   */
  public static BiFunction<Supplier<? extends FlowingFluid>, BlockBehaviour.Properties, LiquidBlock> createBurning(MapColor color, int lightLevel, int burnTime, float damage) {
    return (fluid, props) -> new BurningLiquidBlock(fluid, props.mapColor(color).lightLevel(state -> lightLevel), burnTime, damage);
  }
}
