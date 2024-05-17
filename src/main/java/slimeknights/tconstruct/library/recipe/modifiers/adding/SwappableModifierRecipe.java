package slimeknights.tconstruct.library.recipe.modifiers.adding;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.json.field.MergingField;
import slimeknights.tconstruct.library.json.field.MergingField.MissingMode;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe.modifiersForResult;
import static slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe.withModifiers;

/**
 * Standard recipe to add a modifier
 */
public class SwappableModifierRecipe extends ModifierRecipe {
  private static final String ALREADY_PRESENT = TConstruct.makeTranslationKey("recipe", "swappable.already_present");
  public static final RecordLoadable<SwappableModifierRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(),
    INPUTS_FIELD, TOOLS_FIELD, MAX_TOOL_SIZE_FIELD,
    new MergingField<>(ModifierId.PARSER.requiredField("name", r -> r.result.getId()), "result", MissingMode.DISALLOWED),
    new MergingField<>(StringLoadable.DEFAULT.requiredField("value", r -> r.value), "result", MissingMode.DISALLOWED),
    SLOTS_FIELD, ALLOW_CRYSTAL_FIELD,
    SwappableModifierRecipe::new);

  /** Value of the modifier being swapped, distinguishing this recipe from others for the same modifier */
  private final String value;
  public SwappableModifierRecipe(ResourceLocation id, List<SizedIngredient> inputs, Ingredient toolRequirement, int maxToolSize, ModifierId result, String value, @Nullable SlotCount slots, boolean allowCrystal) {
    super(id, inputs, toolRequirement, maxToolSize, result, new IntRange(1, 1), slots, allowCrystal, false);
    this.value = value;
  }

  @Override
  public RecipeResult<ItemStack> getValidatedResult(ITinkerStationContainer inv) {
    ItemStack tinkerable = inv.getTinkerableStack();
    ToolStack tool = ToolStack.from(tinkerable);

    // if the tool has the modifier already, can skip most requirements
    ModifierId modifier = result.getId();

    boolean needsModifier;
    if (tool.getUpgrades().getLevel(modifier) == 0) {
      needsModifier = true;
      Component commonError = validatePrerequisites(tool);
      if (commonError != null) {
        return RecipeResult.failure(commonError);
      }
    } else {
      needsModifier = false;
    }

    // do not allow adding the modifier if this variant is already present
    // TODO: include variant in name? need variant to be translatable for that probably
    if (tool.getPersistentData().getString(modifier).equals(value)) {
      return RecipeResult.failure(ALREADY_PRESENT, result.get().getDisplayName());
    }

    // consume slots
    tool = tool.copy();
    ModDataNBT persistentData = tool.getPersistentData();
    if (needsModifier) {
      SlotCount slots = getSlots();
      if (slots != null) {
        persistentData.addSlots(slots.type(), -slots.count());
      }
    }

    // set the new value to the modifier
    persistentData.putString(modifier, value);

    // add modifier if needed
    if (needsModifier) {
      tool.addModifier(result.getId(), 1);
    } else {
      tool.rebuildStats();
    }

    // ensure no modifier problems
    Component toolValidation = tool.tryValidate();
    if (toolValidation != null) {
      return RecipeResult.failure(toolValidation);
    }
    return RecipeResult.success(tool.createStack(Math.min(tinkerable.getCount(), shrinkToolSlotBy())));
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerModifiers.swappableModifierSerializer.get();
  }


  /* Display */

  @Override
  public List<ItemStack> getToolWithModifier() {
    if (toolWithModifier == null) {
      ResourceLocation id = result.getId();
      ModifierEntry result = getDisplayResult();
      toolWithModifier = getToolInputs().stream().map(stack -> withModifiers(stack, modifiersForResult(result, result), data -> data.putString(id, value))).collect(Collectors.toList());
    }
    return toolWithModifier;
  }
}
