package slimeknights.tconstruct.library.tools.definition.module.build;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierHookProvider;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.MultiplierNBT;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;

/** Module to set global multipliers on the tool */
public record MultiplyStatsModule(MultiplierNBT multipliers) implements ToolStatsHook, ToolModule {
  public static final RecordLoadable<MultiplyStatsModule> LOADER = RecordLoadable.create(MultiplierNBT.LOADABLE.requiredField("multipliers", MultiplyStatsModule::multipliers), MultiplyStatsModule::new);
  private static final List<ModifierHook<?>> DEFAULT_HOOKS = ModifierHookProvider.<MultiplyStatsModule>defaultHooks(ToolHooks.TOOL_STATS);

  @Override
  public RecordLoadable<MultiplyStatsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModifierHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
    for (INumericToolStat<?> stat : multipliers.getContainedStats()) {
      stat.multiplyAll(builder, multipliers.get(stat));
    }
  }
}
