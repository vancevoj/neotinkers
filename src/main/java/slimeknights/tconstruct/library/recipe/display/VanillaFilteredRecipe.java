package slimeknights.tconstruct.library.recipe.display;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Interface for vanilla recipes to allow them to expose filtered recipes. Used since JEI will not filter the list to remove filtered recipes from the recipe manager. */
public interface VanillaFilteredRecipe<T extends FilteredRecipe> {
  /**
   * Gets a list of filtered recipes for adding to a recipe manager plugin.
   * @return  List of recipes
   * @param access  Registry access instance
   */
  List<T> getFilteredRecipes(RegistryAccess access);


  /** Gets a  modifiable list of filtered recipes matching the given recipe type. */
  static <T extends FilteredRecipe, I extends RecipeInput, R extends Recipe<I>> List<T> getRecipes(RegistryAccess access, RecipeManager manager, RecipeType<R> type, Class<T> recipeClass) {
    // 1.21: RecipeManager#byType is private, getAllRecipesFor returns the recipe holders
    return manager.getAllRecipesFor(type).stream().map(RecipeHolder::value)
      .flatMap(recipe -> recipe instanceof VanillaFilteredRecipe<?> filtered ? filtered.getFilteredRecipes(access).stream() : Stream.empty())
      .filter(recipeClass::isInstance).map(recipeClass::cast).collect(Collectors.toList());
  }
}
