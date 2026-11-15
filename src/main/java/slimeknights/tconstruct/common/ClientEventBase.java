package slimeknights.tconstruct.common;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * Contains helpers to use for registering client events
 * <p>
 * Item colors are no longer registered in code, so the old block item color alias is gone. Block items that should share the tint
 * of their block use {@link BlockItemTintSource} ({@code tconstruct:block_item}) as the tint in their client item JSON.
 */
public abstract class ClientEventBase {
  /**
   * Registers block tint sources for the given block supplier
   * @param event        Block tint event
   * @param tintSources  Tint source for each tint layer
   * @param block        Block to register
   */
  protected static void registerBlockTint(RegisterColorHandlersEvent.BlockTintSources event, List<BlockTintSource> tintSources, Supplier<? extends Block> block) {
    event.register(tintSources, block.get());
  }

  /**
   * Registers block tint sources for all blocks in the given instance
   * @param event        Block tint event
   * @param tintSources  Tint source for each tint layer
   * @param blocks       EnumBlock instance
   */
  protected static void registerBlockTint(RegisterColorHandlersEvent.BlockTintSources event, List<BlockTintSource> tintSources, EnumObject<?,? extends Block> blocks) {
    event.register(tintSources, blocks.values().toArray(new Block[0]));
  }
}
