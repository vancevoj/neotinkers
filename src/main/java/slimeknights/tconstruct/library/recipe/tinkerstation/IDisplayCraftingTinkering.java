package slimeknights.tconstruct.library.recipe.tinkerstation;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface for tool modifications that should show in the crafting recipe category in JEI.
 * Generally implemented on a crafting recipe, but may be used for dynamic recipes.
 * If you wish to use {@link #isFiltered()} or {@link #showUnfocused()} in the crafting table, this must not be implemented on the recipe, and instead returned from {@link slimeknights.tconstruct.library.recipe.display.VanillaFilteredRecipe}.
 */
public interface IDisplayCraftingTinkering extends IDisplayToolTinkering, CraftingRecipe {
  /**
   * Gets the ID for the crafting table tab.
   * In 1.21 recipes no longer carry their ID, so this is used to wrap the display recipe in a {@link net.minecraft.world.item.crafting.RecipeHolder} for JEI.
   */
  Identifier getId();

  /** Gets the ID for the tinker station tab. */
  @Nullable
  @Override
  default Identifier getRecipeId() {
    return getId();
  }

  /** Gets the tooltip for the information icon in the crafting table tab. By default, calls {@link #getTitle()} and {@link #getTooltip()}, but can be overridden to return separate strings. */
  default List<Component> getInformation() {
    return List.of(getTitle(), getTooltip());
  }


  /* Implement crafting table methods, probably no need to override. */

  @Override
  default boolean isSpecial() {
    // ensure standard JEI skips this recipe
    return true;
  }

  @Override
  default boolean canCraftInDimensions(int width, int height) {
    return getInputCount() + 1 < width * height;
  }


  /* Implement various methods that don't matter. Can always be overridden on an actual crafting recipe. */

  @Override
  default CraftingBookCategory category() {
    return CraftingBookCategory.MISC;
  }

  @Override
  default boolean matches(CraftingInput container, Level pLevel) {
    return false;
  }

  @Override
  default ItemStack getResultItem(HolderLookup.Provider access) {
    return ItemStack.EMPTY;
  }

  @Override
  default ItemStack assemble(CraftingInput container, HolderLookup.Provider access) {
    return ItemStack.EMPTY;
  }

  @Override
  default RecipeSerializer<?> getSerializer() {
    throw new UnsupportedOperationException();
  }
}
