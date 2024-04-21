package slimeknights.tconstruct.library.modifiers.modules.display;

import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Simple module to change the color of the durability bar.
 * If you have a usecase of something more complex in JSON, feel free to request it, but for now just programming what we use.
 */
public record DurabilityBarColorModule(int color) implements DurabilityDisplayModifierHook, ModifierModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<DurabilityBarColorModule>defaultHooks(ModifierHooks.DURABILITY_DISPLAY);
  public static final RecordLoadable<DurabilityBarColorModule> LOADER = RecordLoadable.create(ColorLoadable.NO_ALPHA.requiredField("color", DurabilityBarColorModule::color), DurabilityBarColorModule::new);

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }
  @Nullable
  @Override
  public Boolean showDurabilityBar(IToolStackView tool, ModifierEntry modifier) {
    return null; // null means no change
  }

  @Override
  public int getDurabilityWidth(IToolStackView tool, ModifierEntry modifier) {
    return 0; // 0 means no change
  }

  @Override
  public int getDurabilityRGB(IToolStackView tool, ModifierEntry modifier) {
    return color;
  }

  @Override
  public RecordLoadable<DurabilityBarColorModule> getLoader() {
    return LOADER;
  }
}
