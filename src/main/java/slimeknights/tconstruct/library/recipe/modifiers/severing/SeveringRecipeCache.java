package slimeknights.tconstruct.library.recipe.modifiers.severing;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class handling a recipe cache for entity melting recipes, since any given entity type has one recipe
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SeveringRecipeCache {
  /** Cache of recipes per entity type, concurrent as the reload listener may run off the main thread */
  private static final Map<EntityType<?>,List<SeveringRecipe>> CACHE = new ConcurrentHashMap<>();

  static {
    RecipeCacheInvalidator.addReloadListener(client -> CACHE.clear());
  }

  /**
   * Gets the recipe for the given type
   * @param recipes  Recipe map, from {@link RecipeHelper#recipes(Level)} or {@link RecipeManager#recipeMap()}
   * @param type     Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  public static List<SeveringRecipe> findRecipe(RecipeMap recipes, EntityType<?> type) {
    if (CACHE.containsKey(type)) {
      return CACHE.get(type);
    }

    // find all severing recipes for the entity
    List<SeveringRecipe> list = new ArrayList<>();
    for (SeveringRecipe recipe : RecipeHelper.getRecipes(recipes, TinkerRecipeTypes.SEVERING.get(), SeveringRecipe.class)) {
      if (recipe.matches(type)) {
        list.add(recipe);
      }
    }
    if (list.isEmpty()) {
      list = Collections.emptyList();
    }

    // cache nothing was found
    CACHE.put(type, list);
    return list;
  }

  /**
   * Gets the recipe for the given type
   * @param manager  Recipe manager
   * @param type     Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  public static List<SeveringRecipe> findRecipe(RecipeManager manager, EntityType<?> type) {
    return findRecipe(manager.recipeMap(), type);
  }

  /**
   * Gets the recipe for the given type. On the client, only works if severing recipes are synced.
   * @param level  Level instance
   * @param type   Entity type
   * @return  Recipe, or null if no recipe for this type
   */
  public static List<SeveringRecipe> findRecipe(@Nullable Level level, EntityType<?> type) {
    return findRecipe(RecipeHelper.recipes(level), type);
  }
}
