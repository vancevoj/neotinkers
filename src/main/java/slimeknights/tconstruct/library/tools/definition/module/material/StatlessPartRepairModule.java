package slimeknights.tconstruct.library.tools.definition.module.material;

import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/** Module to repair a tool using materials which are not conventionally repair materials */
public record StatlessPartRepairModule(int partIndex, int repairAmount) implements MaterialRepairToolHook, ToolModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<MaterialRepairModule>defaultHooks(ToolHooks.MATERIAL_REPAIR);
  public static final RecordLoadable<StatlessPartRepairModule> LOADER = RecordLoadable.create(
    IntLoadable.FROM_ZERO.requiredField("part_index", StatlessPartRepairModule::partIndex),
    IntLoadable.FROM_ONE.requiredField("repair_amount", StatlessPartRepairModule::repairAmount),
    StatlessPartRepairModule::new);

  @Override
  public RecordLoadable<? extends IHaveLoader> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public boolean isRepairMaterial(IToolStackView tool, MaterialId material) {
    return material.equals(tool.getMaterial(partIndex).getId());
  }

  @Override
  public float getRepairAmount(IToolStackView tool, MaterialId material) {
    return isRepairMaterial(tool, material) ? repairAmount : 0;
  }
}
