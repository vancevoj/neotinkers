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

/** Stat builder for slimeskull helmets */
public class SkullMaterialStatProvider extends MaterialStatProvider {
  public SkullMaterialStatProvider(ResourceLocation id) {
    super(id, Set.of(SkullStats.ID), Set.of());
  }

  @Override
  public void addStats(List<WeightedStatType> statTypes, MaterialNBT materials, ModifierStatsBuilder builder) {
    List<SkullStats> skulls = listOfCompatibleWith(SkullStats.ID, materials, statTypes);
    if (!skulls.isEmpty()) {
      addStats(skulls, builder);
    }
  }

  @VisibleForTesting
  public void addStats(List<SkullStats> skulls, ModifierStatsBuilder builder) {
    // add in specific stat types handled by our materials
    ToolStats.DURABILITY.update(builder, getAverageValue(skulls, SkullStats::durability));
    ToolStats.ARMOR.add(builder, getAverageValue(skulls, SkullStats::armor));
  }
}
