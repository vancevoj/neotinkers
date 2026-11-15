package slimeknights.tconstruct.tables.block.entity.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/** Interface for tinker chest TEs */
public interface IChestItemHandler extends ResourceHandler<ItemResource>, IndexModifier<ItemResource>, ValueIOSerializable, IScalingContainer {
  /** Sets the parent of this block */
  void setParent(MantleBlockEntity parent);

  /**
   * Gets the stack in the given slot
   * @param slot  Slot index
   * @return  Stack in the slot, must not be modified
   */
  ItemStack getStackInSlot(int slot);

  /**
   * Sets the stack in the given slot directly. Should not be called during a transaction.
   * @param slot   Slot index
   * @param stack  New stack
   */
  default void setStackInSlot(int slot, ItemStack stack) {
    set(slot, stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack), stack.getCount());
  }
}
