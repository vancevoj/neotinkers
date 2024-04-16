package slimeknights.tconstruct.library.tools.definition.module;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.modifiers.modules.ModifierHookProvider;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.WithHooks;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

/**
 * Base interface for modules within the tool definition data
 */
public interface ToolModule extends IHaveLoader, ModifierHookProvider {
  /** Loader instance for any modules loadable in tools */
  GenericLoaderRegistry<ToolModule> LOADER = new GenericLoaderRegistry<>("Tool Module", false);
  /** Loadable for modules including hooks */
  RecordLoadable<WithHooks<ToolModule>> WITH_HOOKS = WithHooks.makeLoadable(LOADER);

  /** Interface for armor module builders, which are builders designed to create slightly varied modules based on the armor slot */
  interface ArmorModuleBuilder<T extends ToolModule> {
    /** Builds the module for the given slot */
    T build(ArmorSlotType slot);
  }
}
