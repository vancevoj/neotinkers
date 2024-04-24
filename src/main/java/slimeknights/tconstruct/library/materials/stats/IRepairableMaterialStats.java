package slimeknights.tconstruct.library.materials.stats;

import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;

/**
 * Material stats that support repairing, requires durability as part of the stats
 */
public interface IRepairableMaterialStats extends IMaterialStats {
  LoadableField<Integer,IRepairableMaterialStats> DURABILITY_FIELD = IntLoadable.FROM_ONE.requiredField("durability", IRepairableMaterialStats::durability);

  /**
   * Gets the amount of durability for this stat type
   * @return  Durability
   */
  int durability();
}
