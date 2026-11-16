package slimeknights.tconstruct.plugin.jei.transfer;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Transfer context with a filtered list of recipe slots, delegating everything else (including the transfer result) to the original context.
 * Same as JEI's internal {@code RecipeTransferContext#copyWithRecipeSlots}, which is not API.
 * @param parent       Original context from JEI
 * @param recipeSlots  Filtered recipe slots
 */
record FilteredTransferContext<R, C extends AbstractContainerMenu>(IRecipeTransferContext<R,C> parent, IRecipeSlotsView recipeSlots) implements IRecipeTransferContext<R,C> {
  @Override
  public int getTransferId() {
    return parent.getTransferId();
  }

  @Override
  public void completeRecipeTransfer(RecipeTransferResult result) {
    parent.completeRecipeTransfer(result);
  }

  @Override
  public R getRecipe() {
    return parent.getRecipe();
  }

  @Override
  public IRecipeType<R> getRecipeType() {
    return parent.getRecipeType();
  }

  @Override
  public C getContainer() {
    return parent.getContainer();
  }

  @Override
  public AbstractContainerScreen<C> getScreen() {
    return parent.getScreen();
  }

  @Override
  public IRecipeSlotsView getRecipeSlots() {
    return recipeSlots;
  }

  @Override
  public Player getPlayer() {
    return parent.getPlayer();
  }

  @Override
  public boolean isMaxTransfer() {
    return parent.isMaxTransfer();
  }
}
