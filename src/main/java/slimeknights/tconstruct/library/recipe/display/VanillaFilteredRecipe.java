package slimeknights.tconstruct.library.recipe.display;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
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


  /**
   * Gets a modifiable list of filtered recipes matching the given recipe type.
   * @param access   Registry access
   * @param recipes  Recipe map. On the client, use {@link slimeknights.mantle.recipe.helper.RecipeHelper#recipes(net.minecraft.world.level.Level)}, which only contains synced recipe types.
   * @param type     Recipe type
   * @param recipeClass  Filtered recipe class
   * @return  List of filtered recipes
   */
  static <T extends FilteredRecipe, I extends RecipeInput, R extends Recipe<I>> List<T> getRecipes(RegistryAccess access, RecipeMap recipes, RecipeType<R> type, Class<T> recipeClass) {
    return recipes.byType(type).stream().map(RecipeHolder::value)
      .flatMap(recipe -> recipe instanceof VanillaFilteredRecipe<?> filtered ? filtered.getFilteredRecipes(access).stream() : Stream.empty())
      .filter(recipeClass::isInstance).map(recipeClass::cast).collect(Collectors.toList());
  }

  /** Gets a modifiable list of filtered recipes matching the given recipe type from the server recipe manager, see {@link #getRecipes(RegistryAccess, RecipeMap, RecipeType, Class)} */
  static <T extends FilteredRecipe, I extends RecipeInput, R extends Recipe<I>> List<T> getRecipes(RegistryAccess access, RecipeManager manager, RecipeType<R> type, Class<T> recipeClass) {
    return getRecipes(access, manager.recipeMap(), type, recipeClass);
  }
}
