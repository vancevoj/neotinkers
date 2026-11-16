package slimeknights.tconstruct.library.tools.definition.module.material;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.List;

/** Module to directly add parts to a tool without using part stats, mainly used to allow a module to have some parts and some fixed stat types. */
public record PartsModule(List<IToolPart> parts) implements ToolPartsHook, ToolModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<PartsModule>defaultHooks(ToolHooks.TOOL_PARTS);
  public static final RecordLoadable<PartsModule> LOADER = RecordLoadable.create(TinkerLoadables.TOOL_PART_ITEM.list(1).requiredField("parts", m -> m.parts), PartsModule::new);

  @Override
  public List<IToolPart> getParts(ToolDefinition definition) {
    return parts;
  }

  @Override
  public RecordLoadable<PartsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }
}
