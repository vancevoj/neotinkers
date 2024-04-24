package slimeknights.tconstruct.tools.stats;

import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IRepairableMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

/** Stats for slimeskull skulls */
public record SkullStats(int durability, int armor) implements IRepairableMaterialStats {
  public static final MaterialStatsId ID = new MaterialStatsId(TConstruct.getResource("skull"));
  public static final MaterialStatType<SkullStats> TYPE = new MaterialStatType<>(ID, new SkullStats(1, 0), RecordLoadable.create(
    IRepairableMaterialStats.DURABILITY_FIELD,
    IntLoadable.FROM_ZERO.defaultField("armor", 0, true, SkullStats::armor),
    SkullStats::new));
  // tooltip descriptions
  private static final List<Component> DESCRIPTION = List.of(ToolStats.DURABILITY.getDescription(), ToolStats.ARMOR.getDescription());

  @Override
  public MaterialStatType<?> getType() {
    return TYPE;
  }

  @Override
  public List<Component> getLocalizedInfo() {
    return List.of(
      ToolStats.DURABILITY.formatValue(this.durability),
      ToolStats.ARMOR.formatValue(this.armor)
    );
  }

  @Override
  public List<Component> getLocalizedDescriptions() {
    return DESCRIPTION;
  }
}
