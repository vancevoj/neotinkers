package slimeknights.tconstruct.smeltery.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.data.recipe.IByproduct;
import slimeknights.tconstruct.library.recipe.FluidValues;

import java.util.Locale;

/** Standard ore byproducts for smeltery ores, this enum exists to simplify our builders to allow passing 3 args in varargs */
@RequiredArgsConstructor
public enum Byproduct implements IByproduct {
  // base metals
  COPPER    (true, TinkerFluids.moltenCopper),
  IRON      (true, TinkerFluids.moltenIron),
  GOLD      (true, TinkerFluids.moltenGold),
  SMALL_GOLD("gold", true, TinkerFluids.moltenGold, FluidValues.NUGGET * 3),
  COBALT    (true, TinkerFluids.moltenCobalt),
  // compat metals
  TIN     (false, TinkerFluids.moltenTin),
  SILVER  (false, TinkerFluids.moltenSilver),
  NICKEL  (false, TinkerFluids.moltenNickel),
  LEAD    (false, TinkerFluids.moltenLead),
  PLATINUM("platinum", false, TinkerFluids.moltenPlatinum, FluidValues.NUGGET * 3),
  // gems
  DIAMOND ("diamond",  true, TinkerFluids.moltenDiamond, FluidValues.GEM),
  AMETHYST("amethyst", true, TinkerFluids.moltenAmethyst, FluidValues.GEM),
  QUARTZ  ("quartz",   true, TinkerFluids.moltenQuartz, FluidValues.GEM);

  @Getter
  private final String name;
  @Getter
  private final boolean alwaysPresent;
  @Getter
  private final FluidObject<?> fluid;
  @Getter
  private final int amount;

  Byproduct(boolean alwaysPresent, FluidObject<?> fluid) {
    this.name = name().toLowerCase(Locale.ROOT);
    this.alwaysPresent = alwaysPresent;
    this.fluid = fluid;
    this.amount = FluidValues.INGOT;
  }

  @Override
  public FluidOutput getFluid(float scale) {
    return fluid.result((int)(amount * scale));
  }
}
