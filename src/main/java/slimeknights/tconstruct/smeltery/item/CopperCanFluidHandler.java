package slimeknights.tconstruct.smeltery.item;

import lombok.AllArgsConstructor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.recipe.FluidValues;

import java.util.Objects;

/**
 * Capability handler instance for the copper can item. Fills and drains exactly one ingot per can, all cans in the access at once.
 * The filled or emptied can is exchanged through the {@link ItemAccess}, so it requires an access that can modify the item.
 */
@AllArgsConstructor
public class CopperCanFluidHandler implements ResourceHandler<FluidResource> {
  private final ItemAccess access;


  /* Tank properties */

  @Override
  public int size() {
    return 1;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return true;
  }

  /** Gets the stack size sensitive capacity of the container */
  private int getCapacity() {
    // scale up by the stack size to prevent dupes with people trying to fill a stack of containers
    return FluidValues.INGOT * access.getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return getCapacity();
  }

  /** Gets the contained fluid with the given amount */
  private FluidStack getFluid(int amount) {
    ItemResource resource = access.getResource();
    if (resource.isEmpty()) {
      return FluidStack.EMPTY;
    }
    return CopperCanItem.getFluidStack(resource.toStack(), amount);
  }

  @Override
  public FluidResource getResource(int index) {
    Objects.checkIndex(index, size());
    return FluidResource.of(getFluid(FluidValues.INGOT));
  }

  @Override
  public long getAmountAsLong(int index) {
    Objects.checkIndex(index, size());
    return getFluid(FluidValues.INGOT).isEmpty() ? 0 : getCapacity();
  }


  /* Interaction */

  /** Exchanges every can in the access for the given can, all or nothing */
  private boolean exchange(ItemStack newCan, TransactionContext transaction) {
    int count = access.getAmount();
    try (Transaction exchange = Transaction.open(transaction)) {
      if (access.exchange(ItemResource.of(newCan), count, exchange) == count) {
        exchange.commit();
        return true;
      }
    }
    return false;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // must not be filled, must have enough
    int capacity = getCapacity();
    if (capacity <= 0 || !getFluid(FluidValues.INGOT).isEmpty() || amount < capacity) {
      return 0;
    }
    // update fluid and return
    // this is not size sensitive so no need to shrink resource for stack size
    ItemStack filled = access.getResource().toStack();
    CopperCanItem.setFluid(filled, resource.toStack(FluidValues.INGOT));
    return exchange(filled, transaction) ? capacity : 0;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // must be draining at least an ingot
    int capacity = getCapacity();
    if (capacity <= 0 || amount < capacity) {
      return 0;
    }
    // must have a fluid, must match what they are draining including components
    FluidStack output = getFluid(capacity);
    if (output.isEmpty() || !resource.matches(output)) {
      return 0;
    }
    // output 1 ingot times stack size
    ItemStack emptied = access.getResource().toStack();
    CopperCanItem.setFluid(emptied, FluidStack.EMPTY);
    return exchange(emptied, transaction) ? capacity : 0;
  }
}
