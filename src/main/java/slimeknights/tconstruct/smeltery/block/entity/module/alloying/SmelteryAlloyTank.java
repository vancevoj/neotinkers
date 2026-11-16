package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

/**
 * Smeltery implementation of the alloy tank, basically just a wrapper around the smeltery tank
 */
@RequiredArgsConstructor
public class SmelteryAlloyTank implements IMutableAlloyTank {
  /**
   * Handler parent
   */
  private final SmelteryTank handler;
  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter @Setter
  private int temperature = 0;

  @Override
  public int getTanks() {
    return handler.size();
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    return handler.getFluidInTank(tank);
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    // the fluid fits if the net gain in fluid fits in the empty space
    return (fluid.getAmount() - removed) <= handler.getRemainingSpace();
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    try (Transaction transaction = Transaction.openRoot()) {
      FluidStack drained = handler.drain(fluidStack, transaction);
      transaction.commit();
      return drained;
    }
  }

  @Override
  public int fill(FluidStack fluidStack) {
    try (Transaction transaction = Transaction.openRoot()) {
      int filled = handler.fill(fluidStack, transaction);
      transaction.commit();
      return filled;
    }
  }
}
