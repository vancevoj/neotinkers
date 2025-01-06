package slimeknights.tconstruct.library.data.recipe;

import slimeknights.mantle.recipe.helper.FluidOutput;

/** Interface for a byproduct for datagen, not required but makes parameters easier */
public interface IByproduct {
  /** Name of this byproduct */
  String getName();

  /** If true, this byproduct is not conditional, it will always be present if the data genning mod is loaded */
  boolean isAlwaysPresent();

  /** Gets the fluid of this byproduct */
  FluidOutput getFluid(float scale);
}
