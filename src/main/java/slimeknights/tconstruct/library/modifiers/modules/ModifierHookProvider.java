package slimeknights.tconstruct.library.modifiers.modules;

import slimeknights.tconstruct.library.modifiers.ModifierHook;

import java.util.List;

/** Interface to simplify building of modifier hook maps */
public interface ModifierHookProvider {
  /** Gets the default list of hooks this module implements. */
  List<ModifierHook<?>> getDefaultHooks();

  /**
   * Helper method to validate generics on the hooks when building a default hooks list. To use, make sure you set the generics instead of leaving it automatic.
   */
  @SafeVarargs
  static <T> List<ModifierHook<?>> defaultHooks(ModifierHook<? super T>... hooks) {
    return List.of(hooks);
  }
}
