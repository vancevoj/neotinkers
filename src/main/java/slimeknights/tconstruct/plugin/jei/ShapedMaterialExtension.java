package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialRecipe;
import slimeknights.tconstruct.plugin.jei.material.MaterialsCraftingExtension;
import slimeknights.tconstruct.plugin.jei.material.ShapedMaterialsExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Logic to show {@link ShapedMaterialRecipe} in JEI
 * @deprecated use {@link ShapedMaterialsExtension}
 */
@Deprecated
public class ShapedMaterialExtension implements ICraftingCategoryExtension<ShapedMaterialRecipe> {
  @Override
  public List<SlotDisplay> getIngredients(RecipeHolder<ShapedMaterialRecipe> holder) {
    return holder.value().getIngredients().stream().map(Ingredient::optionalIngredientToDisplay).toList();
  }

  @Override
  public int getWidth(RecipeHolder<ShapedMaterialRecipe> holder) {
    return holder.value().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedMaterialRecipe> holder) {
    return holder.value().getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<ShapedMaterialRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focusGroup) {
    ShapedMaterialRecipe recipe = holder.value();
    MaterialValueIngredient materials = recipe.getMaterial();
    ItemStack plainResult = recipe.getResultItem();
    List<ItemStack> result;
    if (materials != null) {
      result = MaterialRecipeCache.getAllRecipes().stream().filter(materials::test).flatMap(mat -> {
        ItemStack stack = plainResult.copy();
        recipe.setMaterial(stack, mat.getMaterial().getVariant());
        // add one copy of the stack per item in the nested ingredient, so the lengths match up
        return IntStream.range(0, IngredientHelper.getStacks(mat.getIngredient()).size()).mapToObj(i -> stack);
      }).toList();
    } else {
      result = List.of(plainResult);
    }
    // 1.21.2: shaped recipes store optional ingredients, empty for blank slots
    List<Optional<Ingredient>> inputs = recipe.getIngredients();
    int[] materialSlots = IntStream.range(0, inputs.size()).filter(i -> inputs.get(i).map(Ingredient::getCustomIngredient).orElse(null) instanceof MaterialValueIngredient).toArray();
    MaterialsCraftingExtension.setRecipe(recipe.getWidth(), recipe.getHeight(), builder, craftingGridHelper, recipe, result, plainResult, materialSlots);
  }
}
