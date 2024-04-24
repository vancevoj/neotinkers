package slimeknights.tconstruct.tools.stats;


import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IRepairableMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collections;
import java.util.List;

/** Internal stat type to make a material repairable without making it a head material. Only required if you use no other repairable material stat type */
public record RepairKitStats(int durability) implements IRepairableMaterialStats {
  public static final MaterialStatsId ID = new MaterialStatsId(TConstruct.getResource("repair_kit"));
  public static final MaterialStatType<RepairKitStats> TYPE = new MaterialStatType<>(ID, new RepairKitStats(1), RecordLoadable.create(IRepairableMaterialStats.DURABILITY_FIELD, RepairKitStats::new));

  private static final List<Component> DESCRIPTION = ImmutableList.of(ToolStats.DURABILITY.getDescription());

  @Override
  public MaterialStatType<?> getType() {
    return TYPE;
  }

  @Override
  public List<Component> getLocalizedInfo() {
    return Collections.singletonList(ToolStats.DURABILITY.formatValue(this.durability));
  }

  @Override
  public List<Component> getLocalizedDescriptions() {
    return DESCRIPTION;
  }
}
