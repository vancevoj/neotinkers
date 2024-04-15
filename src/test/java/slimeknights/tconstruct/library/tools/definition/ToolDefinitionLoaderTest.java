package slimeknights.tconstruct.library.tools.definition;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
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
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.weapon.MeleeHitToolHook;
import slimeknights.tconstruct.library.tools.definition.module.weapon.SweepWeaponAttack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.test.JsonFileLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ToolDefinitionLoaderTest extends BaseMcTest {
  private static final ToolDefinitionData WRONG_DATA = ToolDefinitionDataBuilder.builder().stat(ToolStats.DURABILITY, 100).build();
  private static final JsonFileLoader fileLoader = new JsonFileLoader(JsonHelper.DEFAULT_GSON, ToolDefinitionLoader.FOLDER);
  private static final ToolDefinition NO_PARTS_MINIMAL = ToolDefinition.builder(TConstruct.getResource("minimal_no_parts")).noParts().build();
  private static final ToolDefinition NO_PARTS_FULL = ToolDefinition.builder(TConstruct.getResource("full_no_parts")).noParts().build();
  private static final ToolDefinition MELEE_HARVEST_MINIMAL = ToolDefinition.builder(TConstruct.getResource("minimal_with_parts")).meleeHarvest().build();
  private static final ToolDefinition MELEE_HARVEST_FULL = ToolDefinition.builder(TConstruct.getResource("full_with_parts")).meleeHarvest().build();
  private static final ToolDefinition HAS_PARTS_NO_NEED = ToolDefinition.builder(TConstruct.getResource("has_parts_no_need")).noParts().build();
  private static final ToolDefinition NEED_PARTS_HAS_NONE = ToolDefinition.builder(TConstruct.getResource("need_parts_has_none")).meleeHarvest().build();
  private static final ToolDefinition WRONG_PART_TYPE = ToolDefinition.builder(TConstruct.getResource("wrong_part_type")).meleeHarvest().build();

  @BeforeAll
  static void beforeAll() {
    RegistrationFixture.register(ToolModule.LOADER, "modifier_slots", ToolSlotsModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "traits", ToolTraitsModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "actions", ToolActionsModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "is_effective", IsEffectiveModule.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "circle", CircleAOEIterator.LOADER);
    RegistrationFixture.register(ToolModule.LOADER, "sweep", SweepWeaponAttack.LOADER);
  }

  /** Helper to do all the stats checks */
  private static void checkFullNonParts(ToolDefinitionData data) {
    // base stats
    assertThat(data.getAllBaseStats()).hasSize(4);
    assertThat(data.getAllBaseStats()).contains(ToolStats.DURABILITY);
    assertThat(data.getAllBaseStats()).contains(ToolStats.ATTACK_DAMAGE);
    assertThat(data.getAllBaseStats()).contains(ToolStats.ATTACK_SPEED);
    assertThat(data.getAllBaseStats()).contains(ToolStats.MINING_SPEED);
    assertThat(data.getBaseStat(ToolStats.DURABILITY)).isEqualTo(100f);
    assertThat(data.getBaseStat(ToolStats.ATTACK_DAMAGE)).isEqualTo(2.5f);
    assertThat(data.getBaseStat(ToolStats.ATTACK_SPEED)).isEqualTo(3.75f);
    assertThat(data.getBaseStat(ToolStats.MINING_SPEED)).isEqualTo(4f);
    // multiplier stats
    assertThat(data.multipliers.getContainedStats()).hasSize(3);
    assertThat(data.multipliers.getContainedStats()).contains(ToolStats.DURABILITY);
    assertThat(data.multipliers.getContainedStats()).contains(ToolStats.ATTACK_DAMAGE);
    assertThat(data.multipliers.getContainedStats()).contains(ToolStats.MINING_SPEED);
    assertThat(data.getMultiplier(ToolStats.DURABILITY)).isEqualTo(1.5f);
    assertThat(data.getMultiplier(ToolStats.ATTACK_DAMAGE)).isEqualTo(2f);
    assertThat(data.getMultiplier(ToolStats.MINING_SPEED)).isEqualTo(0.5f);
    // slots
    VolatileDataToolHook volatileHook = data.getHook(ToolHooks.VOLATILE_DATA);
    assertThat(volatileHook).isInstanceOf(ToolSlotsModule.class);
    Map<SlotType,Integer> slots = ((ToolSlotsModule) volatileHook).slots();
    assertThat(slots).hasSize(3);
    assertThat(slots).containsEntry(SlotType.UPGRADE, 3);
    assertThat(slots).containsEntry(SlotType.DEFENSE, 2);
    assertThat(slots).containsEntry(SlotType.ABILITY, 1);
    // traits
    List<ModifierEntry> traits = data.getHook(ToolHooks.TOOL_TRAITS).getTraits(Items.DIAMOND_PICKAXE, ToolDefinition.EMPTY);
    assertThat(traits).hasSize(2);
    assertThat(traits.get(0).getId()).isEqualTo(ModifierFixture.TEST_1);
    assertThat(traits.get(0).getLevel()).isEqualTo(1);
    assertThat(traits.get(1).getId()).isEqualTo(ModifierFixture.TEST_2);
    assertThat(traits.get(1).getLevel()).isEqualTo(3);
    // actions
    ToolActionToolHook actions = data.getHook(ToolHooks.TOOL_ACTION);
    assertThat(actions).isInstanceOf(ToolActionsModule.class);
    assertThat(((ToolActionsModule) actions).actions()).hasSize(2);
    IToolStackView tool = mock(IToolStackView.class);
    assertThat(data.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, ToolActions.AXE_DIG)).isTrue();
    assertThat(data.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, ToolAction.get("custom_action"))).isTrue();
    // harvest
    IsEffectiveToolHook harvestLogic = data.getHook(ToolHooks.IS_EFFECTIVE);
    assertThat(harvestLogic).isInstanceOf(IsEffectiveModule.class);
    assertThat(harvestLogic.isToolEffective(tool, Blocks.DIAMOND_BLOCK.defaultBlockState())).isTrue();
    // aoe
    AreaOfEffectIterator aoe = data.getHook(ToolHooks.AOE_ITERATOR);
    assertThat(aoe).isInstanceOf(CircleAOEIterator.class);
    assertThat(((CircleAOEIterator)aoe).diameter()).isEqualTo(3);
    assertThat(((CircleAOEIterator)aoe).is3D()).isTrue();
    // weapon
    MeleeHitToolHook attack = data.getHook(ToolHooks.MELEE_HIT);
    assertThat(attack).isInstanceOf(SweepWeaponAttack.class);
    assertThat(((SweepWeaponAttack)attack).range()).isEqualTo(5);
  }

  @BeforeAll
  static void setup() {
    SlotType.init();
    MaterialItemFixture.init();
    ModifierFixture.init();
  }

  @Test
  void noParts_minimal() {
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(NO_PARTS_MINIMAL.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));

    ToolDefinitionData data = NO_PARTS_MINIMAL.getData();
    assertThat(data).isNotNull();
    // will not be the empty instance, but will be filled with empty data
    assertThat(data).isNotSameAs(NO_PARTS_MINIMAL.getStatProvider().getDefaultData());
    ToolDefinitionDataTest.checkToolDataEmpty(data);
  }

  @Test
  void noParts_full() {
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(NO_PARTS_FULL.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));

    ToolDefinitionData data = NO_PARTS_FULL.getData();
    assertThat(data).isNotNull();
    assertThat(data).isNotSameAs(NO_PARTS_FULL.getStatProvider().getDefaultData());
    assertThat(data.getParts()).isEmpty();
    checkFullNonParts(data);
  }

  @Test
  void noParts_hasUnneededParts_defaults() {
    HAS_PARTS_NO_NEED.setData(WRONG_DATA); // set to wrong data to ensure something changes
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(HAS_PARTS_NO_NEED.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));
    assertThat(HAS_PARTS_NO_NEED.getData()).isSameAs(HAS_PARTS_NO_NEED.getStatProvider().getDefaultData());
  }

  @Test
  void missingStats_defaults() {
    NO_PARTS_FULL.setData(WRONG_DATA); // set to wrong data to ensure something changes
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(HAS_PARTS_NO_NEED.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));
    assertThat(NO_PARTS_FULL.getData()).isSameAs(NO_PARTS_FULL.getStatProvider().getDefaultData());
  }

  @Test
  void meleeHarvest_minimal() {
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(MELEE_HARVEST_MINIMAL.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));

    ToolDefinitionData data = MELEE_HARVEST_MINIMAL.getData();
    assertThat(data).isNotNull();
    // will not be the empty instance, but will be filled with empty data
    assertThat(data).isNotSameAs(MELEE_HARVEST_MINIMAL.getStatProvider().getDefaultData());
    assertThat(data.getParts()).isNotNull();
    assertThat(data.getParts()).hasSize(1);
    assertThat(data.getParts().get(0).getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HEAD);
    assertThat(data.getParts().get(0).getWeight()).isEqualTo(1);
    ToolDefinitionDataTest.checkToolDataNonPartsEmpty(data);
  }

  @Test
  void meleeHarvest_full() {
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(MELEE_HARVEST_FULL.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));

    ToolDefinitionData data = MELEE_HARVEST_FULL.getData();
    assertThat(data).isNotNull();
    assertThat(data).isNotSameAs(MELEE_HARVEST_FULL.getStatProvider().getDefaultData());
    assertThat(data.getParts()).hasSize(3);
    assertThat(data.getParts().get(0).getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_EXTRA);
    assertThat(data.getParts().get(0).getWeight()).isEqualTo(2);
    assertThat(data.getParts().get(1).getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HEAD);
    assertThat(data.getParts().get(1).getWeight()).isEqualTo(1);
    assertThat(data.getParts().get(2).getPart()).isEqualTo(MaterialItemFixture.MATERIAL_ITEM_HANDLE);
    assertThat(data.getParts().get(2).getWeight()).isEqualTo(3);
    checkFullNonParts(data);
  }

  @Test
  void meleeHarvest_missingParts_defaults() {
    NEED_PARTS_HAS_NONE.setData(WRONG_DATA); // set to wrong data to ensure something changes
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(NEED_PARTS_HAS_NONE.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));
    assertThat(NEED_PARTS_HAS_NONE.getData()).isSameAs(NEED_PARTS_HAS_NONE.getStatProvider().getDefaultData());
  }

  @Test
  void meleeHarvest_wrongPartType_defaults() {
    WRONG_PART_TYPE.setData(WRONG_DATA); // set to wrong data to ensure something changes
    Map<ResourceLocation,JsonElement> splashList = fileLoader.loadFilesAsSplashlist(WRONG_PART_TYPE.getId().getPath());
    ToolDefinitionLoader.getInstance().apply(splashList, mock(ResourceManager.class), mock(ProfilerFiller.class));
    assertThat(WRONG_PART_TYPE.getData()).isSameAs(WRONG_PART_TYPE.getStatProvider().getDefaultData());
  }
}
