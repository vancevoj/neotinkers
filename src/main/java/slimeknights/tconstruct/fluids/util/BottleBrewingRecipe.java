package slimeknights.tconstruct.fluids.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PotionIngredient;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;

import java.util.Optional;

/**
 * Recipe for transforming a bottle using a reagent, without caring about the potion contents.
 * <p>
 * Since 26.3 brewing is a datapack recipe type ({@link net.minecraft.world.item.crafting.RecipeType#BREWING}), so this is a vanilla brewing recipe
 * with plain item ingredients. It serializes as a vanilla brewing recipe; create it in datagen (see {@link slimeknights.tconstruct.fluids.TinkerFluids}).
 */
public class BottleBrewingRecipe extends BrewingRecipe {
  public BottleBrewingRecipe(Ingredient input, Ingredient ingredient, ItemStackTemplate output) {
    super(new PotionIngredient(input, Optional.empty()), new PotionIngredient(ingredient, Optional.empty()), output);
  }

  /** @deprecated item stacks cannot be created during datagen or recipe loading, use {@link #BottleBrewingRecipe(Ingredient, Ingredient, ItemStackTemplate)} */
  @Deprecated
  public BottleBrewingRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
    this(input, ingredient, ItemStackTemplate.fromNonEmptyStack(output));
  }

  /** @deprecated container-mix lookup is no longer available, the vanilla container mixes are data driven recipes; pass the ingredient explicitly instead */
  @Deprecated
  public BottleBrewingRecipe(Ingredient input, Item from, Item to, ItemStack output) {
    // brewing container mixes are datapack recipes since 26.3, so the from/to container-mix ingredient cannot be looked up when constructing the recipe
    // (already the case on 1.21.1, where the mixes were private). Unused, same as 1.21.1 the reagent never matches (empty ingredient).
    this(input, EmptyIngredient.VANILLA, output);
  }
}
