package slimeknights.tconstruct.tools.stats;

import com.google.common.annotations.VisibleForTesting;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.tools.stat.ToolStatsBuilder;

import java.util.List;

/** Stat builder for slimeskull helmets */
public class SkullToolStatsBuilder extends ToolStatsBuilder {
  private final List<SkullStats> skulls;

  @VisibleForTesting
  protected SkullToolStatsBuilder(List<SkullStats> skulls) {
    this.skulls = skulls;
  }

  /** Creates a builder from the definition and materials */
  public static ToolStatsBuilder from(List<WeightedStatType> statTypes, MaterialNBT materials) {
    return new SkullToolStatsBuilder(listOfCompatibleWith(SkullStats.ID, materials, statTypes));
  }

  @Override
  public void addStats(ModifierStatsBuilder builder) {
    // add in specific stat types handled by our materials
    ToolStats.DURABILITY.update(builder, getAverageValue(skulls, SkullStats::getDurability));
    ToolStats.ARMOR.add(builder, getAverageValue(skulls, SkullStats::getArmor));
  }
}
