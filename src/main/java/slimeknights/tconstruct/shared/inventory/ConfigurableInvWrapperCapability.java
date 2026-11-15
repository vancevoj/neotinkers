package slimeknights.tconstruct.shared.inventory;

import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.mantle.inventory.ContainerItemHandler;

/**
 * Item handler over a container that can optionally block insertion or extraction through the handler.
 * Setting slots directly (such as through menu slots) is always allowed.
 */
public class ConfigurableInvWrapperCapability extends ContainerItemHandler {

  private final boolean canInsert;
  private final boolean canExtract;

  public ConfigurableInvWrapperCapability(Container inv, boolean canInsert, boolean canExtract) {
    super(inv);
    this.canInsert = canInsert;
    this.canExtract = canExtract;
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    if (!this.canInsert) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return 0;
    }
    return super.insert(index, resource, amount, transaction);
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    if (!this.canExtract) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return 0;
    }
    return super.extract(index, resource, amount, transaction);
  }
}
