package slimeknights.tconstruct.library.fluid;

import net.minecraft.world.level.Level;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

/**
 * Fluid tank owned by a block entity, syncs its contents to the client on change.
 * Changes through the transfer API apply on commit of the root transaction, so {@link #onContentsChanged()} never runs for simulations.
 */
public class FluidTankBase<T extends MantleBlockEntity> extends FluidTank {

  protected T parent;

  public FluidTankBase(int capacity, T parent) {
    super(capacity);
    this.parent = parent;
  }

  // the old fill override fixing onContentsChanged during fill is no longer needed:
  // insert computes the filled amount locally and onContentsChanged only runs after the transaction commits

  @Override
  public void onContentsChanged() {
    if (parent instanceof IFluidTankUpdater) {
      ((IFluidTankUpdater) parent).onTankContentsChanged();
    }

    parent.setChanged();
    Level level = parent.getLevel();
    if(level != null && !level.isClientSide()) {
      TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(parent.getBlockPos(), this.getFluid()), level, parent.getBlockPos());
    }
  }
}
