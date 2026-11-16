package slimeknights.tconstruct.library.tools.definition;

import lombok.Getter;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.module.WithHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;

import java.util.List;

/**
 * This class contains all data pack configurable data for a tool, before materials are factored in.
 * Contains info about how to craft a tool and how it behaves.
 */
public class ToolDefinitionData {
  /** Empty tool data definition instance */
  public static final ToolDefinitionData EMPTY = new ToolDefinitionData(List.of(), ErrorFactory.RUNTIME);
  /** Loadable to parse definition data from JSON */
  public static final RecordLoadable<ToolDefinitionData> LOADABLE = RecordLoadable.create(ToolModule.WITH_HOOKS.list(0).defaultField("modules", List.of(), d -> d.modules), ErrorFactory.FIELD, ToolDefinitionData::new);

  private final List<WithHooks<ToolModule>> modules;
  @Getter
  private final transient ModuleHookMap hooks;

  protected ToolDefinitionData(List<WithHooks<ToolModule>> modules, ErrorFactory error) {
    this.modules = modules;
    this.hooks = ModuleHookMap.createMap(modules, error);
  }


  /* Getters */

  /** Gets the given module from the tool */
  public <T> T getHook(ModuleHook<T> hook) {
    return hooks.getOrDefault(hook);
  }
}
