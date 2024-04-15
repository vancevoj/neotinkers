package slimeknights.tconstruct.library.tools.definition.module.material;

import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;

import java.util.List;

/** Hook for building material tool stats. Not mergable. */
public interface MaterialStatsToolHook {
  /**
   * Gets the list of parts on the tool
   * @param definition  Tool definition instance
   * @return  List of part requirements
   */
  List<WeightedStatType> getStatTypes(ToolDefinition definition);

  /** Checks if this definition has materials */
  default boolean hasMaterials(ToolDefinition definition) {
    return !getStatTypes(definition).isEmpty();
  }

  /** Gets the material stats that represent repairing */
  default int[] getRepairIndices(ToolDefinition definition) {
    return new int[0];
  }

  default int maxRepairWeight(ToolDefinition definition) {
    return 1;
  }

  /**
   * Builds the stats for the given definition and materials
   * @param definition  Definition instance
   * @param materials   Materials instance
   * @return  Tool stats
   */
  StatsNBT buildStats(ToolDefinition definition, MaterialNBT materials);

  /** Stat with weights */
  record WeightedStatType(MaterialStatsId stat, int weight) {
    public static final RecordLoadable<WeightedStatType> LOADABLE = RecordLoadable.create(
      MaterialStatsId.PARSER.requiredField("stat", WeightedStatType::stat),
      IntLoadable.FROM_ONE.defaultField("weight", 1, WeightedStatType::weight),
      WeightedStatType::new).compact(MaterialStatsId.PARSER.flatXmap(id -> new WeightedStatType(id, 1), WeightedStatType::stat), s -> s.weight == 1);

    /** Checks if the given material can be used */
    public boolean canUseMaterial(MaterialId material) {
      return MaterialRegistry.getInstance().getMaterialStats(material.getId(), stat).isPresent();
    }
  }

  /** Gets the stat types from the given definition */
  static List<WeightedStatType> stats(ToolDefinition definition) {
    return definition.getHook(ToolHooks.MATERIAL_STATS).getStatTypes(definition);
  }
}
