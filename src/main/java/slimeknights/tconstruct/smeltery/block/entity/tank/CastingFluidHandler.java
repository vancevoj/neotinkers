package slimeknights.tconstruct.smeltery.block.entity.tank;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity.NewCasting;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Fluid handler for casting tables and basins. Filling an empty table looks up a casting recipe to determine the capacity.
 * <p>
 * Changes through the transfer API are journaled: starting the casting recipe, {@link CastingBlockEntity#onContentsChanged()} and {@link CastingBlockEntity#reset()}
 * all run once the root transaction commits, so simulated fills never change the block entity.
 */
@RequiredArgsConstructor
public class CastingFluidHandler extends SnapshotJournal<CastingFluidHandler.Snapshot> implements ResourceHandler<FluidResource> {
  private final CastingBlockEntity tile;
  /** Fluid in the table, must not be modified, use {@link #setFluid(FluidStack)} */
  @Getter @Setter
  private FluidStack fluid = FluidStack.EMPTY;
  @Setter
  private int capacity = 0;
  private Fluid filter = Fluids.EMPTY;
  /** Casting recipe to start once the transaction commits, null if none */
  @Nullable
  private NewCasting pendingCasting = null;
  /** If true, the tank was emptied during the transaction so the recipe must reset once the transaction commits */
  private boolean pendingReset = false;

  /** Checks if the given fluid is valid */
  public boolean isFluidValid(FluidStack stack) {
    return !stack.isEmpty() && (filter == Fluids.EMPTY || stack.getFluid() == filter);
  }

  /** Checks if the fluid is empty */
  public boolean isEmpty() {
    return fluid.isEmpty();
  }

  /** Gets the current capacity of this fluid handler */
  public int getCapacity() {
    if (capacity == 0) {
      return fluid.getAmount();
    }
    return capacity;
  }

  /** Resets the tanks filter. Changes the tank directly, so must not be called during a transaction */
  public void reset() {
    capacity = 0;
    fluid = FluidStack.EMPTY;
    filter = Fluids.EMPTY;
  }


  /* Properties */

  @Override
  public int size() {
    return 1;
  }

  @Override
  public FluidResource getResource(int index) {
    Objects.checkIndex(index, size());
    return FluidResource.of(fluid);
  }

  @Override
  public long getAmountAsLong(int index) {
    Objects.checkIndex(index, size());
    return fluid.getAmount();
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return getCapacity();
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return isFluidValid(resource.toStack(1));
  }


  /* Filling and draining */

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    FluidStack stack = resource.toStack(amount);
    if (amount == 0 || !isFluidValid(stack)) {
      return 0;
    }

    // update filter and capacity
    int capacity = this.capacity;
    NewCasting casting = null;
    if (filter == null || this.capacity == 0) {
      casting = tile.findNewCasting(stack);
      if (casting == null || casting.fluidAmount() <= 0) {
        return 0;
      }
      capacity = casting.fluidAmount();
      // the old handler started the casting as soon as it found the recipe, before checking the fluid
      updateSnapshots(transaction);
      this.pendingCasting = casting;
      this.capacity = capacity;
      this.filter = resource.getFluid();
    }

    // if no fluid yet, copy it in
    if (fluid.isEmpty()) {
      int filled = Math.min(capacity, amount);
      updateSnapshots(transaction);
      fluid = resource.toStack(filled);
      return filled;
    }

    // safety: should never be false, but good to check
    if (!resource.matches(fluid)) {
      return 0;
    }

    // if full, nothing to do
    int space = capacity - fluid.getAmount();
    if (space <= 0) {
      return 0;
    }
    // if it fits, it grows, too much sets it to the max
    int filled = Math.min(amount, space);
    updateSnapshots(transaction);
    fluid = fluid.copyWithAmount(fluid.getAmount() + filled);
    return filled;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    return insert(resource, amount, transaction);
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (!resource.matches(fluid)) {
      return 0;
    }
    int drained = Math.min(fluid.getAmount(), amount);
    if (drained <= 0) {
      return 0;
    }

    updateSnapshots(transaction);
    if (drained >= fluid.getAmount()) {
      // since empty, assume the current recipe is invalid now
      // fixes some odd behavior with capacity and recipes going out of sync
      // the tile is reset on commit, reset our own state now so further changes in this transaction see an empty table
      reset();
      pendingCasting = null;
      pendingReset = true;
    } else {
      fluid = fluid.copyWithAmount(fluid.getAmount() - drained);
    }
    return drained;
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    Objects.checkIndex(index, size());
    return extract(resource, amount, transaction);
  }


  /* Transactions */

  /** Snapshot of the tank state */
  protected record Snapshot(FluidStack fluid, int capacity, Fluid filter, @Nullable NewCasting pendingCasting, boolean pendingReset) {}

  @Override
  protected Snapshot createSnapshot() {
    return new Snapshot(fluid.copy(), capacity, filter, pendingCasting, pendingReset);
  }

  @Override
  protected void revertToSnapshot(Snapshot snapshot) {
    fluid = snapshot.fluid();
    capacity = snapshot.capacity();
    filter = snapshot.filter();
    pendingCasting = snapshot.pendingCasting();
    pendingReset = snapshot.pendingReset();
  }

  @Override
  protected void onRootCommit(Snapshot original) {
    NewCasting casting = pendingCasting;
    boolean reset = pendingReset;
    pendingCasting = null;
    pendingReset = false;

    // emptying the table resets the recipe, which also clears this tank, so restore anything filled after emptying
    if (reset) {
      FluidStack newFluid = fluid;
      int newCapacity = capacity;
      Fluid newFilter = filter;
      tile.reset();
      if (!newFluid.isEmpty()) {
        fluid = newFluid;
        capacity = newCapacity;
        filter = newFilter;
      }
    }
    // start the casting found during the transaction
    if (casting != null) {
      tile.startNewCasting(casting);
    }
    // notify the table of the change, reset already notified if nothing was added after
    if (!reset || !fluid.isEmpty()) {
      if (casting != null || !FluidStack.matches(original.fluid(), fluid)) {
        tile.onContentsChanged();
      }
    }
  }


  /* Tag */
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FILTER = "filter";
  private static final String TAG_CAPACITY = "capacity";

  /** Reads the tank from the given input, typically a child input */
  public void readFromTag(ValueInput input) {
    capacity = input.getIntOr(TAG_CAPACITY, 0);
    input.read(TAG_FLUID, FluidStack.OPTIONAL_CODEC).ifPresent(this::setFluid);
    input.getString(TAG_FILTER).ifPresent(name -> {
      Identifier id = Identifier.tryParse(name);
      if (id != null) {
        filter = BuiltInRegistries.FLUID.getValue(id);
      }
    });
  }

  /** Write the tank to the given output, typically a child output */
  public void writeToTag(ValueOutput output) {
    output.putInt(TAG_CAPACITY, capacity);
    if (!fluid.isEmpty()) {
      output.store(TAG_FLUID, FluidStack.CODEC, fluid);
    }
    if (filter != Fluids.EMPTY) {
      output.putString(TAG_FILTER, BuiltInRegistries.FLUID.getKey(filter).toString());
    }
  }
}
