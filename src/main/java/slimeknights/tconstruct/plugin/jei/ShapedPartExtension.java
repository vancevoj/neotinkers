package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.material.ShapedPartRecipe;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** Logic to show {@link ShapedPartRecipe} in JEI */
public class ShapedPartExtension implements ICraftingCategoryExtension {
  private final ShapedPartRecipe recipe;
  private final ItemStack plainResult;
  private final List<ItemStack> result;
  @Nullable
  private final int[] materialSlots;

  private ShapedPartExtension(ShapedPartRecipe recipe) {
    this.recipe = recipe;
    this.plainResult = recipe.getResultItem(Objects.requireNonNull(SafeClientAccess.getRegistryAccess()));

    // if we have just the one part, set the output to match its material
    List<Ingredient> parts = recipe.getParts();
    if (parts.size() == 1) {
      this.result = Arrays.stream(parts.get(0).getItems()).map(variant -> {
        ItemStack stack = plainResult.copy();
        recipe.setMaterial(stack, IMaterialItem.getMaterialFromStack(variant));
        return stack;
      }).toList();
      List<Ingredient> inputs = recipe.getIngredients();
      this.materialSlots = IntStream.range(0, inputs.size()).filter(i -> inputs.get(i) instanceof MaterialIngredient).toArray();
      // otherwise, use a display material. allow display tool part if it has just 1 material
    } else if (recipe.getExtraMaterials().isEmpty() && plainResult.getItem() instanceof IMaterialItem materialItem) {
      this.result = List.of(materialItem.setMaterialForced(plainResult, ToolBuildHandler.getRenderMaterial(0)));
      this.materialSlots = null;
    } else {
      // display tool
      this.result = List.of(IModifiableDisplay.getDisplayStack(plainResult));
      this.materialSlots = null;
    }
  }

  /** {@return Instance of the shaped extension, or null if the recipe is invalid for display} */
  @Nullable
  public static ShapedPartExtension create(ShapedPartRecipe recipe) {
    for (Ingredient ingredient : recipe.getParts()) {
      if (ingredient.getItems().length == 0) {
        return null;
      }
    }
    return new ShapedPartExtension(recipe);
  }

  @Override
  public int getWidth() {
    return recipe.getWidth();
  }

  @Override
  public int getHeight() {
    return recipe.getHeight();
  }

  @Override
  public ResourceLocation getRegistryName() {
    return recipe.getId();
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focusGroup) {
    ShapedMaterialExtension.setRecipe(builder, craftingGridHelper, recipe, result, plainResult, materialSlots);
  }
}
