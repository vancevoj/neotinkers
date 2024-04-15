package slimeknights.tconstruct.library.tools.definition;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.fixture.RegistrationFixture;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierFixture;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.CircleAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionToolHook;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolSlotsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.VolatileDataToolHook;
import slimeknights.tconstruct.library.tools.definition.module.material.PartStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.PartStatsModule.WeightedPart;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.weapon.MeleeHitToolHook;
import slimeknights.tconstruct.library.tools.definition.module.weapon.SweepWeaponAttack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UpdateToolDefinitionDataPacketTest extends BaseMcTest {
  private static final ResourceLocation EMPTY_ID = new ResourceLocation("test", "empty");
  private static final ResourceLocation FILLED_ID = new ResourceLocation("test", "filled");

  @BeforeAll
  static void initialize() {
    MaterialItemFixture.init();
    ModifierFixture.init();
    RegistrationFixture.register(ToolModule.LOADER, "slots", ToolSlotsModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "is_effective", IsEffectiveModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "circle", CircleAOEIterator.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "sweep", SweepWeaponAttack.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "traits", ToolTraitsModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "actions", ToolActionsModule.LOADER);
  }

  @Test
  void testGenericEncodeDecode() {
    ToolDefinitionData empty = ToolDefinitionData.EMPTY;
    ToolDefinitionData filled = ToolDefinitionDataBuilder
      .builder()
      // parts
      .module(PartStatsModule.meleeHarvest()
                             .part(MaterialItemFixture.MATERIAL_ITEM_HEAD, 10)
                             .part(MaterialItemFixture.MATERIAL_ITEM_HANDLE).build())
      // stats
      .stat(ToolStats.DURABILITY, 1000)
      .stat(ToolStats.ATTACK_DAMAGE, 152.5f)
      .multiplier(ToolStats.MINING_SPEED, 10)
      .multiplier(ToolStats.ATTACK_SPEED, 0.5f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1)
      .module(ToolSlotsModule.builder().slots(SlotType.UPGRADE, 5).slots(SlotType.ABILITY, 8).build())
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierFixture.TEST_1, 10).build())
      .module(ToolActionsModule.of(ToolActions.AXE_DIG, ToolActions.SHOVEL_FLATTEN))
      // behavior
      .module(new IsEffectiveModule(BlockPredicate.set(Blocks.GRANITE)))
      .module(new CircleAOEIterator(7, true))
      .module(new SweepWeaponAttack(4))
      .build();

    // send a packet over the buffer
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    UpdateToolDefinitionDataPacket packetToEncode = new UpdateToolDefinitionDataPacket(ImmutableMap.of(EMPTY_ID, empty, FILLED_ID, filled));
    packetToEncode.encode(buffer);
    UpdateToolDefinitionDataPacket decoded = new UpdateToolDefinitionDataPacket(buffer);

    // parse results
    Map<ResourceLocation,ToolDefinitionData> parsedMap = decoded.getDataMap();
    assertThat(parsedMap).hasSize(2);

    // first validate empty
    ToolDefinitionData parsed = parsedMap.get(EMPTY_ID);
    assertThat(parsed).isNotNull();
    // no parts
    assertThat(parsed.getHook(ToolHooks.MATERIAL_STATS).getStatTypes(ToolDefinition.EMPTY)).isEmpty();
    // no stats
    assertThat(parsed.baseStats.getContainedStats()).isEmpty();
    assertThat(parsed.multipliers.getContainedStats()).isEmpty();
    // no slots
    assertThat(parsed.getHook(ToolHooks.VOLATILE_DATA)).isNotInstanceOf(ToolSlotsModule.class);
    // no traits
    assertThat(parsed.getHook(ToolHooks.TOOL_TRAITS).getTraits(ToolDefinition.EMPTY)).isEmpty();

    // next, validate the filled one
    parsed = parsedMap.get(FILLED_ID);
    assertThat(parsed).isNotNull();

    // parts
    ToolPartsHook toolPartsHook = parsed.getHook(ToolHooks.TOOL_PARTS);
    assertThat(toolPartsHook).isInstanceOf(PartStatsModule.class);
    List<WeightedPart> parts = ((PartStatsModule)toolPartsHook).getParts();
    assertThat(parts).hasSize(2);
    assertThat(parts.get(0).part()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HEAD);
    assertThat(parts.get(0).weight()).isEqualTo(10);
    assertThat(parts.get(1).part()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HANDLE);
    assertThat(parts.get(1).weight()).isEqualTo(1);

    // stats
    assertThat(parsed.baseStats.getContainedStats()).hasSize(2);
    assertThat(parsed.baseStats.getContainedStats()).contains(ToolStats.DURABILITY);
    assertThat(parsed.baseStats.getContainedStats()).contains(ToolStats.ATTACK_DAMAGE);
    assertThat(parsed.baseStats.get(ToolStats.DURABILITY)).isEqualTo(1000);
    assertThat(parsed.baseStats.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(152.5f);
    assertThat(parsed.baseStats.get(ToolStats.ATTACK_SPEED)).isEqualTo(ToolStats.ATTACK_SPEED.getDefaultValue());

    assertThat(parsed.multipliers.getContainedStats()).hasSize(2); // attack damage is 1, so its skipped
    assertThat(parsed.multipliers.getContainedStats()).contains(ToolStats.ATTACK_SPEED);
    assertThat(parsed.multipliers.getContainedStats()).contains(ToolStats.MINING_SPEED);
    assertThat(parsed.multipliers.get(ToolStats.MINING_SPEED)).isEqualTo(10);
    assertThat(parsed.multipliers.get(ToolStats.ATTACK_SPEED)).isEqualTo(0.5f);
    assertThat(parsed.multipliers.get(ToolStats.ATTACK_DAMAGE)).isEqualTo(1);
    assertThat(parsed.multipliers.get(ToolStats.DURABILITY)).isEqualTo(1);

    // slots
    VolatileDataToolHook volatileHook = parsed.getHook(ToolHooks.VOLATILE_DATA);
    assertThat(volatileHook).isInstanceOf(ToolSlotsModule.class);
    Map<SlotType,Integer> slots = ((ToolSlotsModule) volatileHook).slots();
    assertThat(slots).hasSize(2);
    assertThat(slots).containsEntry(SlotType.UPGRADE, 5);
    assertThat(slots).containsEntry(SlotType.ABILITY, 8);

    // traits
    List<ModifierEntry> traits = parsed.getHook(ToolHooks.TOOL_TRAITS).getTraits(ToolDefinition.EMPTY);
    assertThat(traits).hasSize(1);
    assertThat(traits.get(0).getModifier()).isEqualTo(ModifierFixture.TEST_MODIFIER_1);
    assertThat(traits.get(0).getLevel()).isEqualTo(10);

    // actions
    ToolActionToolHook actionModule = parsed.getHook(ToolHooks.TOOL_ACTION);
    assertThat(actionModule).isInstanceOf(ToolActionsModule.class);
    assertThat(((ToolActionsModule) actionModule).actions()).hasSize(2);
    IToolStackView tool = mock(IToolStackView.class);
    assertThat(parsed.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, ToolActions.AXE_DIG)).isTrue();
    assertThat(parsed.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, ToolActions.SHOVEL_FLATTEN)).isTrue();

    // harvest
    IsEffectiveToolHook harvestLogic = parsed.getHook(ToolHooks.IS_EFFECTIVE);
    assertThat(harvestLogic).isInstanceOf(IsEffectiveModule.class);
    assertThat(harvestLogic.isToolEffective(mock(IToolStackView.class), Blocks.GRANITE.defaultBlockState())).isTrue();

    // aoe
    AreaOfEffectIterator aoe = parsed.getHook(ToolHooks.AOE_ITERATOR);
    assertThat(aoe).isInstanceOf(CircleAOEIterator.class);
    assertThat(((CircleAOEIterator)aoe).diameter()).isEqualTo(7);
    assertThat(((CircleAOEIterator)aoe).is3D()).isTrue();

    // weapon
    MeleeHitToolHook attack = parsed.getHook(ToolHooks.MELEE_HIT);
    assertThat(attack).isInstanceOf(SweepWeaponAttack.class);
    assertThat(((SweepWeaponAttack)attack).range()).isEqualTo(4);
  }
}
