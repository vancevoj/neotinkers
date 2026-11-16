package slimeknights.tconstruct.library.fluid;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;

import java.util.function.Predicate;

/**
 * Single fluid tank, replacement for the NeoForge {@code templates.FluidTank} removed in 26.x.
 * <p>
 * Changes made through the transfer API ({@link #insert(int, FluidResource, int, TransactionContext)} and friends) are applied in the transaction,
 * and {@link #onContentsChanged()} runs once the root transaction commits, so simulations never trigger side effects.
 * <p>
 * Saves using the same NBT layout as the old NeoForge tank (fluid stored under {@value #TAG_FLUID}), so old worlds and items keep their fluid.
 */
public class FluidTank extends FluidStacksResourceHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  /** Key used by the old NeoForge tank to store the fluid, kept for save compatibility */
  public static final String TAG_FLUID = "Fluid";

  /** Validator for fluids filled into this tank */
  protected Predicate<FluidStack> validator;

  public FluidTank(int capacity) {
    this(capacity, fluid -> true);
  }

  public FluidTank(int capacity, Predicate<FluidStack> validator) {
    super(1, capacity);
    this.validator = validator;
  }


  /* Properties */

  /** Sets the capacity of this tank */
  public FluidTank setCapacity(int capacity) {
    this.capacity = capacity;
    return this;
  }

  /** Sets the validator for this tank */
  public FluidTank setValidator(Predicate<FluidStack> validator) {
    if (validator != null) {
      this.validator = validator;
    }
    return this;
  }

  /** Checks if the given fluid may be filled into this tank */
  public boolean isFluidValid(FluidStack stack) {
    return validator.test(stack);
  }

  /** Gets the capacity of this tank */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Gets the fluid in this tank, including uncommitted changes from the current transaction.
   * The returned stack must not be modified, use {@link #setFluid(FluidStack)} or the transfer methods to change it.
   */
  public FluidStack getFluid() {
    return stacks.get(0);
  }

  /** Gets the amount of fluid in this tank */
  public int getFluidAmount() {
    return getFluid().getAmount();
  }

  /** Checks if this tank is empty */
  public boolean isEmpty() {
    return getFluid().isEmpty();
  }

  /** Gets the empty space in this tank */
  public int getSpace() {
    return Math.max(0, capacity - getFluidAmount());
  }

  /**
   * Sets the fluid in the tank directly, without calling {@link #onContentsChanged()} (same as the old NeoForge tank).
   * Should not be called during a transaction, as the change would be lost if the transaction is aborted.
   * @param stack  New fluid, stored directly
   */
  public void setFluid(FluidStack stack) {
    stacks.set(0, stack);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    return isFluidValid(resource.toStack(1));
  }


  /* Change listener */

  @Override
  protected void onContentsChanged(int index, FluidStack previousContents) {
    onContentsChanged();
  }

  /** Called when the contents of the tank change through the transfer API, after the root transaction commits */
  protected void onContentsChanged() {}


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
    FluidStack current = getFluid();
    if (maxDrain <= 0 || current.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource fluid = FluidResource.of(current);
    int drained = extract(0, fluid, maxDrain, transaction);
    return drained > 0 ? fluid.toStack(drained) : FluidStack.EMPTY;
  }


  /* NBT */

  /**
   * Reads the tank from NBT, same format as the old NeoForge tank
   * @param registries  Registry access
   * @param nbt         Tank NBT
   * @return  This tank
   */
  public FluidTank readFromNBT(HolderLookup.Provider registries, CompoundTag nbt) {
    setFluid(nbt.read(TAG_FLUID, FluidStack.OPTIONAL_CODEC, registries.createSerializationContext(NbtOps.INSTANCE)).orElse(FluidStack.EMPTY));
    return this;
  }

  /**
   * Writes the tank to NBT, same format as the old NeoForge tank
   * @param registries  Registry access
   * @param nbt         Tag to fill
   * @return  The passed tag
   */
  public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag nbt) {
    FluidStack fluid = getFluid();
    if (!fluid.isEmpty()) {
      FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), fluid)
                      .resultOrPartial(error -> LOGGER.error("Failed to save fluid {}: {}", fluid, error))
                      .ifPresent(tag -> nbt.put(TAG_FLUID, tag));
    }
    return nbt;
  }

  /**
   * Reads the tank from the given input, same layout as {@link #readFromNBT(HolderLookup.Provider, CompoundTag)}. Typically called with a child input.
   * @param input  Input
   * @return  This tank
   */
  public FluidTank readFromNBT(ValueInput input) {
    setFluid(input.read(TAG_FLUID, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
    return this;
  }

  /**
   * Writes the tank to the given output, same layout as {@link #writeToNBT(HolderLookup.Provider, CompoundTag)}. Typically called with a child output.
   * @param output  Output
   */
  public void writeToNBT(ValueOutput output) {
    FluidStack fluid = getFluid();
    if (!fluid.isEmpty()) {
      output.store(TAG_FLUID, FluidStack.CODEC, fluid);
    }
  }

  /* Use the old layout instead of the generic stacks list so saves stay compatible */

  @Override
  public void serialize(ValueOutput output) {
    writeToNBT(output);
  }

  @Override
  public void deserialize(ValueInput input) {
    readFromNBT(input);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getFluid() + "/" + capacity + "]";
  }
}
