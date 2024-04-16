package slimeknights.tconstruct.tools.stats;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.tools.stat.ToolStatsBuilder;

import java.util.List;

/**
 * Standard stat builder for ranged tools. Includes some melee attributes for melee bows
 */
@Getter(AccessLevel.PROTECTED)
public class RangedToolStatsBuilder extends ToolStatsBuilder {
  private final List<LimbMaterialStats> limbs;
  private final List<GripMaterialStats> grips;
  private final List<BowstringMaterialStats> strings;

  @VisibleForTesting
  public RangedToolStatsBuilder(List<LimbMaterialStats> limbs, List<GripMaterialStats> grips, List<BowstringMaterialStats> strings) {
    this.limbs = limbs;
    this.grips = grips;
    this.strings = strings;
  }

  /** Creates a builder from the definition and materials */
  public static ToolStatsBuilder from(List<WeightedStatType> statTypes, MaterialNBT materials) {
    return new RangedToolStatsBuilder(
      listOfCompatibleWith(LimbMaterialStats.ID, materials, statTypes),
      listOfCompatibleWith(GripMaterialStats.ID, materials, statTypes),
      listOfCompatibleWith(BowstringMaterialStats.ID, materials, statTypes)
    );
  }

  @Override
  public void addStats(ModifierStatsBuilder builder) {
    // add in specific stat types handled by our materials
    ToolStats.DURABILITY.update(builder, getTotalValue(limbs, LimbMaterialStats::getDurability));
    ToolStats.DURABILITY.multiply(builder, getAverageValue(grips, GripMaterialStats::getDurability, 1));
    ToolStats.DRAW_SPEED.add(builder, getTotalValue(limbs, LimbMaterialStats::getDrawSpeed));
    ToolStats.VELOCITY.add(builder, getTotalValue(limbs, LimbMaterialStats::getVelocity));
    ToolStats.ACCURACY.add(builder, getTotalValue(limbs, LimbMaterialStats::getAccuracy) + getTotalValue(grips, GripMaterialStats::getAccuracy));
    ToolStats.ATTACK_DAMAGE.update(builder, getAverageValue(grips, GripMaterialStats::getMeleeAttack));
  }
}
