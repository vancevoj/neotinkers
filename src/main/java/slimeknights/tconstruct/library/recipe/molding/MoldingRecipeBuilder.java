package slimeknights.tconstruct.library.recipe.molding;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EmptyIngredient;
import slimeknights.tconstruct.common.recipe.LoadableFinishedRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "molding")
public class MoldingRecipeBuilder extends AbstractRecipeBuilder<MoldingRecipeBuilder> {
  private final ItemOutput output;
  /** Registered serializer, must be type aware. Since 26.1 serializers are records, see {@link LoadableFinishedRecipe#typeAware(RecipeSerializer)} */
  private final RecipeSerializer<MoldingRecipe> serializer;
  private Ingredient material = EmptyIngredient.VANILLA;
  private Ingredient pattern = EmptyIngredient.VANILLA;
  private boolean patternConsumed = false;

  /**
   * Creates a new builder of the given item
   * @param item  Item output
   * @return  Recipe
   */
  public static MoldingRecipeBuilder moldingTable(ItemLike item) {
    return molding(ItemOutput.fromItem(item), TinkerSmeltery.moldingTableSerializer.get());
  }

  /**
   * Creates a new builder of the given item
   * @param item  Item output
   * @return  Recipe
   */
  public static MoldingRecipeBuilder moldingBasin(ItemLike item) {
    return molding(ItemOutput.fromItem(item), TinkerSmeltery.moldingBasinSerializer.get());
  }

  /* Inputs */

  /** Sets the material item, on the table */
  public MoldingRecipeBuilder setMaterial(Ingredient ingredient) {
    this.material = ingredient;
    return this;
  }

  /** Sets the material item, on the table */
  public MoldingRecipeBuilder setMaterial(ItemLike item) {
    return setMaterial(Ingredient.of(item));
  }

  /** Sets the material item, on the table */
  public MoldingRecipeBuilder setMaterial(TagKey<Item> tag) {
    return setMaterial(IngredientHelper.tag(tag));
  }

  /** Sets the mold item, in the players hand */
  public MoldingRecipeBuilder setPattern(Ingredient ingredient, boolean consumed) {
    this.pattern = ingredient;
    this.patternConsumed = consumed;
    return this;
  }

  /** Sets the mold item, in the players hand */
  public MoldingRecipeBuilder setPattern(ItemLike item, boolean consumed) {
    return setPattern(Ingredient.of(item), consumed);
  }

  /** Sets the mold item, in the players hand */
  public MoldingRecipeBuilder setPattern(TagKey<Item> tag, boolean consumed) {
    return setPattern(IngredientHelper.tag(tag), consumed);
  }


  /* Building */

  @Override
  public void save(RecipeOutput output) {
    save(output, LoadableFinishedRecipe.getItemId(this.output));
  }

  @Override
  public void save(RecipeOutput recipeOutput, Identifier id) {
    if (IngredientLoadable.isEmpty(material)) {
      throw new IllegalStateException("Missing material for molding recipe");
    }
    AdvancementHolder advancement = buildOptionalAdvancement(recipeOutput, id, "molding");
    recipeOutput.accept(recipeKey(id), new MoldingRecipe(LoadableFinishedRecipe.typeAware(serializer), id, material, pattern, patternConsumed, output), advancement);
  }
}
