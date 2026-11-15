package slimeknights.tconstruct.tables.block.entity.inventory;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import javax.annotation.Nullable;

/** Base logic for scaling chest inventories */
public abstract class ScalingChestItemHandler extends ItemStacksResourceHandler implements IChestItemHandler {
  /** Default maximum size */
  protected static final int DEFAULT_MAX = 256;
  /** Current size for display in containers */
  @Getter
  private int visualSize = 1;
  /** TE owning this inventory */
  @Setter @Nullable
  private MantleBlockEntity parent;

  public ScalingChestItemHandler(int size) {
    super(size);
  }

  public ScalingChestItemHandler() {
    this(DEFAULT_MAX);
  }

  /** Gets the maximum number of items in each slot */
  public abstract int getSlotLimit(int slot);

  /** Checks if the given stack is valid in the slot. Note the slot contents may already contain the stack. */
  public abstract boolean isItemValid(int slot, ItemStack stack);

  @Override
  public ItemStack getStackInSlot(int slot) {
    return stacks.get(slot);
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return isItemValid(index, resource.toStack());
  }

  @Override
  protected int getCapacity(int index, ItemResource resource) {
    // matches the old item stack handler: slot limit, but never more than the item stacks to
    int limit = getSlotLimit(index);
    return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
  }

  /** Called after loading to update the visual size */
  protected void onLoad() {
    int newLimit = size();
    if (newLimit > 1 && this.getStackInSlot(newLimit - 1).isEmpty()) {
      while (newLimit > 1 && this.getStackInSlot(newLimit - 2).isEmpty()) {
        newLimit--;
      }
    }
    this.visualSize = newLimit;
  }

  @Override
  public void deserialize(ValueInput input) {
    super.deserialize(input);
    onLoad();
  }

  /** Updates the visual size of the inventory */
  private void updateVisualSize(int slotChanged, ItemStack stack) {
    // if the slot is too large, nothing to do
    int maxSlots = size();
    if (slotChanged >= maxSlots) {
      return;
    }
    // if the slot is past the current one, update to there
    if (stack.isEmpty()) {
      // if the current index was the last slot, decrease size
      if (slotChanged + 1 == visualSize || (slotChanged + 2 == visualSize && this.getStackInSlot(visualSize - 1).isEmpty())) {
        while (visualSize > 1 && this.getStackInSlot(visualSize - 2).isEmpty()) {
          visualSize--;
        }
      }
    } else {
      // if the current index is past the max, increase visual size to this plus 1
      if (visualSize < maxSlots && visualSize < slotChanged + 2) {
        visualSize =slotChanged + 2;
      }
    }
  }


  /* Hook in visual size update */

  /**
   * Called after a slot changes. For {@link #set(int, ItemResource, int)} this is called immediately, for insertion and extraction this is called
   * once the root transaction commits, so simulated transfers never update the visual size.
   */
  @Override
  protected void onContentsChanged(int slot, ItemStack previous) {
    updateVisualSize(slot, getStackInSlot(slot));
    if (parent != null) {
      parent.setChangedFast();
    }
  }
}
