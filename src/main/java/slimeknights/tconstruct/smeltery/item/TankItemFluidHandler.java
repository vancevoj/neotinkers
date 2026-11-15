package slimeknights.tconstruct.smeltery.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import java.util.Objects;

/**
 * Handler that works with a tank item to adjust its tank in NBT.
 * Amounts scale with the number of items in the access, every tank in the stack is filled or drained by the same amount (same as the old scaled tank).
 * The updated item is written through the {@link ItemAccess} in the transaction.
 */
public class TankItemFluidHandler extends ItemAccessResourceHandler<FluidResource> {
  private final TankItem tankItem;

  public TankItemFluidHandler(TankItem tankItem, ItemAccess access) {
    super(access, 1);
    this.tankItem = tankItem;
  }

  @Override
  protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
    Objects.checkIndex(index, size());
    return FluidResource.of(TankItem.getFluid(accessResource.toStack()));
  }

  @Override
  protected int getAmountFrom(ItemResource accessResource, int index) {
    Objects.checkIndex(index, size());
    return TankItem.getFluid(accessResource.toStack()).getAmount();
  }

  @Override
  protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
    Objects.checkIndex(index, size());
    ItemStack stack = accessResource.toStack();
    TankItem.setTank(stack, newAmount <= 0 || newResource.isEmpty() ? FluidStack.EMPTY : newResource.toStack(newAmount));
    return ItemResource.of(stack);
  }

  @Override
  public boolean isValid(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    // disallow filling if the current size is larger than 16
    return tankItem.canFill(itemAccess.getAmount());
  }

  @Override
  protected int getCapacity(int index, FluidResource resource) {
    Objects.checkIndex(index, size());
    return TankBlockEntity.getCapacity(itemAccess.getResource().getItem());
  }
}
