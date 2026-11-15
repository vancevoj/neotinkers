package slimeknights.tconstruct.gadgets.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.inventory.EmptyItemHandler;

import javax.annotation.Nullable;

/** @deprecated tinkers no longer does rails. If you need this reimplement it */
@Deprecated(forRemoval = true)
public class DropperRailBlock extends RailBlock {
  public DropperRailBlock(Properties properties) {
    super(properties);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    super.entityInside(state, world, pos, entity, effectApplier, isPrecise);
    if (!(entity instanceof AbstractMinecart cart) || !(cart instanceof Hopper)) {
      return;
    }
    // NeoForge 1.21: capabilities return null when absent rather than an empty optional
    if (Capabilities.Item.ENTITY.getCapability(cart, null) == null) {
      return;
    }
    BlockEntity tileEntity = world.getBlockEntity(pos.below());
    if (tileEntity == null || world.getCapability(Capabilities.Item.BLOCK, pos.below(), Direction.DOWN) == null) {
      return;
    }

    ResourceHandler<ItemResource> itemHandlerCart = orEmpty(Capabilities.Item.ENTITY.getCapability(cart, null));
    ResourceHandler<ItemResource> itemHandlerTE = orEmpty(world.getCapability(Capabilities.Item.BLOCK, pos.below(), Direction.UP));

    // move a single item from the cart into the block below
    for (int i = 0; i < itemHandlerCart.size(); i++) {
      ItemResource resource = itemHandlerCart.getResource(i);
      if (resource.isEmpty()) {
        continue;
      }
      try (Transaction transaction = Transaction.openRoot()) {
        if (itemHandlerCart.extract(i, resource, 1, transaction) == 1 && itemHandlerTE.insert(resource, 1, transaction) == 1) {
          transaction.commit();
          break;
        }
      }
    }
  }

  /** Returns the given handler, or the empty handler if null */
  private static ResourceHandler<ItemResource> orEmpty(@Nullable ResourceHandler<ItemResource> handler) {
    return handler == null ? EmptyItemHandler.INSTANCE : handler;
  }

}
