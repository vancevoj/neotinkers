package slimeknights.tconstruct.tools.stats;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialStatsModule.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.MaterialStatProvider;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;
import java.util.Set;

/**
 * Standard stat builder for ranged tools. Includes some melee attributes for melee bows
 */
public class RangedMaterialStatProvider extends MaterialStatProvider {
  public RangedMaterialStatProvider(ResourceLocation id) {
    super(id, Set.of(LimbMaterialStats.ID), Set.of(StatlessMaterialStats.BOWSTRING.getIdentifier(), GripMaterialStats.ID));
  }

  @Override
  public void addStats(List<WeightedStatType> statTypes, MaterialNBT materials, ModifierStatsBuilder builder) {
    List<LimbMaterialStats> limbs = listOfCompatibleWith(LimbMaterialStats.ID, materials, statTypes);
    if (!limbs.isEmpty()) {
      addStats(limbs, listOfCompatibleWith(GripMaterialStats.ID, materials, statTypes), builder);
    }
  }

  @VisibleForTesting
  public void addStats(List<LimbMaterialStats> limbs, List<GripMaterialStats> grips, ModifierStatsBuilder builder) {
    // add in specific stat types handled by our materials
    ToolStats.DURABILITY.update(builder, getTotalValue(limbs, LimbMaterialStats::durability));
    ToolStats.DURABILITY.multiply(builder, getAverageValue(grips, GripMaterialStats::durability, 1));
    ToolStats.DRAW_SPEED.add(builder, getTotalValue(limbs, LimbMaterialStats::drawSpeed));
    ToolStats.VELOCITY.add(builder, getTotalValue(limbs, LimbMaterialStats::velocity));
    ToolStats.ACCURACY.add(builder, getTotalValue(limbs, LimbMaterialStats::accuracy) + getTotalValue(grips, GripMaterialStats::accuracy));
    ToolStats.ATTACK_DAMAGE.update(builder, getAverageValue(grips, GripMaterialStats::meleeDamage));
  }
}
