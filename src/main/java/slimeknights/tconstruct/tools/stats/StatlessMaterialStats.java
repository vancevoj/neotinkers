package slimeknights.tconstruct.tools.stats;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;

import java.util.List;

/** Shared code for material stats types with no stats */
public enum StatlessMaterialStats implements IMaterialStats {
  BINDING("binding"),
  BOWSTRING("bowstring"),
  CHAINMAIL("chainmail"),
  /** Internal stat type that forces a repair kit to appear, used for things that repair without having a head stat */
  REPAIR_KIT("repair_kit");

  private static final List<Component> LOCALIZED = List.of(IMaterialStats.makeTooltip(TConstruct.getResource("extra.no_stats")));
  private static final List<Component> DESCRIPTION = List.of(Component.empty());
  @Getter
  private final MaterialStatType<StatlessMaterialStats> type;

  // no stats

  StatlessMaterialStats(String name) {
    this.type = MaterialStatType.singleton(new MaterialStatsId(TConstruct.getResource(name)), this);
  }

  @Override
  public List<Component> getLocalizedInfo() {
    return LOCALIZED;
  }

  @Override
  public List<Component> getLocalizedDescriptions() {
    return DESCRIPTION;
  }
}
