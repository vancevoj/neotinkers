package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.casting.potion.ModifierPotionCastingRecipe;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Casting recipe applying a potion to a tool.
 * TODO 1.21: move to {@link slimeknights.tconstruct.library.recipe.casting.potion}
 */
public class TippingCastingRecipe extends ModifierPotionCastingRecipe {
  protected static final LoadableField<Ingredient, ModifierPotionCastingRecipe> TOOL_FIELD = ModifierPotionCastingRecipe.TOOL_FIELD;
  public static final RecordLoadable<TippingCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    TOOL_FIELD, FLUID_FIELD, COOLING_TIME_FIELD, MODIFIER_FIELD, TippingCastingRecipe::new);

  public TippingCastingRecipe(TypeAwareRecipeSerializer<?> serializer, Identifier id, String group, Ingredient tool, FluidIngredient fluid, int coolingTime, ModifierId modifier) {
    super(serializer, id, group, tool, fluid, Items.AIR, coolingTime, modifier);
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    // must have the modifier to cast
    if (super.matches(inv, level)) {
      // must also have a specific potion, it's what we are going to copy, but it can't match what is
      // already on the stack. 1.21: potion data lives on the FluidStack's POTION_CONTENTS component, not
      // the legacy fluid NBT tag (getFluidTag() now always returns null, so the old check never matched).
      PotionContents contents = inv.getFluidStack().get(DataComponents.POTION_CONTENTS);
      if (contents == null || contents.potion().isEmpty()) {
        return false;
      }
      String potionId = Loadables.POTION.getString(contents.potion().get().value());
      return !ModifierUtil.getPersistentString(inv.getStack(), modifier).equals(potionId);
    }
    return false;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack result = inv.getStack().copy();
    // 1.21: read the potion from the FluidStack's POTION_CONTENTS component (see matches)
    PotionContents contents = inv.getFluidStack().get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      contents.potion().ifPresent(potion ->
        ToolStack.from(result).getPersistentData().putString(modifier, Loadables.POTION.getString(potion.value())));
    }
    return result;
  }


  /* JEI */

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // first, get a list of potions and their IDs, the IDs are what we store in tool persistent data
      List<Holder.Reference<Potion>> potionHolders = getPotions();
      List<String> potions = potionHolders.stream().map(holder -> holder.key().identifier().toString()).toList();

      // next, make 1 copy of the potion fluid with potion contents per potion fluid
      // 1.21: potion data lives on the fluid's POTION_CONTENTS component rather than fluid NBT
      List<FluidStack> basePotions = this.fluid.getFluids();
      List<FluidStack> fluids = basePotions.stream()
        .flatMap(fluid -> potionHolders.stream().map(potion -> {
          FluidStack copy = fluid.copy();
          copy.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
          return copy;
        }))
        .toList();

      // finally, create 1 recipe per input tool
      int basePotionCount = basePotions.size();
      displayRecipes = Arrays.stream(bottle.getItems()).map(stack -> {
        // start with just the tool with the modifier
        ItemStack withModifier = addModifier(stack);
        // next, add the potion to the tools
        List<ItemStack> withPotion = addPotion(withModifier, potions);
        // duplicate the list if we have multiple potion fluids
        if (basePotionCount > 1) {
          List<ItemStack> list = new ArrayList<>(withPotion.size() * basePotionCount);
          for (int i = 0; i < basePotionCount; i++) {
            list.addAll(withPotion);
          }
          withPotion = List.copyOf(list);
        }
        // and finally create the recipe
        return DisplayCastingRecipe.from(this)
          .cast(withModifier).consumed()
          .results(withPotion).fluids(fluids).linkFluidsToOutput()
          .coolingTime(coolingTime).build();
      }).toList();
    }
    return displayRecipes;
  }
}
