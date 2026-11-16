package slimeknights.tconstruct.library.recipe.fuel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.DuelSidedListener;
import slimeknights.tconstruct.library.utils.SimpleCache;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class handling a recipe cache for fuel recipes, since any given entity type has one recipe.
 * Filled by recipe constructors; since 26.3 the server parses recipes in parallel, so registration is synchronized and the collections are thread safe.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MeltingFuelLookup {
  /** Dummy fuel instance sine caches don't support caching null */
  private static final MeltingFuel EMPTY = new MeltingFuel(Identifier.parse("missingno"), FluidIngredient.EMPTY, 0, 0, 0);
  /** Temperature for solid fuels in the heater */
  private static volatile MeltingFuel SOLID = EMPTY;
  /** List of all recipes */
  private static final List<MeltingFuel> RECIPES = new CopyOnWriteArrayList<>();
  /** Order of {@link #RECIPES} once all recipes registered. The server parses recipes in parallel, which would make the first match vary per reload */
  private static final Comparator<MeltingFuel> ORDER = Comparator.comparing(MeltingFuel::getId, RecipeCacheInvalidator.RECIPE_ID_ORDER);
  /** Mapping from fluid to fuel */
  private static final SimpleCache<Fluid,MeltingFuel> CACHE = new SimpleCache<>(fluid -> {
    for (MeltingFuel recipe : RECIPES) {
      if (recipe.matches(fluid)) {
        return recipe;
      }
    }
    return EMPTY;
  });
  /** Listener to check when recipes reload */
  private static final DuelSidedListener LISTENER = RecipeCacheInvalidator.addDuelSidedListener(() -> {
    SOLID = EMPTY;
    RECIPES.clear();
    CACHE.clear();
  }, () -> {
    RECIPES.sort(ORDER);
    CACHE.clear();
  });

  /**
   * Adds a melting fuel to the lookup
   * @param fuel   Fuel
   */
  public static synchronized void addFuel(MeltingFuel fuel) {
    // skip empty fuel, and the integrated client copy
    if (fuel.getRate() == 0 || RecipeCacheInvalidator.skipRegistration()) {
      return;
    }
    LISTENER.checkClear();
    if (fuel.getInput() != FluidIngredient.EMPTY) {
      RECIPES.add(fuel);
    } else if (SOLID == EMPTY) {
      SOLID = fuel;
    } else {
      TConstruct.LOG.warn("Multiple fuel recipes for solid fuel. This usually indicates a datapack error and may cause desyncs. Original {}, latest {}", SOLID.getId(), fuel.getId());
      // recipes parse in parallel, so keep the first by recipe ID rather than the first registered, matching on every reload and on the client
      if (ORDER.compare(fuel, SOLID) < 0) {
        SOLID = fuel;
      }
    }
  }

  /** Checks if the given fluid is a fuel */
  public static boolean isFuel(Fluid fluid) {
    return CACHE.apply(fluid) != EMPTY;
  }

  /** Gets the properties for solid fuel */
  public static MeltingFuel getSolid() {
    return SOLID;
  }

  /**
   * Gets the recipe for the given fluid
   * @param fluid   Fluid found
   * @return  Recipe, or null if no recipe for this type
   */
  @Nullable
  public static MeltingFuel findFuel(Fluid fluid) {
    MeltingFuel recipe = CACHE.apply(fluid);
    if (recipe == EMPTY) {
      return null;
    }
    return recipe;
  }
}
