package slimeknights.tconstruct.library.tools.definition;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.field.MergingField;
import slimeknights.tconstruct.library.json.field.MergingField.MissingMode;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.WithHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MultiplierNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class contains all data pack configurable data for a tool, before materials are factored in.
 * Contains info about how to craft a tool and how it behaves.
 */
public class ToolDefinitionData {
  /** Empty tool data definition instance */
  public static final ToolDefinitionData EMPTY = new ToolDefinitionData(Collections.emptyList(), StatsNBT.EMPTY, MultiplierNBT.EMPTY, List.of(), ErrorFactory.RUNTIME);
  /** Loadable to parse definition data from JSON */
  public static final RecordLoadable<ToolDefinitionData> LOADABLE = RecordLoadable.create(
    PartRequirement.LOADABLE.list(0).defaultField("parts", List.of(), d -> d.parts),
    new MergingField<>(StatsNBT.LOADABLE.defaultField("base", StatsNBT.EMPTY, d -> d.baseStats), "stats", MissingMode.CREATE),
    new MergingField<>(MultiplierNBT.LOADABLE.defaultField("multipliers", MultiplierNBT.EMPTY, d -> d.multipliers), "stats", MissingMode.CREATE),
    ToolModule.WITH_HOOKS.list(0).defaultField("modules", List.of(), d -> d.modules),
    ErrorFactory.FIELD, ToolDefinitionData::new);

  /** Gets a list of all parts in the tool */
  @Getter
  private final List<PartRequirement> parts;
  @VisibleForTesting
  protected final StatsNBT baseStats;
  @VisibleForTesting
  protected final MultiplierNBT multipliers;
  private final List<WithHooks<ToolModule>> modules;
  @Getter
  private final transient ModifierHookMap hooks;

  protected ToolDefinitionData(List<PartRequirement> parts, StatsNBT baseStats, MultiplierNBT multipliers, List<WithHooks<ToolModule>> modules, ErrorFactory error) {
    this.parts = parts;
    this.baseStats = baseStats;
    this.multipliers = multipliers;
    this.modules = modules;
    this.hooks = ModifierHookMap.createMap(modules, error);
  }


  /* Getters */

  /** Gets the given module from the tool */
  public <T> T getHook(ModifierHook<T> hook) {
    return hooks.getOrDefault(hook);
  }


  /* Stats */

  /** Gets a set of bonuses applied to this tool, for stat building */
  public Set<IToolStat<?>> getAllBaseStats() {
    return baseStats.getContainedStats();
  }

  /** Determines if the given stat is defined in this definition, for stat building */
  public boolean hasBaseStat(IToolStat<?> stat) {
    return baseStats.hasStat(stat);
  }

  /** Gets the value of a stat in this tool, or the default value if missing */
  public <T> T getBaseStat(IToolStat<T> toolStat) {
    return baseStats.get(toolStat);
  }

  /**
   * Gets the multiplier for this stat to use for modifiers
   *
   * In most cases, its better to use {@link IToolStackView#getMultiplier(INumericToolStat)} as that takes the modifier multiplier into account
   */
  public float getMultiplier(INumericToolStat<?> toolStat) {
    return multipliers.get(toolStat);
  }


  /* Tool building */

  /**
   * Applies the extra tool stats to the tool like a modifier
   * @param builder  Tool stats builder
   */
  public void buildStatMultipliers(ModifierStatsBuilder builder) {
    for (INumericToolStat<?> stat : multipliers.getContainedStats()) {
      stat.multiplyAll(builder, multipliers.get(stat));
    }
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
