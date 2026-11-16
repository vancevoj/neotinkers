package slimeknights.tconstruct.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

/**
 * Head item for enderman head, which counts as a pumpkin on the head.
 * The NeoForge ender mask item hook is gone, so the mask is applied by {@code WorldEvents} through the enderman anger event.
 */
public class EndermanHeadItem extends StandingAndWallBlockItem {
  public EndermanHeadItem(Block pBlock, Block pWallBlock, Properties pProperties, Direction pAttachmentDirection) {
    super(pBlock, pWallBlock, pAttachmentDirection, pProperties);
  }

  /** Checks if the given player is wearing an enderman head, preventing endermen from getting angry when looked at */
  public static boolean isEnderMask(Player player) {
    return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof EndermanHeadItem;
  }
}
