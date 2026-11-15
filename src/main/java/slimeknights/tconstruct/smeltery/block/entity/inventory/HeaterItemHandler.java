package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.SingleItemHandler;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;

/**
 * Item handler holding the heater inventory
 */
public class HeaterItemHandler extends SingleItemHandler<MantleBlockEntity> {
  public HeaterItemHandler(MantleBlockEntity parent) {
    super(parent, 64);
  }

  @Override
  protected boolean isItemValid(ItemStack stack) {
    // fuel module divides by 4, so anything 3 or less is treated as 0
    // the burn time component can only be resolved on the server, the client just checks that it is a fuel
    if (parent.getLevel() instanceof ServerLevel level) {
      return FuelModule.getBurnTime(stack, level, parent) > 3;
    }
    return FuelModule.isSolidFuel(stack);
  }
}
