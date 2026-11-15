package slimeknights.tconstruct.tools.recipe;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.library.recipe.worktable.AbstractSizedIngredientRecipeBuilder;

/** Builder for modifier sorting recipes */
@RequiredArgsConstructor(staticName = "sorting")
public class ModifierSortingRecipeBuilder extends AbstractSizedIngredientRecipeBuilder<ModifierSortingRecipeBuilder> {

  @Override
  public void save(RecipeOutput output) {
    save(output, BuiltInRegistries.ITEM.getKey(inputs.get(0).getIngredient().items().findFirst().orElseThrow().value()));
  }

  @Override
  public void save(RecipeOutput output, Identifier id) {
    if (inputs.isEmpty()) {
      throw new IllegalStateException("Must have at least one ingredient");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(output, id, "modifiers");
    output.accept(recipeKey(id), new ModifierSortingRecipe(id, inputs), advancement);
  }
}
