package slimeknights.tconstruct.smeltery.block.entity.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;

import java.util.function.Consumer;

/**
 * Fluid handler wrapping the smeltery tank, exposing only the fluid matching the duct filter.
 * Filling and draining is left to the parent, which decides where the fluid goes.
 */
public class DuctTankWrapper implements ResourceHandler<FluidResource> {
  private final ResourceHandler<FluidResource> parent;
  private final DuctItemHandler itemHandler;
  private int[] tankMapping;

  public DuctTankWrapper(ResourceHandler<FluidResource> parent, DuctItemHandler itemHandler) {
    this.parent = parent;
    this.itemHandler = itemHandler;
    // clear cache when the fluid changes or the smeltery list changes
    Consumer<DuctTankWrapper> consumer = self -> self.tankMapping = null;
    itemHandler.addListener(this, consumer);
    if (parent instanceof IMultitankListChange notifier) {
      notifier.addTankListListener(this, consumer);
    }
  }

  /** Gets the mapping from index to matching tank */
  private int[] getTankMapping() {
    if (tankMapping == null) {
      FluidStack filter = itemHandler.getFluid();
      int count = parent.size();
      if (filter.isEmpty()) {
        // no filter means only the empty space is exposed
        if (count > 0 && parent.getResource(count - 1).isEmpty()) {
          tankMapping = new int[] { count - 1 };
        } else {
          tankMapping = new int[0];
        }
      } else {
        IntList list = new IntArrayList(count);
        for (int i = 0; i < count; i++) {
          FluidResource contained = parent.getResource(i);
          if (contained.isEmpty() || contained.matches(filter)) {
            list.add(i);
          }
        }
        tankMapping = list.toIntArray();
      }
    }
    return tankMapping;
  }

  /** Checks if the given resource matches the duct filter */
  private boolean matchesFilter(FluidResource resource) {
    return resource.matches(itemHandler.getFluid());
  }


  /* Properties */

  @Override
  public int size() {
    return getTankMapping().length;
  }

  @Override
  public FluidResource getResource(int index) {
    int[] mapping = getTankMapping();
    if (index < 0 || index >= mapping.length) {
      return FluidResource.EMPTY;
    }
    return parent.getResource(mapping[index]);
  }

  @Override
  public long getAmountAsLong(int index) {
    int[] mapping = getTankMapping();
    if (index < 0 || index >= mapping.length) {
      return 0;
    }
    return parent.getAmountAsLong(mapping[index]);
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    int[] mapping = getTankMapping();
    if (index < 0 || index >= mapping.length) {
      return 0;
    }
    return parent.getCapacityAsLong(mapping[index], resource);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return matchesFilter(resource);
  }


  /* Interactions */

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (!matchesFilter(resource)) {
      return 0;
    }
    return parent.insert(resource, amount, transaction);
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    // the parent decides where the fluid goes
    return insert(resource, amount, transaction);
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (!matchesFilter(resource)) {
      return 0;
    }
    return parent.extract(resource, amount, transaction);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    // filter only matches one fluid, which the parent holds in at most one place
    return extract(resource, amount, transaction);
  }
}
