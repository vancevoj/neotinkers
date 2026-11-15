package slimeknights.tconstruct.smeltery.block.entity.tank;

import lombok.Getter;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.EmptyFluidHandlerItem;
import slimeknights.tconstruct.library.fluid.IFluidTankUpdater;

import javax.annotation.Nullable;

/**
 * Item handler holding a fluid container item, with a fluid handler that proxies to the item's fluid capability.
 * A single class cannot be both an item and a fluid resource handler, so the fluid side is {@link #getFluidHandler()}.
 * Fluid changes update the item through an {@link ItemAccess} on this handler, so they apply with the item once the root transaction commits.
 */
public class ProxyItemTank<T extends MantleBlockEntity & IFluidTankUpdater> extends SingleItemHandler<T> {
  /** Access to the stored item for the item's fluid capability, changes through it apply to this handler in the transaction */
  private final ItemAccess access;
  /** Fluid handler proxying to the stored item */
  @Getter
  private final ResourceHandler<FluidResource> fluidHandler = new ItemFluidHandler();
  /** Cached fluid capability of the stored item */
  @Nullable
  private ResourceHandler<FluidResource> itemTank;
  /** Item the cached capability was created for */
  private ItemResource cachedResource = ItemResource.EMPTY;
  /** Set while the item's fluid capability changes the item. Skips validation, as the item may turn into a container not valid to insert directly */
  private boolean changingFluid = false;

  public ProxyItemTank(T parent) {
    super(parent, 1);
    this.access = ItemAccess.forHandlerIndexStrict(this, 0);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected boolean isItemValid(ItemStack stack) {
    // fluid capabilities may swap the item for its container, which the old handler stored directly
    if (changingFluid) {
      return true;
    }
    // can only store items that are fluid handlers, though allow blacklist in case something is really broken
    // blacklist is mostly used for items that don't support incremental filling, as this block really isn't good at working with them
    // we check the container item so we don't have to put every bucket in the tag. Not bothering with complex container items; odds are item stack sensitive just returns the same item
    ItemStackTemplate craftRemainingItem = stack.getItem().getCraftingRemainder();
    return !stack.is(TinkerTags.Items.PROXY_TANK_BLACKLIST)
      && (craftRemainingItem == null || !craftRemainingItem.is(TinkerTags.Items.PROXY_TANK_BLACKLIST))
      && (!stack.isEmpty() && ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM) != null);
  }

  @Override
  public void setStack(ItemStack newStack) {
    // if swapping to an empty stack, switch to the empty stack instance
    // prevents accidently having a 0 stack size capability
    if (newStack.isEmpty()) {
      newStack = ItemStack.EMPTY;
    }
    // update stack
    ItemStack oldStack = getStack();
    super.setStack(newStack);

    // server side may need to sync
    Level world = parent.getLevel();
    boolean needsUpdate = world != null && !world.isClientSide();
    if (oldStack != newStack && needsUpdate) {
      // both stacks being empty means our stack shrunk by 1 and is being replaced with ItemStack.EMPTY
      // fluid changes always change the item components, so they sync here
      needsUpdate = (oldStack.isEmpty() && newStack.isEmpty()) || !ItemStack.isSameItemSameComponents(oldStack, newStack);
    } else {
      needsUpdate = false;
    }
    // sync changes
    if (needsUpdate) {
      parent.onTankContentsChanged();
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, 0, pos), world, pos);
    }
  }

  /**
   * Gets the stack in the given slot, for rendering.
   * @param slot  Slot index, only 0 is valid
   * @return  Stack in the slot, ignoring uncommitted transactions. Must not be modified
   */
  public ItemStack getStackInSlot(int slot) {
    return slot == 0 ? getStack() : ItemStack.EMPTY;
  }

  /** Gets the fluid capability of the current item, including uncommitted changes */
  private ResourceHandler<FluidResource> getItemTank() {
    ItemResource current = getResource(0);
    if (itemTank == null || !current.equals(cachedResource)) {
      cachedResource = current;
      ResourceHandler<FluidResource> handler = current.isEmpty() ? null : access.getCapability(Capabilities.Fluid.ITEM);
      itemTank = handler != null ? handler : EmptyFluidHandlerItem.INSTANCE;
    }
    return itemTank;
  }

  /**
   * Gets the fluid in the given tank of the item
   * @param tank  Tank index
   * @return  Copy of the fluid, or empty if the tank does not exist
   */
  public FluidStack getFluidInTank(int tank) {
    ResourceHandler<FluidResource> handler = getItemTank();
    if (tank < 0 || tank >= handler.size()) {
      return FluidStack.EMPTY;
    }
    return FluidUtil.getStack(handler, tank);
  }

  /**
   * Gets the capacity of the given tank of the item
   * @param tank  Tank index
   * @return  Capacity, or 0 if the tank does not exist
   */
  public int getTankCapacity(int tank) {
    ResourceHandler<FluidResource> handler = getItemTank();
    if (tank < 0 || tank >= handler.size()) {
      return 0;
    }
    return handler.getCapacityAsInt(tank, FluidResource.EMPTY);
  }

  /** Fluid handler delegating to the item fluid capability */
  private class ItemFluidHandler implements ResourceHandler<FluidResource> {
    @Override
    public int size() {
      return getItemTank().size();
    }

    @Override
    public FluidResource getResource(int index) {
      return getItemTank().getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
      return getItemTank().getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
      return getItemTank().getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
      return getItemTank().isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
      ResourceHandler<FluidResource> tank = getItemTank();
      boolean wasChanging = changingFluid;
      changingFluid = true;
      try {
        return tank.insert(index, resource, amount, transaction);
      } finally {
        changingFluid = wasChanging;
      }
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
      ResourceHandler<FluidResource> tank = getItemTank();
      boolean wasChanging = changingFluid;
      changingFluid = true;
      try {
        return tank.insert(resource, amount, transaction);
      } finally {
        changingFluid = wasChanging;
      }
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      ResourceHandler<FluidResource> tank = getItemTank();
      boolean wasChanging = changingFluid;
      changingFluid = true;
      try {
        return tank.extract(index, resource, amount, transaction);
      } finally {
        changingFluid = wasChanging;
      }
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
      ResourceHandler<FluidResource> tank = getItemTank();
      boolean wasChanging = changingFluid;
      changingFluid = true;
      try {
        return tank.extract(resource, amount, transaction);
      } finally {
        changingFluid = wasChanging;
      }
    }

    @Override
    public String toString() {
      return "ProxyItemTank.ItemFluidHandler[" + getStack() + "]";
    }
  }
}
