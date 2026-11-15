package slimeknights.tconstruct.fluids.block;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @deprecated use {@link slimeknights.mantle.block.fluid.MobEffectLiquidBlock}. Now just an alias of it, the entity logic lives in Mantle.
 * Effect cures are gone in 26.3, so instead of clearing the milk cure, Mantle prevents removing the effect while standing in the fluid.
 */
@Deprecated(forRemoval = true)
public class MobEffectLiquidBlock extends slimeknights.mantle.block.fluid.MobEffectLiquidBlock {
  public MobEffectLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, Supplier<MobEffectInstance> effect) {
    super(supplier, properties, effect);
  }

  /**
   * Creates a new block constructor for {@link slimeknights.mantle.registration.deferred.FluidDeferredRegister.Builder#block(BiFunction, Supplier)}.
   * Block properties need the block ID since 1.21.2, so they come from the register; this sets the map color and light level on them.
   */
  public static BiFunction<Supplier<? extends FlowingFluid>, BlockBehaviour.Properties, LiquidBlock> createEffect(MapColor color, int lightLevel, Supplier<MobEffectInstance> effect) {
    return (fluid, props) -> new MobEffectLiquidBlock(fluid, props.mapColor(color).lightLevel(state -> lightLevel), effect);
  }
}
