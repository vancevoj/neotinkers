package slimeknights.tconstruct.library.tools.definition.module.material;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolStatsHook;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.stat.MaterialStatProvider;
import slimeknights.tconstruct.library.tools.stat.MaterialStatProviders;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/** Module for building tool stats using materials */
public class MaterialStatsModule implements ToolStatsHook, ToolTraitHook, ToolMaterialHook, MaterialRepairToolHook, ToolModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<MaterialStatsModule>defaultHooks(ToolHooks.TOOL_STATS, ToolHooks.TOOL_TRAITS, ToolHooks.TOOL_MATERIALS, ToolHooks.MATERIAL_REPAIR);
  protected static final LoadableField<MaterialStatProvider,MaterialStatsModule> STAT_PROVIDER_FIELD = MaterialStatProviders.REGISTRY.requiredField("stat_provider", m -> m.statProvider);
  public static final RecordLoadable<MaterialStatsModule> LOADER = RecordLoadable.create(
    STAT_PROVIDER_FIELD,
    WeightedStatType.LOADABLE.list(1).requiredField("stat_types", m -> m.statTypes),
    MaterialStatsModule::new).validate((module, error) -> {
      module.validate(error);
      return module;
    });

  private final MaterialStatProvider statProvider;
  private final List<WeightedStatType> statTypes;
  private final List<MaterialStatsId> flatStats;
  private int[] repairIndices;
  private int maxRepairWeight = 0;

  public MaterialStatsModule(MaterialStatProvider statProvider, List<WeightedStatType> statTypes) {
    this.statProvider = statProvider;
    this.statTypes = statTypes;
    this.flatStats = statTypes.stream().map(WeightedStatType::stat).toList();
  }

  /** Validates the stat types against the stat provider */
  protected void validate(ErrorFactory error) {
    statProvider.validate(statTypes, error);
  }

  @Override
  public RecordLoadable<? extends MaterialStatsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public List<MaterialStatsId> getStatTypes(ToolDefinition definition) {
    return flatStats;
  }

  /** Gets the repair indices, calculating them if needed */
  private int[] getRepairIndices() {
    if (repairIndices == null) {
      IMaterialRegistry registry = MaterialRegistry.getInstance();
      repairIndices = IntStream.range(0, statTypes.size()).filter(i -> registry.canRepair(statTypes.get(i).stat())).toArray();
    }
    return repairIndices;
  }

  /** Gets the largest weight of all repair materials */
  private int maxRepairWeight() {
    if (maxRepairWeight == 0) {
      maxRepairWeight = IntStream.of(getRepairIndices()).map(i -> statTypes.get(i).weight()).max().orElse(1);
    }
    return maxRepairWeight;
  }

  @Override
  public boolean isRepairMaterial(IToolStackView tool, MaterialId material) {
    for (int part : getRepairIndices()) {
      if (tool.getMaterial(part).matches(material)) {
        return true;
      }
    }
    return false;
  }

  /** Shared logic for both repair value functions */
  private float getRepair(IToolStackView tool, MaterialId material, IntUnaryOperator mapper) {
    return IntStream.of(getRepairIndices())
                    .filter(i -> tool.getMaterial(i).matches(material))
                    .map(mapper)
                    .reduce(0, Integer::max)
           / (float)maxRepairWeight();
  }

  @Override
  public float getRepairFactor(IToolStackView tool, MaterialId material) {
    return getRepair(tool, material, i -> statTypes.get(i).weight());
  }

  @Override
  public float getRepairAmount(IToolStackView tool, MaterialId material) {
    ResourceLocation toolId = tool.getDefinition().getId();
    return getRepair(tool, material, i -> {
      WeightedStatType statType = statTypes.get(i);
      return MaterialRepairModule.getDurability(toolId, material, statType.stat()) * statType.weight();
    });
  }

  @Override
  public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
    MaterialNBT materials = context.getMaterials();
    if (materials.size() > 0) {
      statProvider.addStats(statTypes, context.getMaterials(), builder);
    }
  }

  @Override
  public void addTraits(ToolDefinition definition, MaterialNBT materials, ModifierNBT.Builder builder) {
    int max = Math.min(materials.size(), statTypes.size());
    if (max > 0) {
      IMaterialRegistry materialRegistry = MaterialRegistry.getInstance();
      for (int i = 0; i < max; i++) {
        builder.add(materialRegistry.getTraits(materials.get(i).getId(), statTypes.get(i).stat()));
      }
    }
  }


  /* Builder */

  /** Creates a new builder instance */
  public static Builder stats(MaterialStatProvider statProvider) {
    return new Builder(statProvider);
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final ImmutableList.Builder<WeightedStatType> stats = ImmutableList.builder();
    private final MaterialStatProvider statProvider;

    /** Adds a stat type */
    public Builder stat(MaterialStatsId stat, int weight) {
      stats.add(new WeightedStatType(stat, weight));
      return this;
    }

    /** Adds a stat type */
    public Builder stat(MaterialStatsId stat) {
      return stat(stat, 1);
    }

    /** Builds the module */
    public MaterialStatsModule build() {
      return new MaterialStatsModule(statProvider, stats.build());
    }
  }

  /** Stat with weights */
  public record WeightedStatType(MaterialStatsId stat, int weight) {
    public static final RecordLoadable<WeightedStatType> LOADABLE = RecordLoadable.create(
      MaterialStatsId.PARSER.requiredField("stat", WeightedStatType::stat),
      IntLoadable.FROM_ONE.defaultField("weight", 1, WeightedStatType::weight),
      WeightedStatType::new).compact(MaterialStatsId.PARSER.flatXmap(id -> new WeightedStatType(id, 1), WeightedStatType::stat), s -> s.weight == 1);
  }
}
