package slimeknights.tconstruct.library.tools.stat;

import net.minecraft.world.item.Tiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.library.materials.MaterialRegistryExtension;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MaterialRegistryExtension.class)
class StatProviderTest extends BaseMcTest {
  @BeforeAll
  static void beforeAll() {
    MaterialItemFixture.init();
    setupTierSorting();
  }

  /** Builds the given stats */
  private static ModifierStatsBuilder addStats(ModifierStatsBuilder builder, IMaterialStats... stats) {
    for (IMaterialStats stat : stats) {
      stat.apply(builder, 1);
    }
    return builder;
  }

  @Test
  void calculateValues_noStats() {
    StatsNBT stats = addStats(ModifierStatsBuilder.builder()).build();
    assertThat(stats.getInt(ToolStats.DURABILITY)).isEqualTo(1);
    assertThat(stats.get(ToolStats.HARVEST_TIER)).isEqualTo(Tiers.WOOD);
    assertThat(stats.get(ToolStats.MINING_SPEED)).isGreaterThan(0).isLessThanOrEqualTo(1);
    assertThat(stats.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(0);
    assertThat(stats.get(ToolStats.ATTACK_SPEED)).isEqualTo(1);
  }

  @Test
  void buildDurability_multiple_head() {
    HeadMaterialStats stats1 = new HeadMaterialStats(100, 0, Tiers.WOOD, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(50, 0, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.DURABILITY.update(builder, 100f);
    addStats(builder, stats1, stats2);

    assertThat(builder.build().get(ToolStats.DURABILITY)).isEqualTo(250); // 100 + 100 + 50
  }

  @Test
  void buildDurability_testHandleDurability() {
    HeadMaterialStats statsHead = new HeadMaterialStats(200, 0, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle = new HandleMaterialStats(-0.5f, 0, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, statsHead, statsHandle);

    assertThat(builder.build().getInt(ToolStats.DURABILITY)).isEqualTo(100); // 200 * 0.5
  }

  @Test
  void buildMiningSpeed_testHandleMiningSpeed() {
    HeadMaterialStats statsHead = new HeadMaterialStats(0, 2.0f, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle = new HandleMaterialStats(0, 4f, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, statsHead, statsHandle);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(10f); // 2 * 5
  }

  @Test
  void buildDurability_testHandleDurability_multiple() {
    HeadMaterialStats statsHead = new HeadMaterialStats(200, 0, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle1 = new HandleMaterialStats(0.5f, 0, 0, 0);
    HandleMaterialStats statsHandle2 = new HandleMaterialStats(-0.2f, 0, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, statsHead, statsHandle1, statsHandle2);

    assertThat(builder.build().getInt(ToolStats.DURABILITY)).isEqualTo(260); // 200 * (1 + 0.5 - 0.2)
  }

  @Test
  void buildMiningSpeed_testHandleMiningSpeed_multiple() {
    HeadMaterialStats statsHead = new HeadMaterialStats(0, 5.0f, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle1 = new HandleMaterialStats(0, 0.25f, 0, 0);
    HandleMaterialStats statsHandle2 = new HandleMaterialStats(0, 0.75f, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, statsHead, statsHandle1, statsHandle2);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(10); // 5 * (1 + 0.25 * 0.75)
  }

  @Test
  void buildMiningSpeed_multiple() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 10, Tiers.WOOD, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 5, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.MINING_SPEED.update(builder, 10f);
    addStats(builder, stats1, stats2);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(25f); // 10+10+5
  }

  @Test
  void buildAttack_multiple() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 0, Tiers.WOOD, 5);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 0, Tiers.WOOD, 10);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_DAMAGE.update(builder, 10f);
    addStats(builder, stats1, stats2);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(25); // 10+10+5
  }

  @Test
  void buildHarvestLevel_ensureMax() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 1, Tiers.IRON, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 1, Tiers.STONE, 0);
    HeadMaterialStats stats3 = new HeadMaterialStats(1, 1, Tiers.DIAMOND, 0);
    HeadMaterialStats stats4 = new HeadMaterialStats(1, 1, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, stats1, stats2, stats3, stats4);

    assertThat(builder.build().get(ToolStats.HARVEST_TIER)).isEqualTo(Tiers.DIAMOND);
  }

  @Test
  void buildAttackSpeed_set() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_SPEED.update(builder, 1.5f);
    addStats(builder);
    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f);
  }

  @Test
  void buildAttackSpeed_testHandleAttackDamage() {
    HeadMaterialStats head = new HeadMaterialStats(0, 0, Tiers.WOOD, 2);
    HandleMaterialStats handle = new HandleMaterialStats(0, 0, 0, -0.5f);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, head, handle);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(1.0f); // 2 * 0.5
  }

  @Test
  void buildAttackSpeed_testHandleAttackDamage_multiple() {
    HeadMaterialStats head = new HeadMaterialStats(0, 0, Tiers.WOOD, 4);
    HandleMaterialStats handle1 = new HandleMaterialStats(0, 0, 0, 0.25f);
    HandleMaterialStats handle2 = new HandleMaterialStats(0, 0, 0, 0.75f);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_DAMAGE.update(builder, 2f);
    addStats(builder, head, handle1, handle2);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(12); // (4+2) * (1 + 0.25 + 0.75))
  }

  @Test
  void buildAttackSpeed_testHandleAttackSpeed() {
    HandleMaterialStats stats = new HandleMaterialStats(0, 0, 1.5f, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, stats);

    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(2.5f); // 1 * 2.5f
  }

  @Test
  void buildAttackSpeed_testHandleAttackSpeed_multiple() {
    HandleMaterialStats stats1 = new HandleMaterialStats(0, 0, 0.25f, 0);
    HandleMaterialStats stats2 = new HandleMaterialStats(0, 0, 0.75f, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    addStats(builder, stats1, stats2);

    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(2f); // 1 * (1 + 0.25 + 0.75)
  }

  @Test
  void calculateValues_headScales() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new HeadMaterialStats(100, 5, Tiers.DIAMOND, 8).apply(builder, 2f);
    StatsNBT stats = builder.build();

    assertThat(stats.getInt(ToolStats.DURABILITY)).isEqualTo(200);
    assertThat(stats.get(ToolStats.HARVEST_TIER)).isEqualTo(Tiers.DIAMOND);
    assertThat(stats.get(ToolStats.MINING_SPEED)).isEqualTo(10);
    assertThat(stats.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(16);
  }

  @Test
  void calculateValues_handleScales() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.DURABILITY.update(builder, 2f);
    ToolStats.MINING_SPEED.update(builder, 3f);
    ToolStats.ATTACK_SPEED.update(builder, 4f);
    ToolStats.ATTACK_DAMAGE.update(builder, 5f);
    new HandleMaterialStats(1, 2, 3, 4).apply(builder, 2f);
    StatsNBT stats = builder.build();

    assertThat(stats.getInt(ToolStats.DURABILITY)).isEqualTo(6); // 2 * (1 + 1*2)
    assertThat(stats.get(ToolStats.MINING_SPEED)).isEqualTo(15); // 3 * (1 + 2*2)
    assertThat(stats.get(ToolStats.ATTACK_SPEED)).isEqualTo(28);  // 4 * (1 + 3*2)
    assertThat(stats.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(45); // 5 * (1 + 4*2)
  }
}
