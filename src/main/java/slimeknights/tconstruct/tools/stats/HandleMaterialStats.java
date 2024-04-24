package slimeknights.tconstruct.tools.stats;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.IToolStat;

import java.util.ArrayList;
import java.util.List;

import static slimeknights.tconstruct.library.materials.stats.IMaterialStats.makeTooltip;
import static slimeknights.tconstruct.library.materials.stats.IMaterialStats.makeTooltipKey;

/** Stats for melee harvest handles */
public record HandleMaterialStats(float durability, float miningSpeed, float meleeSpeed, float attackDamage) implements IMaterialStats {
  public static final MaterialStatsId ID = new MaterialStatsId(TConstruct.getResource("handle"));
  public static final MaterialStatType<HandleMaterialStats> TYPE = new MaterialStatType<>(ID, new HandleMaterialStats(1f, 1f, 1f, 1f), RecordLoadable.create(
    FloatLoadable.FROM_ZERO.defaultField("durability", 1f, true, HandleMaterialStats::durability),
    FloatLoadable.FROM_ZERO.defaultField("melee_damage", 1f, true, HandleMaterialStats::attackDamage),
    FloatLoadable.FROM_ZERO.defaultField("melee_speed", 1f, true, HandleMaterialStats::meleeSpeed),
    FloatLoadable.FROM_ZERO.defaultField("mining_speed", 1f, true, HandleMaterialStats::miningSpeed),
    HandleMaterialStats::new));

  // tooltip prefixes
  private static final String DURABILITY_PREFIX = makeTooltipKey(TConstruct.getResource("durability"));
  private static final String ATTACK_DAMAGE_PREFIX = makeTooltipKey(TConstruct.getResource("attack_damage"));
  private static final String ATTACK_SPEED_PREFIX = makeTooltipKey(TConstruct.getResource("attack_speed"));
  private static final String MINING_SPEED_PREFIX = makeTooltipKey(TConstruct.getResource("mining_speed"));
  // tooltip descriptions
  private static final List<Component> DESCRIPTION = List.of(
    makeTooltip(TConstruct.getResource("handle.durability.description")),
    makeTooltip(TConstruct.getResource("handle.attack_damage.description")),
    makeTooltip(TConstruct.getResource("handle.attack_speed.description")),
    makeTooltip(TConstruct.getResource("handle.mining_speed.description")));

  // multipliers

  @Override
  public MaterialStatType<HandleMaterialStats> getType() {
    return TYPE;
  }

  @Override
  public List<Component> getLocalizedInfo() {
    List<Component> list = new ArrayList<>();
    list.add(formatDurability(this.durability));
    list.add(formatAttackDamage(this.attackDamage));
    list.add(formatAttackSpeed(this.meleeSpeed));
    list.add(formatMiningSpeed(this.miningSpeed));
    return list;
  }

  @Override
  public List<Component> getLocalizedDescriptions() {
    return DESCRIPTION;
  }

  /** Applies formatting for durability */
  public static Component formatDurability(float quality) {
    return IToolStat.formatColoredMultiplier(DURABILITY_PREFIX, quality);
  }

  /** Applies formatting for attack speed */
  public static Component formatAttackDamage(float quality) {
    return IToolStat.formatColoredMultiplier(ATTACK_DAMAGE_PREFIX, quality);
  }

  /** Applies formatting for attack speed */
  public static Component formatAttackSpeed(float quality) {
    return IToolStat.formatColoredMultiplier(ATTACK_SPEED_PREFIX, quality);
  }

  /** Applies formatting for mining speed */
  public static Component formatMiningSpeed(float quality) {
    return IToolStat.formatColoredMultiplier(MINING_SPEED_PREFIX, quality);
  }


  /* Builder */

  /** Creates a new builder instance */
  public static Builder builder() {
    return new Builder();
  }

  @Accessors(fluent = true)
  @Setter
  public static class Builder {
    private float durability = 1;
    private float miningSpeed = 1;
    private float attackSpeed = 1;
    private float attackDamage = 1;

    private Builder() {}

    public HandleMaterialStats build() {
      return new HandleMaterialStats(durability, miningSpeed, attackSpeed, attackDamage);
    }
  }
}
