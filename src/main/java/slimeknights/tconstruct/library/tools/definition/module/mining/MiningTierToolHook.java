package slimeknights.tconstruct.library.tools.definition.module.mining;

import net.minecraft.world.item.Tier;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collection;

/** Hook to adjust the harvest tier in a tool */
public interface MiningTierToolHook {
  /** Updates the tier based on this logic */
  Tier modifyTier(IToolStackView tool, Tier tier);

  /** Gets the tier for the given tool */
  static Tier getTier(IToolStackView tool) {
    return tool.getHook(ToolHooks.MINING_TIER).modifyTier(tool, tool.getStats().get(ToolStats.HARVEST_TIER));
  }

  /** Merger that runs all hooks, composing the result */
  record ComposeMerger(Collection<MiningTierToolHook> hooks) implements MiningTierToolHook {
    @Override
    public Tier modifyTier(IToolStackView tool, Tier tier) {
      for (MiningTierToolHook hook : hooks) {
        tier = hook.modifyTier(tool, tier);
      }
      return tier;
    }
  }
}
