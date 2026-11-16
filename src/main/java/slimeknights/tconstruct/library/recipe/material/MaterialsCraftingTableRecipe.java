package slimeknights.tconstruct.library.recipe.material;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.List;

/** Common methods between {@link ShapelessMaterialsRecipe} and {@link ShapedMaterialsRecipe} */
public interface MaterialsCraftingTableRecipe {
  /** Gets the list of parts on this recipe. May be larger than {@link #getPartCount()}, in which case the extra should be ignored. */
  List<Ingredient> getParts();

  /** Gets the number of parts in this recipe. */
  int getPartCount();

  /** Gets a list of extra materials to add after the parts. */
  List<MaterialVariantId> getExtraMaterials();

  /** Sets the output material for a single output recipe. */
  void setMaterial(ItemStack stack, MaterialVariantId material);

  /** Gets the result of this recipe before materials are applied, for display. */
  ItemStack getResultItem();

  /**
   * Gets the result of this recipe before materials are applied, for display.
   * @deprecated use {@link #getResultItem()}, registries are no longer needed. Kept as the vanilla method was removed from recipes in 1.21.2.
   */
  @Deprecated
  default ItemStack getResultItem(HolderLookup.Provider registries) {
    return getResultItem();
  }
}
