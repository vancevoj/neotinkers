package slimeknights.tconstruct.library.tools.definition;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Items;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.WithHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.MultiplierNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

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
  private final transient ModifierHookMap hooks;

  private transient StatsNBT baseStats;
  private transient MultiplierNBT multipliers;

  protected ToolDefinitionData(List<WithHooks<ToolModule>> modules, ErrorFactory error) {
    this.modules = modules;
    this.hooks = ModifierHookMap.createMap(modules, error);
  }


  /* Getters */

  /** Gets the given module from the tool */
  public <T> T getHook(ModifierHook<T> hook) {
    return hooks.getOrDefault(hook);
  }


  /* Stats */

  /** Computes the stats for the given tool */
  private void computeStats() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    getHook(ToolHooks.TOOL_STATS).addToolStats(new DummyToolStack(Items.AIR, ModifierNBT.EMPTY, new ModDataNBT()), builder);
    multipliers = builder.buildMultipliers();
    // cancel out multipliers on the base stats, as people expect base stats to be comparable to be usable in the modifier stats builder
    for (INumericToolStat<?> stat : multipliers.getContainedStats()) {
      // TODO: this is a hack, alternative is somehow telling the builder to ignore multipliers
      stat.multiply(builder, 1 / multipliers.get(stat));
    }
    baseStats = builder.build();
  }

  /** Gets the stats of this tool without materials or modifiers */
  @VisibleForTesting
  protected StatsNBT getBaseStats() {
    if (baseStats == null) {
      computeStats();
    }
    return baseStats;
  }

  /** Gets the multipliers of this tool without materials or modifiers */
  @VisibleForTesting
  protected MultiplierNBT getMultipliers() {
    if (multipliers == null) {
      computeStats();
    }
    return multipliers;
  }

  /**
   * Gets the value of a stat in this tool, or the default value if missing.
   * Generally better to use {@link IToolStackView#getStats()} as it takes the modifier stats into account.
   */
  public <T> T getBaseStat(IToolStat<T> toolStat) {
    return getBaseStats().get(toolStat);
  }

  /**
   * Gets the multiplier for this stat to use for modifiers
   * In most cases, its better to use {@link IToolStackView#getMultiplier(INumericToolStat)} as that takes the modifier multiplier into account
   */
  public float getMultiplier(INumericToolStat<?> toolStat) {
    return getMultipliers().get(toolStat);
  }


  /* Packet buffers */

  /** Writes a tool definition stat object to a packet buffer */
  @Deprecated
  public void write(FriendlyByteBuf buffer) {
    LOADABLE.encode(buffer, this);
  }

  /** Reads a tool definition stat object from a packet buffer */
  @Deprecated
  public static ToolDefinitionData read(FriendlyByteBuf buffer) {
    return LOADABLE.decode(buffer);
  }
}
