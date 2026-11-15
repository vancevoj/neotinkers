package slimeknights.tconstruct.common.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.triggers.PlayerInteractTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.FluidInstance;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.data.advancement.AdvancementIds;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.json.predicate.tool.HasMaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasModifierPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.StatInRangePredicate;
import slimeknights.tconstruct.library.json.predicate.tool.StatInSetPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolActionPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolStackItemPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolStackPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.VolatileDataPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.shared.inventory.BlockContainerOpenedTrigger;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.SearedLanternBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.data.ModifierIds;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.FoliageType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Generates advancements used by this mod.
 * TODO 1.21: move to {@link slimeknights.tconstruct.common.data.advancement}.
 */
public class AdvancementsProvider extends AdvancementSubProvider {
  /** Item lookup for item predicates */
  private final HolderGetter<Item> items;
  /** Block lookup for block predicates */
  private final HolderGetter<Block> blocks;
  /** Entity type lookup for entity predicates */
  private final HolderGetter<EntityType<?>> entityTypes;
  /** Structure lookup for location predicates */
  private final HolderGetter<Structure> structures;
  /** Loot table lookup for rewards */
  private final HolderGetter<LootTable> lootTables;
  /** Tracks duplicate IDs */
  private final Set<Identifier> ids = new HashSet<>();

  /**
   * Creates a new advancement provider. Since 26.3 advancements are a reloadable registry, register with
   * {@code GatherDataRegistryEntriesEvent#advancement(AdvancementsProvider::new)}.
   */
  public AdvancementsProvider(BootstrapContext<Advancement> output) {
    super(output);
    this.items = output.lookup(Registries.ITEM);
    this.blocks = output.lookup(Registries.BLOCK);
    this.entityTypes = output.lookup(Registries.ENTITY_TYPE);
    this.structures = output.lookup(Registries.STRUCTURE);
    this.lootTables = output.lookup(Registries.LOOT_TABLE);
  }

  /** Generates the advancements */
  @Override
  public void generate() {
    // tinkering path
    // 26.3: backgrounds are client assets, the ID omits the textures folder and the png extension
    AdvancementHolder materialsAndYou = builder(TinkerCommons.materialsAndYou, resource("tools/materials_and_you"), resource("gui/advancement_background"), AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_book", hasItem(TinkerCommons.materialsAndYou)));
    AdvancementHolder partBuilder = builder(TinkerTables.partBuilder, resource("tools/part_builder"), materialsAndYou, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_block", hasItem(TinkerTables.partBuilder)));
    builder(materialTemplate(TinkerToolParts.pickHead.get(), MaterialIds.wood), resource("tools/make_part"), partBuilder, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_part", hasTag(TinkerTags.Items.TOOL_PARTS)));
    AdvancementHolder tinkerStation = builder(TinkerTables.tinkerStation, resource("tools/tinker_station"), partBuilder, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_block", hasItem(TinkerTables.tinkerStation)));
    AdvancementHolder tinkerTool = builder(renderTool(TinkerTools.pickaxe.get()), resource("tools/tinker_tool"), tinkerStation, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_tool", hasTag(TinkerTags.Items.MULTIPART_TOOL)));
    AdvancementHolder harvestLevel = builder(Items.NETHERITE_INGOT, resource("tools/netherite_tier"), tinkerTool, AdvancementType.GOAL, builder ->
      builder.addCriterion("harvest_level", hasTool(ToolStackItemPredicate.ofTool(ToolStackPredicate.and(
        ToolStackPredicate.tag(TinkerTags.Items.HARVEST),
        new StatInSetPredicate<>(ToolStats.HARVEST_TIER, ToolMaterial.NETHERITE)
      )))));
    builder(Items.TARGET, resource("tools/perfect_aim"), tinkerTool, AdvancementType.GOAL, builder ->
      builder.addCriterion("accuracy", hasTool(ToolStackItemPredicate.ofTool(ToolStackPredicate.and(
        ToolStackPredicate.tag(TinkerTags.Items.BOWS),
        StatInRangePredicate.match(ToolStats.ACCURACY, 1)
      )))));
    // note that attack damage gets +1 from player attributes, so 20 is actually 21 damage with the tool
    builder(Items.ZOMBIE_HEAD, resource("tools/one_shot"), tinkerTool, AdvancementType.GOAL, builder ->
      builder.addCriterion("damage", hasTool(ToolStackItemPredicate.ofTool(StatInRangePredicate.min(ToolStats.ATTACK_DAMAGE, 20)))));
    builder(TinkerMaterials.manyullyn.getIngot(), resource("tools/material_master"), harvestLevel, AdvancementType.CHALLENGE, builder -> {
      Consumer<MaterialId> with = id -> builder.addCriterion(id.getPath(), hasTool(ToolStackItemPredicate.ofContext(new HasMaterialPredicate(id))));
      // tier 1
      with.accept(MaterialIds.wood);
      with.accept(MaterialIds.flint);
      with.accept(MaterialIds.rock);
      with.accept(MaterialIds.bone);
      with.accept(MaterialIds.necroticBone);
      with.accept(MaterialIds.leather);
      with.accept(MaterialIds.string);
      with.accept(MaterialIds.vine);
      with.accept(MaterialIds.bamboo);
      with.accept(MaterialIds.chorus);
      // tier 2
      with.accept(MaterialIds.iron);
      with.accept(MaterialIds.searedStone);
      with.accept(MaterialIds.scorchedStone);
      with.accept(MaterialIds.copper);
      with.accept(MaterialIds.slimewood);
      with.accept(MaterialIds.slimeskin);
      with.accept(MaterialIds.skyslimeVine);
      with.accept(MaterialIds.weepingVine);
      with.accept(MaterialIds.twistingVine);
      with.accept(MaterialIds.whitestone);
      // tier 3
      with.accept(MaterialIds.roseGold);
      with.accept(MaterialIds.slimesteel);
      with.accept(MaterialIds.nahuatl);
      with.accept(MaterialIds.amethystBronze);
      with.accept(MaterialIds.pigIron);
      with.accept(MaterialIds.cobalt);
      with.accept(MaterialIds.darkthread);
      with.accept(MaterialIds.ichorskin);
      // tier 4
      with.accept(MaterialIds.manyullyn);
      with.accept(MaterialIds.hepatizon);
      with.accept(MaterialIds.cinderslime);
      with.accept(MaterialIds.queensSlime);
      with.accept(MaterialIds.blazingBone);
      with.accept(MaterialIds.blazewood);
      with.accept(MaterialIds.jeweledHide);
      with.accept(MaterialIds.knightmetal);
      with.accept(MaterialIds.knightslime);
      with.accept(MaterialIds.enderslimeVine);
    });
    builder(renderTool(TinkerTools.travelersGear.get(ArmorType.HELMET)), resource("tools/travelers_gear"), tinkerStation, AdvancementType.TASK, builder ->
      TinkerTools.travelersGear.forEach((type, armor) -> builder.addCriterion("crafted_" + type.getName(), hasItem(armor))));
    builder(renderTool(TinkerTools.pickaxe.get()), resource("tools/tool_smith"), tinkerTool, AdvancementType.CHALLENGE, builder -> {
      Consumer<Item> with = item -> builder.addCriterion(BuiltInRegistries.ITEM.getKey(item).getPath(), hasItem(item));
      with.accept(TinkerTools.pickaxe.get());
      with.accept(TinkerTools.mattock.get());
      with.accept(TinkerTools.pickadze.get());
      with.accept(TinkerTools.handAxe.get());
      with.accept(TinkerTools.kama.get());
      with.accept(TinkerTools.dagger.get());
      with.accept(TinkerTools.sword.get());
    });
    AdvancementHolder modified = builder(Items.REDSTONE, resource("tools/modified"), tinkerTool, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_tool", hasTool(ToolContextPredicate.HAS_UPGRADES)));
    builder(Items.WRITABLE_BOOK, resource("tools/upgrade_slots"), modified, AdvancementType.CHALLENGE, builder -> builder.addCriterion("has_modified", hasTool(
      HasModifierPredicate.hasUpgrade(ModifierIds.writable, 1),
      HasModifierPredicate.hasUpgrade(ModifierIds.recapitated, 1),
      HasModifierPredicate.hasUpgrade(ModifierIds.harmonious, 1),
      HasModifierPredicate.hasUpgrade(ModifierIds.forecast, 1),
      HasModifierPredicate.hasUpgrade(ModifierIds.gilded, 1)))
    );

    // smeltery path
    AdvancementHolder punySmelting = builder(TinkerCommons.punySmelting, resource("smeltery/puny_smelting"), materialsAndYou, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_book", hasItem(TinkerCommons.punySmelting)));
    AdvancementHolder melter = builder(TinkerSmeltery.searedMelter, resource("smeltery/melter"), punySmelting, AdvancementType.TASK, builder -> {
      Consumer<Block> with = block -> builder.addCriterion(BuiltInRegistries.BLOCK.getKey(block).getPath(), ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, block));
      with.accept(TinkerSmeltery.searedMelter.get());
      with.accept(TinkerSmeltery.searedTable.get());
      with.accept(TinkerSmeltery.searedBasin.get());
      with.accept(TinkerSmeltery.searedFaucet.get());
      with.accept(TinkerSmeltery.searedHeater.get());
      TinkerSmeltery.searedTank.forEach(with);
      // first 4 are required, and then any of the last 5
      builder.requirements(new CountRequirementsStrategy(1, 1, 1, 1, 1 + TankType.values().length));
    });
    builder(TinkerSmeltery.toolHandleCast.getSand(), resource("smeltery/sand_casting"), melter, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_cast", hasTag(TinkerTags.Items.BLANK_SINGLE_USE_CASTS)));
    AdvancementHolder goldCasting = builder(TinkerSmeltery.pickHeadCast, resource("smeltery/gold_casting"), melter, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_cast", hasTag(TinkerTags.Items.GOLD_CASTS)));
    builder(TinkerSmeltery.hammerHeadCast, resource("smeltery/cast_collector"), goldCasting, AdvancementType.GOAL, builder -> {
      Consumer<CastItemObject> with = cast -> builder.addCriterion(cast.getName().getPath(), hasItem(cast.get()));
      with.accept(TinkerSmeltery.ingotCast);
      with.accept(TinkerSmeltery.nuggetCast);
      with.accept(TinkerSmeltery.gemCast);
      with.accept(TinkerSmeltery.rodCast);
      with.accept(TinkerSmeltery.repairKitCast);
      // parts
      with.accept(TinkerSmeltery.pickHeadCast);
      with.accept(TinkerSmeltery.smallAxeHeadCast);
      with.accept(TinkerSmeltery.smallBladeCast);
      with.accept(TinkerSmeltery.adzeHeadCast);
      with.accept(TinkerSmeltery.hammerHeadCast);
      with.accept(TinkerSmeltery.broadBladeCast);
      with.accept(TinkerSmeltery.broadAxeHeadCast);
      with.accept(TinkerSmeltery.largePlateCast);
      with.accept(TinkerSmeltery.toolBindingCast);
      with.accept(TinkerSmeltery.toughBindingCast);
      with.accept(TinkerSmeltery.toolHandleCast);
      with.accept(TinkerSmeltery.toughHandleCast);
      with.accept(TinkerSmeltery.bowLimbCast);
      with.accept(TinkerSmeltery.bowGripCast);
      with.accept(TinkerSmeltery.helmetPlatingCast);
      with.accept(TinkerSmeltery.chestplatePlatingCast);
      with.accept(TinkerSmeltery.leggingsPlatingCast);
      with.accept(TinkerSmeltery.bootsPlatingCast);
      with.accept(TinkerSmeltery.mailleCast);
    });
    AdvancementHolder mightySmelting = builder(TinkerCommons.mightySmelting, resource("smeltery/mighty_smelting"), melter, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_book", hasItem(TinkerCommons.mightySmelting)));
    AdvancementHolder smeltery = builder(TinkerSmeltery.smelteryController, resource("smeltery/structure"), mightySmelting, AdvancementType.TASK, builder ->
      builder.addCriterion("open_smeltery", BlockContainerOpenedTrigger.Instance.container(TinkerSmeltery.smeltery.get())));
    AdvancementHolder anvil = builder(TinkerTables.tinkersAnvil, resource("smeltery/tinkers_anvil"), smeltery, AdvancementType.GOAL, builder -> {
      builder.addCriterion("crafted_overworld", hasItem(TinkerTables.tinkersAnvil));
      builder.addCriterion("crafted_nether", hasItem(TinkerTables.scorchedAnvil));
      builder.requirements(AdvancementRequirements.Strategy.OR);
    });
    builder(renderTool(TinkerTools.veinHammer.get()), resource("smeltery/tool_forge"), anvil, AdvancementType.CHALLENGE, builder -> {
      Consumer<ItemObject<?>> with = item -> builder.addCriterion(item.getId().getPath(), hasItem(item));
      with.accept(TinkerTools.sledgeHammer);
      with.accept(TinkerTools.veinHammer);
      with.accept(TinkerTools.excavator);
      with.accept(TinkerTools.broadAxe);
      with.accept(TinkerTools.scythe);
      with.accept(TinkerTools.cleaver);
      with.accept(TinkerTools.longbow);
      with.accept(TinkerTools.javelin);
    });
    builder(TinkerModifiers.silkyCloth, resource("smeltery/abilities"), anvil, AdvancementType.CHALLENGE, builder -> {
      Consumer<ModifierId> with = modifier -> builder.addCriterion(modifier.getPath(), hasTool(ToolStackItemPredicate.ofContext(HasModifierPredicate.hasUpgrade(modifier, 1))));
      Consumer<LazyModifier> withL = modifier -> with.accept(modifier.getId());

      // sorted like the modifier tag provider tags
      // general
      with.accept(ModifierIds.expanded);
      with.accept(ModifierIds.gilded);
      with.accept(ModifierIds.luck);
      with.accept(ModifierIds.unbreakable);
      withL.accept(TinkerModifiers.melting);

      // melee
      with.accept(ModifierIds.blocking);
      withL.accept(TinkerModifiers.parrying);
      withL.accept(TinkerModifiers.dualWielding);
      with.accept(ModifierIds.spilling);

      // harvest
      with.accept(ModifierIds.autosmelt);
      withL.accept(TinkerModifiers.exchanging);
      with.accept(ModifierIds.silky);

      // ranged
      with.accept(ModifierIds.bulkQuiver);
      with.accept(ModifierIds.trickQuiver);
      with.accept(ModifierIds.crystalshot);
      with.accept(ModifierIds.multishot);
      with.accept(ModifierIds.ballista);
      with.accept(ModifierIds.slimeball);
      with.accept(ModifierIds.sliver);
      // fishing
      with.accept(ModifierIds.grapple);
      // throwing
      with.accept(ModifierIds.throwing);
      with.accept(ModifierIds.returning);
      with.accept(ModifierIds.channeling);

      // interaction
      with.accept(ModifierIds.bucketing);
      with.accept(ModifierIds.firestarter);
      with.accept(ModifierIds.glowing);
      with.accept(ModifierIds.pathing);
      with.accept(ModifierIds.stripping);
      with.accept(ModifierIds.tilling);
      with.accept(ModifierIds.brushing);
      // fluid
      with.accept(ModifierIds.spitting);
      with.accept(ModifierIds.splashing);
      with.accept(ModifierIds.slurping);
      // staff
      with.accept(ModifierIds.bonking);
      with.accept(ModifierIds.flinging);
      with.accept(ModifierIds.springing);
      with.accept(ModifierIds.warping);
      with.accept(ModifierIds.drillAttack);

      // armor
      with.accept(ModifierIds.protection);
      withL.accept(TinkerModifiers.bursting);
      withL.accept(TinkerModifiers.wetting);
      // helmet
      with.accept(ModifierIds.aquaAffinity);
      // chestplate
      withL.accept(TinkerModifiers.ambidextrous);
      with.accept(ModifierIds.reach);
      with.accept(ModifierIds.strength);
      with.accept(ModifierIds.wings);
      // leggings
      with.accept(ModifierIds.pockets);
      with.accept(ModifierIds.toolBelt);
      with.accept(ModifierIds.soulBelt);
      with.accept(ModifierIds.craftingTable);
      // boots
      with.accept(ModifierIds.bouncy);
      with.accept(ModifierIds.doubleJump);
      with.accept(ModifierIds.flamewake);
      with.accept(ModifierIds.frostWalker);
      with.accept(ModifierIds.snowdrift);
      // shield
      with.accept(ModifierIds.boundless);
      with.accept(ModifierIds.reflecting);
    });

    // foundry path
    AdvancementHolder fantasticFoundry = builder(TinkerCommons.fantasticFoundry, resource("foundry/fantastic_foundry"), materialsAndYou, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_book", hasItem(TinkerCommons.fantasticFoundry)));
    builder(TinkerCommons.encyclopedia, resource("foundry/encyclopedia"), fantasticFoundry, AdvancementType.GOAL, builder ->
      builder.addCriterion("crafted_book", hasItem(TinkerCommons.encyclopedia)));
    AdvancementHolder alloyer = builder(TinkerSmeltery.scorchedAlloyer, resource("foundry/alloyer"), fantasticFoundry, AdvancementType.TASK, builder -> {
      Consumer<Block> with = block -> builder.addCriterion(BuiltInRegistries.BLOCK.getKey(block).getPath(), ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(blocks, block));
      with.accept(TinkerSmeltery.scorchedAlloyer.get());
      with.accept(TinkerSmeltery.scorchedFaucet.get());
      with.accept(TinkerSmeltery.scorchedTable.get());
      with.accept(TinkerSmeltery.scorchedBasin.get());
      for (TankType type : TankType.values()) {
        with.accept(TinkerSmeltery.scorchedTank.get(type));
      }
      builder.requirements(new CountRequirementsStrategy(1, 1, 1, 1, 2, 2));
    });
    AdvancementHolder foundry = builder(TinkerSmeltery.foundryController, resource("foundry/structure"), alloyer, AdvancementType.TASK, builder ->
      builder.addCriterion("open_foundry", BlockContainerOpenedTrigger.Instance.container(TinkerSmeltery.foundry.get())));
    AdvancementHolder blazingBlood = builder(tankTemplate(TinkerSmeltery.scorchedTank.get(TankType.FUEL_GAUGE), TinkerFluids.blazingBlood.get(), TankType.FUEL_GAUGE.getCapacity()),
            resource("foundry/blaze"), foundry, AdvancementType.GOAL, builder -> {
      Consumer<SearedTankBlock> with = block -> {
        builder.addCriterion(BuiltInRegistries.BLOCK.getKey(block).getPath(),
                              InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, block).withComponents(tankComponents(block, TinkerFluids.blazingBlood.get(), block.getCapacity()))));
        builder.requirements(AdvancementRequirements.Strategy.OR);
      };
      TinkerSmeltery.searedTank.forEach(with);
      TinkerSmeltery.scorchedTank.forEach(with);
    });
    builder(renderTool(TinkerTools.plateArmor.get(ArmorType.CHESTPLATE)), resource("foundry/plate_armor"), blazingBlood, AdvancementType.GOAL, builder ->
      TinkerTools.plateArmor.forEach((type, armor) -> builder.addCriterion("crafted_" + type.getName(), hasItem(armor))));
    builder(tankTemplate(TinkerSmeltery.scorchedLantern, TinkerFluids.moltenManyullyn.get(), TinkerSmeltery.scorchedLantern.get().getCapacity()),
            resource("foundry/manyullyn_lanterns"), foundry, AdvancementType.CHALLENGE, builder -> {
      Consumer<SearedLanternBlock> with = block -> {
        builder.addCriterion(BuiltInRegistries.BLOCK.getKey(block).getPath(),
                              InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, block).withCount(MinMaxBounds.Ints.atLeast(64)).withComponents(tankComponents(block, TinkerFluids.moltenManyullyn.get(), block.getCapacity()))));
        builder.requirements(AdvancementRequirements.Strategy.OR);
      };
      with.accept(TinkerSmeltery.searedLantern.get());
      with.accept(TinkerSmeltery.scorchedLantern.get());
    });

    // exploration path
    AdvancementHolder tinkersGadgetry = builder(TinkerCommons.tinkersGadgetry, resource("world/tinkers_gadgetry"), materialsAndYou, AdvancementType.TASK, builder ->
      builder.addCriterion("crafted_book", hasItem(TinkerCommons.tinkersGadgetry)));
    builder(TinkerWorld.slimeSapling.get(FoliageType.EARTH), resource("world/earth_island"), tinkersGadgetry, AdvancementType.GOAL, builder ->
      builder.addCriterion("found_island", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structure(TinkerStructures.earthSlimeIsland)))));
    AdvancementHolder skyslimeIsland = builder(TinkerWorld.slimeSapling.get(FoliageType.SKY), resource("world/sky_island"), tinkersGadgetry, AdvancementType.GOAL, builder ->
      builder.addCriterion("found_island", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structure(TinkerStructures.skySlimeIsland)))));
    builder(TinkerWorld.slimeSapling.get(FoliageType.BLOOD), resource("world/blood_island"), tinkersGadgetry, AdvancementType.GOAL, builder ->
      builder.addCriterion("found_island", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structure(TinkerStructures.bloodIsland)))));
    builder(TinkerWorld.slimeSapling.get(FoliageType.ENDER), resource("world/ender_island"), tinkersGadgetry, AdvancementType.GOAL, builder ->
      builder.addCriterion("found_island", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structure(TinkerStructures.endSlimeIsland)))));
    builder(Items.CLAY_BALL, resource("world/clay_island"), tinkersGadgetry, AdvancementType.GOAL, builder ->
      builder.addCriterion("found_island", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structure(TinkerStructures.clayIsland)))));
    builder(TinkerCommons.slimeball.get(SlimeType.ICHOR), resource("world/slime_collector"), tinkersGadgetry, AdvancementType.TASK, builder -> {
      for (SlimeType type : SlimeType.values()) {
        builder.addCriterion(type.getSerializedName(), hasTag(type.getSlimeballTag()));
      }
      builder.addCriterion("magma_cream", hasItem(Items.MAGMA_CREAM));
    });
    builder(TinkerGadgets.piggyBackpack, resource("world/piggybackpack"), tinkersGadgetry, AdvancementType.GOAL, builder ->
      builder.addCriterion("used_pack", PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(Optional.empty(), ItemPredicate.Builder.item().of(items, TinkerGadgets.piggyBackpack), EntityPredicate.wrap(Optional.of(EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.PIG).build())))));
    AdvancementHolder slimesuit = builder(toolTemplate(TinkerTools.slimesuit.get(ArmorType.CHESTPLATE), new MaterialIdNBT(List.of(MaterialIds.bone, MaterialIds.skyslime)).updateNBT(new CompoundTag())), resource("world/slimesuit"), skyslimeIsland, AdvancementType.GOAL, builder ->
      TinkerTools.slimesuit.forEach((type, armor) -> builder.addCriterion("crafted_" + type.getName(), hasItem(armor))));
    builder(toolTemplate(TinkerTools.slimesuit.get(ArmorType.HELMET), new MaterialIdNBT(List.of(MaterialIds.gunpowder, MaterialIds.enderslime)).updateNBT(new CompoundTag())),
            resource("world/slimeskull"), slimesuit, AdvancementType.CHALLENGE, builder -> {
      Item helmet = TinkerTools.slimesuit.get(ArmorType.HELMET);
      Consumer<MaterialId> with = mat -> builder.addCriterion(mat.getPath(), hasTool(ToolContextPredicate.set(helmet), new HasMaterialPredicate(mat, 0)));
      with.accept(MaterialIds.gunpowder);
      with.accept(MaterialIds.blaze);
      // zombie
      with.accept(MaterialIds.leather);
      with.accept(MaterialIds.iron);
      with.accept(MaterialIds.copper);
      // spider
      with.accept(MaterialIds.string);
      with.accept(MaterialIds.darkthread);
      // skeleton
      with.accept(MaterialIds.bone);
      with.accept(MaterialIds.ice);
      with.accept(MaterialIds.necroticBone);
      // piglin
      with.accept(MaterialIds.gold);
      with.accept(MaterialIds.roseGold);
      with.accept(MaterialIds.pigIron);
      // end
      with.accept(MaterialIds.enderPearl);
      with.accept(MaterialIds.dragonScale);
      // crafted
      with.accept(MaterialIds.knightmetal);
    });
    builder(renderTool(TinkerTools.battlesign.get()), resource("world/ancient_tools"), tinkersGadgetry, AdvancementType.CHALLENGE, builder -> {
      Consumer<ItemObject<?>> with = item -> builder.addCriterion(item.getId().getPath(), hasItem(item));
      with.accept(TinkerTools.meltingPan);
      with.accept(TinkerTools.warPick);
      with.accept(TinkerTools.battlesign);
      with.accept(TinkerTools.swasher);
    });

    // internal advancements
    hiddenBuilder(resource("internal/starting_book"), ConfigEnabledCondition.SPAWN_WITH_BOOK, builder -> {
      builder.addCriterion("tick", PlayerTrigger.TriggerInstance.tick());
      builder.rewards(AdvancementRewards.Builder.loot(lootTables.getOrThrow(ResourceKey.create(Registries.LOOT_TABLE, TConstruct.getResource("gameplay/starting_book")))));
    });

    // grant vanilla advancements
    // crafting station for the crafting table advancement
    grantAdvancement(resource("internal/crafting_station"), AdvancementIds.STORY_ROOT, builder -> builder.addCriterion("crafting_station", hasItem(TinkerTables.craftingStation)));
    // pickaxes that mine stone tier grant upgrade tool advancement
    grantAdvancement(resource("internal/stone_pickaxe"), AdvancementIds.STONE_PICK, builder -> builder.addCriterion("stone_pick", hasToolStack(
      ToolStackPredicate.tag(TinkerTags.Items.HARVEST),
      new ToolActionPredicate(PICKAXE_DIG),
      new StatInSetPredicate<>(ToolStats.HARVEST_TIER, ToolMaterial.STONE)
    )));
    // pickaxes that mine iron tier grant iron tools advancement
    grantAdvancement(resource("internal/iron_pickaxe"), AdvancementIds.IRON_PICK, builder -> builder.addCriterion("iron_pick", hasToolStack(
      ToolStackPredicate.tag(TinkerTags.Items.HARVEST),
      new ToolActionPredicate(PICKAXE_DIG),
      new StatInSetPredicate<>(ToolStats.HARVEST_TIER, ToolMaterial.IRON)
    )));
    // anything with tilling and netherite grants netherite hoe
    grantAdvancement(resource("internal/netherite_hoe"), AdvancementIds.NETHERITE_HOE, builder -> builder.addCriterion("iron_pick", hasToolStack(
      ToolStackPredicate.tag(TinkerTags.Items.INTERACTABLE_RIGHT),
      new ToolActionPredicate(HOE_TILL),
      new VolatileDataPredicate(AdvancementIds.NETHERITE)
    )));
    // boots with the snow boots flag can walk on powder snow
    grantAdvancement(resource("internal/walk_on_powder_snow"), AdvancementIds.WALK_ON_POWDER_SNOW, builder ->
      builder.addCriterion("has_boots", PlayerTrigger.TriggerInstance.located(EntityPredicate.Builder.entity()
        .equipment(EntityEquipmentPredicate.Builder.equipment().feet(toolItem(ToolStackItemPredicate.ofTool(new VolatileDataPredicate(ModifiableArmorItem.SNOW_BOOTS)))))
        .steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, Blocks.POWDER_SNOW)))))
    );
    // despite having multiple criteria, these use an any condition so easier to just bypass criteria
    // iron armor expects iron from armor plating's trait
    IJsonPredicate<IToolStackView> hasTag = ToolStackPredicate.tag(TinkerTags.Items.WORN_ARMOR);
    grantAdvancement(resource("internal/iron_armor"), AdvancementIds.OBTAIN_ARMOR, builder -> builder.addCriterion("has_armor", hasToolStack(hasTag, new VolatileDataPredicate(AdvancementIds.IRON_ARMOR))));
    // diamond armor wants the diamond modifier
    grantAdvancement(resource("internal/diamond_armor"), AdvancementIds.SHINY_GEAR, builder -> builder.addCriterion("has_armor", hasToolStack(hasTag, new VolatileDataPredicate(AdvancementIds.DIAMOND_ARMOR))));
    // netherite armor wants a whole set of netherite armor in the inventory at once
    grantAdvancement(resource("internal/netherite_armor"), AdvancementIds.NETHERITE_ARMOR, builder -> {
      ToolStackPredicate hasNetherite = new VolatileDataPredicate(AdvancementIds.NETHERITE);
      Function<TagKey<Item>,ItemPredicate.Builder> predicate = tag -> toolItem(ToolStackItemPredicate.ofTool(ToolStackPredicate.and(ToolStackPredicate.tag(tag), hasNetherite)));
      builder.addCriterion("has_armor", InventoryChangeTrigger.TriggerInstance.hasItems(
        predicate.apply(TinkerTags.Items.HELMETS),
        predicate.apply(TinkerTags.Items.CHESTPLATES),
        predicate.apply(TinkerTags.Items.LEGGINGS),
        predicate.apply(TinkerTags.Items.BOOTS)
      ));
    });
  }

  /*
   * Display and predicate stacks. Item components are not bound during datagen, so no item stacks can be created here,
   * all displays are templates built from the same data the matching stack helpers write.
   */

  /** Creates a template with the given tool data, matching {@link MaterialIdNBT#updateStack} and similar tool data helpers */
  private static ItemStackTemplate toolTemplate(ItemLike item, CompoundTag toolData) {
    return new ItemStackTemplate(item.asItem(), DataComponentPatch.builder().set(ToolDataComponents.TOOL_DATA.get(), toolData).build());
  }

  /** Recreation of {@link ToolBuildHandler#buildToolForRendering} as a template, during datagen the render materials are always added */
  private static ItemStackTemplate renderTool(ItemLike item) {
    CompoundTag toolData = new MaterialIdNBT(IntStream.range(0, 5).mapToObj(ToolBuildHandler::getRenderMaterial).toList()).updateNBT(new CompoundTag());
    toolData.putBoolean(TooltipUtil.KEY_DISPLAY, true);
    return toolTemplate(item, toolData);
  }

  /** Recreation of {@link IMaterialItem#withMaterialForDisplay} as a template */
  private static ItemStackTemplate materialTemplate(ItemLike item, MaterialId material) {
    CompoundTag data = new CompoundTag();
    data.putString(IMaterialItem.MATERIAL_TAG, material.toString());
    return new ItemStackTemplate(item.asItem(), DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(data)).build());
  }

  /** Gets the custom data of a tank filled with the given fluid, matching {@code TankItem#setTank(ItemStack, FluidStack)} */
  private static CustomData tankData(Fluid fluid, int capacity) {
    CompoundTag fluidTag = new CompoundTag();
    fluidTag.putString(FluidInstance.FIELD_ID, BuiltInRegistries.FLUID.getKey(fluid).toString());
    fluidTag.putInt(FluidInstance.FIELD_AMOUNT, capacity);
    CompoundTag tank = new CompoundTag();
    // TankItem's private TAG_FLUID
    tank.put("Fluid", fluidTag);
    CompoundTag data = new CompoundTag();
    data.put(NBTTags.TANK, tank);
    return CustomData.of(data);
  }

  /** Gets a tank template filled with the given fluid */
  private static ItemStackTemplate tankTemplate(ItemLike tank, Fluid fluid, int capacity) {
    return new ItemStackTemplate(tank.asItem(), DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, tankData(fluid, capacity)).build());
  }

  /** Creates a component matcher for a tank item containing the given fluid, matching the whole custom data component like the 1.21.1 predicate */
  private static DataComponentMatchers tankComponents(ItemLike tank, Fluid fluid, int capacity) {
    return DataComponentMatchers.Builder.components().exact(DataComponentExactPredicate.expect(DataComponents.CUSTOM_DATA, tankData(fluid, capacity))).build();
  }

  /** Resolves a structure holder from its key using the datagen registry lookup */
  private Holder<Structure> structure(ResourceKey<Structure> key) {
    return structures.getOrThrow(key);
  }

  /** Item ability for digging with a pickaxe, removed from NeoForge in 26.3 but still used by Tinkers tool definitions */
  private static final ItemAbility PICKAXE_DIG = ItemAbility.get("pickaxe_dig");
  /** Item ability for tilling, removed from NeoForge in 26.3 but still used by the tilling modifier */
  private static final ItemAbility HOE_TILL = ItemAbility.get("till");
  /** Registered tool component predicate type (registered in {@code Registries.DATA_COMPONENT_PREDICATE_TYPE} by TinkerTools). */
  private static final DataComponentPredicate.Type<ToolStackItemPredicate> TOOL_PREDICATE_TYPE = ToolStackItemPredicate.TYPE;

  /** Wraps a tool sub-predicate into an item predicate builder */
  private static ItemPredicate.Builder toolItem(ToolStackItemPredicate predicate) {
    return ItemPredicate.Builder.item().withComponents(DataComponentMatchers.Builder.components().partial(TOOL_PREDICATE_TYPE, predicate).build());
  }

  /** Wraps a tool sub-predicate into a criterion via an ItemPredicate */
  private Criterion<?> hasTool(ToolStackItemPredicate predicate) {
    return InventoryChangeTrigger.TriggerInstance.hasItems(toolItem(predicate));
  }

  /** Creates an item criterion for a tag */
  private Criterion<?> hasTag(TagKey<Item> tag) {
    return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, tag).build());
  }

  /** Creates an item criterion for an item */
  private Criterion<?> hasItem(ItemLike item) {
    return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, item).build());
  }

  /** Creates a predicate for a tool stack */
  private Criterion<?> hasToolStack(IJsonPredicate<IToolStackView> predicate) {
    return hasTool(ToolStackItemPredicate.ofTool(predicate));
  }

  /** Creates a predicate for a tool stack */
  @SafeVarargs
  private Criterion<?> hasToolStack(IJsonPredicate<IToolStackView>... predicate) {
    return hasToolStack(ToolStackPredicate.and(predicate));
  }

  /** Creates a predicate for a tool context */
  private Criterion<?> hasTool(IJsonPredicate<IToolContext> predicate) {
    return hasTool(ToolStackItemPredicate.ofContext(predicate));
  }

  /** Creates a predicate for a tool context */
  @SafeVarargs
  private Criterion<?> hasTool(IJsonPredicate<IToolContext>... predicate) {
    return hasTool(ToolContextPredicate.and(predicate));
  }

  /* Helpers */

  /** Gets a tinkers resource location */
  protected Identifier resource(String name) {
    return TConstruct.getResource(name);
  }

  /** Registers a finished advancement, checking for duplicates */
  private AdvancementHolder register(AdvancementHolder holder, ICondition... conditions) {
    if (!ids.add(holder.id())) {
      throw new IllegalStateException("Duplicate advancement " + holder.id());
    }
    holder.register(output, conditions);
    return holder;
  }

  /**
   * Helper for making an advancement builder
   * @param display      Item to display
   * @param name         Advancement name
   * @param parent       Parent advancement
   * @param frame        Frame type
   * @return  Builder
   */
  protected AdvancementHolder builder(ItemLike display, Identifier name, AdvancementHolder parent, AdvancementType frame, Consumer<Advancement.Builder> consumer) {
    return builder(new ItemStackTemplate(display.asItem()), name, parent, frame, consumer);
  }

  /**
   * Helper for making an advancement builder
   * @param display      Stack to display, a template as datagen cannot create item stacks
   * @param name         Advancement name
   * @param parent       Parent advancement
   * @param frame        Frame type
   * @return  Builder
   */
  protected AdvancementHolder builder(ItemStackTemplate display, Identifier name, AdvancementHolder parent, AdvancementType frame, Consumer<Advancement.Builder> consumer) {
    return builder(display, name, (Identifier)null, frame, builder -> {
      builder.parent(parent);
      consumer.accept(builder);
    });
  }

  /**
   * Helper for making an advancement builder
   * @param display      Item to display
   * @param name         Advancement name
   * @param background   Background texture, without the {@code textures/} folder and {@code .png} extension
   * @param frame        Frame type
   * @return  Builder
   */
  protected AdvancementHolder builder(ItemLike display, Identifier name, @Nullable Identifier background, AdvancementType frame, Consumer<Advancement.Builder> consumer) {
    return builder(new ItemStackTemplate(display.asItem()), name, background, frame, consumer);
  }

  /** Makes an advancement translation key from the given ID */
  private static String makeTranslationKey(Identifier advancement) {
    return "advancements." + advancement.getNamespace() + "." + advancement.getPath().replace('/', '.');
  }

  /**
   * Helper for making an advancement builder
   * @param display      Stack to display, a template as datagen cannot create item stacks
   * @param name         Advancement name
   * @param background   Background texture, without the {@code textures/} folder and {@code .png} extension
   * @param frame        Frame type
   * @return  Builder
   */
  protected AdvancementHolder builder(ItemStackTemplate display, Identifier name, @Nullable Identifier background, AdvancementType frame, Consumer<Advancement.Builder> consumer) {
    Advancement.Builder builder = Advancement.Builder.recipeAdvancement().display(new DisplayInfo(
      display,
      Component.translatable(makeTranslationKey(name) + ".title"),
      Component.translatable(makeTranslationKey(name) + ".description"),
      Optional.ofNullable(background).map(ClientAsset.ResourceTexture::new), frame, true, frame != AdvancementType.TASK, false));
    consumer.accept(builder);
    return register(builder.build(name));
  }

  /**
   * Helper for making a hidden (config gated) advancement builder
   * @param name       Advancement name
   * @param condition  Condition for the advancement to exist
   * @param consumer   Consumer to add conditions
   */
  @SuppressWarnings("SameParameterValue")
  protected void hiddenBuilder(Identifier name, ICondition condition, Consumer<Advancement.Builder> consumer) {
    Advancement.Builder builder = Advancement.Builder.recipeAdvancement();
    consumer.accept(builder);
    register(builder.build(name), condition);
  }

  /**
   * Helper for making an internal advancement running a function.
   * @param name         Advancement name
   * @param function     Function to run
   * @param consumer     Consumer to add conditions
   */
  protected void runFunction(Identifier name, Identifier function, Consumer<Advancement.Builder> consumer) {
    Advancement.Builder builder = Advancement.Builder.recipeAdvancement();
    // grant the advancement
    builder.rewards(AdvancementRewards.Builder.function(function));
    consumer.accept(builder);
    register(builder.build(name));
  }

  /**
   * Helper for making an internal advancement granting a vanilla one
   * @param name         Advancement name
   * @param advancement  Vanilla advancement to grant
   * @param consumer     Consumer to add conditions
   */
  protected void grantAdvancement(Identifier name, Identifier advancement, Consumer<Advancement.Builder> consumer) {
    runFunction(name, AdvancementIds.function(advancement), consumer);
  }

  /**
   * Helper for making an internal advancement granting a vanilla criteria
   * @param name         Advancement name
   * @param advancement  Vanilla advancement to grant
   * @param criteria     Criteria string to grant
   * @param consumer     Consumer to add conditions
   */
  protected void grantAdvancement(Identifier name, Identifier advancement, String criteria, Consumer<Advancement.Builder> consumer) {
    runFunction(name, AdvancementIds.function(advancement, criteria), consumer);
  }
}
