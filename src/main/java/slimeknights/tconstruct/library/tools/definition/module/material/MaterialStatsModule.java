package slimeknights.tconstruct.library.tools.definition.module.material;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierHookProvider;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolStatsHook;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;
import java.util.stream.IntStream;

/** Module for building tool stats using materials */
public class MaterialStatsModule implements ToolStatsHook, ToolTraitHook, ToolMaterialHook, ToolModule {
  private static final List<ModifierHook<?>> DEFAULT_HOOKS = ModifierHookProvider.<MaterialStatsModule>defaultHooks(ToolHooks.TOOL_STATS, ToolHooks.TOOL_TRAITS, ToolHooks.TOOL_MATERIALS);
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
  private int[] repairIndices;
  private int maxRepairWeight = 0;

  public MaterialStatsModule(MaterialStatProvider statProvider, List<WeightedStatType> statTypes) {
    this.statProvider = statProvider;
    this.statTypes = statTypes;
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
  public List<ModifierHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public List<WeightedStatType> getStatTypes(ToolDefinition definition) {
    return statTypes;
  }

  /** Gets the repair indices, calculating them if needed */
  private int[] getRepairIndices() {
    if (repairIndices == null) {
      IMaterialRegistry registry = MaterialRegistry.getInstance();
      repairIndices = IntStream.range(0, statTypes.size()).filter(i -> registry.canRepair(statTypes.get(i).stat())).toArray();
    }
    return repairIndices;
  }

  @Override
  public int[] getRepairIndices(ToolDefinition definition) {
    return getRepairIndices();
  }

  @Override
  public int maxRepairWeight(ToolDefinition definition) {
    if (maxRepairWeight == 0) {
      maxRepairWeight = IntStream.of(getRepairIndices()).map(i -> statTypes.get(i).weight()).max().orElse(1);
    }
    return maxRepairWeight;
  }

  @Override
  public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
    statProvider.addStats(statTypes, context.getMaterials(), builder);
  }

  @Override
  public void addTraits(ToolDefinition definition, MaterialNBT materials, ModifierNBT.Builder builder) {
    int stats = statTypes.size();
    // if the NBT is invalid, no-op to prevent an exception here (could kill itemstacks)
    if (materials.size() == stats) {
      IMaterialRegistry materialRegistry = MaterialRegistry.getInstance();
      for (int i = 0; i < stats; i++) {
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
}
