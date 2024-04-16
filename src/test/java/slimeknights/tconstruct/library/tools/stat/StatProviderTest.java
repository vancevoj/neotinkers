package slimeknights.tconstruct.library.tools.stat;

import net.minecraft.world.item.Tiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.library.materials.MaterialRegistryExtension;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MaterialRegistryExtension.class)
class StatProviderTest extends BaseMcTest {
  @BeforeAll
  static void beforeAll() {
    MaterialItemFixture.init();
    setupTierSorting();
  }

  @Test
  void calculateValues_noStats() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(Collections.emptyList(), Collections.emptyList(), builder);

    StatsNBT stats = builder.build();
    assertThat(stats.getInt(ToolStats.DURABILITY)).isEqualTo(1);
    assertThat(stats.get(ToolStats.HARVEST_TIER)).isEqualTo(Tiers.WOOD);
    assertThat(stats.get(ToolStats.MINING_SPEED)).isGreaterThan(0).isLessThanOrEqualTo(1);
    assertThat(stats.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(0);
    assertThat(stats.get(ToolStats.ATTACK_SPEED)).isEqualTo(1);
  }

  @Test
  void buildDurability_ensureAverage_head() {
    HeadMaterialStats stats1 = new HeadMaterialStats(100, 0, Tiers.WOOD, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(50, 0, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.DURABILITY.update(builder, 100f);
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(stats1, stats2), Collections.emptyList(), builder);
    assertThat(builder.build().get(ToolStats.DURABILITY)).isEqualTo(175); // 100 + average of 100 and 75
  }

  @Test
  void buildDurability_testHandleDurability() {
    HeadMaterialStats statsHead = new HeadMaterialStats(200, 0, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle = new HandleMaterialStats(0.5f, 0, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(statsHead), List.of(statsHandle), builder);

    assertThat(builder.build().getInt(ToolStats.DURABILITY)).isEqualTo(100); // 100 * 0.5
  }

  @Test
  void buildMiningSpeed_testHandleMiningSpeed() {
    HeadMaterialStats statsHead = new HeadMaterialStats(0, 2.0f, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle = new HandleMaterialStats(0, 5f, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(statsHead), List.of(statsHandle), builder);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(10f); // 2 * 5
  }

  @Test
  void buildDurability_testHandleDurability_average() {
    HeadMaterialStats statsHead = new HeadMaterialStats(200, 0, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle1 = new HandleMaterialStats(0.3f, 0, 0, 0);
    HandleMaterialStats statsHandle2 = new HandleMaterialStats(0.7f, 0, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(statsHead), List.of(statsHandle1, statsHandle2), builder);

    assertThat(builder.build().getInt(ToolStats.DURABILITY)).isEqualTo(100); // 200 * (0.3 + 0.7)/2
  }

  @Test
  void buildMiningSpeed_testHandleMiningSpeed_average() {
    HeadMaterialStats statsHead = new HeadMaterialStats(0, 5.0f, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle1 = new HandleMaterialStats(0, 0.3f, 0, 0);
    HandleMaterialStats statsHandle2 = new HandleMaterialStats(0, 0.7f, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(statsHead), List.of(statsHandle1, statsHandle2), builder);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(2.5f); // 5 * (0.3 + 0.7)/2
  }

  @Test
  void buildMiningSpeed_ensureAverage() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 10, Tiers.WOOD, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 5, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.MINING_SPEED.update(builder, 10f);
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(stats1, stats2), Collections.emptyList(), builder);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(17.5f); // 10+(10+5)/2
  }

  @Test
  void buildAttack_ensureAverage() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 0, Tiers.WOOD, 5);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 0, Tiers.WOOD, 10);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_DAMAGE.update(builder, 10f);
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(stats1, stats2), Collections.emptyList(), builder);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(17.5f); // 10+(10+5)/2
  }

  @Test
  void buildHarvestLevel_ensureMax() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 1, Tiers.IRON, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 1, Tiers.STONE, 0);
    HeadMaterialStats stats3 = new HeadMaterialStats(1, 1, Tiers.DIAMOND, 0);
    HeadMaterialStats stats4 = new HeadMaterialStats(1, 1, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(stats1, stats2, stats3, stats4), Collections.emptyList(), builder);

    assertThat(builder.build().get(ToolStats.HARVEST_TIER)).isEqualTo(Tiers.DIAMOND);
  }

  @Test
  void buildAttackSpeed_set() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_SPEED.update(builder, 1.5f);
    MaterialStatProviders.MELEE_HARVEST.addStats(Collections.emptyList(), Collections.emptyList(), builder);
    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f);
  }

  @Test
  void buildAttackSpeed_testHandleAttackDamage() {
    HeadMaterialStats head = new HeadMaterialStats(0, 0, Tiers.WOOD, 2);
    HandleMaterialStats stats = new HandleMaterialStats(0, 0, 0, 0.5f);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(head), List.of(stats), builder);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(1.0f); // 2 * 0.5
  }

  @Test
  void buildAttackSpeed_testHandleAttackDamage_average() {
    HeadMaterialStats head = new HeadMaterialStats(0, 0, Tiers.WOOD, 4);
    HandleMaterialStats stats1 = new HandleMaterialStats(0, 0, 0, 1.3f);
    HandleMaterialStats stats2 = new HandleMaterialStats(0, 0, 0, 1.7f);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_DAMAGE.update(builder, 2f);
    MaterialStatProviders.MELEE_HARVEST.addStats(List.of(head), List.of(stats1, stats2), builder);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(9); // (4+2) * (1.3+1.7)/2
  }

  @Test
  void buildAttackSpeed_testHandleAttackSpeed() {
    HandleMaterialStats stats = new HandleMaterialStats(0, 0, 1.5f, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(Collections.emptyList(), List.of(stats), builder);

    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f); // 1 * 1.5f
  }

  @Test
  void buildAttackSpeed_testHandleAttackSpeed_average() {
    HandleMaterialStats stats1 = new HandleMaterialStats(0, 0, 1.3f, 0);
    HandleMaterialStats stats2 = new HandleMaterialStats(0, 0, 1.7f, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    MaterialStatProviders.MELEE_HARVEST.addStats(Collections.emptyList(), List.of(stats1, stats2), builder);

    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f); // 1 * (1.3+1.7)/2
  }
}
