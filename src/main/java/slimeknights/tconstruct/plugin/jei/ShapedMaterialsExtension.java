package slimeknights.tconstruct.plugin.jei;

import com.google.common.collect.Streams;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Logic to show {@link ShapedMaterialsRecipe} in JEI */
public class ShapedMaterialsExtension implements ICraftingCategoryExtension {
  private final ShapedMaterialsRecipe recipe;
  private final ItemStack plainResult;
  private final List<ItemStack> result;
  @Nullable
  private final int[] materialSlots;

  private ShapedMaterialsExtension(ShapedMaterialsRecipe recipe) {
    this.recipe = recipe;
    this.plainResult = recipe.getResultItem(Objects.requireNonNull(SafeClientAccess.getRegistryAccess()));

    // if we have just the one part, set the output to match its material
    List<Ingredient> parts = recipe.getParts();
    if (parts.size() == 1) {
      Ingredient firstPart = parts.get(0);
      this.result = Arrays.stream(firstPart.getItems()).map(variant -> {
        ItemStack stack = plainResult.copy();
        if (variant.getItem() instanceof IMaterialItem materialItem) {
          recipe.setMaterial(stack, materialItem.getMaterial(variant));
        } else {
          recipe.setMaterial(stack, MaterialRecipeCache.findRecipe(variant).getMaterial().getVariant());
        }
        return stack;
      }).toList();
      List<Ingredient> inputs = recipe.getIngredients();
      this.materialSlots = IntStream.range(0, inputs.size()).filter(i -> inputs.get(i) == firstPart).toArray();
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
  public static ShapedMaterialsExtension create(ShapedMaterialsRecipe recipe) {
    for (Ingredient ingredient : recipe.getParts()) {
      if (ingredient.getItems().length == 0) {
        return null;
      }
    }
    return new ShapedMaterialsExtension(recipe);
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

  /** Sets the recipe in the builder */
  public static void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, ShapedRecipe recipe, List<ItemStack> result, ItemStack plainResult, @Nullable int[] materialSlots) {
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(plainResult);

    // apply ingredient stacks
    List<List<ItemStack>> inputStacks = recipe.getIngredients().stream().map(ingredient -> List.of(ingredient.getItems())).toList();
    int width = recipe.getWidth();
    int height = recipe.getHeight();
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, result);
    if (inputs.size() != 9) {
      Mantle.logger.error("Failed to create focus link for {} as the layout {} is not 3x3", recipe.getId(), builder.getClass().getName());
    } else if (materialSlots != null) {
      // apply focus links
      builder.createFocusLink(Streams.concat(
        Stream.of(output),
        Arrays.stream(materialSlots).mapToObj(i -> inputs.get(getCraftingIndex(i, width, height)))
      ).toArray(IRecipeSlotBuilder[]::new));
    }
  }

  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focusGroup) {
    setRecipe(builder, craftingGridHelper, recipe, result, plainResult, materialSlots);
  }

  /** Borrowed from {@link ICraftingGridHelper} implementation. TODO: make mantle variant public */
  private static int getCraftingIndex(int i, int width, int height) {
    int index;
    if (width == 1) {
      if (height == 3) {
        index = (i * 3) + 1;
      } else if (height == 2) {
        index = (i * 3) + 1;
      } else {
        index = 4;
      }
    } else if (height == 1) {
      index = i + 3;
    } else if (width == 2) {
      index = i;
      if (i > 1) {
        index++;
        if (i > 3) {
          index++;
        }
      }
    } else if (height == 2) {
      index = i + 3;
    } else {
      index = i;
    }
    return index;
  }
}
