package slimeknights.tconstruct.tools.stats;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.tools.stat.ToolStatsBuilder;

import java.util.Comparator;
import java.util.List;

/**
 * Standard stat builder for melee and harvest tools. Calculates the five main stat types, and handles the bonuses for other types
 */
@Getter(AccessLevel.PROTECTED)
public final class MeleeHarvestToolStatsBuilder extends ToolStatsBuilder {
  private final List<HeadMaterialStats> heads;
  private final List<HandleMaterialStats> handles;
  private final List<ExtraMaterialStats> extras;

  @VisibleForTesting
  public MeleeHarvestToolStatsBuilder(List<HeadMaterialStats> heads, List<HandleMaterialStats> handles, List<ExtraMaterialStats> extras) {
    this.heads = heads;
    this.handles = handles;
    this.extras = extras;
  }

  /** Creates a builder from the definition and materials */
  public static ToolStatsBuilder from(List<WeightedStatType> statTypes, MaterialNBT materials) {
    return new MeleeHarvestToolStatsBuilder(
      listOfCompatibleWith(HeadMaterialStats.ID, materials, statTypes),
      listOfCompatibleWith(HandleMaterialStats.ID, materials, statTypes),
      listOfCompatibleWith(ExtraMaterialStats.ID, materials, statTypes)
    );
  }

  @Override
  public void addStats(ModifierStatsBuilder builder) {
    // add in specific stat types handled by our materials
    ToolStats.DURABILITY.update(builder, getAverageValue(heads, HeadMaterialStats::getDurability));
    ToolStats.DURABILITY.multiply(builder, getAverageValue(handles, HandleMaterialStats::getDurability, 1f));
    ToolStats.HARVEST_TIER.update(builder, buildHarvestLevel());
    ToolStats.ATTACK_DAMAGE.update(builder, getAverageValue(heads, HeadMaterialStats::getAttack));
    ToolStats.ATTACK_DAMAGE.multiply(builder, getAverageValue(handles, HandleMaterialStats::getAttackDamage, 1f));
    ToolStats.ATTACK_SPEED.multiply(builder, getAverageValue(handles, HandleMaterialStats::getAttackSpeed, 1f));
    // ignore default value
    ToolStats.MINING_SPEED.update(builder, getAverageValue(heads, HeadMaterialStats::getMiningSpeed));
    ToolStats.MINING_SPEED.multiply(builder, getAverageValue(handles, HandleMaterialStats::getMiningSpeed, 1));
  }

  /** Builds the harvest level for the tool */
  private Tier buildHarvestLevel() {
    List<Tier> sortedTiers = TierSortingRegistry.getSortedTiers();
    return heads.stream()
      .map(HeadMaterialStats::getTier)
      .max(Comparator.comparingInt(sortedTiers::indexOf))
      .orElse(sortedTiers.get(0));
  }
}
