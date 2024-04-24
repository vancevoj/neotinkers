package slimeknights.tconstruct.tools.stats;

import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;

import java.util.List;

/** Material stats for a melee/harvest binding */
public enum BindingMaterialStats implements IMaterialStats {
  DEFAULT;

  public static final MaterialStatsId ID = new MaterialStatsId(TConstruct.getResource("binding"));
  public static final MaterialStatType<BindingMaterialStats> TYPE = MaterialStatType.singleton(ID, DEFAULT);

  private static final Component NO_STATS = IMaterialStats.makeTooltip(TConstruct.getResource("extra.no_stats"));
  private static final List<Component> LOCALIZED = List.of(NO_STATS);
  private static final List<Component> DESCRIPTION = List.of(Component.empty());

  // no stats

  @Override
  public MaterialStatType<?> getType() {
    return TYPE;
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
