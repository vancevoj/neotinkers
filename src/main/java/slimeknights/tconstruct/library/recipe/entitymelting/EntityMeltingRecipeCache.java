package slimeknights.tconstruct.library.recipe.entitymelting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class handling a recipe cache for entity melting recipes, since any given entity type has one recipe.
 * Since 26.3 the recipes come from a {@link RecipeMap}: on the server from the recipe manager, on the client from the synced recipes.
 * The cache is cleared on reload and whenever a different recipe map is queried, so it never mixes recipes of two recipe loads.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityMeltingRecipeCache {
  private static final Map<EntityType<?>,EntityMeltingRecipe> CACHE = new HashMap<>();
  /** Recipe map the cache was built from */
  @Nullable
  private static RecipeMap cachedRecipes = null;

  static {
    RecipeCacheInvalidator.addReloadListener(client -> clear());
  }

  /** Clears the cache */
  private static synchronized void clear() {
    CACHE.clear();
    cachedRecipes = null;
  }

  /**
   * Gets the recipe for the given type
   * @param recipes  Recipe map, from {@link RecipeHelper#recipes(Level)} or {@link RecipeManager#recipeMap()}
   * @param type     Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  @Nullable
  public static synchronized EntityMeltingRecipe findRecipe(RecipeMap recipes, EntityType<?> type) {
    // recipe maps are replaced on reload and on client sync, so a different map means the cache is outdated
    if (cachedRecipes != recipes) {
      CACHE.clear();
      cachedRecipes = recipes;
    }
    if (CACHE.containsKey(type)) {
      return CACHE.get(type);
    }

    // find a recipe if none exist
    for (EntityMeltingRecipe recipe : RecipeHelper.getRecipes(recipes, TinkerRecipeTypes.ENTITY_MELTING.get(), EntityMeltingRecipe.class)) {
      if (recipe.matches(type)) {
        CACHE.put(type, recipe);
        return recipe;
      }
    }

    // cache nothing was found
    CACHE.put(type, null);
    return null;
  }

  /**
   * Gets the recipe for the given type
   * @param manager  Recipe manager
   * @param type     Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  @Nullable
  public static EntityMeltingRecipe findRecipe(RecipeManager manager, EntityType<?> type) {
    return findRecipe(manager.recipeMap(), type);
  }

  /**
   * Gets the recipe for the given type using the recipes available on the level's side
   * @param level  Level instance, on the client only synced recipes are available
   * @param type   Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  @Nullable
  public static EntityMeltingRecipe findRecipe(@Nullable Level level, EntityType<?> type) {
    return findRecipe(RecipeHelper.recipes(level), type);
  }
}
