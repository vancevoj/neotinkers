package slimeknights.tconstruct.library.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Simple single tank fluid handler, with more control over the fluid storage than {@link FluidTank}.
 * Implementations provide {@link #getFluid()}, {@link #setFluid(FluidStack)} and {@link #getCapacity()}.
 * <p>
 * Changes made through the transfer API are kept pending while the transaction is open and are applied with a single
 * {@link #updateFluid(FluidStack, int)} call when the root transaction commits, so overrides (for instance to sync the fluid) never run for simulations.
 */
public abstract class SimpleFluidTank extends SnapshotJournal<SimpleFluidTank.Pending> implements ResourceHandler<FluidResource> {
  /** Fluid set during the current transaction, null if unchanged. Never modified, only replaced */
  @Nullable
  private FluidStack pending = null;

  /** Gets the fluid in the tank, ignoring changes in transactions that are not yet committed */
  public abstract FluidStack getFluid();

  /** Called to set the fluid after it has changed */
  public abstract void setFluid(FluidStack fluid);

  /** Gets the capacity of the tank */
  public abstract int getCapacity();

  /**
   * Called when a root transaction commits a change to the fluid. Allows updating the fluid without needing to call {@link #getFluid()} again, in case it has a cost.
   * Only called if the fluid actually changed.
   * @param updated  New fluid stack
   * @param change   Net amount the fluid grew or shrunk by. May be 0 if the fluid was replaced by a different fluid of the same amount.
   */
  public void updateFluid(FluidStack updated, int change) {
    setFluid(updated);
  }

  /** Gets the amount of fluid in the tank */
  public int getFluidAmount() {
    return getFluid().getAmount();
  }

  /** Checks if the given fluid is valid for this tank */
  public boolean isFluidValid(FluidStack stack) {
    return true;
  }

  /** Gets the fluid including changes from the current transaction. Must not be modified */
  protected FluidStack getCurrentFluid() {
    return pending != null ? pending : getFluid();
  }


  /* Properties */

  @Override
  public int size() {
    return 1;
  }

  @Override
  public FluidResource getResource(int index) {
    Objects.checkIndex(index, size());
    return FluidResource.of(getCurrentFluid());
  }

  @Override
  public long getAmountAsLong(int index) {
    Objects.checkIndex(index, size());
    return getCurrentFluid().getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return resource.isEmpty() || isValid(index, resource) ? getCapacity() : 0;
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmpty(resource);
    return isFluidValid(resource.toStack(1));
  }


  /* Filling and draining */

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || !isFluidValid(resource.toStack(amount))) {
      return 0;
    }
    FluidStack fluid = getCurrentFluid();
    // if we have nothing, fill as much as possible
    if (fluid.isEmpty()) {
      int filled = Math.min(getCapacity(), amount);
      if (filled > 0) {
        updateSnapshots(transaction);
        pending = resource.toStack(filled);
      }
      return Math.max(filled, 0);
    }
    // if unable to fill, nothing more to do
    if (!resource.matches(fluid)) {
      return 0;
    }
    int filled = Math.min(getCapacity() - fluid.getAmount(), amount);
    if (filled > 0) {
      updateSnapshots(transaction);
      pending = fluid.copyWithAmount(fluid.getAmount() + filled);
      return filled;
    }
    return 0;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    FluidStack fluid = getCurrentFluid();
    if (amount == 0 || fluid.isEmpty() || !resource.matches(fluid)) {
      return 0;
    }
    // limit max drain to current fluid
    int drained = Math.min(amount, fluid.getAmount());
    updateSnapshots(transaction);
    pending = drained < fluid.getAmount() ? fluid.copyWithAmount(fluid.getAmount() - drained) : FluidStack.EMPTY;
    return drained;
  }


  /* Transaction helpers, replacements for the old fill and drain */

  /**
   * Fills the tank with the given fluid
   * @param resource     Fluid to fill
   * @param transaction  Transaction for the change, commit it to keep the change
   * @return  Amount filled
   */
  public int fill(FluidStack resource, TransactionContext transaction) {
    if (resource.isEmpty()) {
      return 0;
    }
    return insert(0, FluidResource.of(resource), resource.getAmount(), transaction);
  }

  /**
   * Drains the given fluid from the tank
   * @param resource     Fluid to drain
   * @param transaction  Transaction for the change, commit it to keep the change
   * @return  Fluid drained
   */
  public FluidStack drain(FluidStack resource, TransactionContext transaction) {
    if (resource.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource fluid = FluidResource.of(resource);
    int drained = extract(0, fluid, resource.getAmount(), transaction);
    return drained > 0 ? fluid.toStack(drained) : FluidStack.EMPTY;
  }

  /**
   * Drains fluid from the tank
   * @param maxDrain     Maximum amount to drain
   * @param transaction  Transaction for the change, commit it to keep the change
   * @return  Fluid drained
   */
  public FluidStack drain(int maxDrain, TransactionContext transaction) {
    FluidStack current = getCurrentFluid();
    if (maxDrain <= 0 || current.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource fluid = FluidResource.of(current);
    int drained = extract(0, fluid, maxDrain, transaction);
    return drained > 0 ? fluid.toStack(drained) : FluidStack.EMPTY;
  }


  /* Transactions */

  /** Snapshot of the pending fluid */
  protected record Pending(@Nullable FluidStack fluid) {}

  @Override
  protected Pending createSnapshot() {
    return new Pending(pending);
  }

  @Override
  protected void revertToSnapshot(Pending snapshot) {
    pending = snapshot.fluid();
  }

  @Override
  protected void onRootCommit(Pending originalState) {
    if (pending != null) {
      FluidStack updated = pending;
      pending = null;
      FluidStack original = getFluid();
      if (!FluidStack.matches(original, updated)) {
        updateFluid(updated, updated.getAmount() - original.getAmount());
      }
    }
  }
}
