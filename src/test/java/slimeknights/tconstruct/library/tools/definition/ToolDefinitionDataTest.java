package slimeknights.tconstruct.library.tools.definition;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ToolActions;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.context.ToolRebuildContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolSlotsModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ToolDefinitionDataTest extends BaseMcTest {
  /** Checks that the stats are all empty */
  protected static void checkStatsEmpty(ToolDefinitionData data) {
    assertThat(data.baseStats).isNotNull();
    assertThat(data.baseStats.getContainedStats()).isEmpty();
    assertThat(data.multipliers).isNotNull();
    assertThat(data.multipliers.getContainedStats()).isEmpty();
  }

  /** Checks that the stats are all empty */
  protected static void checkToolDataNonPartsEmpty(ToolDefinitionData data) {
    checkStatsEmpty(data);
    assertThat(data.getHook(ToolHooks.TOOL_TRAITS).getTraits(Items.DIAMOND_PICKAXE, ToolDefinition.EMPTY)).isEmpty();
  }

  /** Checks that the stats are all empty */
  protected static void checkToolDataEmpty(ToolDefinitionData data) {
    assertThat(data.getParts()).isNotNull();
    assertThat(data.getParts()).isEmpty();
    checkToolDataNonPartsEmpty(data);
    assertThat(data.getHooks().getAllModules()).isEmpty();
  }

  @Test
  void data_emptyContainsNoData() {
    checkToolDataEmpty(ToolDefinitionData.EMPTY);
  }

  @Test
  void data_getStatBonus() {
    assertThat(ToolDefinitionData.EMPTY.getAllBaseStats()).isEmpty();
    ToolDefinitionData withBonuses = ToolDefinitionDataBuilder
      .builder()
      .stat(ToolStats.DURABILITY, 100)
      .stat(ToolStats.ATTACK_SPEED, 5.5f)
      .build();

    // ensure stats are in the right place
    assertThat(withBonuses.getAllBaseStats()).hasSize(2);
    assertThat(withBonuses.multipliers.getContainedStats()).isEmpty();
    assertThat(withBonuses.getBaseStat(ToolStats.DURABILITY)).isEqualTo(100);
    assertThat(withBonuses.getBaseStat(ToolStats.ATTACK_SPEED)).isEqualTo(5.5f);
    // note mining speed was chosen as it has a non-zero default
    assertThat(withBonuses.getBaseStat(ToolStats.MINING_SPEED)).isEqualTo(ToolStats.MINING_SPEED.getDefaultValue());
  }

  @Test
  void data_getStatMultiplier() {
    ToolDefinitionData withMultipliers = ToolDefinitionDataBuilder
      .builder()
      .multiplier(ToolStats.DURABILITY, 10)
      .multiplier(ToolStats.ATTACK_SPEED, 2.5f)
      .build();

    // ensure stats are in the right place
    assertThat(withMultipliers.getAllBaseStats()).isEmpty();
    assertThat(withMultipliers.multipliers.getContainedStats()).hasSize(2);
    assertThat(withMultipliers.getMultiplier(ToolStats.DURABILITY)).isEqualTo(10);
    assertThat(withMultipliers.getMultiplier(ToolStats.ATTACK_SPEED)).isEqualTo(2.5f);
    assertThat(withMultipliers.getMultiplier(ToolStats.MINING_SPEED)).isEqualTo(1);
  }

  @Test
  void data_buildStats_empty() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.MINING_SPEED.add(builder, 5);
    ToolStats.ATTACK_DAMAGE.add(builder, 3);
    ToolDefinitionData.EMPTY.buildStatMultipliers(builder);

    StatsNBT stats = builder.build(StatsNBT.EMPTY);
    assertThat(stats.getContainedStats()).hasSize(2);
    assertThat(stats.getContainedStats()).contains(ToolStats.MINING_SPEED);
    assertThat(stats.getContainedStats()).contains(ToolStats.ATTACK_DAMAGE);
    assertThat(stats.get(ToolStats.MINING_SPEED)).isEqualTo(6);
    assertThat(stats.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(3);
  }

  @Test
  void data_buildStats_withData() {
    ModifierStatsBuilder builder = ModifierStatsBuilder.builder();
    ToolStats.MINING_SPEED.add(builder, 5);
    ToolStats.DURABILITY.add(builder, 100);

    ToolDefinitionData data = ToolDefinitionDataBuilder
      .builder()
      .multiplier(ToolStats.MINING_SPEED, 5)
      .multiplier(ToolStats.ATTACK_SPEED, 2)
      .build();
    data.buildStatMultipliers(builder);

    StatsNBT stats = builder.build(StatsNBT.EMPTY);
    assertThat(stats.getContainedStats()).hasSize(3);
    assertThat(stats.getContainedStats()).contains(ToolStats.DURABILITY);
    assertThat(stats.getContainedStats()).contains(ToolStats.MINING_SPEED);
    assertThat(stats.getContainedStats()).contains(ToolStats.ATTACK_SPEED);
    assertThat(stats.get(ToolStats.MINING_SPEED)).isEqualTo(30);
    assertThat(stats.get(ToolStats.DURABILITY)).isEqualTo(101);
    assertThat(stats.get(ToolStats.ATTACK_SPEED)).isEqualTo(2);
  }

  @Test
  void data_buildSlots_withData() {
    ModDataNBT modData = new ModDataNBT();
    ToolDefinitionData data = ToolDefinitionDataBuilder
      .builder()
      .module(new ToolSlotsModule(ImmutableMap.of(SlotType.UPGRADE, 5, SlotType.ABILITY, 2)))
      .build();
    data.getHook(ToolHooks.VOLATILE_DATA).addVolatileData(mock(ToolRebuildContext.class), modData);
    assertThat(modData.getSlots(SlotType.UPGRADE)).isEqualTo(5);
    assertThat(modData.getSlots(SlotType.ABILITY)).isEqualTo(2);
    for (SlotType type : SlotType.getAllSlotTypes()) {
      if (type != SlotType.UPGRADE && type != SlotType.ABILITY) {
        assertThat(modData.getSlots(type)).overridingErrorMessage("Slot type %s has a value", type.getName()).isEqualTo(0);
      }
    }

    // packet buffers handled in packet test
  }

  @Test
  void actions_canPerform() {
    IToolStackView context = mock(IToolStackView.class);
    assertThat(ToolDefinitionData.EMPTY.getHook(ToolHooks.TOOL_ACTION).canPerformAction(context, ToolActions.SHOVEL_FLATTEN)).isFalse();
    assertThat(ToolDefinitionData.EMPTY.getHook(ToolHooks.TOOL_ACTION).canPerformAction(context, ToolActions.SWORD_DIG)).isFalse();
    ToolDefinitionData newData = ToolDefinitionDataBuilder.builder().module(ToolActionsModule.of(ToolActions.SHOVEL_FLATTEN)).build();
    assertThat(newData.getHook(ToolHooks.TOOL_ACTION).canPerformAction(context, ToolActions.SHOVEL_FLATTEN)).isTrue();
    assertThat(newData.getHook(ToolHooks.TOOL_ACTION).canPerformAction(context, ToolActions.SWORD_DIG)).isFalse();
  }
}
