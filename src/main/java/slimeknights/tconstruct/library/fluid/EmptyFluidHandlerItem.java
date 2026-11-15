package slimeknights.tconstruct.library.fluid;

import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Empty item fluid handler instance, used as a fallback when an item has no fluid capability.
 * Item fluid handlers no longer hold their container, they update the item through an {@link net.neoforged.neoforge.transfer.access.ItemAccess},
 * so a single shared empty instance replaces the old per container instances.
 */
public final class EmptyFluidHandlerItem {
  /** Empty fluid handler, has no tanks */
  public static final ResourceHandler<FluidResource> INSTANCE = EmptyResourceHandler.instance();

  private EmptyFluidHandlerItem() {}
}
