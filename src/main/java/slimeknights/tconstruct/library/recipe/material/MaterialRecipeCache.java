package slimeknights.tconstruct.library.recipe.material;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.DuelSidedListener;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.utils.SimpleCache;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Cache of details related to materials.
 * Filled by recipe constructors; since 26.3 the server parses recipes in parallel, so registration is synchronized and the collections are thread safe.
 * Iterating a synchronized multimap view requires its lock, so readers copy the values under the lock.
 */
public class MaterialRecipeCache {
  /** Full list of recipes in the cache */
  private static final List<MaterialRecipe> RECIPES = new CopyOnWriteArrayList<>();
  /** Predicate for sorted recipe lists, ensuring they are visible */
  private static final Predicate<MaterialRecipe> RECIPE_FILTER = MaterialRecipe::isVisible;
  /** Full list of recipes in the cache */
  private static volatile List<MaterialRecipe> SORTED_RECIPES = null;
  /** Order of {@link #RECIPES} once all recipes registered. The server parses recipes in parallel, which would make the first match vary per reload */
  private static final Comparator<MaterialRecipe> REGISTRATION_ORDER = Comparator.comparing(MaterialRecipe::getId, RecipeCacheInvalidator.RECIPE_ID_ORDER);
  /** Comparator used to create {@link #SORTED_RECIPES}, ties sorted by recipe ID as the per material lists come from a hash multimap */
  private static final Comparator<MaterialRecipe> RECIPE_COMPARATOR = Comparator
    .comparing(MaterialRecipe::getMaterial)
    .thenComparing(MaterialRecipe::getValue)
    .thenComparing(MaterialRecipe::getNeeded)
    .thenComparing(REGISTRATION_ORDER);
  /** Lookup from item ID to recipe */
  private static final Map<Item, MaterialRecipe> RECIPE_BY_ITEM = new ConcurrentHashMap<>();
  /** Lookup from material variant ID to recipe */
  private static final Multimap<MaterialVariantId, MaterialRecipe> RECIPES_BY_MATERIAL = Multimaps.synchronizedSetMultimap(HashMultimap.create());
  /** Gets the list of recipes per material in sorted order */
  private static final SimpleCache<MaterialVariantId, List<MaterialRecipe>> SORTED_RECIPES_BY_MATERIAL = new SimpleCache<>(id ->
    getAllRecipes(id).stream().filter(RECIPE_FILTER).sorted(RECIPE_COMPARATOR).toList());
  /** Map from material variant ID to item stack list for display */
  private static final SimpleCache<MaterialVariantId, List<ItemStack>> ITEMS_BY_MATERIAL = new SimpleCache<>(variant ->
    getRecipes(variant).stream().flatMap(r -> {
      Stream<ItemStack> stacks = IngredientHelper.getStacks(r.getIngredient()).stream();
      // if we need multiple, increase the stack size of the display stacks
      if (r.needed > r.value) {
        int size = (r.needed + r.value - 1) / r.value;
        stacks = stacks.map(stack -> stack.copyWithCount(size));
      }
      return stacks;
    }).toList());

  /** Mapping from material ID to all variants for the material */
  private static final Multimap<MaterialId, MaterialVariantId> KNOWN_VARIANTS = Multimaps.synchronizedSetMultimap(HashMultimap.create());
  /** List of all material variants in sorted order. See also {@link IMaterialRegistry#getVisibleMaterials()} */
  @Nullable
  private static volatile List<MaterialVariantId> SORTED_VARIANTS = null;
  /** List of all hidden material variants in sorted order. See also {@link IMaterialRegistry#getVisibleMaterials()} */
  @Nullable
  private static volatile List<MaterialVariantId> HIDDEN_VARIANTS = null;

  /** Listener for clearing the cache */
  private static final DuelSidedListener LISTENER = RecipeCacheInvalidator.addDuelSidedListener(() -> {
    RECIPES.clear();
    SORTED_RECIPES = null;
    RECIPE_BY_ITEM.clear();
    RECIPES_BY_MATERIAL.clear();
    SORTED_RECIPES_BY_MATERIAL.clear();
    ITEMS_BY_MATERIAL.clear();
    KNOWN_VARIANTS.clear();
    SORTED_VARIANTS = null;
    HIDDEN_VARIANTS = null;
  }, () -> {
    // recipe list complete, put the first match lookup in a deterministic order
    RECIPES.sort(REGISTRATION_ORDER);
    SORTED_RECIPES = null;
    RECIPE_BY_ITEM.clear();
    SORTED_RECIPES_BY_MATERIAL.clear();
    ITEMS_BY_MATERIAL.clear();
  });

  /** Registers a recipe with the cache */
  public static synchronized void registerRecipe(MaterialRecipe recipe) {
    if (recipe.getValue() > 0 && !RecipeCacheInvalidator.skipRegistration()) {
      // ensure c ache does not need to be cleared
      LISTENER.checkClear();
      // add recipe for item lookup; too early to resolve ingredient
      RECIPES.add(recipe);
      SORTED_RECIPES = null;
      // mark the variant as known
      MaterialVariantId variant = recipe.getMaterial().getVariant();
      addKnownVariant(variant);
      // add lookup for the variant
      RECIPES_BY_MATERIAL.put(variant, recipe);
      SORTED_RECIPES_BY_MATERIAL.remove(variant);
    }
  }

  /**
   * Locates a recipe by stack
   * @param stack  Stack to check
   * @return Recipe, or {@link MaterialRecipe#EMPTY} if no match.
   */
  public static MaterialRecipe findRecipe(ItemStack stack) {
    if (stack.isEmpty()) {
      return MaterialRecipe.EMPTY;
    }
    return RECIPE_BY_ITEM.computeIfAbsent(stack.getItem(), item -> {
      for (MaterialRecipe recipe : RECIPES) {
        if (recipe.getIngredient().test(stack)) {
          return recipe;
        }
      }
      return MaterialRecipe.EMPTY;
    });
  }
  
  /** Gets the material from the given stack, using {@link IMaterialItem} if present, otherwise falling back to {@link #findRecipe(ItemStack)} */
  public static MaterialVariantId getMaterial(ItemStack stack) {
    if (stack.isEmpty()) {
      return MaterialId.UNKNOWN;
    }
    if (stack.getItem() instanceof IMaterialItem materialItem) {
      return materialItem.getMaterial(stack);
    }
    return findRecipe(stack).getMaterial().getVariant();
  }

  /** Gets a list of all material recipes, including hidden */
  public static Collection<MaterialRecipe> getAllRecipes() {
    return RECIPES;
  }

  /** Gets a list of all non-hidden material recipes, sorted by tier, sort key, and value. */
  public static Collection<MaterialRecipe> getSortedRecipes() {
    List<MaterialRecipe> sorted = SORTED_RECIPES;
    if (sorted == null) {
      sorted = RECIPES.stream().filter(RECIPE_FILTER).sorted(RECIPE_COMPARATOR).toList();
      SORTED_RECIPES = sorted;
    }
    return sorted;
  }

  /** Gets all unsorted recipes for the given material variant */
  public static Collection<MaterialRecipe> getAllRecipes(MaterialVariantId variant) {
    synchronized (RECIPES_BY_MATERIAL) {
      return List.copyOf(RECIPES_BY_MATERIAL.get(variant));
    }
  }

  /** Gets the known variants for the given material, copied under the map lock */
  private static Collection<MaterialVariantId> getKnownVariants(MaterialId materialId) {
    synchronized (KNOWN_VARIANTS) {
      return List.copyOf(KNOWN_VARIANTS.get(materialId));
    }
  }

  /** Gets sorted recipes for the given material variant */
  public static Collection<MaterialRecipe> getRecipes(MaterialVariantId variant) {
    return SORTED_RECIPES_BY_MATERIAL.apply(variant);
  }

  /** Gets all recipes for the given material variant */
  public static List<ItemStack> getItems(MaterialVariantId variant) {
    return ITEMS_BY_MATERIAL.apply(variant);
  }

  /** Adds all items for the given material variant and cost to the given list. */
  public static void addItems(MaterialVariantId variant, int cost, List<ItemStack> items) {
    for (MaterialRecipe recipe : getRecipes(variant)) {
      int count = recipe.getItemsUsed(cost);
      List<ItemStack> stacks = IngredientHelper.getStacks(recipe.getIngredient());
      if (count == 1) {
        items.addAll(stacks);
      } else {
        for (ItemStack stack : stacks) {
          stack = stack.copyWithCount(count);
          items.add(stack);
        }
      }
    }
  }


  /* Material variants */

  /** Registers a material variant for the lookups. */
  public static synchronized void addKnownVariant(MaterialVariantId variant) {
    if (RecipeCacheInvalidator.skipRegistration()) {
      return;
    }
    LISTENER.checkClear();
    KNOWN_VARIANTS.put(variant.getId(), variant);
    // null cache of sorted variants as its outdated now
    SORTED_VARIANTS = null;
    HIDDEN_VARIANTS = null;
  }

  /** Gets a list of known material variants for the given material ID */
  public static Collection<MaterialVariantId> getVariants(MaterialId materialId) {
    Collection<MaterialVariantId> variants = getKnownVariants(materialId);
    if (variants.isEmpty()) {
      return List.of(materialId);
    }
    return variants;
  }

  /** Gets a sorted list of all known non-hidden material variants */
  public static List<MaterialVariantId> getAllVariants() {
    List<MaterialVariantId> sorted = SORTED_VARIANTS;
    if (sorted == null) {
      Comparator<MaterialVariantId> variantSorter = Comparator.comparing(MaterialVariantId::getVariant);
      sorted = MaterialRegistry.getInstance().getVisibleMaterials().stream()
        .flatMap(material -> {
          // if no variants are registered, just list the material itself; useful for uncraftable materials
          MaterialId id = material.getIdentifier();
          Collection<MaterialVariantId> variants = getKnownVariants(id);
          if (variants.isEmpty()) {
            return Stream.of(id);
          }
          return variants.stream().sorted(variantSorter);
        }).toList();
      SORTED_VARIANTS = sorted;
    }
    return sorted;
  }

  /** Gets a sorted list of all known hidden material variants. Should not be directly displayed in recipes, rather added as a hidden input. */
  public static List<MaterialVariantId> getHiddenVariants() {
    List<MaterialVariantId> hidden = HIDDEN_VARIANTS;
    if (hidden == null) {
      Comparator<MaterialVariantId> variantSorter = Comparator.comparing(MaterialVariantId::getVariant);
      hidden = MaterialRegistry.getInstance().getAllMaterials().stream()
        .filter(IMaterial::isHidden).sorted()
        .flatMap(material -> {
          // if no variants are registered, just list the material itself; useful for uncraftable materials
          MaterialId id = material.getIdentifier();
          Collection<MaterialVariantId> variants = getKnownVariants(id);
          if (variants.isEmpty()) {
            return Stream.of(id);
          }
          return variants.stream().sorted(variantSorter);
        }).toList();
      HIDDEN_VARIANTS = hidden;
    }
    return hidden;
  }
}
