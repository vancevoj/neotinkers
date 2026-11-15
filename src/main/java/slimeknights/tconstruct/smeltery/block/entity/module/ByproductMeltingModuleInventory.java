package slimeknights.tconstruct.smeltery.block.entity.module;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

public class ByproductMeltingModuleInventory extends MeltingModuleInventory {
  public ByproductMeltingModuleInventory(MantleBlockEntity parent, ResourceHandler<FluidResource> fluidHandler, IOreRate oreRate, int size) {
    super(parent, fluidHandler, oreRate, size);
  }

  public ByproductMeltingModuleInventory(MantleBlockEntity parent, ResourceHandler<FluidResource> fluidHandler, IOreRate oreRate) {
    super(parent, fluidHandler, oreRate);
  }

  @Override
  protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
    if (super.tryFillTank(index, recipe)) {
      // the main output transaction is closed at this point, byproducts are filled on their own
      recipe.handleByproducts(getModule(index), fluidHandler);
      return true;
    }
    return false;
  }
}
