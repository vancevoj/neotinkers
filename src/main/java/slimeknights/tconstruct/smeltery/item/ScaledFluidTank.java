package slimeknights.tconstruct.smeltery.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.FluidTank;

/**
 * Fluid tank representing a stack of multiple fluid tanks. All operations must affect every tack in the stack at the same time, so must in increments of the scale.
 * Internally works the same as a fluid tank with {@code capacity * scale}, except operations are truncated to the nearest scale (e.g. if scale is 4, we must fill in 4mb increments).
 */
public class ScaledFluidTank extends FluidTank {
  private final int scale;
  private ScaledFluidTank(int capacity, int scale) {
    super(capacity * scale);
    this.scale = scale;
  }

  /** Creates a new instance */
  public static FluidTank create(int capacity, int scale) {
    if (scale == 1) {
      return new FluidTank(capacity);
    }
    return new ScaledFluidTank(capacity, scale);
  }

  /* Helpers */

  /** enforces the amount matches the scale */
  private int enforceScale(int amount) {
    // no working with fluids of partial amounts
    int remainder = amount % scale;
    if (remainder != 0) {
      amount -= remainder;
    }
    return amount;
  }

  /** enforces the fluid matches the scale */
  private FluidStack enforceScale(FluidStack stack, boolean copy) {
    // no working with fluids of partial amounts
    int remainder = stack.getAmount() % scale;
    if (remainder != 0) {
      if (copy) {
        stack = stack.copy();
      }
      stack.shrink(remainder);
    }
    return stack;
  }


  /* Fluid tank methods */

  @Override
  public FluidTank setCapacity(int capacity) {
    return super.setCapacity(enforceScale(capacity));
  }

  @Override
  public void setFluid(FluidStack stack) {
    super.setFluid(enforceScale(stack, false));
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return super.insert(index, resource, enforceScale(amount), transaction);
  }

  @Override
  public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
    return super.extract(index, resource, enforceScale(amount), transaction);
  }


  /* NBT */

  /** Scales the fluid on reading from NBT, as each instance should store the fluid relative to stack size 1 */
  private void setScaledFluid(FluidStack fluid) {
    if (!fluid.isEmpty()) {
      fluid.setAmount(fluid.getAmount() * scale);
    }
    setFluid(fluid);
  }

  /** Scales the fluid on writing to NBT, as each instance should store the fluid relative to stack size 1 */
  private FluidStack getScaledFluid() {
    FluidStack fluid = getFluid();
    return fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(fluid.getAmount() / scale);
  }

  @Override
  public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
    setScaledFluid(nbt.read(TAG_FLUID, FluidStack.OPTIONAL_CODEC, lookupProvider.createSerializationContext(NbtOps.INSTANCE)).orElse(FluidStack.EMPTY));
    return this;
  }

  @Override
  public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
    FluidStack fluid = getScaledFluid();
    if (!fluid.isEmpty()) {
      nbt.store(TAG_FLUID, FluidStack.CODEC, lookupProvider.createSerializationContext(NbtOps.INSTANCE), fluid);
    }
    return nbt;
  }

  @Override
  public FluidTank readFromNBT(ValueInput input) {
    setScaledFluid(input.read(TAG_FLUID, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY));
    return this;
  }

  @Override
  public void writeToNBT(ValueOutput output) {
    FluidStack fluid = getScaledFluid();
    if (!fluid.isEmpty()) {
      output.store(TAG_FLUID, FluidStack.CODEC, fluid);
    }
  }
}
