package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.utils.WeakListenerList;
import slimeknights.tconstruct.smeltery.block.entity.component.DuctBlockEntity;

import java.util.function.Consumer;

/**
 * Item handler for the duct
 */
public class DuctItemHandler extends SingleItemHandler<DuctBlockEntity> {
  private final WeakListenerList onUpdate = new WeakListenerList();
  private FluidStack fluid = null;
  public DuctItemHandler(DuctBlockEntity parent) {
    super(parent, 1);
  }

  /** Called when the fluid changes to alert listeners */
  private void updateFluid() {
    fluid = null;
    onUpdate.run();
  }

  /**
   * Adds a listener to run when the fluid updates.
   * Generally these should just clear cache rather than immediately consuming the new fluid.
   */
  public <T> void addListener(T parent, Consumer<T> listener) {
    onUpdate.addListener(parent, listener);
  }

  /**
   * Sets the stack in this duct
   * @param newStack  New stack
   */
  @Override
  public void setStack(ItemStack newStack) {
    Level world = parent.getLevel();
    ItemStack current = getStack();
    // if both are empty, assume shift click so we need to update
    boolean hasChange = (current.isEmpty() && newStack.isEmpty()) || !ItemStack.matches(current, newStack);
    super.setStack(newStack);
    if (hasChange) {
      updateFluid();
      if (world != null) {
        if (!world.isClientSide()) {
          BlockPos pos = parent.getBlockPos();
          TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
        } else {
          parent.updateFluid();
        }
      }
    }
  }

  @Override
  protected boolean isItemValid(ItemStack stack) {
    // the item or its container must be in the tag
    if (!stack.is(TinkerTags.Items.DUCT_CONTAINERS)) {
      ItemStackTemplate container = stack.getCraftingRemainder();
      if (container == null || !container.is(TinkerTags.Items.DUCT_CONTAINERS)) {
        return false;
      }
    }
    // the item must contain fluid (no empty cans or buckets)
    return !getFirstFluid(stack).isEmpty();
  }

  /** Gets the fluid in the first tank of the given item */
  private static FluidStack getFirstFluid(ItemStack stack) {
    if (stack.isEmpty()) {
      return FluidStack.EMPTY;
    }
    ResourceHandler<FluidResource> cap = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
    return cap != null && cap.size() > 0 ? FluidUtil.getStack(cap, 0) : FluidStack.EMPTY;
  }

  /**
   * Gets the fluid filter for this duct
   * @return  Fluid filter
   */
  public FluidStack getFluid() {
    if (fluid == null) {
      fluid = getFirstFluid(getStack());
    }
    return fluid;
  }
}
