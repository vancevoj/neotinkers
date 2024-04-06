package slimeknights.tconstruct.library.recipe.modifiers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.DuelSidedListener;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Logic to check various modifier recipe based properties */
public class ModifierRecipeLookup {
  /** Map of salvage recipes for each modifier */
  private static final Multimap<ModifierId,ModifierSalvage> SALVAGE = HashMultimap.create();

  /** Map of slot type to modifiers added via that slot, better for fetching lists */
  private static final Multimap<SlotType,LazyModifier> RECIPE_MODIFIERS = HashMultimap.create();
  /** Map of slot type to modifier IDs added via that slot, better for lookup */
  private static final Multimap<SlotType,ModifierId> RECIPE_MODIFIER_IDS = HashMultimap.create();
  /** List of modifiers to show in JEI */
  private static List<ModifierEntry> RECIPE_MODIFIER_LIST = null;

  /** Listener for clearing the caches on recipe reload */
  private static final DuelSidedListener LISTENER = RecipeCacheInvalidator.addDuelSidedListener(() -> {
    SALVAGE.clear();
    RECIPE_MODIFIERS.clear();
    RECIPE_MODIFIER_IDS.clear();
    RECIPE_MODIFIER_LIST = null;
  });


  /* Salvage */

  /**
   * Stores a salvage recipe
   * @param salvage  Salvage recipe
   */
  public static void addSalvage(ModifierSalvage salvage) {
    LISTENER.checkClear();
    SALVAGE.put(salvage.getModifier(), salvage);
  }

  /**
   * Gets a salvage recipe
   * @param tool            Tool stack, primarily used for tag checks, but may do weird things
   * @param modifier        Modifier instance
   * @param modifierLevel   Modifier level
   * @return  Salvage recipe, or null if no salvage is found
   */
  @Nullable
  public static ModifierSalvage getSalvage(ItemStack stack, IToolStackView tool, ModifierId modifier, int modifierLevel) {
    for (ModifierSalvage salvage : SALVAGE.get(modifier)) {
      if (salvage.matches(stack, tool, modifierLevel)) {
        return salvage;
      }
    }
    return null;
  }


  /* Recipe modifiers */
  
  /**
   * Adds a modifier requirement, typically called by the recipe
   * @param slotType  Slot type for the modifier, use null for slotless
   * @param modifier  Modifier in that slot
   */
  public static void addRecipeModifier(@Nullable SlotType slotType, LazyModifier modifier) {
    LISTENER.checkClear();
    RECIPE_MODIFIERS.put(slotType, modifier);
    RECIPE_MODIFIER_IDS.put(slotType, modifier.getId());
  }

  /** Gets a stream of all modifiers obtainable via recipes */
  public static Stream<Modifier> getAllRecipeModifiers() {
    return RECIPE_MODIFIERS.values().stream().map(LazyModifier::get).distinct();
  }

  /** Gets a list of modifier entries for display in JEI, basically the same as creating your own, but the result is cached */
  public static List<ModifierEntry> getRecipeModifierList() {
    // do not cache an empty list during game start, recipes have not yet loaded
    if (RECIPE_MODIFIERS.isEmpty()) {
      return Collections.emptyList();
    }
    if (RECIPE_MODIFIER_LIST == null) {
      RECIPE_MODIFIER_LIST = RECIPE_MODIFIERS.values().stream().distinct().sorted(Comparator.comparing(LazyModifier::getId)).map(mod -> new ModifierEntry(mod, 1)).toList();
    }
    return RECIPE_MODIFIER_LIST;
  }

  /** Gets a stream of all modifiers obtainable via the given slot type */
  public static Stream<Modifier> getRecipeModifiers(@Nullable SlotType slotType) {
    return RECIPE_MODIFIERS.get(slotType).stream().map(LazyModifier::get);
  }

  /** Checks if the given modifier ID is a recipe modifier */
  public static boolean isRecipeModifier(ModifierId modifier) {
    return RECIPE_MODIFIER_IDS.containsValue(modifier);
  }

  /** Checks if the given modifier ID is a recipe modifier */
  public static boolean isRecipeModifier(@Nullable SlotType slotType, ModifierId modifier) {
    return RECIPE_MODIFIER_IDS.containsEntry(slotType, modifier);
  }
}
