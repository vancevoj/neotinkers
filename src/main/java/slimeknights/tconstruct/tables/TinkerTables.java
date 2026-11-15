package slimeknights.tconstruct.tables;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.SimpleRecipeSerializer;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.ItemPartRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.PartRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.recycle.PartBuilderRecycle;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.FixedMaterialSwappingRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.MaterialValueSwappingRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.PartSwappingOverrideRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolMaterialSwappingRecipe;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.shared.block.TableBlock;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.block.ChestBlock;
import slimeknights.tconstruct.tables.block.CraftingStationBlock;
import slimeknights.tconstruct.tables.block.GenericTableBlock;
import slimeknights.tconstruct.tables.block.ScorchedAnvilBlock;
import slimeknights.tconstruct.tables.block.TinkerStationBlock;
import slimeknights.tconstruct.tables.block.TinkersAnvilBlock;
import slimeknights.tconstruct.tables.block.TinkersChestBlock;
import slimeknights.tconstruct.tables.block.entity.chest.CastChestBlockEntity;
import slimeknights.tconstruct.tables.block.entity.chest.PartChestBlockEntity;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;
import slimeknights.tconstruct.tables.block.entity.table.ModifierWorktableBlockEntity;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.data.TableRecipeProvider;
import slimeknights.tconstruct.tables.item.AnvilBlockItem;
import slimeknights.tconstruct.tables.item.RetexturedTableBlockItem;
import slimeknights.tconstruct.tables.item.TinkersChestBlockItem;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;
import slimeknights.tconstruct.tables.menu.ModifierWorktableContainerMenu;
import slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu;
import slimeknights.tconstruct.tables.menu.TinkerChestContainerMenu;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.recipe.PartBuilderToolRecycle;
import slimeknights.tconstruct.tables.recipe.TinkerStationDamagingRecipe;
import slimeknights.tconstruct.tables.recipe.TinkerStationPartSwapping;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles all the table for tool creation
 */
@SuppressWarnings("unused")
public final class TinkerTables extends TinkerModule {
  /** Creative tab for general items, or those that lack another tab */
  public static final DeferredHolder<? super CreativeModeTab, CreativeModeTab> tabTables = CREATIVE_TABS.register(
    "tables", () -> CreativeModeTab.builder().title(TConstruct.makeTranslation("itemGroup", "tables"))
      .icon(() -> new ItemStack(TinkerTables.tinkersAnvil))
      .displayItems(TinkerTables::addTableVariants)
      .withTabsBefore(TinkerWorld.tabWorld.getId())
      .withSearchBar()
      .build());
  /*
   * Blocks
   */
  public static final ItemObject<TableBlock> craftingStation, tinkerStation, partBuilder, tinkersChest, partChest;
  /** Block item for retextured tables, adds the texture tooltip as blocks no longer have a tooltip hook */
  private static final BiFunction<Block, Item.Properties, BlockItem> TABLE_ITEM = RetexturedTableBlockItem::new;
  static {
    // properties need the block ID since 1.21.2, so each table gets a new instance
    Supplier<Block.Properties> WOOD_TABLE = () -> builder(MapColor.WOOD, SoundType.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.0F, 5.0F).noOcclusion();
    craftingStation = BLOCKS.register("crafting_station", CraftingStationBlock::new, WOOD_TABLE, TABLE_ITEM);
    tinkerStation = BLOCKS.register("tinker_station", props -> new TinkerStationBlock(props, 4), WOOD_TABLE, TABLE_ITEM);
    partBuilder = BLOCKS.register("part_builder", props -> new GenericTableBlock(props, PartBuilderBlockEntity::new), WOOD_TABLE, TABLE_ITEM);
    tinkersChest = BLOCKS.register("tinkers_chest", props -> new TinkersChestBlock(props, TinkersChestBlockEntity::new, true), WOOD_TABLE, TinkersChestBlockItem::new);
    partChest = BLOCKS.register("part_chest", props -> new ChestBlock(props, PartChestBlockEntity::new, true), WOOD_TABLE, BLOCK_ITEM);
  }

  public static final ItemObject<TableBlock> castChest, modifierWorktable;
  static {
    Supplier<Block.Properties> STONE_TABLE = () -> builder(MapColor.COLOR_GRAY, SoundType.METAL).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 9.0F).noOcclusion();
    castChest = BLOCKS.register("cast_chest", props -> new ChestBlock(props, CastChestBlockEntity::new, false), STONE_TABLE, BLOCK_ITEM);
    modifierWorktable = BLOCKS.register("modifier_worktable", props -> new GenericTableBlock(props, ModifierWorktableBlockEntity::new), STONE_TABLE, TABLE_ITEM);
  }

  public static final ItemObject<TableBlock> tinkersAnvil, scorchedAnvil;
  static {
    Supplier<Block.Properties> METAL_TABLE = () -> builder(MapColor.COLOR_GRAY, SoundType.ANVIL).pushReaction(PushReaction.IMMOVEABLE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).noOcclusion();
    BiFunction<Block, Item.Properties, BlockItem> blockItem = (block, props) -> new AnvilBlockItem(block, props, TinkerToolParts.fakeStorageBlockItem, TinkerTags.Materials.COMPATABILITY_ALLOYS);
    tinkersAnvil = BLOCKS.register("tinkers_anvil", props -> new TinkersAnvilBlock(props, 6), METAL_TABLE, blockItem);
    scorchedAnvil = BLOCKS.register("scorched_anvil", props -> new ScorchedAnvilBlock(props, 6), METAL_TABLE, blockItem);
  }
  /*
   * Items
   */
  public static final ItemObject<Item> pattern = ITEMS.register("pattern", ITEM_PROPS);

  /*
   * Tile entites
   */
  public static final DeferredHolder<? super BlockEntityType<CraftingStationBlockEntity>, BlockEntityType<CraftingStationBlockEntity>> craftingStationTile = BLOCK_ENTITIES.register("crafting_station", CraftingStationBlockEntity::new, craftingStation);
  public static final DeferredHolder<? super BlockEntityType<TinkerStationBlockEntity>, BlockEntityType<TinkerStationBlockEntity>> tinkerStationTile = BLOCK_ENTITIES.register("tinker_station", TinkerStationBlockEntity::new, builder ->
    builder.add(tinkerStation.get(), tinkersAnvil.get(), scorchedAnvil.get()));
  public static final DeferredHolder<? super BlockEntityType<PartBuilderBlockEntity>, BlockEntityType<PartBuilderBlockEntity>> partBuilderTile = BLOCK_ENTITIES.register("part_builder", PartBuilderBlockEntity::new, partBuilder);
  public static final DeferredHolder<? super BlockEntityType<ModifierWorktableBlockEntity>, BlockEntityType<ModifierWorktableBlockEntity>> modifierWorktableTile = BLOCK_ENTITIES.register("modifier_worktable", ModifierWorktableBlockEntity::new, modifierWorktable);
  // legacy name as tile entities cannot be remapped
  public static final DeferredHolder<? super BlockEntityType<TinkersChestBlockEntity>, BlockEntityType<TinkersChestBlockEntity>> tinkersChestTile = BLOCK_ENTITIES.register("modifier_chest", TinkersChestBlockEntity::new, tinkersChest);
  public static final DeferredHolder<? super BlockEntityType<PartChestBlockEntity>, BlockEntityType<PartChestBlockEntity>> partChestTile = BLOCK_ENTITIES.register("part_chest", PartChestBlockEntity::new, partChest);
  public static final DeferredHolder<? super BlockEntityType<CastChestBlockEntity>, BlockEntityType<CastChestBlockEntity>> castChestTile = BLOCK_ENTITIES.register("cast_chest", CastChestBlockEntity::new, castChest);

  /*
   * Containers
   */
  public static final DeferredHolder<? super MenuType<CraftingStationContainerMenu>, MenuType<CraftingStationContainerMenu>> craftingStationContainer = MENUS.register("crafting_station", CraftingStationContainerMenu::new);
  public static final DeferredHolder<? super MenuType<TinkerStationContainerMenu>, MenuType<TinkerStationContainerMenu>> tinkerStationContainer = MENUS.register("tinker_station", TinkerStationContainerMenu::new);
  public static final DeferredHolder<? super MenuType<PartBuilderContainerMenu>, MenuType<PartBuilderContainerMenu>> partBuilderContainer = MENUS.register("part_builder", PartBuilderContainerMenu::new);
  public static final DeferredHolder<? super MenuType<ModifierWorktableContainerMenu>, MenuType<ModifierWorktableContainerMenu>> modifierWorktableContainer = MENUS.register("modifier_worktable", ModifierWorktableContainerMenu::new);
  public static final DeferredHolder<? super MenuType<TinkerChestContainerMenu>, MenuType<TinkerChestContainerMenu>> tinkerChestContainer = MENUS.register("tinker_chest", TinkerChestContainerMenu::new);

  /*
   * Recipes
   */
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaterialRecipe>> materialRecipeSerializer = RECIPE_SERIALIZERS.register("material", () -> LoadableRecipeSerializer.of(MaterialRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolBuildingRecipe>> toolBuildingRecipeSerializer = RECIPE_SERIALIZERS.register("tool_building", () -> LoadableRecipeSerializer.of(ToolBuildingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TinkerStationPartSwapping>> tinkerStationPartSwappingSerializer = RECIPE_SERIALIZERS.register("tinker_station_part_swapping", () -> LoadableRecipeSerializer.of(TinkerStationPartSwapping.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TinkerStationDamagingRecipe>> tinkerStationDamagingSerializer = RECIPE_SERIALIZERS.register("tinker_station_damaging", () -> LoadableRecipeSerializer.of(TinkerStationDamagingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FixedMaterialSwappingRecipe>> fixedMaterialSwapping = RECIPE_SERIALIZERS.register("fixed_material_swapping", () -> LoadableRecipeSerializer.of(FixedMaterialSwappingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaterialValueSwappingRecipe>> materialValueSwapping = RECIPE_SERIALIZERS.register("material_value_swapping", () -> LoadableRecipeSerializer.of(MaterialValueSwappingRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PartSwappingOverrideRecipe>> partSwappingOverride = RECIPE_SERIALIZERS.register("part_swapping_override", () -> LoadableRecipeSerializer.of(PartSwappingOverrideRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolMaterialSwappingRecipe>> toolMaterialSwapping = RECIPE_SERIALIZERS.register("tool_material_swapping", () -> LoadableRecipeSerializer.of(ToolMaterialSwappingRecipe.LOADER));
  @Deprecated
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedMaterialRecipe>> shapedMaterialRecipeSerializer = RECIPE_SERIALIZERS.register("crafting_shaped_material", () -> new ShapedMaterialRecipe.Serializer().toSerializer());
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedMaterialsRecipe>> shapedMaterialsRecipeSerializer = RECIPE_SERIALIZERS.register("crafting_shaped_materials", () -> new ShapedMaterialsRecipe.Serializer().toSerializer());
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapelessMaterialsRecipe>> shapelessMaterialsRecipeSerializer = RECIPE_SERIALIZERS.register("crafting_shapeless_materials", () -> new ShapelessMaterialsRecipe.Serializer().toSerializer());
  // part builder
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PartRecipe>> partRecipeSerializer = RECIPE_SERIALIZERS.register("part_builder", () -> LoadableRecipeSerializer.of(PartRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemPartRecipe>> itemPartBuilderSerializer = RECIPE_SERIALIZERS.register("item_part_builder", () -> LoadableRecipeSerializer.of(ItemPartRecipe.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PartBuilderToolRecycle>> partBuilderToolRecycling = RECIPE_SERIALIZERS.register("part_builder_tool_recycling", () -> LoadableRecipeSerializer.of(PartBuilderToolRecycle.LOADER));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PartBuilderRecycle>> partBuilderDamageableRecycling = RECIPE_SERIALIZERS.register("part_builder_recycling", () -> LoadableRecipeSerializer.of(PartBuilderRecycle.LOADER));
  // repair - standard
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TinkerStationRepairRecipe>> tinkerStationRepairSerializer = RECIPE_SERIALIZERS.register("tinker_station_repair", () -> SimpleRecipeSerializer.of(() -> new TinkerStationRepairRecipe(TConstruct.getResource("tinker_station_repair"))));
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CraftingTableRepairKitRecipe>> craftingTableRepairSerializer = RECIPE_SERIALIZERS.register("crafting_table_repair", () -> SimpleRecipeSerializer.of(() -> new CraftingTableRepairKitRecipe(TConstruct.getResource("crafting_table_repair"))));

  @SubscribeEvent
  void commonSetup(final FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      StationSlotLayoutLoader loader = StationSlotLayoutLoader.getInstance();
      loader.registerRequiredLayout(tinkerStation.getId());
      loader.registerRequiredLayout(tinkersAnvil.getId());
      loader.registerRequiredLayout(scorchedAnvil.getId());
    });
  }

  /** Recipes are a registry since 26.3, so recipe providers are registry bootstraps */
  @SubscribeEvent
  void gatherRegistries(final GatherDataRegistryEntriesEvent event) {
    event.recipe(TableRecipeProvider::new);
  }

  /** Adds all relevant items to the creative tab, called in the general tab */
  public static void addTabItems(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
    // add one of each standard table
    output.accept(craftingStation);
    output.accept(partBuilder);
    output.accept(tinkerStation);
    output.accept(tinkersAnvil);
    output.accept(scorchedAnvil);
    output.accept(modifierWorktable);

    // chests, have less variants so go first
    output.accept(tinkersChest);
    output.accept(partChest);
    output.accept(castChest);

    // pattern last, so its below the nice row of 9
    output.accept(pattern);
  }

  /** Adds all relevant items to the variants creative tab. */
  private static void addTableVariants(ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
    // put anvil variants first as they are not in another tab and have the least variants
    Consumer<ItemStack> consumer = output::accept;
    ((IMaterialItem) tinkersAnvil.asItem()).addVariants(consumer, "");
    ((IMaterialItem) scorchedAnvil.asItem()).addVariants(consumer, "");

    // table variants at the end as there may be a lot
    Predicate<ItemStack> variants = stack -> {
      output.accept(stack);
      return false;
    };
    // wooden tables
    RetexturedHelper.addTagVariants(variants, craftingStation, ItemTags.LOGS);
    RetexturedHelper.addTagVariants(variants, partBuilder, ItemTags.PLANKS);
    RetexturedHelper.addTagVariants(variants, tinkerStation, ItemTags.PLANKS);
    // stone tables
    RetexturedHelper.addTagVariants(variants, modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK);
    // end with smeltery controller and drain variants
    TinkerSmeltery.addTableVariants(itemDisplayParameters, output);
  }
}
