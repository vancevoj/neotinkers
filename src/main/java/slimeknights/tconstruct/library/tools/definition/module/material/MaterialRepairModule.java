package slimeknights.tconstruct.library.tools.definition.module.material;

import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.IRepairableMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierHookProvider;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.List;

/** Module for repairing a tool using a non-tool part material */
public final class MaterialRepairModule implements MaterialRepairToolHook, ToolModule {
  private static final List<ModifierHook<?>> DEFAULT_HOOKS = ModifierHookProvider.<MaterialRepairModule>defaultHooks(ToolHooks.MATERIAL_REPAIR);
  public static final RecordLoadable<MaterialRepairModule> LOADER = RecordLoadable.create(
    MaterialId.PARSER.requiredField("material", m -> m.material),
    MaterialStatsId.PARSER.requiredField("stat_type", m -> m.statType),
    FloatLoadable.FROM_ZERO.defaultField("factor", 1f, m -> m.factor),
    MaterialRepairModule::new);

  /** Material used for repairing */
  private final MaterialId material;
  /** Stat type used for repairing, null means it will be fetched as the first available stat type */
  private final MaterialStatsId statType;
  /** Repair factor, allows making some materials repair with more weight */
  private final float factor;
  /** Amount to repair */
  private float repairAmount = -1;

  public MaterialRepairModule(MaterialId material, MaterialStatsId statType, float factor) {
    this.material = material;
    this.statType = statType;
    this.factor = factor;
  }

  public MaterialRepairModule(MaterialId material, MaterialStatsId statType) {
    this(material, statType, 1);
  }

  @Override
  public boolean isRepairMaterial(IToolStackView tool, MaterialId material) {
    return this.material.equals(material);
  }

  @Override
  public float getRepairFactor(IToolStackView tool, MaterialId material) {
    return this.material.equals(material) ? factor : 0;
  }

  /** Gets and caches the repair amount for this module */
  private float getRepairAmount(ToolDefinition definition) {
    if (repairAmount == -1) {
      repairAmount = getDurability(definition.getId(), material, statType) * factor;
    }
    return repairAmount;
  }

  @Override
  public float getRepairAmount(IToolStackView tool, MaterialId material) {
    return this.material.equals(material) ? getRepairAmount(tool.getDefinition()) : 0;
  }

  @Override
  public RecordLoadable<MaterialRepairModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModifierHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }


  /** Gets the durability for the given stat type */
  public static int getDurability(@Nullable ResourceLocation toolId, MaterialId material, MaterialStatsId statType) {
    IMaterialStats stats = MaterialRegistry.getInstance().getMaterialStats(material, statType).orElse(null);
    if (stats instanceof IRepairableMaterialStats repairable) {
      return repairable.getDurability();
    } else {
      if (toolId != null) {
        TConstruct.LOG.warn("Attempting to repair {} using {}, but stat type {}{}. This usually indicates a broken datapack.", toolId, material, statType, stats == null ? " does not exist for the material" : " does not contain durability");
      }
      return 0;
    }
  }
}
