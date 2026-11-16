package slimeknights.tconstruct.library.recipe.modifiers.adding;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.tconstruct.common.TinkerTags;

/**
 * Builder for overslime recipes
 */
@RequiredArgsConstructor(staticName = "modifier")
public class OverslimeModifierRecipeBuilder extends AbstractRecipeBuilder<OverslimeModifierRecipeBuilder> {
  @Setter @Accessors(chain = true)
  private Ingredient tools = IngredientHelper.tag(TinkerTags.Items.DURABILITY);
  private final Ingredient ingredient;
  private final int restoreAmount;

  /** Creates a new builder for the given item */
  public static OverslimeModifierRecipeBuilder modifier(ItemLike item, int restoreAmount) {
    return modifier(Ingredient.of(item), restoreAmount);
  }

  @Override
  public void save(RecipeOutput consumer) {
    Item item = ingredient.items().findFirst().map(Holder::value).orElseThrow(() -> new IllegalStateException("Empty ingredient not allowed"));
    save(consumer, Loadables.ITEM.getKey(item));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    if (IngredientLoadable.isEmpty(ingredient)) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(consumer, id, "modifiers");
    consumer.accept(recipeKey(id), new OverslimeModifierRecipe(id, tools, ingredient, restoreAmount), advancement);
  }

  /** Creates a crafting table overslime repair recipe */
  public OverslimeModifierRecipeBuilder saveCrafting(RecipeOutput consumer, Identifier id) {
    if (IngredientLoadable.isEmpty(ingredient)) {
      throw new IllegalStateException("Empty ingredient not allowed");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(consumer, id, "modifiers");
    consumer.accept(recipeKey(id), new OverslimeCraftingTableRecipe(id, tools, ingredient, restoreAmount), advancement);
    return this;
  }
}
