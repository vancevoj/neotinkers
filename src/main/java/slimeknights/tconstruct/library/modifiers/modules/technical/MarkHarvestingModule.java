package slimeknights.tconstruct.library.modifiers.modules.technical;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;

import java.util.List;

/** Simple module with hooks form of {@link BlockHarvestModifierHook.MarkHarvesting}. */
public enum MarkHarvestingModule implements BlockHarvestModifierHook.MarkHarvesting, HookProvider {
  INSTANCE;

  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<MarkHarvestingModule>defaultHooks(ModifierHooks.BLOCK_HARVEST);

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }
}
