package slimeknights.tconstruct.library.tools.definition.module.mining;

import net.minecraft.world.item.ToolMaterial;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collection;

/** Hook to adjust the harvest tier in a tool */
public interface MiningTierToolHook {
  /** Updates the tier based on this logic */
  ToolMaterial modifyTier(IToolStackView tool, ToolMaterial tier);

  /** Gets the tier for the given tool */
  static ToolMaterial getTier(IToolStackView tool) {
    return tool.getHook(ToolHooks.MINING_TIER).modifyTier(tool, tool.getStats().get(ToolStats.HARVEST_TIER));
  }

  /** Merger that runs all hooks, composing the result */
  record ComposeMerger(Collection<MiningTierToolHook> hooks) implements MiningTierToolHook {
    @Override
    public ToolMaterial modifyTier(IToolStackView tool, ToolMaterial tier) {
      for (MiningTierToolHook hook : hooks) {
        tier = hook.modifyTier(tool, tier);
      }
      return tier;
    }
  }
}
