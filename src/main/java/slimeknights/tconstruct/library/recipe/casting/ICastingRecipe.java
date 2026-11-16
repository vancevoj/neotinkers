package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.ICommonRecipe;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

import static slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe.getTemperature;

/**
 * Base interface for all casting recipes
 */
public interface ICastingRecipe extends ICommonRecipe<ICastingContainer> {
  /** Gets the ID of this recipe. 1.21: recipes no longer know their own ID, so casting recipes keep it for display recipes */
  Identifier getId();

  /**
   * Gets the amount of fluid required for this recipe
   * @param inv  Inventory instance
   * @return Fluid amount when using the fluid in the inventory
   */
  int getFluidAmount(ICastingContainer inv);

  /**
   * TODO 1.21: move this method to {@link IDisplayableCastingRecipe}.
   * {@return true if the cast item is consumed on crafting}
   */
  boolean isConsumed();

  /** {@return true if the cast item is consumed on crafting} */
  default boolean isConsumed(ICastingContainer inv) {
    return isConsumed();
  }

  /** {@return true if the recipe output is placed into the casting input slot} */
  boolean switchSlots();

  /**
   * @param inv ICastingInventory for casting recipe
   * {@return cooling time for the output}
   */
  int getCoolingTime(ICastingContainer inv);

  /**
   * Calculates the cooling time for a recipe based on the amount and temperature
   * @param temperature  Temperature baseline in celsius
   * @param amount       Output amount
   * @return  Cooling time based on the given inputs
   */
  static int calcCoolingTime(int temperature, int amount) {
    // the time in melting recipes assumes updating 5 times a second
    // we update 20 times a second, so get roughly a quarter of those values
    return IMeltingRecipe.calcTimeForAmount(temperature, amount);
  }

  /**
   * Calculates the cooling time for a recipe based on the fluid input
   * @param fluid  Fluid input
   * @return  Time for the recipe
   */
  static int calcCoolingTime(FluidStack fluid) {
    return calcCoolingTime(getTemperature(fluid), fluid.getAmount());
  }


  /* Type aware helpers */

  /**
   * Gets the recipe type from a type aware serializer for use in {@link Recipe#getType()}.
   * Since 1.21.2 the return type is bound by the recipe input, which the type aware serializer cannot express.
   * @param serializer  Type aware serializer from the recipe loadable
   * @return  Recipe type
   */
  @SuppressWarnings("unchecked")
  static RecipeType<? extends Recipe<ICastingContainer>> castingType(TypeAwareRecipeSerializer<?> serializer) {
    return (RecipeType<? extends Recipe<ICastingContainer>>)serializer.getType();
  }

  /**
   * Gets the registered serializer from a type aware serializer for use in {@link Recipe#getSerializer()}.
   * Since 26.1 a type aware serializer is no longer a {@link RecipeSerializer}, the recipe must return the registered serializer record.
   * @param serializer  Type aware serializer from the recipe loadable
   * @return  Registered recipe serializer
   */
  @SuppressWarnings("unchecked")
  static RecipeSerializer<? extends Recipe<ICastingContainer>> castingSerializer(TypeAwareRecipeSerializer<?> serializer) {
    return (RecipeSerializer<? extends Recipe<ICastingContainer>>)(RecipeSerializer<?>)serializer.getSerializer();
  }
}
