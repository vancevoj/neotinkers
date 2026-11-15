package slimeknights.tconstruct.library.utils;

import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;

/**
 * Logic to keep track of the side of the block that was last hit
 */
public class BlockSideHitListener {
  private static final TinkerDataKey<Direction> HIT_FACE = TConstruct.createKey("hit_face");
  @Getter
  private static Direction clientSideHit = Direction.UP;
  private static boolean init = false;

  /** @apiNote Internal method to initialize the listener. */
  @Internal
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    NeoForge.EVENT_BUS.addListener(BlockSideHitListener::onLeftClickBlock);
  }

  /** Called when the player left-clicks a block to store the face */
  private static void onLeftClickBlock(LeftClickBlock event) {
    if (event.getAction() == Action.START) {
      Direction face = event.getFace();
      if (face != null) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
          clientSideHit = face;
        } else {
          TinkerDataCapability.Holder data = TinkerDataCapability.getData(player);
          if (data != null) {
            data.put(HIT_FACE, face);
          }
        }
      }
    }
  }

  /**
   * Gets the side this player last hit, should return correct values in most modifier hooks related to block breaking
   * @param player  Player
   * @return  Side last hit
   */
  public static Direction getSideHit(Player player) {
    if (player.level().isClientSide()) {
      return clientSideHit;
    }
    TinkerDataCapability.Holder data = TinkerDataCapability.getData(player);
    if (data != null) {
      return data.get(HIT_FACE, Direction.UP);
    }
    return Direction.UP;
  }
}
