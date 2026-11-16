package slimeknights.tconstruct.library.tools.definition.module.build;

import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.Collection;

/**
 * Hook for adding direct stats to a tool. Stats show in the tooltip and need not be tied to attributes, plus are easier to query and have nicer builders.
 * Overall, its what Mojang really should have done for many attributes.
 */
public interface ToolStatsHook {
  /**
   * Adds raw stats to the tool. Called whenever tool stats are rebuilt. If you wish to do conditional stats, use a trait.
   * @param context   Context about the tool beilt. Partial view of {@link IToolStackView} as the tool is not fully built. Note this hook runs after volatile data builds
   * @param builder   Tool stat builder
   */
  void addToolStats(IToolContext context, ModifierStatsBuilder builder);

  /** Merger that runs all hooks */
  record AllMerger(Collection<ToolStatsHook> modules) implements ToolStatsHook {
    @Override
    public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
      for (ToolStatsHook module : modules) {
        module.addToolStats(context, builder);
      }
    }
  }
}
