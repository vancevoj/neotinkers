package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.inventory.ContainerItemHandler;

import javax.annotation.Nullable;

/**
 * Item handler exposing one face of a {@link WorldlyContainer}, replacement for the removed {@code SidedInvWrapper}.
 * Unlike NeoForge's {@link net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper}, changes go through a {@link ContainerItemHandler},
 * so {@link WorldlyContainer#setItem(int, net.minecraft.world.item.ItemStack)} and its side effects only run once the root transaction commits.
 */
public class SidedContainerItemHandler implements ResourceHandler<ItemResource> {
  private final WorldlyContainer container;
  private final ContainerItemHandler handler;
  @Nullable
  private final Direction side;

  public SidedContainerItemHandler(WorldlyContainer container, @Nullable Direction side) {
    this.container = container;
    this.handler = new ContainerItemHandler(container);
    this.side = side;
  }

  /** Converts an index on this face to a container slot */
  private int convertSlot(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Cannot access container with negative slot index: " + index);
    }
    if (side == null) {
      return index;
    }
    int[] slots = container.getSlotsForFace(side);
    if (index >= slots.length) {
      throw new IndexOutOfBoundsException("Cannot access worldly container on side " + side + ": out of bounds slot index " + index + " with size " + slots.length);
    }
    return slots[index];
  }

  @Override
  public int size() {
    return side == null ? container.getContainerSize() : container.getSlotsForFace(side).length;
  }

  @Override
  public ItemResource getResource(int index) {
    return handler.getResource(convertSlot(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return handler.getAmountAsLong(convertSlot(index));
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    return handler.getCapacityAsLong(convertSlot(index), resource);
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return handler.isValid(convertSlot(index), resource);
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int slot = convertSlot(index);
    if (!container.canPlaceItemThroughFace(slot, resource.toStack(), side)) {
      return 0;
    }
    return handler.insert(slot, resource, amount, transaction);
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    int slot = convertSlot(index);
    if (side != null && !container.canTakeItemThroughFace(slot, resource.toStack(), side)) {
      return 0;
    }
    return handler.extract(slot, resource, amount, transaction);
  }

  @Override
  public String toString() {
    return "SidedContainerItemHandler[" + container + ", " + side + "]";
  }
}
