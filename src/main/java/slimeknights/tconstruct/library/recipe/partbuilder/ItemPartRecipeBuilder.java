package slimeknights.tconstruct.library.recipe.partbuilder;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.common.recipe.LoadableFinishedRecipe;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

/** Builder for {@link ItemPartRecipe} */
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "item")
public class ItemPartRecipeBuilder extends AbstractRecipeBuilder<ItemPartRecipeBuilder> {
  private final Pattern pattern;
  private final ItemOutput result;
  @Setter
  private Ingredient patternItem = IPartBuilderRecipe.DEFAULT_PATTERNS;
  private MaterialId materialId = MaterialId.UNKNOWN;
  private int cost = 0;
  @Setter
  private Identifier titleKey = null;

  /** Creates a builder for the given pattern ID */
  public static ItemPartRecipeBuilder item(Identifier pattern, ItemOutput result) {
    return item(new Pattern(pattern), result);
  }

  /** Sets the material Id and cost */
  public ItemPartRecipeBuilder material(MaterialId material, int cost) {
    this.materialId = material;
    this.cost = cost;
    return this;
  }

  @Override
  public void save(RecipeOutput consumer) {
    save(consumer, LoadableFinishedRecipe.getItemId(result));
  }

  @Override
  public void save(RecipeOutput consumer, Identifier id) {
    AdvancementHolder advancementId = buildOptionalAdvancement(consumer, id, "parts");
    consumer.accept(recipeKey(id), new ItemPartRecipe(id, materialId, pattern, patternItem, cost, result, titleKey), advancementId);
  }
}
