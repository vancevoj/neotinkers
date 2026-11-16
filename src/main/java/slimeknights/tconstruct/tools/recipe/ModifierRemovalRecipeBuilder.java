package slimeknights.tconstruct.tools.recipe;

import com.mojang.datafixers.util.Function6;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;
import slimeknights.tconstruct.library.tools.SlotType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Builder for {@link ModifierRemovalRecipe} and {@link ExtractModifierRecipe} */
@RequiredArgsConstructor(staticName = "removal")
public class ModifierRemovalRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierRemovalRecipeBuilder> {
  private final Function6<Identifier,String,SizedIngredient,List<SizedIngredient>,List<ItemOutput>,IJsonPredicate<ModifierId>,ModifierRemovalRecipe> constructor;
  private final List<ItemOutput> leftovers = new ArrayList<>();
  /** Item of the first leftover, used for the default recipe ID as stacks cannot be created during datagen */
  @Nullable
  private Item firstLeftover = null;
  @Accessors(chain = true)
  @Setter
  private String name = "modifiers";
  private SizedIngredient tools = ModifierRemovalRecipe.DEFAULT_TOOLS;
  @Accessors(fluent = true)
  @Setter
  private IJsonPredicate<ModifierId> modifierPredicate = ModifierPredicate.ANY;

  public static ModifierRemovalRecipeBuilder removal() {
    return removal(ModifierRemovalRecipe::new);
  }

  public static ModifierRemovalRecipeBuilder extract() {
    return removal(ExtractModifierRecipe::new);
  }

  /** Sets the name from the given slot */
  public ModifierRemovalRecipeBuilder slotName(SlotType slot) {
    return setName(slot.getName());
  }

  /**
   * Sets the tool requirement for this recipe
   */
  public ModifierRemovalRecipeBuilder setTools(SizedIngredient ingredient) {
    this.tools = ingredient;
    return this;
  }

  /**
   * Sets the tool requirement for this recipe
   */
  public ModifierRemovalRecipeBuilder setTools(Ingredient ingredient) {
    return setTools(SizedIngredient.of(ingredient));
  }

  /**
   * Adds a leftover output to the recipe
   */
  public ModifierRemovalRecipeBuilder addLeftover(ItemOutput output) {
    leftovers.add(output);
    return this;
  }

  /**
   * Adds a leftover stack to the recipe
   */
  public ModifierRemovalRecipeBuilder addLeftover(ItemStack stack) {
    if (firstLeftover == null) {
      firstLeftover = stack.getItem();
    }
    return addLeftover(ItemOutput.fromStack(stack));
  }

  /**
   * Adds a leftover stack to the recipe
   */
  public ModifierRemovalRecipeBuilder addLeftover(ItemLike item) {
    if (firstLeftover == null) {
      firstLeftover = item.asItem();
    }
    // lazy output, as stacks cannot be created during datagen
    return addLeftover(ItemOutput.fromItem(item));
  }

  @Override
  public void save(RecipeOutput output) {
    Item item = firstLeftover;
    if (item == null) {
      // only reached if the first leftover was an explicit output
      item = leftovers.get(0).get().getItem();
    }
    save(output, BuiltInRegistries.ITEM.getKey(item));
  }

  @Override
  public void save(RecipeOutput output, Identifier id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one input");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(output, id, "modifiers");
    output.accept(recipeKey(id), constructor.apply(id, name, tools, inputs, leftovers, modifierPredicate), advancement);
  }
}
