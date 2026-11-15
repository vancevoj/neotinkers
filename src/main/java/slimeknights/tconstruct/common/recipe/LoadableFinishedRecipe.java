package slimeknights.tconstruct.common.recipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Simple helper for saving a recipe serialized with a loadable.
 * In 1.21 {@link RecipeOutput} encodes the recipe through its serializer codec, so the finished recipe wrapper and loader are no longer needed.
 * Recipes also no longer expose a common ID getter, so the ID is passed in.
 * TODO: move to Mantle
 */
public final class LoadableFinishedRecipe {
  private LoadableFinishedRecipe() {}

  /** One-liner to accept a new recipe with no advancement */
  public static <T extends Recipe<?>> void save(RecipeOutput consumer, Identifier id, T recipe) {
    consumer.accept(id, recipe, null);
  }
}
