package slimeknights.tconstruct.tools;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.loot.ToolPartLootEntry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IRepairKitItem;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.library.tools.part.block.MaterialBlock;
import slimeknights.tconstruct.library.tools.part.block.MaterialBlockEntity;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.item.FakeIngotItem;
import slimeknights.tconstruct.tools.item.FakeStorageBlockItem;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;
import slimeknights.tconstruct.tools.item.RepairKitItem;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;
import slimeknights.tconstruct.tools.stats.PlatingMaterialStats;
import slimeknights.tconstruct.tools.stats.RepairStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TinkerToolParts extends TinkerModule {
  /** Tab for all tool parts or tool components with many variants */
  public static final DeferredHolder<? super CreativeModeTab, CreativeModeTab> tabToolParts = CREATIVE_TABS.register(
    "tool_parts", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "tool_parts"))
                                       .icon(() -> {
                                         MaterialVariantId material;
                                         if (MaterialRegistry.isFullyLoaded()) {
                                           material = ToolBuildHandler.RANDOM.getMaterial(HeadMaterialStats.ID, RandomSource.create());
                                         } else {
                                           material = ToolBuildHandler.getRenderMaterial(0);
                                         }
                                         return TinkerToolParts.pickHead.get().withMaterialForDisplay(material);
                                       })
                                       .displayItems(dedupeTab(TinkerToolParts::addTabItems))
                                       .withTabsBefore(TinkerTools.tabTools.getId())
                                       .withSearchBar()
                                       .build());

  // repair kits
  public static final ItemObject<RepairKitItem> repairKit = ITEMS.register("repair_kit", RepairKitItem::new, ITEM_PROPS);
  /** Fake ingot tool part for the sake of compat materials that lack an ingot form. Mainly used for compat alloys such as bronze which can be activated by their components. */
  public static final ItemObject<FakeIngotItem> fakeIngot = ITEMS.register("fake_ingot", p -> new FakeIngotItem(p, 1, TinkerTags.Materials.COMPATABILITY_METALS), ITEM_PROPS);
  /** Fake block tool part for the sake of compat materials that lack an ingot form. Mainly used for compat alloys such as bronze which can be activated by their components. */
  public static final ItemObject<MaterialBlock> fakeStorageBlock = BLOCKS.register("fake_storage_block", p -> new MaterialBlock(p, MaterialBlockEntity::new), () -> metalBuilder(MapColor.COLOR_GRAY), (block, p) -> new FakeStorageBlockItem(block, p, 9, TinkerTags.Materials.COMPATABILITY_BLOCKS));
  /** Same as {@link #fakeStorageBlock} but casted to an appropriate item interface form */
  public static final Supplier<IRepairKitItem> fakeStorageBlockItem = () -> (IRepairKitItem) fakeStorageBlock.asItem();

  // rock
  public static final ItemObject<ToolPartItem> pickHead = ITEMS.register("pick_head", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> hammerHead = ITEMS.register("hammer_head", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  // axe
  public static final ItemObject<ToolPartItem> smallAxeHead = ITEMS.register("small_axe_head", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> broadAxeHead = ITEMS.register("broad_axe_head", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  // blades
  public static final ItemObject<ToolPartItem> smallBlade = ITEMS.register("small_blade", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> broadBlade = ITEMS.register("broad_blade", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  // plates
  public static final ItemObject<ToolPartItem> adzeHead = ITEMS.register("adze_head", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> largePlate = ITEMS.register("large_plate", p -> new ToolPartItem(p, HeadMaterialStats.ID), ITEM_PROPS);
  // bows
  public static final ItemObject<ToolPartItem> bowLimb = ITEMS.register("bow_limb", p -> new ToolPartItem(p, LimbMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> bowGrip = ITEMS.register("bow_grip", p -> new ToolPartItem(p, GripMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> bowstring = ITEMS.register("bowstring", p -> new ToolPartItem(p, StatlessMaterialStats.BOWSTRING.getIdentifier()), ITEM_PROPS);
  // ammo
  public static final ItemObject<ToolPartItem> arrowHead = ITEMS.register("arrow_head", p -> new ToolPartItem(p, StatlessMaterialStats.ARROW_HEAD.getIdentifier()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> arrowShaft = ITEMS.register("arrow_shaft", p -> new ToolPartItem(p, StatlessMaterialStats.ARROW_SHAFT.getIdentifier()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> fletching = ITEMS.register("fletching", p -> new ToolPartItem(p, StatlessMaterialStats.FLETCHING.getIdentifier()), ITEM_PROPS);
  // other parts
  public static final ItemObject<ToolPartItem> toolBinding = ITEMS.register("tool_binding", p -> new ToolPartItem(p, StatlessMaterialStats.BINDING.getIdentifier()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> toughBinding = ITEMS.register("tough_binding", p -> new ToolPartItem(p, StatlessMaterialStats.BINDING.getIdentifier()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> toolHandle = ITEMS.register("tool_handle", p -> new ToolPartItem(p, HandleMaterialStats.ID), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> toughHandle = ITEMS.register("tough_handle", p -> new ToolPartItem(p, HandleMaterialStats.ID), ITEM_PROPS);
  // armor
  public static final EnumObject<ArmorType,ToolPartItem> plating = ITEMS.registerEnum(new ArmorType[]{ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS}, "plating", (type, p) -> new ToolPartItem(p, PlatingMaterialStats.TYPES.get(type.ordinal()).getId()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> maille = ITEMS.register("maille", p -> new ToolPartItem(p, StatlessMaterialStats.MAILLE.getIdentifier()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> shieldCore = ITEMS.register("shield_core", p -> new ToolPartItem(p, StatlessMaterialStats.SHIELD_CORE.getIdentifier()), ITEM_PROPS);
  // slimesuit
  public static final ItemObject<ToolPartItem> ribcage = ITEMS.register("ribcage", p -> new ToolPartItem(p, RepairStats.RIBCAGE.getId()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> shell = ITEMS.register("shell", p -> new ToolPartItem(p, RepairStats.SHELL.getId()), ITEM_PROPS);
  public static final ItemObject<ToolPartItem> laces = ITEMS.register("laces", p -> new ToolPartItem(p, RepairStats.LACES.getId()), ITEM_PROPS);



  // block entities
  public static final DeferredHolder<? super BlockEntityType<MaterialBlockEntity>, BlockEntityType<MaterialBlockEntity>> materialBlock = BLOCK_ENTITIES.register("material_block", MaterialBlockEntity::new, fakeStorageBlock);

  // loot
  public static final DeferredHolder<MapCodec<? extends LootPoolEntryContainer>, MapCodec<ToolPartLootEntry>> toolPartLootEntry = LOOT_ENTRIES.register("tool_part", () -> ToolPartLootEntry.CODEC);

  /** Adds all relevant items to the creative tab */
  private static void addTabItems(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output tab) {
    Consumer<ItemStack> output = tab::accept;
    accept(output, repairKit);
    // fake ingot is in materials tab, helps the illusion
    // small heads
    accept(output, pickHead);
    accept(output, smallAxeHead);
    accept(output, smallBlade);
    accept(output, adzeHead);
    // large heads
    accept(output, hammerHead);
    accept(output, broadAxeHead);
    accept(output, broadBlade);
    accept(output, largePlate);
    // binding and rods
    accept(output, toolHandle);
    accept(output, toolBinding);
    accept(output, toughHandle);
    accept(output, toughBinding);
    // ranged
    accept(output, bowLimb);
    accept(output, bowGrip);
    accept(output, bowstring);
    accept(output, arrowHead);
    accept(output, arrowShaft);
    accept(output, fletching);
    // plating, pair each one with the dummy plating item
    for (ArmorType type : ArmorType.values()) {
      if (type == ArmorType.BODY) continue; // 1.21's BODY (animal armor) type has no Tinkers plating; the EnumObjects only hold the 4 humanoid types
      tab.accept(TinkerSmeltery.dummyPlating.get(type));
      plating.get(type).addVariants(output, "");
    }
    accept(output, maille);
    accept(output, shieldCore);
    // slimesuit
    accept(output, ribcage);
    accept(output, shell);
    accept(output, laces);

    // end with modifier crystal dynamic listing
    ModifierCrystalItem.addVariants(output);
  }

  /** Adds a tool part to the tab */
  private static void accept(Consumer<ItemStack> output, Supplier<? extends IMaterialItem> item) {
    item.get().addVariants(output, "");
  }
}
