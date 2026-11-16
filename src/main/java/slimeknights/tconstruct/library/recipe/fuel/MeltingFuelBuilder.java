package slimeknights.tconstruct.library.recipe.fuel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import static slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe.getTemperature;

/**
 * Builds a new recipe for a melter or smeltery fuel
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MeltingFuelBuilder extends AbstractRecipeBuilder<MeltingFuelBuilder> {
  private final FluidIngredient input;
  private final int duration;
  private final int temperature;
  @Setter
  @Accessors(fluent = true)
  private int rate;

  /**
   * Creates a new builder instance with set temperature
   * @param fluid     Fluid stack
   * @param duration  Fluid duration
   * @return  Builder instance
   */
  public static MeltingFuelBuilder fuel(FluidIngredient fluid, int duration, int temperature) {
    return new MeltingFuelBuilder(fluid, duration, temperature, temperature / 100);
  }

  /**
   * Creates a new builder instance with automatic temperature
   * @param fluid     Fluid stack
   * @param duration  Fluid duration
   * @return  Builder instance
   */
  public static MeltingFuelBuilder fuel(FluidStack fluid, int duration) {
    return fuel(FluidIngredient.of(fluid), duration, getTemperature(fluid));
  }

  /**
   * Creates a new builder instance with automatic temperature. Unlike {@link #fuel(FluidStack, int)}, safe to use in datagen as no stack is created.
   * @param fluid     Fuel fluid
   * @param amount    Fluid amount consumed
   * @param duration  Fluid duration
   * @return  Builder instance
   */
  public static MeltingFuelBuilder fuel(Fluid fluid, int amount, int duration) {
    return fuel(FluidIngredient.of(fluid, amount), duration, getTemperature(fluid));
  }

  /** Setups the builder for solid fuel */
  @Internal
  public static MeltingFuelBuilder solid(int temperature) {
    return fuel(FluidIngredient.EMPTY, 0, temperature);
  }

  @Override
  public void save(RecipeOutput consumer) {
    if (input.getFluids().isEmpty()) {
      throw new IllegalStateException("Must have at least one fluid for dynamic input");
    }
    save(consumer, Loadables.FLUID.getKey(input.getFluids().get(0).getFluid()));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    AdvancementHolder advancement = this.buildOptionalAdvancement(consumer, id, "melting_fuel");
    consumer.accept(recipeKey(id), new MeltingFuel(id, input, duration, temperature, rate), advancement);
  }
}
