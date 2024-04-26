package slimeknights.tconstruct.library.tools.definition.module.build;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import java.util.List;

/** Module to set stats on the tool */
public record SetStatsModule(StatsNBT stats) implements ToolStatsHook, ToolModule {
  public static final RecordLoadable<SetStatsModule> LOADER = RecordLoadable.create(StatsNBT.LOADABLE.requiredField("stats", SetStatsModule::stats), SetStatsModule::new);
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<SetStatsModule>defaultHooks(ToolHooks.TOOL_STATS);

  @Override
  public RecordLoadable<SetStatsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  /** Sets the stat into the builder */
  private <T> void setStat(IToolStat<T> stat, ModifierStatsBuilder builder) {
    stat.update(builder, stats.get(stat));
  }

  @Override
  public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
    for (IToolStat<?> stat : stats.getContainedStats()) {
      setStat(stat, builder);
    }
  }


  /** Creates a builder instance */
  public static ArmorBuilder armor(List<ArmorSlotType> slots) {
    return new ArmorBuilder(slots);
  }

  public static class ArmorBuilder implements ArmorSlotType.ArmorBuilder<SetStatsModule> {

    private final List<ArmorSlotType> slotTypes;
    private final StatsNBT.Builder[] builders = new StatsNBT.Builder[4];

    private ArmorBuilder(List<ArmorSlotType> slotTypes) {
      this.slotTypes = slotTypes;
      for (ArmorSlotType slotType : slotTypes) {
        builders[slotType.getIndex()] = StatsNBT.builder();
      }
    }

    /** Gets the builder for the given slot */
    protected StatsNBT.Builder getBuilder(ArmorSlotType slotType) {
      StatsNBT.Builder builder = builders[slotType.getIndex()];
      if (builder == null) {
        throw new IllegalArgumentException("Unsupported slot type " + slotType);
      }
      return builder;
    }

    /** Adds a bonus to the builder */
    public <T> ArmorBuilder set(ArmorSlotType slotType, IToolStat<T> stat, T value) {
      getBuilder(slotType).set(stat, value);
      return this;
    }

    /** Adds a bonus to the builder */
    public ArmorBuilder set(ArmorSlotType slotType, IToolStat<Float> stat, float value) {
      return set(slotType, stat, (Float) value);
    }

    /** Sets the same bonus on all pieces */
    public <T> ArmorBuilder setAll(IToolStat<T> stat, T value) {
      for (ArmorSlotType slotType : slotTypes) {
        set(slotType, stat, value);
      }
      return this;
    }

    /** Sets the same bonus on all pieces */
    public ArmorBuilder setAll(IToolStat<Float> stat, float value) {
      return setAll(stat, (Float) value);
    }

    /** Sets a different bonus on all pieces, float overload as it comes up commonly */
    public final ArmorBuilder setEach(IToolStat<Float> stat, float... values) {
      if (values.length != slotTypes.size()) {
        throw new IllegalStateException("Wrong number of stats set");
      }
      for (int i = 0; i < values.length; i++) {
        set(slotTypes.get(i), stat, values[i]);
      }
      return this;
    }

    /**
     * Sets the durability for all parts like vanilla armor materials
     * @param maxDamageFactor  Durability modifier applied to the base value for each slot
     * @return  Builder
     */
    public ArmorBuilder durabilityFactor(float maxDamageFactor) {
      for (ArmorSlotType slotType : slotTypes) {
        set(slotType, ToolStats.DURABILITY, ArmorSlotType.MAX_DAMAGE_ARRAY[slotType.getIndex()] * maxDamageFactor);
      }
      return this;
    }

    /** Builds the final module */
    @Override
    public SetStatsModule build(ArmorSlotType slot) {
      return new SetStatsModule(getBuilder(slot).build());
    }
  }
}
