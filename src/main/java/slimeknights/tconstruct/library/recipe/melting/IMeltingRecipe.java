package slimeknights.tconstruct.library.recipe.melting;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;

/**
 * Common interface for all melting recipes
 */
public interface IMeltingRecipe extends ICustomOutputRecipe<IMeltingContainer> {
  /**
   * Gets a new instance of the output stack for this recipe
   * @param inv  Input inventory
   * @return  Output stack
   */
  FluidStack getOutput(IMeltingContainer inv);

  /**
   * Gets the minimum temperature to melt this item
   * @param inv  Inventory instance
   * @return  Recipe temperature
   */
  int getTemperature(IMeltingContainer inv);

  /**
   * Gets the time this recipe takes in 1/5th second increments. Smeltery updates every 4 ticks
   * @param inv  Inventory instance
   * @return  Recipe time
   */
  int getTime(IMeltingContainer inv);

  /**
   * Adds recipe byproducts into the given inventory, used in the foundry, but not all smelteries.
   * Override this method to add byproducts, as {@link #handleByproducts(IMeltingContainer, ResourceHandler)} calls it.
   * @param inv          Input inventory
   * @param handler      Fluid handler to fill with the byproduct
   * @param transaction  Transaction for filling the handler. The byproducts are only added if the caller commits it.
   */
  default void handleByproducts(IMeltingContainer inv, ResourceHandler<FluidResource> handler, TransactionContext transaction) {}

  /**
   * Adds recipe byproducts into the given inventory, used in the foundry, but not all smelteries.
   * Since 26.3 fluid handlers use transactions, this opens and commits a root transaction, so it may not be called while a transaction is open.
   * @param inv      Input inventory
   * @param handler  Fluid handler to fill with the byproduct
   * @see #handleByproducts(IMeltingContainer, ResourceHandler, TransactionContext)
   */
  default void handleByproducts(IMeltingContainer inv, ResourceHandler<FluidResource> handler) {
    try (Transaction transaction = Transaction.openRoot()) {
      handleByproducts(inv, handler, transaction);
      transaction.commit();
    }
  }

  /**
   * Fills the given fluid into the handler, replacing the old fluid handler fill call. Does nothing for an empty fluid.
   * @param handler      Handler to fill
   * @param fluid        Fluid to fill
   * @param transaction  Transaction for the fill
   * @return  Amount filled
   */
  static int fill(ResourceHandler<FluidResource> handler, FluidStack fluid, TransactionContext transaction) {
    if (fluid.isEmpty()) {
      return 0;
    }
    return handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
  }

  /* Recipe data */

  @Override
  default RecipeType<? extends Recipe<IMeltingContainer>> getType() {
    return TinkerRecipeTypes.MELTING.get();
  }

  /* Utils */

  double LOG9_2 = 0.31546487678;

  /** Gets the temperature for a fluid */
  static int getTemperature(Fluid fluid) {
    return fluid.getFluidType().getTemperature() - 300;
  }

  /** Gets the temperature for a fluid */
  static int getTemperature(FluidStack fluid) {
    return fluid.getFluid().getFluidType().getTemperature(fluid) - 300;
  }

  /** Gets the temperature for a fluid */
  static int getTemperature(FluidObject<?> fluid) {
    return fluid.getType().getTemperature() - 300;
  }

  /**
   * Calculates the temperature for a recipe based on the given temperature and factor
   * @param temperature  Required melting temperature in Celsius
   * @param factor       Multiplier based on material type
   * @return  Time for the recipe in celsius
   */
  static int calcTime(int temperature, float factor) {
    // base formula is temp^(.585), which will produce a time in 1/5th second increments
    return (int)Math.round((Math.pow(temperature + 300, 0.585f) * factor));
  }

  /**
   * Calculates a time factor for the given fluid amount
   * @param amount  Fluid amount
   * @return  Time factor
   */
  static float calcTimeFactor(int amount) {
    return (float)Math.sqrt(amount / (float)FluidValues.INGOT);
  }

  /**
   * Calculates the temperature for a recipe based on the given temperature and factor
   * @param temperature  Required melting temperature
   * @param amount       Amount of relevant fluid
   * @return  Time for the recipe in celsius
   */
  static int calcTimeForAmount(int temperature, int amount) {
    return calcTime(temperature, calcTimeFactor(amount));
  }
}
