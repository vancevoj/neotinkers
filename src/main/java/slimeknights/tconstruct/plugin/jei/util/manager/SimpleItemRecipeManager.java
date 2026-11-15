package slimeknights.tconstruct.plugin.jei.util.manager;

import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.library.recipe.display.FilteredItemRecipe;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Recipe manager handling filtering recipes that use just items for inputs and outputs. Used notably in {@link slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringCategory}. */
public class SimpleItemRecipeManager<T extends FilteredItemRecipe> implements ISimpleRecipeManagerPlugin<T> {
  @Getter
  private final List<T> allRecipes;
  private final ItemRecipeCache<T> inputItemCache, outputItemCache;

  public SimpleItemRecipeManager(IIngredientManager ingredientManager, List<T> recipes) {
    this.allRecipes = FilteredRecipe.alwaysVisible(recipes);
    IIngredientHelper<ItemStack> itemHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
    inputItemCache = new ItemRecipeCache<>(itemHelper, recipes, false);
    outputItemCache = new ItemRecipeCache<>(itemHelper, recipes, true);
  }

  @Override
  public boolean isHandledInput(ITypedIngredient<?> input) {
    return input.getType() == VanillaTypes.ITEM_STACK;
  }

  @Override
  public boolean isHandledOutput(ITypedIngredient<?> output) {
    return output.getType() == VanillaTypes.ITEM_STACK;
  }

  @Override
  public List<T> getRecipesForInput(ITypedIngredient<?> input) {
    ITypedIngredient<ItemStack> item = input.cast(VanillaTypes.ITEM_STACK);
    if (item != null) {
      return inputItemCache.filterRecipes(item.getIngredient());
    }
    return List.of();
  }

  @Override
  public List<T> getRecipesForOutput(ITypedIngredient<?> output) {
    ITypedIngredient<ItemStack> item = output.cast(VanillaTypes.ITEM_STACK);
    if (item != null) {
      return outputItemCache.filterRecipes(item.getIngredient());
    }
    return List.of();
  }

  /**
   * Creates an instance of the category for the crafting table.
   * 1.21: the JEI crafting category works on {@link RecipeHolder}s, so the recipes are passed in as holders
   * and the filtered results are mapped back to them.
   */
  public static <T extends CraftingRecipe & FilteredItemRecipe> ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> createCrafting(IIngredientManager ingredientManager, List<RecipeHolder<T>> recipes) {
    return new HolderRecipeManager<>(ingredientManager, recipes);
  }

  /** Wrapper around {@link SimpleItemRecipeManager} that returns recipe holders, as the crafting category expects. */
  private static class HolderRecipeManager<T extends CraftingRecipe & FilteredItemRecipe> implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
    private final SimpleItemRecipeManager<T> manager;
    private final Map<T, RecipeHolder<CraftingRecipe>> holders;
    @Getter
    private final List<RecipeHolder<CraftingRecipe>> allRecipes;

    @SuppressWarnings("unchecked")
    private HolderRecipeManager(IIngredientManager ingredientManager, List<RecipeHolder<T>> recipes) {
      this.holders = new IdentityHashMap<>(recipes.size());
      List<T> values = new ArrayList<>(recipes.size());
      for (RecipeHolder<T> holder : recipes) {
        T recipe = holder.value();
        values.add(recipe);
        // holders are read only, so its safe to treat a holder of T as a holder of crafting recipe
        holders.put(recipe, (RecipeHolder<CraftingRecipe>) (RecipeHolder<?>) holder);
      }
      this.manager = new SimpleItemRecipeManager<>(ingredientManager, values);
      this.allRecipes = toHolders(manager.getAllRecipes());
    }

    /** Maps the list of recipes to their holders */
    private List<RecipeHolder<CraftingRecipe>> toHolders(List<T> recipes) {
      if (recipes.isEmpty()) {
        return List.of();
      }
      List<RecipeHolder<CraftingRecipe>> list = new ArrayList<>(recipes.size());
      for (T recipe : recipes) {
        list.add(holders.get(recipe));
      }
      return list;
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
      return manager.isHandledInput(input);
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
      return manager.isHandledOutput(output);
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
      return toHolders(manager.getRecipesForInput(input));
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
      return toHolders(manager.getRecipesForOutput(output));
    }
  }
}
