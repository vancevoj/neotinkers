package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.recipe.data.ItemNameOutput;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.DuelSidedListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Lookup for items which are castable and other relevant information.
 * Filled by recipe constructors; since 26.3 the server parses recipes in parallel, so registration is synchronized and the collections are thread safe.
 */
public class CastingRecipeLookup {
  /** Set of all castable items */
  private static final Map<Item,Boolean> CASTABLE_ITEMS = new ConcurrentHashMap<>();
  /** List of tags that represent castable items */
  private static final List<TagKey<Item>> CASTABLE_TAGS = new CopyOnWriteArrayList<>();
  /** Item outputs registered while recipes are parsed. Since 26.3 item components are only bound after data loading, so the stacks are resolved on the first lookup */
  private static final List<ItemOutput> PENDING_OUTPUTS = new CopyOnWriteArrayList<>();
  /** Function to evaluate whether something is castable */
  private static final Function<Item,Boolean> COMPUTE_CASTABLE = item -> {
    for (TagKey<Item> tag : CASTABLE_TAGS) {
      if (RegistryHelper.contains(tag, item)) {
        return true;
      }
    }
    return false;
  };
  /** Cache invalidator */
  private static final DuelSidedListener INVALIDATOR = RecipeCacheInvalidator.addDuelSidedListener(() -> {
    CASTABLE_ITEMS.clear();
    CASTABLE_TAGS.clear();
    PENDING_OUTPUTS.clear();
  });

  private CastingRecipeLookup() {}

  /** Marks the given item as castable */
  public static synchronized void registerCastable(ItemLike item) {
    if (RecipeCacheInvalidator.skipRegistration()) {
      return;
    }
    INVALIDATOR.checkClear();
    CASTABLE_ITEMS.put(item.asItem(), true);
  }

  /** Marks the given tag as castable */
  public static synchronized void registerCastable(TagKey<Item> tag) {
    if (RecipeCacheInvalidator.skipRegistration()) {
      return;
    }
    INVALIDATOR.checkClear();
    CASTABLE_TAGS.add(tag);
  }

  /** Marks the given item output as castable */
  public static synchronized void registerCastable(ItemOutput output) {
    TagKey<Item> tag = output.getTag();
    if (tag != null) {
      registerCastable(tag);
    // item name output fails here, but that only happens at datagen so just ignore that
    } else if (!output.isEmpty() && output.getClass() != ItemNameOutput.class) {
      if (RecipeCacheInvalidator.skipRegistration()) {
        return;
      }
      INVALIDATOR.checkClear();
      // resolving the output creates a stack, which is not possible while recipes are parsed, so resolve it on the first lookup
      PENDING_OUTPUTS.add(output);
    }
  }

  /** Resolves item outputs registered during recipe parsing into castable items */
  private static void resolvePendingOutputs() {
    if (!PENDING_OUTPUTS.isEmpty()) {
      synchronized (CastingRecipeLookup.class) {
        for (ItemOutput output : PENDING_OUTPUTS) {
          ItemStack stack = output.get();
          if (!stack.isEmpty()) {
            CASTABLE_ITEMS.put(stack.getItem(), true);
          }
        }
        PENDING_OUTPUTS.clear();
      }
    }
  }

  /** Checks if the given item is castable */
  public static boolean isCastable(ItemLike item) {
    resolvePendingOutputs();
    return CASTABLE_ITEMS.computeIfAbsent(item.asItem(), COMPUTE_CASTABLE);
  }
}
