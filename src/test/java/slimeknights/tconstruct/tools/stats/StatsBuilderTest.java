package slimeknights.tconstruct.tools.stats;

import net.minecraft.world.item.Tiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.fixture.MaterialStatsFixture;
import slimeknights.tconstruct.fixture.ToolDefinitionFixture;
import slimeknights.tconstruct.library.materials.MaterialRegistryExtension;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.tools.stat.ToolStatsBuilder;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_ALL_STATS;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_EXTRA;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_HANDLE;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_HEAD;

@ExtendWith(MaterialRegistryExtension.class)
class StatsBuilderTest extends BaseMcTest {
  @BeforeAll
  static void beforeAll() {
    MaterialItemFixture.init();
  }

  /**
   * Gets a builder for the given materials list, validating the size is correct
   * @param materials  List of materials
   * @return  Melee harvest tool stats builder
   */
  static MeleeHarvestToolStatsBuilder getBuilder(IMaterial... materials) {
    ToolStatsBuilder builder = MeleeHarvestToolStatsBuilder.from(ToolMaterialHook.stats(ToolDefinitionFixture.getStandardToolDefinition()), MaterialNBT.of(materials));
    assertThat(builder).overridingErrorMessage("Given materials list is the wrong size").isInstanceOf(MeleeHarvestToolStatsBuilder.class);
    return (MeleeHarvestToolStatsBuilder) builder;
  }

  @Test
  void init_onlyHead() {
    MeleeHarvestToolStatsBuilder builder = getBuilder(MATERIAL_WITH_HEAD, MATERIAL_WITH_HEAD, MATERIAL_WITH_HEAD);

    assertThat(builder.getHeads()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HEAD);
    assertThat(builder.getHandles()).containsExactly(HandleMaterialStats.DEFAULT);
    assertThat(builder.getExtras()).containsExactly(ExtraMaterialStats.DEFAULT);
  }

  @Test
  void init_onlyHandle() {
    MeleeHarvestToolStatsBuilder builder = getBuilder(MATERIAL_WITH_HANDLE, MATERIAL_WITH_HANDLE, MATERIAL_WITH_HANDLE);

    assertThat(builder.getHeads()).containsExactly(HeadMaterialStats.DEFAULT);
    assertThat(builder.getHandles()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HANDLE);
    assertThat(builder.getExtras()).containsExactly(ExtraMaterialStats.DEFAULT);
  }

  @Test
  void init_onlyExtra() {
    MeleeHarvestToolStatsBuilder builder = getBuilder(MATERIAL_WITH_EXTRA, MATERIAL_WITH_EXTRA, MATERIAL_WITH_EXTRA);

    assertThat(builder.getHeads()).containsExactly(HeadMaterialStats.DEFAULT);
    assertThat(builder.getHandles()).containsExactly(HandleMaterialStats.DEFAULT);
    assertThat(builder.getExtras()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_EXTRA);
  }

  @Test
  void init_allCorrectStats() {
    MeleeHarvestToolStatsBuilder builder = getBuilder(MATERIAL_WITH_HEAD, MATERIAL_WITH_HANDLE, MATERIAL_WITH_EXTRA);

    assertThat(builder.getHeads()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HEAD);
    assertThat(builder.getHandles()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HANDLE);
    assertThat(builder.getExtras()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_EXTRA);
  }

  @Test
  void init_wrongOrder() {
    MeleeHarvestToolStatsBuilder builder = getBuilder(MATERIAL_WITH_HANDLE, MATERIAL_WITH_EXTRA, MATERIAL_WITH_HEAD);

    assertThat(builder.getHeads()).containsExactly(HeadMaterialStats.DEFAULT);
    assertThat(builder.getHandles()).containsExactly(HandleMaterialStats.DEFAULT);
    assertThat(builder.getExtras()).containsExactly(ExtraMaterialStats.DEFAULT);
  }

  @Test
  void init_singleMaterialAllStats() {
    MeleeHarvestToolStatsBuilder builder = getBuilder(MATERIAL_WITH_ALL_STATS, MATERIAL_WITH_ALL_STATS, MATERIAL_WITH_ALL_STATS);

    assertThat(builder.getHeads()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HEAD);
    assertThat(builder.getHandles()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HANDLE);
    assertThat(builder.getExtras()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_EXTRA);
  }

  @Test
  void calculateValues_noStats() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()).addStats(builder);

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
    new MeleeHarvestToolStatsBuilder(List.of(stats1, stats2), Collections.emptyList(), Collections.emptyList()).addStats(builder);
    assertThat(builder.build().get(ToolStats.DURABILITY)).isEqualTo(175); // 100 + average of 100 and 75
  }

  @Test
  void buildDurability_testHandleDurability() {
    HeadMaterialStats statsHead = new HeadMaterialStats(200, 0, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle = new HandleMaterialStats(0.5f, 0, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(List.of(statsHead), List.of(statsHandle), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().getInt(ToolStats.DURABILITY)).isEqualTo(100); // 100 * 0.5
  }

  @Test
  void buildMiningSpeed_testHandleMiningSpeed() {
    HeadMaterialStats statsHead = new HeadMaterialStats(0, 2.0f, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle = new HandleMaterialStats(0, 5f, 0, 0);
    ExtraMaterialStats statsExtra = ExtraMaterialStats.DEFAULT;

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(List.of(statsHead), List.of(statsHandle), List.of(statsExtra)).addStats(builder);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(10f); // 2 * 5
  }

  @Test
  void buildDurability_testHandleDurability_average() {
    HeadMaterialStats statsHead = new HeadMaterialStats(200, 0, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle1 = new HandleMaterialStats(0.3f, 0, 0, 0);
    HandleMaterialStats statsHandle2 = new HandleMaterialStats(0.7f, 0, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(List.of(statsHead), List.of(statsHandle1, statsHandle2), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().getInt(ToolStats.DURABILITY)).isEqualTo(100); // 200 * (0.3 + 0.7)/2
  }

  @Test
  void buildMiningSpeed_testHandleMiningSpeed_average() {
    HeadMaterialStats statsHead = new HeadMaterialStats(0, 5.0f, Tiers.WOOD, 0);
    HandleMaterialStats statsHandle1 = new HandleMaterialStats(0, 0.3f, 0, 0);
    HandleMaterialStats statsHandle2 = new HandleMaterialStats(0, 0.7f, 0, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(List.of(statsHead), List.of(statsHandle1, statsHandle2), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(2.5f); // 5 * (0.3 + 0.7)/2
  }

  @Test
  void buildMiningSpeed_ensureAverage() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 10, Tiers.WOOD, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 5, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.MINING_SPEED.update(builder, 10f);
    new MeleeHarvestToolStatsBuilder(List.of(stats1, stats2), Collections.emptyList(), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.MINING_SPEED)).isEqualTo(17.5f); // 10+(10+5)/2
  }

  @Test
  void buildAttack_ensureAverage() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 0, Tiers.WOOD, 5);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 0, Tiers.WOOD, 10);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_DAMAGE.update(builder, 10f);
    new MeleeHarvestToolStatsBuilder(List.of(stats1, stats2), Collections.emptyList(), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(17.5f); // 10+(10+5)/2
  }

  @Test
  void buildHarvestLevel_ensureMax() {
    HeadMaterialStats stats1 = new HeadMaterialStats(1, 1, Tiers.IRON, 0);
    HeadMaterialStats stats2 = new HeadMaterialStats(1, 1, Tiers.STONE, 0);
    HeadMaterialStats stats3 = new HeadMaterialStats(1, 1, Tiers.DIAMOND, 0);
    HeadMaterialStats stats4 = new HeadMaterialStats(1, 1, Tiers.WOOD, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(List.of(stats1, stats2, stats3, stats4), Collections.emptyList(), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.HARVEST_TIER)).isEqualTo(Tiers.DIAMOND);
  }

  @Test
  void buildAttackSpeed_set() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_SPEED.update(builder, 1.5f);
    new MeleeHarvestToolStatsBuilder(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()).addStats(builder);
    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f);
  }

  @Test
  void buildAttackSpeed_testHandleAttackDamage() {
    HeadMaterialStats head = new HeadMaterialStats(0, 0, Tiers.WOOD, 2);
    HandleMaterialStats stats = new HandleMaterialStats(0, 0, 0, 0.5f);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(List.of(head), List.of(stats), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(1.0f); // 2 * 0.5
  }

  @Test
  void buildAttackSpeed_testHandleAttackDamage_average() {
    HeadMaterialStats head = new HeadMaterialStats(0, 0, Tiers.WOOD, 4);
    HandleMaterialStats stats1 = new HandleMaterialStats(0, 0, 0, 1.3f);
    HandleMaterialStats stats2 = new HandleMaterialStats(0, 0, 0, 1.7f);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.ATTACK_DAMAGE.update(builder, 2f);
    new MeleeHarvestToolStatsBuilder(List.of(head), List.of(stats1, stats2), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.ATTACK_DAMAGE)).isEqualTo(9); // (4+2) * (1.3+1.7)/2
  }

  @Test
  void buildAttackSpeed_testHandleAttackSpeed() {
    HandleMaterialStats stats = new HandleMaterialStats(0, 0, 1.5f, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(Collections.emptyList(), List.of(stats), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f); // 1 * 1.5f
  }

  @Test
  void buildAttackSpeed_testHandleAttackSpeed_average() {
    HandleMaterialStats stats1 = new HandleMaterialStats(0, 0, 1.3f, 0);
    HandleMaterialStats stats2 = new HandleMaterialStats(0, 0, 1.7f, 0);

    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    new MeleeHarvestToolStatsBuilder(Collections.emptyList(), List.of(stats1, stats2), Collections.emptyList()).addStats(builder);

    assertThat(builder.build().get(ToolStats.ATTACK_SPEED)).isEqualTo(1.5f); // 1 * (1.3+1.7)/2
  }
}
