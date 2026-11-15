package slimeknights.tconstruct.smeltery.block.entity.tank;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;
import slimeknights.tconstruct.library.utils.WeakListenerList;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler.FluidChange;
import slimeknights.tconstruct.smeltery.network.SmelteryTankUpdatePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluid handler implementation for the smeltery.
 * <p>
 * Each index holds one fluid, with the bottom fluid at index 0. If the tank is not full, one extra empty index represents the free space.
 * Draining without a specific fluid (e.g. {@link net.neoforged.neoforge.transfer.ResourceHandlerUtil#extractFirst}) drains the bottom fluid.
 * Changes made through the transfer API are journaled, {@link ISmelteryTankHandler#notifyFluidsChanged(FluidChange, FluidStack)} runs when the root transaction commits.
 */
public class SmelteryTank<T extends MantleBlockEntity & ISmelteryTankHandler> extends SnapshotJournal<SmelteryTank.Snapshot> implements ResourceHandler<FluidResource>, IMultitankListChange {
  private final T parent;
  /** Fluids actually contained in the tank. Must not be modified outside the tank */
  @Getter
  private final List<FluidStack> fluids;
  /** Maximum capacity of the smeltery */
  private int capacity;
  /** Current amount of fluid in the tank */
  @Getter
  private int contained;
  /**
   * Listener for the tank list changing. Listeners may only clear caches, as they also run during transactions and when a transaction is reverted.
   */ // TODO: does this replace ISmelteryTankHandler?
  private final WeakListenerList tankListChange = new WeakListenerList();

  public SmelteryTank(T parent) {
    fluids = Lists.newArrayList();
    capacity = 0;
    contained = 0;
    this.parent = parent;
  }

  /**
   * Called when the fluids change to sync to client
   */
  public void syncFluids() {
    Level world = parent.getLevel();
    if (world != null && !world.isClientSide()) {
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new SmelteryTankUpdatePacket(pos, fluids), world, pos);
    }
  }


  /* Capacity and space */

  /**
   * Updates the maximum tank capacity
   * @param maxCapacity  New max capacity
   */
  public void setCapacity(int maxCapacity) {
    this.capacity = maxCapacity;
  }

  /**
   * Gets the maximum amount of space in the smeltery tank
   * @return  Tank capacity
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Gets the amount of empty space in the tank
   * @return  Remaining space in the tank
   */
  public int getRemainingSpace() {
    if (contained >= capacity) {
      return 0;
    }
    return capacity - contained;
  }


  /* Fluids */

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return true;
  }

  @Override
  public int size() {
    if (contained < capacity) {
      return fluids.size() + 1;
    }
    return fluids.size();
  }

  /** @deprecated use {@link #size()} */
  @Deprecated
  public int getTanks() {
    return size();
  }

  /**
   * Gets the fluid at the given index
   * @param tank  Index of the fluid
   * @return  Fluid in the tank, or empty if out of bounds. Must not be modified
   */
  public FluidStack getFluidInTank(int tank) {
    if (tank < 0 || tank >= fluids.size()) {
      return FluidStack.EMPTY;
    }
    return fluids.get(tank);
  }

  @Override
  public FluidResource getResource(int index) {
    return FluidResource.of(getFluidInTank(index));
  }

  @Override
  public long getAmountAsLong(int index) {
    return getFluidInTank(index).getAmount();
  }

  /**
   * Gets the capacity of the given index, being the fluid amount plus the remaining space
   * @param tank  Index
   * @return  Capacity
   */
  public int getTankCapacity(int tank) {
    if (tank < 0 || tank > fluids.size()) {
      return 0;
    }
    // index of the tank size means the "empty" segment
    int remaining = Math.max(0, capacity - contained);
    if (tank == fluids.size()) {
      return remaining;
    }
    // any valid index, return the amount contained and the extra space
    return fluids.get(tank).getAmount() + remaining;
  }

  @Override
  public long getCapacityAsLong(int index, FluidResource resource) {
    return getTankCapacity(index);
  }

  /**
   * Moves the fluid with the passed index to the beginning/bottom of the fluid tank stack.
   * Changes the tank directly, so must not be called during a transaction.
   * @param index  Index to move
   */
  public void moveFluidToBottom(int index) {
    if (index < fluids.size()) {
      FluidStack fluid = fluids.get(index);
      fluids.remove(index);
      fluids.add(0, fluid);
      parent.notifyFluidsChanged(FluidChange.CHANGED, FluidStack.EMPTY);
      tankListChange.run();
    }
  }


  /* Filling and draining */

  /** Finds the index of the given fluid, or -1 if missing */
  private int indexOf(FluidResource resource) {
    for (int i = 0; i < fluids.size(); i++) {
      if (resource.matches(fluids.get(i))) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    // if full or nothing being filled, do nothing
    if (contained >= capacity || amount == 0) {
      return 0;
    }

    // determine how much we can fill
    int usable = Math.min(capacity - contained, amount);
    // could be negative if the smeltery size changes then you try filling it
    if (usable <= 0) {
      return 0;
    }

    // snapshot before changing anything
    updateSnapshots(transaction);
    // add contained fluid amount
    contained += usable;

    // check if we already have the given liquid
    int index = indexOf(resource);
    if (index != -1) {
      // yup. add it
      FluidStack fluid = fluids.get(index);
      fluids.set(index, fluid.copyWithAmount(fluid.getAmount() + usable));
      // notify as we lost the "empty tank"
      if (contained >= capacity) {
        tankListChange.run();
      }
      return usable;
    }

    // not present yet, add it
    fluids.add(resource.toStack(usable));
    // notify as we added a new fluid
    tankListChange.run();
    return usable;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    // the smeltery decides where fluids go, so filling any index is the same as filling the tank
    return insert(resource, amount, transaction);
  }

  /** Drains the fluid at the given index, preconditions: valid index and amount is positive */
  private int drainIndex(int index, int maxDrain, TransactionContext transaction) {
    FluidStack fluid = fluids.get(index);
    int drainable = Math.min(maxDrain, fluid.getAmount());
    if (drainable <= 0) {
      return 0;
    }
    boolean wasFull = contained >= capacity;
    // snapshot before changing anything
    updateSnapshots(transaction);
    contained -= drainable;
    // if now empty, remove from the list
    if (drainable >= fluid.getAmount()) {
      fluids.remove(index);
      tankListChange.run();
    } else {
      fluids.set(index, fluid.copyWithAmount(fluid.getAmount() - drainable));
      // need to notify if we were full but are no longer as its adds an empty tank
      if (wasFull && contained < capacity) {
        tankListChange.run();
      }
    }
    return drainable;
  }

  @Override
  public int extract(FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0) {
      return 0;
    }
    // search for the resource
    int index = indexOf(resource);
    if (index == -1) {
      return 0;
    }
    return drainIndex(index, amount, transaction);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
    if (amount == 0 || index < 0 || index >= fluids.size() || !resource.matches(fluids.get(index))) {
      return 0;
    }
    return drainIndex(index, amount, transaction);
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
    return insert(FluidResource.of(resource), resource.getAmount(), transaction);
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
    int drained = extract(fluid, resource.getAmount(), transaction);
    return drained > 0 ? fluid.toStack(drained) : FluidStack.EMPTY;
  }

  /**
   * Drains the bottom fluid from the tank
   * @param maxDrain     Maximum amount to drain
   * @param transaction  Transaction for the change, commit it to keep the change
   * @return  Fluid drained
   */
  public FluidStack drain(int maxDrain, TransactionContext transaction) {
    if (fluids.isEmpty() || maxDrain <= 0) {
      return FluidStack.EMPTY;
    }
    // simply drain the first one
    FluidResource fluid = FluidResource.of(fluids.get(0));
    int drained = drainIndex(0, maxDrain, transaction);
    return drained > 0 ? fluid.toStack(drained) : FluidStack.EMPTY;
  }


  /* Transactions */

  /** Snapshot of the tank contents */
  protected record Snapshot(List<FluidStack> fluids, int contained) {}

  @Override
  protected Snapshot createSnapshot() {
    // the tank replaces stacks instead of modifying them, but copy anyways in case someone modified a stack from the list
    return new Snapshot(fluids.stream().map(FluidStack::copy).toList(), contained);
  }

  @Override
  protected void revertToSnapshot(Snapshot snapshot) {
    fluids.clear();
    for (FluidStack fluid : snapshot.fluids()) {
      fluids.add(fluid.copy());
    }
    contained = snapshot.contained();
    // list may have changed, clear caches
    tankListChange.run();
  }

  @Override
  protected void onRootCommit(Snapshot original) {
    // compare the original fluids to the new fluids to notify listeners of the changes
    List<FluidStack> remaining = new ArrayList<>(original.fluids());
    for (FluidStack fluid : fluids) {
      FluidStack old = null;
      for (int i = 0; i < remaining.size(); i++) {
        if (FluidStack.isSameFluidSameComponents(remaining.get(i), fluid)) {
          old = remaining.remove(i);
          break;
        }
      }
      if (old == null) {
        parent.notifyFluidsChanged(FluidChange.ADDED, fluid);
      } else if (old.getAmount() != fluid.getAmount()) {
        parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
      }
    }
    for (FluidStack removed : remaining) {
      parent.notifyFluidsChanged(FluidChange.REMOVED, removed);
    }
  }


  /* Saving and loading */

  private static final String TAG_FLUIDS = "fluids";
  private static final String TAG_CAPACITY = "capacity";

  /**
   * Updates fluids in the tank, typically from a packet. Changes the tank directly, so must not be called during a transaction.
   * @param fluids  List of fluids
   */
  public void setFluids(List<FluidStack> fluids) {
    FluidStack oldFirst = getFluidInTank(0);
    this.fluids.clear();
    this.fluids.addAll(fluids);
    contained = fluids.stream().mapToInt(FluidStack::getAmount).reduce(0, Integer::sum);
    FluidStack newFirst = getFluidInTank(0);
    if (!FluidStack.isSameFluidSameComponents(oldFirst, newFirst)) {
      parent.notifyFluidsChanged(FluidChange.ORDER_CHANGED, newFirst);
      tankListChange.run();
    }
  }

  /** Writes the tank to the given output, typically a child output */
  public void write(ValueOutput output) {
    ValueOutput.TypedOutputList<FluidStack> list = output.list(TAG_FLUIDS, FluidStack.CODEC);
    for (FluidStack liquid : fluids) {
      if (!liquid.isEmpty()) {
        list.add(liquid);
      }
    }
    output.putInt(TAG_CAPACITY, capacity);
  }

  /** Reads the tank from the given input, typically a child input */
  public void read(ValueInput input) {
    fluids.clear();
    contained = 0;
    for (FluidStack fluid : input.listOrEmpty(TAG_FLUIDS, FluidStack.OPTIONAL_CODEC)) {
      if (!fluid.isEmpty()) {
        fluids.add(fluid);
        contained += fluid.getAmount();
      }
    }
    capacity = input.getIntOr(TAG_CAPACITY, 0);
    // list changed, clear caches
    tankListChange.run();
  }


  /* Listeners */

  @Override
  public <TE> void addTankListListener(TE parent, Consumer<TE> listener) {
    tankListChange.addListener(parent, listener);
  }

  @Override
  public void removeTankListListeners(Object parent) {
    tankListChange.removeListeners(parent);
  }

  @Override
  public String toString() {
    return "SmelteryTank[" + fluids + ", " + contained + "/" + capacity + "]";
  }
}
