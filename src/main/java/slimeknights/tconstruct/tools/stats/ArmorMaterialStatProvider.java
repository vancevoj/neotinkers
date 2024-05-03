package slimeknights.tconstruct.tools.stats;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialStatsModule.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.MaterialStatProvider;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Standard stat builder for ranged tools. Includes some melee attributes for melee bows
 */
public class ArmorMaterialStatProvider extends MaterialStatProvider {
  public ArmorMaterialStatProvider(ResourceLocation id) {
    super(id, PlatingMaterialStats.TYPES.stream().map(MaterialStatType::getId).collect(Collectors.toSet()), Set.of(StatlessMaterialStats.MAILLE.getIdentifier(), StatlessMaterialStats.SHIELD_CORE.getIdentifier()));
  }

  @Override
  public void addStats(List<WeightedStatType> statTypes, MaterialNBT materials, ModifierStatsBuilder builder) {
    // neat little trick: this line handles all 4 plating types, no need to write a separate handler for each
    List<PlatingMaterialStats> plating = listOfCompatibleWith(requiredType, materials, statTypes);
    if (!plating.isEmpty()) {
      addStats(plating, builder);
    }
  }

  @VisibleForTesting
  public void addStats(List<PlatingMaterialStats> plating, ModifierStatsBuilder builder) {
    ToolStats.DURABILITY.update(builder, getAverageValue(plating, PlatingMaterialStats::durability));
    ToolStats.ARMOR.update(builder, getAverageValue(plating, PlatingMaterialStats::armor));
    ToolStats.ARMOR_TOUGHNESS.update(builder, getAverageValue(plating, PlatingMaterialStats::toughness));
    ToolStats.KNOCKBACK_RESISTANCE.update(builder, getAverageValue(plating, PlatingMaterialStats::knockbackResistance));
  }
}
