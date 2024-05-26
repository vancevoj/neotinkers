package slimeknights.tconstruct.library.tools.definition.module.mining;

import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Hook for checking if a block can be mined */
public interface IsEffectiveToolHook {
  /** Determines if the tool is effective against the given block, ignoring mining tiers. */
  boolean isToolEffective(IToolStackView tool, BlockState state);

  /** Checks if this tool is effective against the given block considering mining tiers */
  static boolean isEffective(IToolStackView tool, BlockState state) {
    return !tool.isBroken() && tool.getHook(ToolHooks.IS_EFFECTIVE).isToolEffective(tool, state);
  }
}
