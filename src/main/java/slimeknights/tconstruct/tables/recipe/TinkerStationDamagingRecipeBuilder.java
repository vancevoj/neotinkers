package slimeknights.tconstruct.tables.recipe;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;

/** Builder for tinker station damaging recipes */
@RequiredArgsConstructor(staticName = "damage")
public class TinkerStationDamagingRecipeBuilder extends AbstractRecipeBuilder<TinkerStationDamagingRecipeBuilder> {

  private final Ingredient ingredient;
  private final int damageAmount;

  @Override
  public void save(RecipeOutput output) {
    Item item = ingredient.items().findFirst().map(Holder::value).orElseThrow(() -> new IllegalStateException("Empty ingredient not allowed"));
    save(output, BuiltInRegistries.ITEM.getKey(item));
  }

  @Override
  public void save(RecipeOutput output, Identifier id) {
    if (IngredientLoadable.isEmpty(ingredient)) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(output, id, "tinker_station");
    output.accept(recipeKey(id), new TinkerStationDamagingRecipe(id, ingredient, damageAmount), advancement);
  }
}
