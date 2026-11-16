package slimeknights.tconstruct.library.recipe.casting.material;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.common.recipe.LoadableFinishedRecipe;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/** Builder for {@link PartSwapCastingRecipe} */
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "castingRecipe")
public class PartSwapCastingRecipeBuilder extends AbstractRecipeBuilder<PartSwapCastingRecipeBuilder> {
  private final Ingredient tools;
  private final int itemCost;
  /** Registered serializer, must be type aware. Since 26.1 serializers are records, see {@link LoadableFinishedRecipe#typeAware(RecipeSerializer)} */
  private final RecipeSerializer<PartSwapCastingRecipe> recipeSerializer;
  @Setter
  @Accessors(fluent = true)
  private int index = 0;
  @Setter
  private IJsonPredicate<MaterialVariantId> allowedMaterials = MaterialPredicate.ANY;

  /**
   * Creates a new part swapping recipe
   * @param tools     List of tools
   * @param itemCost  Amount needed to cast to swap
   * @return  Builder instance
   */
  public static PartSwapCastingRecipeBuilder basinRecipe(Ingredient tools, int itemCost) {
    return castingRecipe(tools, itemCost, TinkerSmeltery.basinPartSwappingSerializer.get());
  }

  /**
   * Creates a new part swapping recipe
   * @param itemCost  Amount needed to cast to swap
   * @return  Builder instance
   */
  public static PartSwapCastingRecipeBuilder tableRecipe(Ingredient tools, int itemCost) {
    return castingRecipe(tools, itemCost, TinkerSmeltery.tablePartSwappingSerializer.get());
  }

  @SuppressWarnings("deprecation")
  @Override
  public void save(RecipeOutput output) {
    // 1.21.2: ingredients expose item holders instead of stacks
    save(output, BuiltInRegistries.ITEM.getKey(tools.items().findFirst().orElseThrow(() -> new IllegalStateException("Part swapping recipe has no tools")).value()));
  }

  @Override
  public void save(RecipeOutput output, Identifier id) {
    AdvancementHolder advancement = this.buildOptionalAdvancement(output, id, "materials");
    output.accept(recipeKey(id), new PartSwapCastingRecipe(LoadableFinishedRecipe.typeAware(recipeSerializer), id, group, tools, itemCost, index, allowedMaterials), advancement);
  }
}
