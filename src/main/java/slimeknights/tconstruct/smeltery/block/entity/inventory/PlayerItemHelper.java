package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Helpers for giving items to players from block entities */
public final class PlayerItemHelper {
  private PlayerItemHelper() {}

  /**
   * Gives an item to the player, preferring the given slot, dropping anything that does not fit. Replacement for the removed NeoForge helper.
   * @param player         Player receiving the item
   * @param stack          Stack to give
   * @param preferredSlot  Slot in the main inventory to try first
   */
  public static void giveItemToPlayer(Player player, ItemStack stack, int preferredSlot) {
    if (stack.isEmpty()) {
      return;
    }
    Level level = player.level();
    ItemResource resource = ItemResource.of(stack);
    int count = stack.getCount();
    int inserted = 0;
    ResourceHandler<ItemResource> inventory = PlayerInventoryWrapper.of(player).getMainSlots();
    try (Transaction transaction = Transaction.openRoot()) {
      // insert into preferred slot first
      if (preferredSlot >= 0 && preferredSlot < inventory.size()) {
        inserted = inventory.insert(preferredSlot, resource, count, transaction);
      }
      // then into the inventory in general
      if (inserted < count) {
        inserted += ResourceHandlerUtil.insertStacking(inventory, resource, count - inserted, transaction);
      }
      transaction.commit();
    }
    // play sound if something got picked up
    if (inserted > 0) {
      level.playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
    }
    // drop remaining itemstack into the level
    if (inserted < count && !level.isClientSide()) {
      ItemEntity entity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copyWithCount(count - inserted));
      entity.setPickUpDelay(40);
      entity.setDeltaMovement(entity.getDeltaMovement().multiply(0, 1, 0));
      level.addFreshEntity(entity);
    }
  }
}
