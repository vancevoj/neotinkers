package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import slimeknights.tconstruct.library.recipe.molding.IMoldingContainer;

/** Wrapper around an item handler for the sake of use as a molding inventory */
@RequiredArgsConstructor
public class MoldingContainerWrapper implements IMoldingContainer {
  private final ResourceHandler<ItemResource> handler;
  private final int slot;

  @Getter @Setter
  private ItemStack pattern = ItemStack.EMPTY;

  @Override
  public ItemStack getMaterial() {
    // copy of the stack, recipes only read it
    return slot < handler.size() ? ItemUtil.getStack(handler, slot) : ItemStack.EMPTY;
  }
}
