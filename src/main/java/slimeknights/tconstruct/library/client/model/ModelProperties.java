package slimeknights.tconstruct.library.client.model;

import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.fluids.FluidStack;

/** Model data properties used in Tinker's Construct */
public class ModelProperties {
  /** Property for fluid stack in a fluid model */
  public static final ModelProperty<FluidStack> FLUID_STACK = new ModelProperty<>();
  /** Maximum size for a fluid tank in a tank model */
  public static final ModelProperty<Integer> TANK_CAPACITY = new ModelProperty<>();
}
