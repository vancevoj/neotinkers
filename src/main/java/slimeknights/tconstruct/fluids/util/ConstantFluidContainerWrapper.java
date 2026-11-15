package slimeknights.tconstruct.fluids.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;

/** @deprecated use {@link slimeknights.mantle.item.ConstantFluidContainerWrapper} */
@Deprecated(forRemoval = true)
public class ConstantFluidContainerWrapper extends slimeknights.mantle.item.ConstantFluidContainerWrapper {
  public ConstantFluidContainerWrapper(FluidStack fluid, ItemAccess access) {
    super(fluid, access);
  }

  public ConstantFluidContainerWrapper(FluidStack fluid, ItemAccess access, ItemStack emptyStack) {
    super(fluid, access, emptyStack);
  }
}
