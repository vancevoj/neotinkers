package slimeknights.tconstruct.tables.recipe;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairToolHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

/**
 * Recipe that replaces a tool part with another
 */
@AllArgsConstructor
public class TinkerStationPartSwapping implements ITinkerStationRecipe {
  private static final RecipeResult<ItemStack> TOO_MANY_PARTS = RecipeResult.failure(TConstruct.makeTranslationKey("recipe", "part_swapping.too_many_parts"));

  @Getter
  protected final ResourceLocation id;

  @Override
  public boolean matches(ITinkerStationContainer inv, Level world) {
    ItemStack tinkerable = inv.getTinkerableStack();
    if (tinkerable.isEmpty() || !tinkerable.is(TinkerTags.Items.MULTIPART_TOOL)|| !(tinkerable.getItem() instanceof IModifiable modifiable)) {
      return false;
    }
    // get the list of parts, empty means its not multipart
    List<IToolPart> parts = ToolPartsHook.parts(modifiable.getToolDefinition());
    if (parts.isEmpty()) {
      return false;
    }

    // we have two concerns on part swapping:
    // part must be valid in the tool, and only up to one part can be swapped at once
    boolean foundItem = false;
    for (int i = 0; i < inv.getInputCount(); i++) {
      ItemStack stack = inv.getInput(i);
      if (!stack.isEmpty()) {
        // too many items
        if (foundItem) {
          return false;
        }
        // part not in list
        Item item = stack.getItem();
        if (!(item instanceof IToolPart) || parts.stream().noneMatch(p -> p.asItem() == item)) {
          return false;
        }
        foundItem = true;
      }
    }
    return foundItem;
  }

  /** @deprecated Use {@link #assemble(ITinkerStationContainer)}  */
  @Deprecated
  @Override
  public ItemStack getResultItem() {
    return ItemStack.EMPTY;
  }

  @Override
  public RecipeResult<ItemStack> getValidatedResult(ITinkerStationContainer inv) {
    // copy the tool NBT to ensure the original tool is intact
    ItemStack tinkerable = inv.getTinkerableStack();
    ToolStack tool = ToolStack.from(tinkerable);
    List<IToolPart> parts = ToolPartsHook.parts(tool.getDefinition());

    // prevent part swapping on large tools in small tables
    if (parts.size() > inv.getInputCount()) {
      return TOO_MANY_PARTS;
    }

    // actual part swap logic
    for (int i = 0; i < inv.getInputCount(); i++) {
      ItemStack stack = inv.getInput(i);
      if (!stack.isEmpty()) {
        // not tool part, should never happen
        Item item = stack.getItem();
        if (!(item instanceof IToolPart part)) {
          return RecipeResult.pass();
        }

        // ensure the part is valid
        MaterialVariantId partVariant = part.getMaterial(stack);
        if (partVariant.equals(IMaterial.UNKNOWN_ID)) {
          return RecipeResult.pass();
        }

        // we have a part and its not at this index, find the first copy of this part
        // means slot only matters if a tool uses a part twice
        int index = i;
        if (i >= parts.size() || parts.get(i).asItem() != item) {
          index = IntStream.range(0, parts.size())
                           .filter(pi -> parts.get(pi).asItem() == item)
                           .findFirst().orElse(-1);
          if (index == -1) {
            return RecipeResult.pass();
          }
        }

        // ensure there is a change in the part or we are repairing the tool, note we compare variants so you could swap oak head for birch head
        MaterialVariant toolVariant = tool.getMaterial(index);
        boolean didChange = !toolVariant.sameVariant(partVariant);
        int repairDurability = MaterialRepairModule.getDurability(null, partVariant.getId(), part.getStatType());
        if (!didChange && (tool.getDamage() == 0 || repairDurability == 0)) {
          return RecipeResult.pass();
        }

        // actual update
        tool = tool.copy();

        // determine which modifiers are going to be removed
        List<Modifier> actuallyRemoved = Collections.emptyList();
        if (didChange) {
          Map<Modifier,Integer> removedTraits = new HashMap<>();
          // start with a map of all modifiers on the old part
          // TODO: this logic looks correct, but I feel like it might be more complicated than needed
          // basically, if the new part has the modifier, its not going to be removed no matter how the levels differ, a set should suffice
          IMaterialRegistry materialRegistry = MaterialRegistry.getInstance();
          for (ModifierEntry entry : materialRegistry.getTraits(toolVariant.getId(), part.getStatType())) {
            removedTraits.put(entry.getModifier(), entry.getLevel());
          }
          // subtract any modifiers on the new part
          for (ModifierEntry entry : materialRegistry.getTraits(partVariant.getId(), part.getStatType())) {
            Modifier modifier = entry.getModifier();
            if (removedTraits.containsKey(modifier)) {
              int value = removedTraits.get(modifier) - entry.getLevel();
              if (value <= 0) {
                removedTraits.remove(modifier);
              } else {
                removedTraits.put(modifier, value);
              }
            }
          }
          // for the remainder, fill a list as we have another hooks to call with them
          actuallyRemoved = new ArrayList<>();
          for (Entry<Modifier,Integer> entry : removedTraits.entrySet()) {
            Modifier modifier = entry.getKey();
            if (tool.getModifierLevel(modifier) <= entry.getValue()) {
              modifier.getHook(ModifierHooks.RAW_DATA).removeRawData(tool, modifier, tool.getRestrictedNBT());
              actuallyRemoved.add(modifier);
            }
          }

          // do the actual part replacement
          tool.replaceMaterial(index, partVariant);
        }

        // if swapping in a new head, repair the tool (assuming the give stats type can repair)
        // ideally we would validate before repairing, but don't want to create the stack before repairing
        if (repairDurability > 0) {
          // must have a registered recipe
          int cost = MaterialCastingLookup.getItemCost(part);
          if (cost > 0) {
            // apply tool repair factor and modifier repair boost, note this works because the material has been swapped already
            float factor = cost / MaterialRecipe.INGOTS_PER_REPAIR * MaterialRepairToolHook.repairFactor(tool, partVariant.getId());
            if (factor > 0) {
              for (ModifierEntry entry : tool.getModifierList()) {
                factor = entry.getHook(ModifierHooks.REPAIR_FACTOR).getRepairFactor(tool, entry, factor);
                if (factor <= 0) {
                  break;
                }
              }
            }
            if (factor > 0) {
              ToolDamageUtil.repair(tool, (int)(repairDurability * factor));
            }
          }
        }

        // ensure no modifier problems after removing
        // modifier validation, handles modifier requirements
        Component error = tool.tryValidate();
        if (error != null) {
          return RecipeResult.failure(error);
        }
        // finally, validate removed modifiers
        for (Modifier modifier : actuallyRemoved) {
          error = modifier.getHook(ModifierHooks.REMOVE).onRemoved(tool, modifier);
          if (error != null) {
            return RecipeResult.failure(error);
          }
        }
        // everything worked, so good to go
        return RecipeResult.success(tool.createStack(Math.min(tinkerable.getCount(), shrinkToolSlotBy())));
      }
    }
    // no item found, should never happen
    return RecipeResult.pass();
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.tinkerStationPartSwappingSerializer.get();
  }
}
