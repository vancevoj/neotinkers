package slimeknights.tconstruct.plugin.jei.transfer;

import com.mojang.datafixers.util.Either;
import lombok.Getter;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implements recipe transfer in the tinker station. */
@Internal
public class TinkerStationTransferInfo<T> implements IRecipeTransferInfo<TinkerStationContainerMenu,T>, IRecipeTransferHandler<TinkerStationContainerMenu,T> {
  @Getter
  private final IRecipeType<T> recipeType;
  private final IRecipeTransferHandlerHelper handlerHelper;
  private final IRecipeTransferHandler<TinkerStationContainerMenu,T> handler;

  public TinkerStationTransferInfo(IRecipeType<T> recipeType, IRecipeTransferHandlerHelper handlerHelper) {
    this.recipeType = recipeType;
    this.handlerHelper = handlerHelper;
    this.handler = handlerHelper.createUnregisteredRecipeTransferHandler(this);
  }

  @Override
  public Class<TinkerStationContainerMenu> getContainerClass() {
    return TinkerStationContainerMenu.class;
  }

  @Override
  public Optional<MenuType<TinkerStationContainerMenu>> getMenuType() {
    return Optional.of(TinkerTables.tinkerStationContainer.get());
  }

  @Override
  public boolean canHandle(TinkerStationContainerMenu container, T recipe) {
    return true;
  }

  /**
   * Ensures the recipe can transfer and filters the recipe slots to fit within the station slots.
   * @return  Error if the recipe cannot transfer, otherwise the slots view to transfer
   */
  private Either<IRecipeTransferError,IRecipeSlotsView> filterSlots(TinkerStationContainerMenu container, IRecipeSlotsView recipeSlotsView) {
    // must have server support
    if (!handlerHelper.recipeTransferHasServerSupport()) {
      return Either.left(handlerHelper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.no.server")));
    }

    // ensure we have space for the entire recipe
    List<IRecipeSlotView> slotViews = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);
    int size = container.getInputSlots().size();
    if (!validateRecipeFits(slotViews, size)) {
      return Either.left(this.handlerHelper.createUserErrorWithTooltip(Component.translatable("jei.tconstruct.tinker_station.too_large")));
    }

    // filter down the recipe slots to fit within the station slots
    IRecipeSlotsView filteredSlotView = recipeSlotsView;
    if (slotViews.size() > size) {
      filteredSlotView = handlerHelper.createRecipeSlotsView(slotViews.subList(0, size));
    }
    return Either.right(filteredSlotView);
  }

  @Nullable
  @Override
  public IRecipeTransferError transferRecipe(IRecipeTransferContext<T,TinkerStationContainerMenu> context, boolean doTransfer) {
    IRecipeSlotsView recipeSlots = context.getRecipeSlots();
    return filterSlots(context.getContainer(), recipeSlots).map(error -> error, filtered -> {
      // keep the original context when nothing was filtered, so JEI tracks the transfer result as usual
      IRecipeTransferContext<T,TinkerStationContainerMenu> transferContext = filtered == recipeSlots ? context : new FilteredTransferContext<>(context, filtered);
      return this.handler.transferRecipe(transferContext, doTransfer);
    });
  }

  /** @deprecated JEI calls {@link #transferRecipe(IRecipeTransferContext, boolean)}, this is still abstract in the interface. */
  @SuppressWarnings("removal")
  @Deprecated
  @Nullable
  @Override
  public IRecipeTransferError transferRecipe(TinkerStationContainerMenu container, T recipe, IRecipeSlotsView recipeSlotsView, Player player, boolean maxTransfer, boolean doTransfer) {
    return filterSlots(container, recipeSlotsView).map(error -> error, filtered -> this.handler.transferRecipe(container, recipe, filtered, player, maxTransfer, doTransfer));
  }

  @Override
  public List<Slot> getRecipeSlots(TinkerStationContainerMenu container, T recipe) {
    return container.getInputSlots();
  }

  @Override
  public List<Slot> getInventorySlots(TinkerStationContainerMenu container, T recipe) {
    List<Slot> slots = new ArrayList<>();
    // skip over inputs, output slot, tool slot, armor, and offhand. Only the 4 humanoid armor slots are added
    // (the menu skips 1.21's BODY type), so use that count rather than ArmorType.values().length (which is 5).
    int start = container.getInputSlots().size() + 3 + ModifiableArmorMaterial.ARMOR_TYPES.length;
    for(int i = start; i < start + 36; i++) {
      Slot slot = container.getSlot(i);
      slots.add(slot);
    }

    return slots;
  }

  /** Ensures the recipe fits in the slot view */
  private static boolean validateRecipeFits(List<IRecipeSlotView> slotViews, int size) {
    int bound = slotViews.size();
    for (int i = size; i < bound; i++) {
      IRecipeSlotView slotView = slotViews.get(i);
      // if it is not empty and it contains an item stack, thats a problem
      // the item stack check is because we show modifier slots as an "input"
      if (!slotView.isEmpty() && slotView.getDisplayedItemStack().isPresent()) {
        return false;
      }
    }
    return true;
  }

}
