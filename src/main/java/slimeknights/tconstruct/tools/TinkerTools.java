package slimeknights.tconstruct.tools;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.config.ConfigurableAction;
import slimeknights.tconstruct.common.data.tags.MaterialTagProvider;
import slimeknights.tconstruct.library.client.data.material.GeneratorPartTextureJsonGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPaletteDebugGenerator;
import slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator;
import slimeknights.tconstruct.library.client.model.data.TinkerClientItemProvider;
import slimeknights.tconstruct.library.json.loot.AddToolDataFunction;
import slimeknights.tconstruct.library.json.predicate.tool.HasMaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasModifierPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasStatTypePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasToolHookPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.PersistentDataPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.StatInRangePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.StatInSetPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolActionPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolStackItemPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolStackPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolVariableRangePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.VolatileDataPredicate;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.modules.capacity.OverslimeModule;
import slimeknights.tconstruct.library.modifiers.modules.interaction.edible.EdibleModule;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.library.json.condition.TagDifferencePresentCondition;
import slimeknights.tconstruct.library.json.condition.TagIntersectionPresentCondition;
import slimeknights.tconstruct.library.json.condition.TagNotEmptyCondition;
import slimeknights.tconstruct.library.recipe.ingredient.BlockTagIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.InstrumentIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.ToolHookIngredient;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.capability.BlockItemProviderModifierHook;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.ToolEnergyCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.BoxAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.CircleAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.ConditionalAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.TreeAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.VeiningAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.build.MultiplyStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.SetStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolSlotsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.VolatileFlagModule;
import slimeknights.tconstruct.library.tools.definition.module.build.VolatileIntModule;
import slimeknights.tconstruct.library.tools.definition.module.display.CustomMaterialName;
import slimeknights.tconstruct.library.tools.definition.module.display.FixedMaterialToolName;
import slimeknights.tconstruct.library.tools.definition.module.display.MaterialToolNameModule;
import slimeknights.tconstruct.library.tools.definition.module.display.SimpleToolName;
import slimeknights.tconstruct.library.tools.definition.module.display.StatTypesToolNameModule;
import slimeknights.tconstruct.library.tools.definition.module.display.UniqueMaterialToolName;
import slimeknights.tconstruct.library.tools.definition.module.interaction.AttackInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.PreferenceSetInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.ToggleableSetInteraction;
import slimeknights.tconstruct.library.tools.definition.module.material.DefaultMaterialsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialRepairModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.MaterialTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.PartStatsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.PartsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.RemappingMaterialsModule;
import slimeknights.tconstruct.library.tools.definition.module.material.StatlessPartRepairModule;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.MaxTierModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedModifierModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.OneClickBreakModule;
import slimeknights.tconstruct.library.tools.definition.module.weapon.CircleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.ParticleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.SweepWeaponAttack;
import slimeknights.tconstruct.library.tools.helper.ModifierLootingHandler;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.ModifiableArrowItem;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ModifiableShurikenItem;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.item.armor.MultilayerArmorItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableBowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.BlockSideHitListener;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.tools.data.ArmorModelProvider;
import slimeknights.tconstruct.tools.data.ModifierIds;
import slimeknights.tconstruct.tools.data.StationSlotLayoutProvider;
import slimeknights.tconstruct.tools.data.ToolDefinitionDataProvider;
import slimeknights.tconstruct.tools.data.ToolItemModelProvider;
import slimeknights.tconstruct.tools.data.ToolsRecipeProvider;
import slimeknights.tconstruct.tools.data.client.ModifierModelMapProvider;
import slimeknights.tconstruct.tools.data.material.MaterialDataProvider;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.tools.data.material.MaterialRecipeProvider;
import slimeknights.tconstruct.tools.data.material.MaterialRenderInfoProvider;
import slimeknights.tconstruct.tools.data.material.MaterialStatsDataProvider;
import slimeknights.tconstruct.tools.data.material.MaterialTraitsDataProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerMaterialSpriteProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerPartSpriteProvider;
import slimeknights.tconstruct.tools.data.sprite.TinkerTrimMaterialPaletteGenerator;
import slimeknights.tconstruct.tools.entity.CombatFishingHook;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;
import slimeknights.tconstruct.tools.entity.ThrownTool;
import slimeknights.tconstruct.tools.item.CrystalshotItem;
import slimeknights.tconstruct.tools.item.CrystalshotItem.CrystalshotEntity;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;
import slimeknights.tconstruct.tools.item.SlimeskullItem;
import slimeknights.tconstruct.tools.logic.EquipmentChangeWatcher;
import slimeknights.tconstruct.tools.logic.ModifiableArrowDispenserBehavior;
import slimeknights.tconstruct.tools.logic.ModifiableShurikenDispenserBehavior;
import slimeknights.tconstruct.tools.menu.ToolContainerMenu;
import slimeknights.tconstruct.tools.modules.MeltingFluidEffectiveModule;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Contains all complete tool items
 */
public final class TinkerTools extends TinkerModule {
  public TinkerTools() {
    SlotType.init();
    BlockSideHitListener.init();
    ModifierLootingHandler.init();
    RandomMaterial.init();
    // register the equipment watcher attachment on the mod bus during construction (DeferredRegisters must be added before registry events fire)
    EquipmentChangeWatcher.register(ModLoadingContext.get().getActiveContainer().getEventBus());
  }

  /** Creative tab for complete tools */
  public static final DeferredHolder<? super CreativeModeTab, CreativeModeTab> tabTools = CREATIVE_TABS.register(
    "tools", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "tools"))
      .icon(() -> TinkerTools.pickaxe.get().getRenderTool())
      .displayItems(dedupeTab(TinkerTools::addTabItems))
      .withTabsBefore(TinkerCommons.tabGeneral.getId())
      .withSearchBar()
      .build());

  /** Loot function type for tool add data */
  public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<AddToolDataFunction>> lootAddToolData = LOOT_FUNCTIONS.register("add_tool_data", () -> AddToolDataFunction.CODEC);

  /*
   * Items
   */
  public static final ItemObject<ModifiableItem> pickaxe = ITEMS.register("pickaxe", p -> new ModifiableItem(p, ToolDefinitions.PICKAXE), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> sledgeHammer = ITEMS.register("sledge_hammer", p -> new ModifiableItem(p, ToolDefinitions.SLEDGE_HAMMER), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> veinHammer = ITEMS.register("vein_hammer", p -> new ModifiableItem(p, ToolDefinitions.VEIN_HAMMER), UNSTACKABLE_PROPS);

  public static final ItemObject<ModifiableItem> mattock = ITEMS.register("mattock", p -> new ModifiableItem(p, ToolDefinitions.MATTOCK), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> pickadze = ITEMS.register("pickadze", p -> new ModifiableItem(p, ToolDefinitions.PICKADZE), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> excavator = ITEMS.register("excavator", p -> new ModifiableItem(p, ToolDefinitions.EXCAVATOR), UNSTACKABLE_PROPS);

  public static final ItemObject<ModifiableItem> handAxe = ITEMS.register("hand_axe", p -> new ModifiableItem(p, ToolDefinitions.HAND_AXE), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> broadAxe = ITEMS.register("broad_axe", p -> new ModifiableItem(p, ToolDefinitions.BROAD_AXE), UNSTACKABLE_PROPS);

  public static final ItemObject<ModifiableItem> kama = ITEMS.register("kama", p -> new ModifiableItem(p, ToolDefinitions.KAMA), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> scythe = ITEMS.register("scythe", p -> new ModifiableItem(p, ToolDefinitions.SCYTHE), UNSTACKABLE_PROPS);

  // TieredItem is gone so no durability workaround is needed any more, just allow stacking 2 daggers (ModifiableItem limits damaged daggers to 1)
  public static final ItemObject<ModifiableItem> dagger = ITEMS.register("dagger", p -> new ModifiableSwordItem(p, ToolDefinitions.DAGGER, 2), p -> p.stacksTo(2));
  public static final ItemObject<ModifiableItem> sword = ITEMS.register("sword", p -> new ModifiableSwordItem(p, ToolDefinitions.SWORD), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> cleaver = ITEMS.register("cleaver", p -> new ModifiableSwordItem(p, ToolDefinitions.CLEAVER), UNSTACKABLE_PROPS);

  public static final ItemObject<ModifiableCrossbowItem> crossbow = ITEMS.register("crossbow", p -> new ModifiableCrossbowItem(p, ToolDefinitions.CROSSBOW), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableBowItem> longbow = ITEMS.register("longbow", p -> new ModifiableBowItem(p, ToolDefinitions.LONGBOW, true), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> fishingRod = ITEMS.register("fishing_rod", p -> new ModifiableItem(p, ToolDefinitions.FISHING_ROD), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> javelin = ITEMS.register("javelin", p -> new ModifiableItem(p, ToolDefinitions.JAVELIN), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableArrowItem> arrow = ITEMS.register("arrow", p -> new ModifiableArrowItem(p, ToolDefinitions.ARROW), ITEM_PROPS);
  public static final ItemObject<ModifiableShurikenItem> shuriken = ITEMS.register("shuriken", p -> new ModifiableShurikenItem(p, ToolDefinitions.SHURIKEN), p -> p.stacksTo(16));
  public static final ItemObject<ModifiableShurikenItem> throwingAxe = ITEMS.register("throwing_axe", p -> new ModifiableShurikenItem(p, ToolDefinitions.THROWING_AXE), p -> p.stacksTo(16));

  public static final ItemObject<ModifiableItem> flintAndBrick = ITEMS.register("flint_and_brick", p -> new ModifiableItem(p, ToolDefinitions.FLINT_AND_BRICK), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> skyStaff = ITEMS.register("sky_staff", p -> new ModifiableItem(p, ToolDefinitions.SKY_STAFF), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> earthStaff = ITEMS.register("earth_staff", p -> new ModifiableItem(p, ToolDefinitions.EARTH_STAFF), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> ichorStaff = ITEMS.register("ichor_staff", p -> new ModifiableItem(p, ToolDefinitions.ICHOR_STAFF), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> enderStaff = ITEMS.register("ender_staff", p -> new ModifiableItem(p, ToolDefinitions.ENDER_STAFF), UNSTACKABLE_PROPS);

  // ancient
  public static final ItemObject<ModifiableItem> meltingPan = ITEMS.register("melting_pan", p -> new ModifiableItem(p, ToolDefinitions.MELTING_PAN), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableCrossbowItem> warPick = ITEMS.register("war_pick", p -> new ModifiableCrossbowItem(p, ToolDefinitions.WAR_PICK), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> battlesign = ITEMS.register("battlesign", p -> new ModifiableItem(p, ToolDefinitions.BATTLESIGN), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> swasher = ITEMS.register("swasher", p -> new ModifiableItem(p, ToolDefinitions.SWASHER), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> minotaurAxe;
  static {
    // conditionally register minotaur axe as it's the easiest way to keep it out of JEI display
    if (ModList.get().isLoaded("twilightforest")) {
      minotaurAxe = ITEMS.register("minotaur_axe", p -> new ModifiableItem(p, ToolDefinitions.MINOTAUR_AXE), UNSTACKABLE_PROPS);
    } else {
      minotaurAxe = new ItemObject<>(DeferredHolder.<Item, ModifiableItem>create(Registries.ITEM, getResource("minotaur_axe")));
    }
  }

  // armor
  public static final EnumObject<ArmorType,ModifiableArmorItem> travelersGear = ITEMS.registerEnum("travelers", ModifiableArmorMaterial.ARMOR_TYPES, (type, p) -> new MultilayerArmorItem(ArmorDefinitions.TRAVELERS, type, p), UNSTACKABLE_PROPS);
  public static final EnumObject<ArmorType,ModifiableArmorItem> plateArmor = ITEMS.registerEnum("plate", ModifiableArmorMaterial.ARMOR_TYPES, (type, p) -> new MultilayerArmorItem(ArmorDefinitions.PLATE, type, p), UNSTACKABLE_PROPS);
  public static final EnumObject<ArmorType,ModifiableArmorItem> slimesuit = new EnumObject.Builder<ArmorType,ModifiableArmorItem>(ArmorType.class)
    .put(ArmorType.HELMET, ITEMS.register("slime_helmet", p -> new SlimeskullItem(ArmorDefinitions.SLIMESUIT, SlimeskullItem.MODEL_LOCATION, p), UNSTACKABLE_PROPS))
    // TODO 1.21: rename to slime chestplate as we no longer need the migration
    .put(ArmorType.CHESTPLATE, ITEMS.register("slimy_chestplate", p -> new MultilayerArmorItem(ArmorDefinitions.SLIMESUIT, ArmorType.CHESTPLATE, p), UNSTACKABLE_PROPS))
    .putAll(ITEMS.registerEnum("slime", new ArmorType[] {ArmorType.LEGGINGS, ArmorType.BOOTS}, (type, p) -> new MultilayerArmorItem(ArmorDefinitions.SLIMESUIT, type, p), UNSTACKABLE_PROPS))
    .build();
  public static final ItemObject<MultilayerArmorItem> slimeWings = ITEMS.register("slime_wings", p -> new MultilayerArmorItem(ArmorDefinitions.SLIMESUIT, ArmorType.CHESTPLATE, p, ArmorDefinitions.SLIME_WINGS, TinkerTools.slimeWings.getId()), UNSTACKABLE_PROPS);

  // shields
  public static final ItemObject<ModifiableItem> travelersShield = ITEMS.register("travelers_shield", p -> new ModifiableItem(p, ArmorDefinitions.TRAVELERS_SHIELD), UNSTACKABLE_PROPS);
  public static final ItemObject<ModifiableItem> plateShield = ITEMS.register("plate_shield", p -> new ModifiableItem(p, ArmorDefinitions.PLATE_SHIELD), UNSTACKABLE_PROPS);

  // arrows
  public static final ItemObject<ArrowItem> crystalshotItem = ITEMS.register("crystalshot", CrystalshotItem::new, ITEM_PROPS);

  /* Particles */
  public static final DeferredHolder<? super SimpleParticleType, SimpleParticleType> hammerAttackParticle = PARTICLE_TYPES.register("hammer_attack", () -> new SimpleParticleType(true));
  public static final DeferredHolder<? super SimpleParticleType, SimpleParticleType> axeAttackParticle = PARTICLE_TYPES.register("axe_attack", () -> new SimpleParticleType(true));
  public static final DeferredHolder<? super SimpleParticleType, SimpleParticleType> bonkAttackParticle = PARTICLE_TYPES.register("bonk", () -> new SimpleParticleType(true));

  /* Entities */
  public static final DeferredHolder<? super EntityType<IndestructibleItemEntity>, EntityType<IndestructibleItemEntity>> indestructibleItem = ENTITIES.register("indestructible_item", () ->
    EntityType.Builder.<IndestructibleItemEntity>of(IndestructibleItemEntity::new, MobCategory.MISC)
                      .sized(0.25F, 0.25F)
                      .fireImmune());
  public static final DeferredHolder<? super EntityType<CrystalshotEntity>, EntityType<CrystalshotEntity>> crystalshotEntity = ENTITIES.register("crystalshot", () ->
    EntityType.Builder.<CrystalshotEntity>of(CrystalshotEntity::new, MobCategory.MISC)
                      .sized(0.5F, 0.5F)
                      .clientTrackingRange(4)
                      .updateInterval(20));
  public static final DeferredHolder<? super EntityType<CombatFishingHook>, EntityType<CombatFishingHook>> fishingHook = ENTITIES.register("fishing_bobber", () -> EntityType.Builder.<CombatFishingHook>of(CombatFishingHook::new, MobCategory.MISC).noSave().noSummon().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(5));
  public static final DeferredHolder<? super EntityType<ModifiableArrow>, EntityType<ModifiableArrow>> materialArrow = ENTITIES.register("arrow", () -> EntityType.Builder.<ModifiableArrow>of(ModifiableArrow::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));
  public static final DeferredHolder<? super EntityType<ThrownShuriken>, EntityType<ThrownShuriken>> thrownShuriken = ENTITIES.register("thrown_shuriken", () -> EntityType.Builder.<ThrownShuriken>of(ThrownShuriken::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
  public static final DeferredHolder<? super EntityType<ThrownTool>, EntityType<ThrownTool>> thrownTool = ENTITIES.register("thrown_tool", () -> EntityType.Builder.<ThrownTool>of(ThrownTool::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(20));
  static {
    // used for the fishing bobber
    DATA_SERIALIZERS.register("material_variant", () -> MaterialVariantId.DATA_ACCESSOR);
  }


  /* Containers */
  public static final DeferredHolder<? super MenuType<ToolContainerMenu>, MenuType<ToolContainerMenu>> toolContainer = MENUS.register("tool_container", ToolContainerMenu::forClient);


  /*
   * Events
   */

  @SubscribeEvent
  void commonSetup(FMLCommonSetupEvent event) {
    ToolCapabilityProvider.register(ToolFluidCapability.Provider::new);
    ToolCapabilityProvider.register(ToolInventoryCapability.Provider::new);
    ToolCapabilityProvider.register((stack, tool) -> new ToolEnergyCapability.Provider(tool));
    ToolCapabilityProvider.register((stack, tool) -> new BlockItemProviderModifierHook.Provider(tool));
    for (ConfigurableAction action : Config.COMMON.toolTweaks) {
      event.enqueueWork(action);
    }
    event.enqueueWork(() -> {
      DispenserBlock.registerBehavior(TinkerTools.arrow.get(), ModifiableArrowDispenserBehavior.INSTANCE);
      DispenserBlock.registerBehavior(TinkerTools.shuriken.get(), ModifiableShurikenDispenserBehavior.INSTANCE);
      DispenserBlock.registerBehavior(TinkerTools.throwingAxe.get(), ModifiableShurikenDispenserBehavior.INSTANCE);
      ModifierUtil.registerShieldDisabler(entity -> {
        // Player#disableShield is gone, the blocking item component now handles disabling (5 seconds matches the old 100 tick cooldown)
        if (entity instanceof Player player && player.level() instanceof ServerLevel level) {
          ItemStack blockingWith = player.getItemBlockingWith();
          if (blockingWith != null) {
            BlocksAttacks blocksAttacks = blockingWith.get(DataComponents.BLOCKS_ATTACKS);
            if (blocksAttacks != null) {
              blocksAttacks.disable(level, player, 5.0f, blockingWith);
            }
          }
        }
      }, EntityTypes.PLAYER);
    });
    ModifierHooks.init();
    ToolHooks.init();
  }

  @SubscribeEvent
  void registerIngredientTypes(RegisterEvent event) {
    // 1.21: custom ingredients are registered as IngredientType on the NeoForge registry (was CraftingHelper.register)
    if (event.getRegistryKey() == NeoForgeRegistries.Keys.INGREDIENT_TYPES) {
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ToolHookIngredient.ID, () -> ToolHookIngredient.TYPE);
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, getResource("no_container"), () -> NoContainerIngredient.TYPE);
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, getResource("block_tag"), () -> BlockTagIngredient.TYPE);
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, getResource("material"), () -> MaterialIngredient.TYPE);
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, getResource("material_value"), () -> MaterialValueIngredient.TYPE);
      event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, InstrumentIngredient.ID, () -> InstrumentIngredient.TYPE);
    }
    // 1.21: custom ICondition codecs register on CONDITION_CODECS (was CraftingHelper.register)
    if (event.getRegistryKey() == NeoForgeRegistries.Keys.CONDITION_CODECS) {
      event.register(NeoForgeRegistries.Keys.CONDITION_CODECS, ConfigEnabledCondition.ID, () -> ConfigEnabledCondition.CODEC);
      event.register(NeoForgeRegistries.Keys.CONDITION_CODECS, TagIntersectionPresentCondition.NAME, () -> TagIntersectionPresentCondition.CODEC);
      event.register(NeoForgeRegistries.Keys.CONDITION_CODECS, TagDifferencePresentCondition.NAME, () -> TagDifferencePresentCondition.CODEC);
      // tconstruct:tag_not_empty is used by every book's index.json (section visibility); without this
      // the FileRepository fails to deserialize and the book breaks ("Unknown registry key ... tag_not_empty").
      event.register(NeoForgeRegistries.Keys.CONDITION_CODECS, TagNotEmptyCondition.NAME, () -> TagNotEmptyCondition.CODEC);
    }
    // tool item component predicate type, used by tool advancements to match Tinker tools (item sub predicates became component predicates in 1.21.5)
    if (event.getRegistryKey() == Registries.DATA_COMPONENT_PREDICATE_TYPE) {
      event.register(Registries.DATA_COMPONENT_PREDICATE_TYPE, ToolStackItemPredicate.ID, () -> ToolStackItemPredicate.TYPE);
    }
  }

  @SubscribeEvent
  void registerRecipeSerializers(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
      // register tool stats that are not defined directly in the class; safer than static init registration
      ToolStats.register(OverslimeModule.OVERSLIME_STAT);
      ToolStats.register(ToolTankHelper.CAPACITY_STAT);
      ToolStats.register(ToolEnergyCapability.MAX_STAT);
      ToolStats.registerConditional(EdibleModule.HUNGER);
      ToolStats.registerConditional(EdibleModule.SATURATION);
      ToolStats.register(EdibleModule.EAT_DURATION);
      ToolStats.registerConditional(EdibleModule.COUNTER_CHANCE);

      ToolModule.LOADER.register(getResource("empty"), ToolModule.EMPTY.getLoader());
      // tool definition components
      ToolModule.LOADER.register(getResource("base_stats"), SetStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("multiply_stats"), MultiplyStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("tool_actions"), ToolActionsModule.LOADER);
      ToolModule.LOADER.register(getResource("traits"), ToolTraitsModule.LOADER);
      ToolModule.LOADER.register(getResource("modifier_slots"), ToolSlotsModule.LOADER);
      ToolModule.LOADER.register(getResource("volatile_flag"), VolatileFlagModule.LOADER);
      ToolModule.LOADER.register(getResource("volatile_int"), VolatileIntModule.LOADER);
      // harvest
      ToolModule.LOADER.register(getResource("is_effective"), IsEffectiveModule.LOADER);
      ToolModule.LOADER.register(getResource("mining_speed_modifier"), MiningSpeedModifierModule.LOADER);
      ToolModule.LOADER.register(getResource("max_tier"), MaxTierModule.LOADER);
      ToolModule.LOADER.register(getResource("one_click_break"), OneClickBreakModule.LOADER);
      // material
      ToolModule.LOADER.register(getResource("material_stats"), MaterialStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("part_stats"), PartStatsModule.LOADER);
      ToolModule.LOADER.register(getResource("material_traits"), MaterialTraitsModule.LOADER);
      ToolModule.LOADER.register(getResource("tool_parts"), PartsModule.LOADER);
      ToolModule.LOADER.register(getResource("material_repair"), MaterialRepairModule.LOADER);
      ToolModule.LOADER.register(getResource("default_materials"), DefaultMaterialsModule.LOADER);
      ToolModule.LOADER.register(getResource("remapping_materials"), RemappingMaterialsModule.LOADER);
      ToolModule.LOADER.register(getResource("statless_part_repair"), StatlessPartRepairModule.LOADER);
      // aoe
      AreaOfEffectIterator.LOADER.register(getResource("empty"), AreaOfEffectIterator.EMPTY.getLoader());
      AreaOfEffectIterator.register(getResource("box_aoe"), BoxAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("circle_aoe"), CircleAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("tree_aoe"), TreeAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("vein_aoe"), VeiningAOEIterator.LOADER);
      AreaOfEffectIterator.register(getResource("conditional_aoe"), ConditionalAOEIterator.LOADER);
      // attack
      ToolModule.LOADER.register(getResource("sweep_melee"), SweepWeaponAttack.LOADER);
      ToolModule.LOADER.register(getResource("circle_melee"), CircleWeaponAttack.LOADER);
      ToolModule.LOADER.register(getResource("melee_particle"), ParticleWeaponAttack.LOADER);
      // generic tool modules
      ToolModule.LOADER.register(getResource("attack_interaction"), AttackInteraction.LOADER);
      ToolModule.LOADER.register(getResource("dual_option_interaction"), DualOptionInteraction.LOADER);
      ToolModule.LOADER.register(getResource("preference_set_interaction"), PreferenceSetInteraction.LOADER);
      ToolModule.LOADER.register(getResource("toggleable_set_interaction"), ToggleableSetInteraction.LOADER);
      // special tool modules
      ToolModule.LOADER.register(getResource("melting_fluid_effective"), MeltingFluidEffectiveModule.LOADER);
      // display name
      ToolModule.LOADER.register(getResource("item_name"), SimpleToolName.ITEM.getLoader());
      ToolModule.LOADER.register(getResource("material_name"), MaterialToolNameModule.LOADER);
      ToolModule.LOADER.register(getResource("stat_types_name"), StatTypesToolNameModule.LOADER);
      ToolModule.LOADER.register(getResource("fixed_material_name"), FixedMaterialToolName.LOADER);
      ToolModule.LOADER.register(getResource("unique_material_name"), UniqueMaterialToolName.LOADER);
      ToolModule.LOADER.register(getResource("custom_material_name"), CustomMaterialName.LOADER);
      // tool predicates
      ToolContextPredicate.LOADER.register(getResource("has_upgrades"), ToolContextPredicate.HAS_UPGRADES.getLoader());
      ToolContextPredicate.LOADER.register(getResource("has_modifier"), HasModifierPredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_material"), HasMaterialPredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_stat_type"), HasStatTypePredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_persistent_key"), PersistentDataPredicate.LOADER);
      ToolContextPredicate.LOADER.register(getResource("has_hook"), HasToolHookPredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("not_broken"), ToolStackPredicate.NOT_BROKEN.getLoader());
      ToolStackPredicate.LOADER.register(getResource("stat_in_range"), StatInRangePredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("stat_in_set"), StatInSetPredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("has_volatile_key"), VolatileDataPredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("variable_range"), ToolVariableRangePredicate.LOADER);
      ToolStackPredicate.LOADER.register(getResource("tool_action"), ToolActionPredicate.LOADER);
    }
  }

  /** Recipes are a registry generated from bootstraps since 26.3 */
  @SubscribeEvent
  void gatherRegistries(final GatherDataRegistryEntriesEvent event) {
    event.recipe(ToolsRecipeProvider::new);
    event.recipe(MaterialRecipeProvider::new);
  }

  /** The single client data run generates both assets and data in 26.3, so server data providers also go on the client event */
  @SubscribeEvent
  void gatherData(final GatherDataEvent.Client event) {
    // server data
    // ExistingFileHelper is gone, providers that validated files read the resources directly
    ResourceManager serverResources = event.getResourceManager(PackType.SERVER_DATA);
    MaterialDataProvider materials = event.createProvider(MaterialDataProvider::new);
    event.createProvider(output -> new MaterialStatsDataProvider(output, materials));
    event.createProvider(output -> new MaterialTraitsDataProvider(output, materials));
    event.createProvider(ToolDefinitionDataProvider::new);
    event.createProvider(StationSlotLayoutProvider::new);
    event.createProvider(output -> new MaterialTagProvider(output, serverResources));

    // assets
    ResourceManager clientResources = event.getResourceManager(PackType.CLIENT_RESOURCES);
    event.createProvider(output -> new ToolItemModelProvider(output, clientResources));
    // client item definitions (assets/tconstruct/items), for every Tinkers item
    event.createProvider(output -> new TinkerClientItemProvider(output, clientResources));
    TinkerMaterialSpriteProvider materialSprites = new TinkerMaterialSpriteProvider();
    TinkerPartSpriteProvider partSprites = new TinkerPartSpriteProvider();
    event.createProvider(output -> new MaterialRenderInfoProvider(output, materialSprites, clientResources));
    event.createProvider(output -> new GeneratorPartTextureJsonGenerator(output, TConstruct.MOD_ID, partSprites));
    event.createProvider(output -> new MaterialPartTextureGenerator(output, clientResources, partSprites, materialSprites));
    event.createProvider(output -> new MaterialPaletteDebugGenerator(output, TConstruct.MOD_ID, materialSprites));
    event.createProvider(ArmorModelProvider::new);
    event.createProvider(output -> new TinkerTrimMaterialPaletteGenerator(output, clientResources, materialSprites));
    event.createProvider(ModifierModelMapProvider::new);
  }

  /** Adds all relevant items to the creative tab */
  private static void addTabItems(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
    // start with tools that lack materials
    Consumer<ItemStack> output = tab::accept;

    acceptTool(output, flintAndBrick);
    acceptTool(output, skyStaff);
    acceptTool(output, earthStaff);
    acceptTool(output, ichorStaff);
    acceptTool(output, enderStaff);

    // small tools
    acceptTool(output, pickaxe);
    acceptTool(output, pickadze);
    acceptTool(output, mattock);
    acceptTool(output, handAxe);
    acceptTool(output, kama);
    acceptTool(output, dagger);
    acceptTool(output, sword);

    // broad tools
    acceptTool(output, sledgeHammer);
    acceptTool(output, veinHammer);
    acceptTool(output, excavator);
    acceptTool(output, broadAxe);
    acceptTool(output, scythe);
    acceptTool(output, cleaver);

    // ranged tools
    acceptTool(output, crossbow);
    acceptTool(output, longbow);
    acceptTool(output, fishingRod);
    acceptTool(output, javelin);
    acceptTool(output, arrow);
    acceptTool(output, shuriken);
    acceptEFLN(shuriken.get(), tab);
    acceptTool(output, throwingAxe);

    // ancient tools
    acceptTool(output, meltingPan);
    acceptTool(output, warPick);
    acceptTool(output, battlesign);
    acceptTool(output, swasher);
    if (ModList.get().isLoaded("twilightforest")) {
      acceptTool(output, minotaurAxe);
    }

    // armor
    acceptTools(output, travelersGear);
    acceptTool(output, travelersShield);
    acceptTools(output, plateArmor);
    acceptTool(output, plateShield);
    acceptTools(output, slimesuit);
    acceptTool(output, slimeWings);
  }

  /** Adds a tool to the tab */
  private static void acceptTool(Consumer<ItemStack> output, Supplier<? extends IModifiable> tool) {
    ToolBuildHandler.addVariants(output, tool.get(), "");
  }

  /** Adds a tool to the tab */
  private static void acceptTools(Consumer<ItemStack> output, EnumObject<?,? extends IModifiable> tools) {
    tools.forEach(tool -> ToolBuildHandler.addVariants(output, tool, ""));
  }

  /**
   * Creates a EFLN using the given shuriken style item.
   * @param item  Item to add
   * @param tab   Creative tab to fill
   */
  private static void acceptEFLN(IModifiable item, CreativeModeTab.Output tab) {
    ToolDefinition definition = item.getToolDefinition();
    if (ToolMaterialHook.stats(definition).size() == 2) {
      IMaterial gunpowder = MaterialRegistry.getMaterial(MaterialIds.gunpowder);
      IMaterial prismarine = MaterialRegistry.getMaterial(MaterialIds.prismarine);
      if (gunpowder != IMaterial.UNKNOWN && prismarine != IMaterial.UNKNOWN) {
        ToolStack efln = ToolStack.createTool(item.asItem(), definition, MaterialNBT.of(gunpowder, prismarine));
        if (ModifierManager.INSTANCE.contains(ModifierIds.redirected)) {
          efln.addModifier(ModifierIds.redirected, 1);
        }
        ItemStack stack = efln.createStack();
        stack.set(DataComponents.CUSTOM_NAME, TConstruct.makeTranslation("item", "efln_ball"));
        tab.accept(stack);
      }
    }
  }
}
