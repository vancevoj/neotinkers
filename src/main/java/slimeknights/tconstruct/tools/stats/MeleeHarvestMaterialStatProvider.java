package slimeknights.tconstruct.tools.stats;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialStatsModule.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.MaterialStatProvider;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Standard stat builder for melee and harvest tools. Calculates the five main stat types, and handles the bonuses for other types
 */
public final class MeleeHarvestMaterialStatProvider extends MaterialStatProvider {
  @VisibleForTesting
  public MeleeHarvestMaterialStatProvider(ResourceLocation id) {
    super(id, Set.of(HeadMaterialStats.ID), Set.of(HandleMaterialStats.ID, StatlessMaterialStats.BINDING.getIdentifier()));
  }

  @Override
  public void addStats(List<WeightedStatType> statTypes, MaterialNBT materials, ModifierStatsBuilder builder) {
    List<HeadMaterialStats> heads = listOfCompatibleWith(HeadMaterialStats.ID, materials, statTypes);
    if (!heads.isEmpty()) {
      addStats(heads, listOfCompatibleWith(HandleMaterialStats.ID, materials, statTypes), builder);
    }
  }

  @VisibleForTesting
  public void addStats(List<HeadMaterialStats> heads, List<HandleMaterialStats> handles, ModifierStatsBuilder builder) {
    // add in specific stat types handled by our materials
    ToolStats.DURABILITY.update(builder, getAverageValue(heads, HeadMaterialStats::durability));
    ToolStats.DURABILITY.multiply(builder, getAverageValue(handles, HandleMaterialStats::durability, 1f));
    ToolStats.HARVEST_TIER.update(builder, buildHarvestLevel(heads));
    ToolStats.ATTACK_DAMAGE.update(builder, getAverageValue(heads, HeadMaterialStats::attack));
    ToolStats.ATTACK_DAMAGE.multiply(builder, getAverageValue(handles, HandleMaterialStats::attackDamage, 1f));
    ToolStats.ATTACK_SPEED.multiply(builder, getAverageValue(handles, HandleMaterialStats::meleeSpeed, 1f));
    // ignore default value
    ToolStats.MINING_SPEED.update(builder, getAverageValue(heads, HeadMaterialStats::miningSpeed));
    ToolStats.MINING_SPEED.multiply(builder, getAverageValue(handles, HandleMaterialStats::miningSpeed, 1));
  }

  /** Builds the harvest level for the tool */
  private static Tier buildHarvestLevel(List<HeadMaterialStats> heads) {
    List<Tier> sortedTiers = TierSortingRegistry.getSortedTiers();
    return heads.stream()
      .map(HeadMaterialStats::tier)
      .max(Comparator.comparingInt(sortedTiers::indexOf))
      .orElse(sortedTiers.get(0));
  }
}
