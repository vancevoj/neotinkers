package slimeknights.tconstruct.plugin.jei.material;

import com.google.common.collect.Streams;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.plugin.jei.MantleJEIConstants;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.MaterialsCraftingTableRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.plugin.jei.util.CategoryUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Common logic for {@link ShapedMaterialsExtension} and {@link ShapelessMaterialsExtension} */
public class MaterialsCraftingExtension<T extends CraftingRecipe & MaterialsCraftingTableRecipe> implements ICraftingCategoryExtension<T> {
  private static final String RESULT_SLOT = "result";

  /** {@return the list of display outputs for the given recipe} */
  protected List<ItemStack> getResult(T recipe, ItemStack plainResult) {
    // if we have just the one part, set the output to match its material
    if (recipe.getPartCount() == 1) {
      Ingredient firstPart = recipe.getParts().get(0);
      return Arrays.stream(firstPart.getItems()).map(variant -> {
        ItemStack stack = plainResult.copy();
        recipe.setMaterial(stack, MaterialRecipeCache.getMaterial(variant));
        return stack;
      }).toList();
    }
    // otherwise, just set a display tool, it should never be seen as the displayed materials replace it
    return List.of(IModifiableDisplay.getDisplayStack(plainResult));
  }

  /** {@return the slot indices matching the first part to link to the output, or null if there are no material slots to focus} */
  @Nullable
  protected int[] getMaterialSlots(T recipe) {
    // only the single part case has focus links
    if (recipe.getPartCount() == 1) {
      return getMaterialSlots(recipe, recipe.getParts().get(0));
    }
    return null;
  }

  /** {@return the list of slot indices matching each part, empty if there is only a single part} */
  protected List<int[]> getPartSlots(T recipe) {
    if (recipe.getPartCount() == 1) {
      return List.of();
    }
    // parts beyond the part count are not materials, so skip them
    return recipe.getParts().stream().limit(recipe.getPartCount()).map(part -> getMaterialSlots(recipe, part)).toList();
  }

  /** Gets the material slots for the given recipe from {@link CraftingRecipe#getIngredients()} */
  protected int[] getMaterialSlots(T recipe, Ingredient part) {
    return new int[] {0};
  }

  /** {@return Instance of the shapeless extension} */
  public static MaterialsCraftingExtension<ShapelessMaterialsRecipe> shapeless() {
    return new ShapelessMaterialsExtension();
  }

  /** {@return whether the recipe is valid for display} */
  protected boolean isValid(T recipe) {
    List<Ingredient> parts = recipe.getParts();
    for (int i = 0; i < recipe.getPartCount(); i++) {
      if (parts.get(i).getItems().length == 0) {
        return false;
      }
    }
    return true;
  }

  /** Gets a modifiable list of inputs to the given recipe */
  private static List<List<ItemStack>> getInputs(CraftingRecipe recipe) {
    // using Collectors.toList to ensure we can mutate the list
    return recipe.getIngredients().stream().map(ingredient -> List.of(ingredient.getItems())).collect(Collectors.toList());
  }

  @Override
  public void setRecipe(RecipeHolder<T> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    T recipe = holder.value();
    if (!isValid(recipe)) {
      return;
    }
    ItemStack plainResult = recipe.getResultItem(Objects.requireNonNull(SafeClientAccess.getRegistryAccess()));
    List<List<ItemStack>> inputs = getInputs(recipe);

    // if we have a focused tool, use it to filter the inputs to match
    // we just assume the first 9 slots are inputs to avoid needing to make a new list
    ItemStack focus = CategoryUtil.getResultItemFocus(focuses);
    // if we have an output focus, set the input slots to match
    if (!focus.isEmpty()) {
      List<int[]> partSlots = getPartSlots(recipe);
      MaterialIdNBT materials = MaterialIdNBT.from(focus);
      // loop through finding all overrides to display, but skip any that are not present on the tool
      int partCount = partSlots.size();
      for (int i = 0; i < partCount; i++) {
        // find all inputs matching the material
        MaterialVariantId material = materials.getMaterial(i);
        List<ItemStack> matchingStacks = Arrays.stream(recipe.getParts().get(i).getItems())
          .filter(stack -> material.matchesVariant(MaterialRecipeCache.getMaterial(stack)))
          .toList();
        // if filtered to empty, just display the original stacks
        // this happens if the focus has materials that are not in this recipe
        if (!matchingStacks.isEmpty()) {
          for (int slot : partSlots.get(i)) {
            inputs.set(slot, matchingStacks);
          }
        }
      }
    }
    setRecipe(getWidth(holder), getHeight(holder), builder, craftingGridHelper, holder.id(), inputs, getResult(recipe, plainResult), plainResult, getMaterialSlots(recipe));
  }

  @Override
  public void onDisplayedIngredientsUpdate(RecipeHolder<T> holder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
    T recipe = holder.value();
    // don't care if only 1 part
    if (recipe.getPartCount() > 1 && isValid(recipe)) {
      IRecipeSlotDrawable resultSlot = CategoryUtil.findSlot(recipeSlots, RESULT_SLOT);
      if (resultSlot != null) {
        int width = getWidth(holder);
        int height = getHeight(holder);
        if (width <= 0 || height <= 0) {
          width = height = CategoryUtil.getShapelessSize(recipe.getIngredients().size());
        }
        // find input materials and use to set the output
        List<int[]> partSlots = getPartSlots(recipe);
        List<MaterialVariantId> variants = new ArrayList<>(partSlots.size());
        for (int[] partSlot : partSlots) {
          ItemStack stack = recipeSlots.get(MantleJEIConstants.getCraftingIndex(partSlot[0], width, height)).getDisplayedItemStack().orElse(ItemStack.EMPTY);
          variants.add(MaterialRecipeCache.getMaterial(stack));
        }
        variants.addAll(recipe.getExtraMaterials());
        ItemStack plainResult = recipe.getResultItem(Objects.requireNonNull(SafeClientAccess.getRegistryAccess()));
        resultSlot.createDisplayOverrides().addItemStack(new MaterialIdNBT(variants).updateStack(plainResult.copy()));
      }
    }
  }

  /**
   * Sets the recipe in the builder
   * @deprecated use {@link #setRecipe(int, int, IRecipeLayoutBuilder, ICraftingGridHelper, Object, List, List, ItemStack, int[])}
   */
  @Deprecated
  public static void setRecipe(int width, int height, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, CraftingRecipe recipe, List<ItemStack> result, ItemStack plainResult, @Nullable int[] materialSlots) {
    setRecipe(width, height, builder, craftingGridHelper, recipe, getInputs(recipe), result, plainResult, materialSlots);
  }

  /**
   * Sets the recipe in the builder
   * @param width               Recipe width, 0 or less for shapeless
   * @param height              Recipe height, 0 or less for shapeless
   * @param builder             Layout builder
   * @param craftingGridHelper  Crafting grid helper
   * @param id                  Recipe ID (or recipe) for error logging
   * @param inputStacks         Displayed stacks for each recipe input
   * @param result              Displayed result stacks
   * @param plainResult         Result without materials, added as an invisible output
   * @param materialSlots       Recipe input indices to focus link with the result
   */
  public static void setRecipe(int width, int height, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, Object id, List<List<ItemStack>> inputStacks, List<ItemStack> result, ItemStack plainResult, @Nullable int[] materialSlots) {
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(plainResult);

    // shapeless needs its width and height set, but we also want to recover those sizes, so calculate it locally
    if (width <= 0 || height <= 0) {
      width = height = CategoryUtil.getShapelessSize(inputStacks.size());
      builder.setShapeless();
    }
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(builder, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, result).setSlotName(RESULT_SLOT);
    if (inputs.size() != 9) {
      TConstruct.LOG.error("Failed to create focus link for {} as the layout {} is not 3x3", id, builder.getClass().getName());
    } else if (materialSlots != null) {
      // apply focus links
      int finalWidth = width;
      int finalHeight = height;
      builder.createFocusLink(Streams.concat(
        Stream.of(output),
        Arrays.stream(materialSlots).mapToObj(i -> inputs.get(MantleJEIConstants.getCraftingIndex(i, finalWidth, finalHeight)))
      ).toArray(IRecipeSlotBuilder[]::new));
    }
  }
}
