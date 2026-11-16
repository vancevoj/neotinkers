package slimeknights.tconstruct.library.recipe.melting;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.DuelSidedListener;
import slimeknights.tconstruct.library.utils.SimpleCache;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lookup for melting recipe, used for a few modifiers.
 * Notably does not contain information on dynamic melting recipes such as tool parts.
 * Filled by recipe constructors; since 26.3 the server parses recipes in parallel, so registration is synchronized and the collections are thread safe.
 */
public class MeltingRecipeLookup {
  private MeltingRecipeLookup() {}

  /** Record holding data from the melting recipe */
  public record MeltingFluid(Ingredient ingredient, FluidOutput result, int temperature) {
    /** Empty instance, used to cache missing results */
    public static final MeltingFluid EMPTY = new MeltingFluid(EmptyIngredient.VANILLA, FluidOutput.EMPTY, 0);

    /** Checks if this result is present */
    public boolean isEmpty() {
      return result.isEmpty();
    }
  }

  /**
   * Fluid registered by a recipe
   * @param recipeId  ID of the registering recipe for ordering, null if not given
   * @param fluid     Melting result
   */
  private record Registration(@Nullable Identifier recipeId, MeltingFluid fluid) {}

  /** If true, cache is currently frozen and will now allow changing {@link #FLUIDS} */
  private static volatile boolean meltingFrozen = false;
  /** List of fluid recipes stored from melting recipe constructors */
  private static final List<Registration> FLUIDS = new CopyOnWriteArrayList<>();
  /** Order for {@link #FLUIDS} once all recipes registered. The server parses recipes in parallel, which would make the first match vary per reload */
  private static final Comparator<Registration> ORDER = Comparator.comparing(Registration::recipeId, RecipeCacheInvalidator.RECIPE_ID_ORDER);
  /** Cache of lookup by item, assumes no recipes vary by NBT. We mostly use this for blocks anyways */
  private static final SimpleCache<Item,MeltingFluid> LOOKUP = new SimpleCache<>(item -> {
    if (item != Items.AIR) {
      ItemStack stack = new ItemStack(item);
      for (Registration registration : FLUIDS) {
        MeltingFluid fluid = registration.fluid;
        if (fluid.ingredient.test(stack)) {
          return fluid;
        }
      }
    }
    return MeltingFluid.EMPTY;
  });

  /** Cache invalidator */
  private static final DuelSidedListener CACHE = RecipeCacheInvalidator.addDuelSidedListener(() -> {
    FLUIDS.clear();
    LOOKUP.clear();
  }, () -> {
    FLUIDS.sort(ORDER);
    LOOKUP.clear();
  });

  /** Adds a fluid to the lookup. Prefer {@link #addMeltingFluid(Identifier, Ingredient, FluidOutput, int)}, fluids without a recipe ID match last */
  public static void addMeltingFluid(Ingredient ingredient, FluidOutput result, int temperature) {
    addMeltingFluid(null, ingredient, result, temperature);
  }

  /**
   * Adds a fluid to the lookup
   * @param recipeId     ID of the recipe adding the fluid, decides which recipe matches first when several melt the same item
   * @param ingredient   Melting input
   * @param result       Melting result
   * @param temperature  Melting temperature
   */
  public static synchronized void addMeltingFluid(@Nullable Identifier recipeId, Ingredient ingredient, FluidOutput result, int temperature) {
    if (meltingFrozen || RecipeCacheInvalidator.skipRegistration()) {
      return;
    }
    CACHE.checkClear();
    FLUIDS.add(new Registration(recipeId, new MeltingFluid(ingredient, result, temperature)));
  }

  /** Logic to find a fluid for a given item */
  public static MeltingFluid findFluid(ItemLike item) {
    return LOOKUP.apply(item.asItem());
  }

  /** Finds the result fluid for the given input and temperature */
  public static FluidStack findResult(ItemLike input, int temperature) {
    MeltingFluid fluid = findFluid(input);
    if (fluid.temperature > temperature) {
      return FluidStack.EMPTY;
    }
    return fluid.result.get();
  }

  /** Checks if the given item can melt */
  public static boolean canMelt(ItemLike input) {
    return !findFluid(input).isEmpty();
  }

  /** Freezes the fluid list in the cache. Used to allow command driven datagen. */
  @Internal
  public static void freeze() {
    meltingFrozen = true;
  }

  /** Unfreezes the fluid list in the cache. Used to allow resuming the normal game without running reload. */
  @Internal
  public static void unfreeze() {
    meltingFrozen = false;
  }
}
