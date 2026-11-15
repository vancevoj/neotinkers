package slimeknights.tconstruct.plugin.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.IExtendableCraftingRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.fluids.fluids.PotionFluidType;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.library.recipe.display.VanillaFilteredRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.IDisplayMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.OverslimeCraftingTableRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayCraftingTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayToolTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.plugin.jei.casting.CastingBasinCategory;
import slimeknights.tconstruct.plugin.jei.casting.CastingRecipeManager;
import slimeknights.tconstruct.plugin.jei.casting.CastingTableCategory;
import slimeknights.tconstruct.plugin.jei.entity.DefaultEntityMeltingRecipe;
import slimeknights.tconstruct.plugin.jei.entity.EntityMeltingRecipeCategory;
import slimeknights.tconstruct.plugin.jei.entity.SeveringCategory;
import slimeknights.tconstruct.plugin.jei.material.MaterialCategory;
import slimeknights.tconstruct.plugin.jei.material.MaterialIconIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.material.MaterialIngredientHelper;
import slimeknights.tconstruct.plugin.jei.material.OverslimeRecipeExtension;
import slimeknights.tconstruct.plugin.jei.material.ShapedMaterialsExtension;
import slimeknights.tconstruct.plugin.jei.material.ShapelessMaterialsExtension;
import slimeknights.tconstruct.plugin.jei.melting.FoundryCategory;
import slimeknights.tconstruct.plugin.jei.melting.FuelCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingCategory;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierBookmarkIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierRecipeCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierWorktableCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringExtension;
import slimeknights.tconstruct.plugin.jei.partbuilder.MaterialItemList;
import slimeknights.tconstruct.plugin.jei.partbuilder.PartBuilderCategory;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientHelper;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.transfer.CraftingStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.TinkerStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.ToolInventoryTransferInfo;
import slimeknights.tconstruct.plugin.jei.util.GuiContainerTankHandler;
import slimeknights.tconstruct.plugin.jei.util.PotionSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.TankHidingIngredientListener;
import slimeknights.tconstruct.plugin.jei.util.ToolPartSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.ToolSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.manager.SimpleItemRecipeManager;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.client.screen.AlloyerScreen;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.MelterScreen;
import slimeknights.tconstruct.smeltery.data.SmelteryCompat;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.client.ToolContainerScreen;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static slimeknights.mantle.util.RetexturedHelper.addTagVariants;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  /** Recipes that are meant as jokes and tend to confuse players, so are hidden */
  private static final Identifier[] EASTER_EGG_RECIPES = {
    TConstruct.getResource("tables/tinkers_forge"),
    TConstruct.getResource("tables/scorched_forge"),
    TConstruct.getResource("tables/seared_forge_material"),
    TConstruct.getResource("tables/scorched_forge_material")
  };
  /** @deprecated no longer used */
  @Deprecated(forRemoval = true)
  public static IModIdHelper modIdHelper;

  @Override
  public Identifier getPluginUid() {
    return TConstructJEIConstants.PLUGIN;
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registry) {
    final IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
    // casting
    registry.addRecipeCategories(new CastingBasinCategory(guiHelper));
    registry.addRecipeCategories(new CastingTableCategory(guiHelper));
    registry.addRecipeCategories(new MoldingRecipeCategory(guiHelper));
    // melting and casting
    registry.addRecipeCategories(new MeltingCategory(guiHelper));
    registry.addRecipeCategories(new AlloyRecipeCategory(guiHelper));
    registry.addRecipeCategories(new EntityMeltingRecipeCategory(guiHelper));
    registry.addRecipeCategories(new FoundryCategory(guiHelper));
    registry.addRecipeCategories(new FuelCategory(guiHelper));
    // tinker station
    registry.addRecipeCategories(new ModifierRecipeCategory(guiHelper));
    registry.addRecipeCategories(new SeveringCategory(guiHelper));
    registry.addRecipeCategories(new ToolBuildingCategory(guiHelper));
    registry.addRecipeCategories(new ToolTinkeringCategory(guiHelper));
    ToolTinkeringExtension.prepareDrawables(guiHelper);
    // part builder
    registry.addRecipeCategories(new MaterialCategory(guiHelper));
    registry.addRecipeCategories(new PartBuilderCategory(guiHelper));
    // modifier worktable
    registry.addRecipeCategories(new ModifierWorktableCategory(guiHelper));
  }

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    List<ModifierEntry> modifiers = Collections.emptyList();
    if (Config.CLIENT.showModifiersInJEI.get()) {
      modifiers = ModifierRecipeLookup.getRecipeModifierList();
    }
    // JEI 31 requires a codec for each ingredient type, used for bookmarks and its config
    registration.register(TConstructJEIConstants.MODIFIER_TYPE, modifiers, new ModifierIngredientHelper(), ModifierBookmarkIngredientRenderer.INSTANCE, TConstructJEIConstants.MODIFIER_CODEC);
    registration.register(TConstructJEIConstants.MATERIAL_TYPE, List.of(), new MaterialIngredientHelper(), MaterialIconIngredientRenderer.INSTANCE, TConstructJEIConstants.MATERIAL_CODEC);
    registration.register(TConstructJEIConstants.PATTERN_TYPE, List.of(), new PatternIngredientHelper(), PatternIngredientRenderer.INSTANCE, TConstructJEIConstants.PATTERN_CODEC);
    List<SlotCount> slots = SlotType.getAllSlotTypes().stream().map(type -> new SlotCount(type, 1)).toList();
    SlotIngredientRenderer.clearCache();
    registration.register(TConstructJEIConstants.SLOT_TYPE, slots, new SlotIngredientHelper(), SlotIngredientRenderer.INGREDIENT, TConstructJEIConstants.SLOT_CODEC);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry) {
    // 1.21: JEI crafting extensions are stateless and receive the recipe holder, so register a single instance per recipe class
    IExtendableCraftingRecipeCategory craftingCategory = registry.getCraftingCategory();
    craftingCategory.addExtension(ShapedMaterialRecipe.class, new ShapedMaterialExtension());
    craftingCategory.addExtension(ShapedMaterialsRecipe.class, ShapedMaterialsExtension.create());
    craftingCategory.addExtension(ShapelessMaterialsRecipe.class, new ShapelessMaterialsExtension());
    craftingCategory.addExtension(OverslimeCraftingTableRecipe.class, new OverslimeRecipeExtension());
    craftingCategory.addExtension(IDisplayCraftingTinkering.class, new ToolTinkeringExtension());
  }

  /**
   * Gets the recipes available on the client. Since 1.21.2 the client has no recipe manager, it only has the recipe types the server sends,
   * see {@link slimeknights.tconstruct.library.recipe.TinkerRecipeTypes#sendRecipesToClient(net.neoforged.neoforge.event.OnDatapackSyncEvent)}.
   */
  private static RecipeMap getRecipes(Level level) {
    return RecipeHelper.recipes(level);
  }

  /** Gets a list of unfiltered casting recipes to give to JEI. */
  private static List<IDisplayableCastingRecipe> getCastingRecipes(RegistryAccess access, RecipeMap recipes, Supplier<? extends RecipeType<ICastingRecipe>> recipeType) {
    return FilteredRecipe.unfiltered(RecipeHelper.getJEIRecipes(access, recipes, recipeType.get(), IDisplayableCastingRecipe.class));
  }

  /** Finds the holder of the first recipe of the given type matching the given class */
  @Nullable
  private static <I extends RecipeInput, R extends Recipe<I>> RecipeHolder<R> findFirstHolder(RecipeMap recipes, RecipeType<R> type, Class<?> clazz) {
    return recipes.byType(type).stream().filter(holder -> clazz.isInstance(holder.value())).findFirst().orElse(null);
  }

  /** Finds the first recipe of the given type */
  @Nullable
  private static <T, I extends RecipeInput, R extends Recipe<I>> T findFirst(RecipeMap recipes, RecipeType<R> type, Class<T> clazz) {
    RecipeHolder<R> holder = findFirstHolder(recipes, type, clazz);
    return holder == null ? null : clazz.cast(holder.value());
  }

  /** Wraps the given display recipe in a holder using its crafting table ID, as the JEI crafting category works on holders. */
  private static RecipeHolder<CraftingRecipe> toHolder(IDisplayCraftingTinkering recipe) {
    return new RecipeHolder<>(RecipeHelper.key(recipe.getId()), recipe);
  }

  /** Wraps the given display recipes in holders using their crafting table ID, as the 1.21 JEI crafting category works on holders. */
  private static <T extends IDisplayCraftingTinkering> List<RecipeHolder<T>> toHolders(List<T> recipes) {
    return recipes.stream().map(recipe -> new RecipeHolder<>(RecipeHelper.key(recipe.getId()), recipe)).toList();
  }

  @Override
  public void registerRecipes(IRecipeRegistration register) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeMap manager = getRecipes(level);
    // casting
    register.addRecipes(TConstructJEIConstants.CASTING_BASIN, getCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_BASIN));
    register.addRecipes(TConstructJEIConstants.CASTING_TABLE, getCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_TABLE));

    // melting
    // need to register the fuels before the categories so they are available
    MeltingFuelHandler.registerSolidFuels(register.getIngredientManager());
    List<MeltingFuel> fuels = RecipeHelper.getRecipes(manager, TinkerRecipeTypes.FUEL.get(), MeltingFuel.class);
    MeltingFuelHandler.setMeltngFuels(fuels);
    register.addRecipes(TConstructJEIConstants.FUEL, fuels);
    // register melting recipes
    List<IDisplayableMeltingRecipe> meltingRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MELTING.get(), IDisplayableMeltingRecipe.class);
    register.addRecipes(TConstructJEIConstants.MELTING, meltingRecipes);
    register.addRecipes(TConstructJEIConstants.FOUNDRY, meltingRecipes);

    // entity melting
    List<EntityMeltingRecipe> entityMeltingRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.ENTITY_MELTING.get(), EntityMeltingRecipe.class);
    // generate a "default" recipe for all other entity types
    entityMeltingRecipes.add(new DefaultEntityMeltingRecipe(entityMeltingRecipes));
    register.addRecipes(TConstructJEIConstants.ENTITY_MELTING, entityMeltingRecipes);

    // alloying
    register.addRecipes(TConstructJEIConstants.ALLOY, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.ALLOYING.get(), AlloyRecipe.class));

    // molding
    List<MoldingRecipe> moldingRecipes = new ArrayList<>();
    moldingRecipes.addAll(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_TABLE.get(), MoldingRecipe.class));
    moldingRecipes.addAll(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_BASIN.get(), MoldingRecipe.class));
    register.addRecipes(TConstructJEIConstants.MOLDING, moldingRecipes);

    // modifiers
    List<IDisplayModifierRecipe> modifierRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class)
                                                               .stream()
                                                               .sorted((r1, r2) -> {
                                                                 SlotType t1 = r1.getSlotType();
                                                                 SlotType t2 = r2.getSlotType();
                                                                 String n1 = t1 == null ? "zzzzzzzzzz" : t1.getName();
                                                                 String n2 = t2 == null ? "zzzzzzzzzz" : t2.getName();
                                                                 return n1.compareTo(n2);
                                                               }).collect(Collectors.toList());
    register.addRecipes(TConstructJEIConstants.MODIFIERS, modifierRecipes);
    register.addRecipes(TConstructJEIConstants.TOOL_MODIFICATION, FilteredRecipe.unfiltered(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class)));

    // beheading
    register.addRecipes(TConstructJEIConstants.SEVERING, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.SEVERING.get(), SeveringRecipe.class));

    // tool building
    List<ToolBuildingRecipe> toolBuilding = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), ToolBuildingRecipe.class)
      .stream()
      .sorted(Comparator.comparingInt(r -> StationSlotLayoutLoader.getInstance().get(r.getLayoutSlotId()).getSortIndex()))
      .toList();
    register.addRecipes(TConstructJEIConstants.TOOL_BUILDING, toolBuilding);

    // materials
    register.addRecipes(TConstructJEIConstants.MATERIALS, Stream.<IDisplayMaterialRecipe>concat(
        MaterialRecipeCache.getSortedRecipes().stream(),
        Stream.concat(MaterialCastingLookup.getSortedCastingFluids().stream(), MaterialCastingLookup.getSortedCompositeFluids().stream()))
      .sorted(Comparator.comparing(IDisplayMaterialRecipe::getMaterial)).toList());

    // part builder
    MaterialItemList.setRecipes(List.of()); // list of recipes is ignored as this whole class is getting ditched in 1.21; it just clears cache right now
    register.addRecipes(TConstructJEIConstants.PART_BUILDER, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.PART_BUILDER.get(), IDisplayPartBuilderRecipe.class));

    // modifier worktable
    register.addRecipes(TConstructJEIConstants.MODIFIER_WORKTABLE, RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), IModifierWorktableRecipe.class)
      .stream().filter(recipe -> {
        if (recipe.getModifierOptions(null).isEmpty()) {
          // 1.21: recipes no longer carry their ID, so log the recipe itself
          TConstruct.LOG.debug("Hiding Modifier Worktable recipe {} as it has no valid modifiers", recipe);
          return false;
        }
        return true;
      }).toList());

    // copy tinker station repair recipes to the crafting table
    RecipeHolder<CraftingRecipe> craftingRepair = findFirstHolder(manager, RecipeType.CRAFTING, CraftingTableRepairKitRecipe.class);
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (craftingRepair != null && tinkerRepair != null) {
      Identifier id = craftingRepair.id().identifier();
      List<IDisplayCraftingTinkering> recipes = tinkerRepair.getRecipes(access);
      List<RecipeHolder<CraftingRecipe>> newRecipes = new ArrayList<>();
      TinkerStationRepairRecipe.setCraftingId(recipes, id);
      for (IDisplayCraftingTinkering recipe : recipes) {
        if (!recipe.isFiltered()) {
          newRecipes.add(toHolder(recipe));
        }
      }
      if (!newRecipes.isEmpty()) {
        register.addRecipes(RecipeTypes.CRAFTING, newRecipes);
      }
    }

    // add an ingredient listener to hide tanks when fluids are hidden
    IIngredientManager ingredientManager = register.getIngredientManager();
    ingredientManager.registerIngredientListener(new TankHidingIngredientListener(ingredientManager, Stream.concat(
      Stream.of(TinkerSmeltery.copperCan, TinkerSmeltery.searedLantern, TinkerSmeltery.scorchedLantern),
      Stream.concat(TinkerSmeltery.searedTank.values().stream(), TinkerSmeltery.scorchedTank.values().stream())
    ).map(ItemLike::asItem).toList()));
  }

  /** Gets a list of filtered casting recipes to give to JEI. */
  private static List<IDisplayableCastingRecipe> getFilteredCastingRecipes(RegistryAccess access, RecipeMap recipes, Supplier<? extends RecipeType<ICastingRecipe>> recipeType) {
    return FilteredRecipe.filtered(RecipeHelper.getJEIRecipes(access, recipes, recipeType.get(), IDisplayableCastingRecipe.class));
  }

  @Override
  public void registerAdvanced(IAdvancedRegistration registration) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeMap manager = getRecipes(level);

    IIngredientManager ingredientManager = registration.getJeiHelpers().getIngredientManager();
    registration.addSimpleRecipeManagerPlugin(TConstructJEIConstants.CASTING_BASIN, new CastingRecipeManager(ingredientManager, getFilteredCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_BASIN)));
    registration.addSimpleRecipeManagerPlugin(TConstructJEIConstants.CASTING_TABLE, new CastingRecipeManager(ingredientManager, getFilteredCastingRecipes(access, manager, TinkerRecipeTypes.CASTING_TABLE)));
    registration.addSimpleRecipeManagerPlugin(TConstructJEIConstants.TOOL_MODIFICATION, new SimpleItemRecipeManager<>(ingredientManager, FilteredRecipe.filtered(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class))));

    // copy tinker station repair recipes to the crafting table
    List<IDisplayCraftingTinkering> craftingRecipes = VanillaFilteredRecipe.getRecipes(access, manager, RecipeType.CRAFTING, IDisplayCraftingTinkering.class);
    // copy tinker repair recipes to the crafting table
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (tinkerRepair != null) {
      List<IDisplayCraftingTinkering> recipes = tinkerRepair.getRecipes(access);
      for (IDisplayCraftingTinkering recipe : recipes) {
        if (recipe.isFiltered()) {
          craftingRecipes.add(recipe);
        }
      }
    }
    // add the plugin if we found anything
    if (!craftingRecipes.isEmpty()) {
      registration.addSimpleRecipeManagerPlugin(RecipeTypes.CRAFTING, SimpleItemRecipeManager.createCrafting(ingredientManager, toHolders(craftingRecipes)));
    }
  }

  /** Adds a table as a catalys */
  private static void addTableCatalyst(IRecipeCatalystRegistration registry, ItemLike table, TagKey<Item> tag, boolean addDefault, IRecipeType<?>... types) {
    List<ItemStack> list = new ArrayList<>();
    // add default variant
    if (addDefault) list.add(new ItemStack(table));
    RetexturedHelper.addTagVariants(stack -> {
      list.add(stack);
      return false;
    }, table, tag);
    for (IRecipeType<?> type : types) {
      registry.addCraftingStations(type, VanillaTypes.ITEM_STACK, list);
    }
  }

  /** Adds an item as a crafting station for each of the given recipe types */
  private static void addCatalyst(IRecipeCatalystRegistration registry, ItemLike item, IRecipeType<?>... types) {
    for (IRecipeType<?> type : types) {
      registry.addCraftingStation(type, item);
    }
  }

  /** Adds a stack as a crafting station for each of the given recipe types */
  private static void addCatalyst(IRecipeCatalystRegistration registry, ItemStack stack, IRecipeType<?>... types) {
    for (IRecipeType<?> type : types) {
      registry.addCraftingStation(type, stack);
    }
  }

  /**
   * Adds an item as a casting catalyst, and as a molding catalyst if it has molding recipes
   * @param registry     Catalyst registry
   * @param item         Item to add
   * @param ownCategory  Category to always add
   * @param type         Molding recipe type
   */
  private static void addCastingCatalyst(IRecipeCatalystRegistration registry, ItemLike item, IRecipeType<IDisplayableCastingRecipe> ownCategory, RecipeType<MoldingRecipe> type) {
    registry.addCraftingStation(ownCategory, item);
    assert Minecraft.getInstance().level != null;
    if (!getRecipes(Minecraft.getInstance().level).byType(type).isEmpty()) {
      registry.addCraftingStation(TConstructJEIConstants.MOLDING, item);
    }
  }

  /** Adds all entries from the given modifier tag to catalysts for the given recipe type. */
  private static void addModifierCatalyst(IRecipeCatalystRegistration registry, TagKey<Modifier> tag, IRecipeType<?>... types) {
    for (Modifier modifier : ModifierManager.getTagValues(tag)) {
      ModifierEntry entry = new ModifierEntry(modifier, 1);
      for (IRecipeType<?> type : types) {
        registry.addCraftingStation(type, TConstructJEIConstants.MODIFIER_TYPE, entry);
      }
    }
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
    // tables
    addTableCatalyst(registry, TinkerTables.craftingStation, ItemTags.LOGS, true, RecipeTypes.CRAFTING);
    addTableCatalyst(registry, TinkerTables.partBuilder, ItemTags.PLANKS, true, TConstructJEIConstants.PART_BUILDER);
    addTableCatalyst(registry, TinkerTables.tinkerStation, ItemTags.PLANKS, true, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.tinkersAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.scorchedAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK, true, TConstructJEIConstants.MODIFIER_WORKTABLE);

    // smeltery
    addCatalyst(registry, TinkerSmeltery.searedMelter, TConstructJEIConstants.MELTING, TConstructJEIConstants.FUEL);
    // the heater burns furnace fuels
    addCatalyst(registry, TinkerSmeltery.searedHeater, RecipeTypes.SMELTING_FUEL, TConstructJEIConstants.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.searedTable, TConstructJEIConstants.CASTING_TABLE, TinkerRecipeTypes.MOLDING_TABLE.get());
    addCastingCatalyst(registry, TinkerSmeltery.searedBasin, TConstructJEIConstants.CASTING_BASIN, TinkerRecipeTypes.MOLDING_BASIN.get());
    addTableCatalyst(registry, TinkerSmeltery.smelteryController, TinkerTags.Items.SMELTERY_BRICKS, false, TConstructJEIConstants.MELTING, TConstructJEIConstants.ALLOY, TConstructJEIConstants.ENTITY_MELTING, TConstructJEIConstants.FUEL);

    // foundry
    addCatalyst(registry, TinkerSmeltery.scorchedAlloyer, TConstructJEIConstants.ALLOY, TConstructJEIConstants.FUEL);
    addCastingCatalyst(registry, TinkerSmeltery.scorchedTable, TConstructJEIConstants.CASTING_TABLE, TinkerRecipeTypes.MOLDING_TABLE.get());
    addCastingCatalyst(registry, TinkerSmeltery.scorchedBasin, TConstructJEIConstants.CASTING_BASIN, TinkerRecipeTypes.MOLDING_BASIN.get());
    addTableCatalyst(registry, TinkerSmeltery.foundryController, TinkerTags.Items.FOUNDRY_BRICKS, false, TConstructJEIConstants.FOUNDRY, TConstructJEIConstants.FUEL);

    // modifiers
    addModifierCatalyst(registry, TinkerTags.Modifiers.CRAFTING, RecipeTypes.CRAFTING);
    addModifierCatalyst(registry, TinkerTags.Modifiers.SMELTING, RecipeTypes.SMELTING);
    addModifierCatalyst(registry, TinkerTags.Modifiers.SEVERING, TConstructJEIConstants.SEVERING);
    addModifierCatalyst(registry, TinkerTags.Modifiers.MELTING, TConstructJEIConstants.MELTING, TConstructJEIConstants.ENTITY_MELTING);
    // add items containing these modifiers to start as catalysts as well
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MODIFIABLE)) {
      if (item.value() instanceof IModifiableDisplay modifiable) {
        // add any tools with a severing trait to severing
        ModifierNBT traits = ToolTraitHook.getTraits(modifiable.getToolDefinition(), MaterialNBT.EMPTY);
        if (traits.has(TinkerTags.Modifiers.CRAFTING)) {
          addCatalyst(registry, modifiable.getRenderTool(), RecipeTypes.CRAFTING);
        }
        if (traits.has(TinkerTags.Modifiers.SMELTING)) {
          addCatalyst(registry, modifiable.getRenderTool(), RecipeTypes.SMELTING);
        }
        if (traits.has(TinkerTags.Modifiers.SEVERING)) {
          addCatalyst(registry, modifiable.getRenderTool(), TConstructJEIConstants.SEVERING);
        }
        // add any tools with a melting trait to melting
        if (traits.has(TinkerTags.Modifiers.MELTING)) {
          // only add to entity melting if its melee too
          if (item.is(TinkerTags.Items.MELEE)) {
            addCatalyst(registry, modifiable.getRenderTool(), TConstructJEIConstants.MELTING, TConstructJEIConstants.ENTITY_MELTING);
          } else {
            addCatalyst(registry, modifiable.getRenderTool(), TConstructJEIConstants.MELTING);
          }
        }
      }
    }
  }

  /** Converts an empty subtype string to null, which JEI 31 uses for no subtype (was an empty string) */
  @Nullable
  private static String nullIfEmpty(String subtype) {
    return subtype.isEmpty() ? null : subtype;
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registry) {
    // retexturable blocks
    ISubtypeInterpreter<ItemStack> tables = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        return nullIfEmpty(RetexturedHelper.getTextureName(stack));
      }
      return null;
    };
    registry.registerSubtypeInterpreter(TinkerTables.craftingStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.partBuilder.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.tinkerStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.modifierWorktable.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.smelteryController.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedDrain.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedDuct.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedChute.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.foundryController.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedDrain.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedDuct.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedChute.asItem(), tables);

    // anvils have both texture and material blocks
    ISubtypeInterpreter<ItemStack> anvils = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        String name = RetexturedHelper.getTextureName(stack);
        if (!name.isEmpty()) {
          return '#' + name;
        }
        return ToolPartSubtypeInterpreter.INSTANCE.getSubtypeData(stack, UidContext.Ingredient);
      }
      return null;
    };
    registry.registerSubtypeInterpreter(TinkerTables.tinkersAnvil.asItem(), anvils);
    registry.registerSubtypeInterpreter(TinkerTables.scorchedAnvil.asItem(), anvils);

    // potions
    registry.registerSubtypeInterpreter(TinkerFluids.potion.asItem(), (PotionSubtypeInterpreter<ItemStack>)(stack -> stack.get(DataComponents.POTION_CONTENTS)));
    registry.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, TinkerFluids.potion.get(), (PotionSubtypeInterpreter<FluidStack>)(fluid -> fluid.get(DataComponents.POTION_CONTENTS)));

    // parts
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
      registry.registerSubtypeInterpreter(item.value(), ToolPartSubtypeInterpreter.INSTANCE);
    }

    // tools
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MULTIPART_TOOL)) {
      Item item = holder.value();
      registry.registerSubtypeInterpreter(item, holder.is(TinkerTags.Items.SINGLEPART_TOOL) ? ToolSubtypeInterpreter.FIRST : ToolSubtypeInterpreter.INGREDIENT);
    }

    // fluid containers have types based on fluid, don't bother with different sizes
    registry.registerSubtypeInterpreter(TinkerSmeltery.copperCan.get(), (stack, context) -> nullIfEmpty(CopperCanItem.getSubtype(stack)));
    ISubtypeInterpreter<ItemStack> tankInterpreter = (stack, context) -> nullIfEmpty(TankItem.getSubtype(stack));
    for (TankType type : TankType.values()) {
      registry.registerSubtypeInterpreter(TinkerSmeltery.searedTank.get(type).asItem(), tankInterpreter);
      registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedTank.get(type).asItem(), tankInterpreter);
    }
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedFluidCannon.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedFluidCannon.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.endFluidCannon.asItem(), tankInterpreter);

    registry.registerSubtypeInterpreter(TinkerModifiers.creativeSlotItem.get(), (stack, context) -> {
      SlotType slotType = CreativeSlotItem.getSlot(stack);
      return slotType != null ? slotType.getName() : null;
    });
    registry.registerSubtypeInterpreter(TinkerModifiers.modifierCrystal.get(), (stack, context) -> {
      ModifierId id = ModifierCrystalItem.getModifier(stack);
      return id == null ? null : id.toString();
    });
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGenericGuiContainerHandler(MelterScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(AlloyerScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(HeatingStructureScreen.class, new GuiContainerTankHandler<>());
    registration.addGenericGuiContainerHandler(ToolContainerScreen.class, new GuiContainerTankHandler<>());
  }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    registration.addRecipeTransferHandler(new CraftingStationTransferInfo());
    IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.MODIFIERS, helper), TConstructJEIConstants.MODIFIERS);
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.TOOL_BUILDING, helper), TConstructJEIConstants.TOOL_BUILDING);
    registration.addRecipeTransferHandler(new ToolInventoryTransferInfo(helper), RecipeTypes.CRAFTING);
  }

  /**
   * Removes a fluid from JEI
   * @param remove  List of ingredients to remove for batching
   * @param fluid   Fluid to remove
   */
  private static void removeFluid(List<FluidStack> remove, Fluid fluid) {
    remove.add(new FluidStack(fluid, FluidType.BUCKET_VOLUME));
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    IIngredientManager manager = jeiRuntime.getIngredientManager();

    // ingredient runtime
    List<ItemStack> removeItems = new ArrayList<>();
    Consumer<ItemStack> removeItem = removeItems::add;
    List<ItemStack> addItems = new ArrayList<>();
    Consumer<ItemStack> addItem = addItems::add;
    // ingredient visibility
    List<ItemStack> hideItems = new ArrayList<>();
    Consumer<ItemStack> hideItem = hideItems::add;
    List<ItemStack> showItems = new ArrayList<>();
    Consumer<ItemStack> showItem = showItems::add;
    // shown via the modifiers
    removeItems.add(new ItemStack(TinkerModifiers.modifierCrystal));
    ModifierCrystalItem.addVariants(removeItem);
    // shown via modifier slots
    removeItems.add(new ItemStack(TinkerModifiers.creativeSlotItem));
    TinkerModifiers.creativeSlotItem.get().addVariants(removeItem);

    // tool config filters to 1 material, easiest to just remove all then add back the 1
    String showOnlyTools = Config.CLIENT.showOnlyToolMaterial.get();
    // if the creative is showing just 1, skip the client option
    if (!showOnlyTools.isEmpty()) {
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MODIFIABLE)) {
        if (item.value() instanceof IModifiable modifiable) {
          ToolBuildHandler.addVariants(hideItem, modifiable, "");
          ToolBuildHandler.addVariants(showItem, modifiable, showOnlyTools);
        }
      }
    }
    // parts work the same as tools
    String showOnlyParts = Config.CLIENT.showOnlyPartMaterial.get();
    if (!showOnlyParts.isEmpty()) {
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
        if (item.value() instanceof IMaterialItem part) {
          part.addVariants(hideItem, "");
          part.addVariants(showItem, showOnlyParts);
        }
      }
    }

    // for smeltery and tables, their creative tabs are hidden so the variants only show if we add them
    Predicate<ItemStack> addVariants = stack -> {
      addItems.add(stack);
      return false;
    };
    // wooden
    if (Config.CLIENT.showAllTableVariants.get()) {
      addTagVariants(addVariants, TinkerTables.craftingStation, ItemTags.LOGS);
      addTagVariants(addVariants, TinkerTables.partBuilder, ItemTags.PLANKS);
      addTagVariants(addVariants, TinkerTables.tinkerStation, ItemTags.PLANKS);
      addTagVariants(addVariants, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK);
    }
    // smeltery
    if (Config.CLIENT.showAllSmelteryVariants.get()) {
      addTagVariants(addVariants, TinkerSmeltery.smelteryController, TinkerTags.Items.SMELTERY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.searedDrain, TinkerTags.Items.SMELTERY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.searedDuct, TinkerTags.Items.SMELTERY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.searedChute, TinkerTags.Items.SMELTERY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.foundryController, TinkerTags.Items.FOUNDRY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.scorchedDrain, TinkerTags.Items.FOUNDRY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.scorchedDuct, TinkerTags.Items.FOUNDRY_BRICKS);
      addTagVariants(addVariants, TinkerSmeltery.scorchedChute, TinkerTags.Items.FOUNDRY_BRICKS);
    }
    // anvils are all variants
    if (Config.CLIENT.showAllAnvilVariants.get()) {
      // if true, add all metal variants
      ((IMaterialItem) TinkerTables.tinkersAnvil.asItem()).addVariants(addItem, "");
      ((IMaterialItem) TinkerTables.scorchedAnvil.asItem()).addVariants(addItem, "");
    }

    // fluid containers tab is hidden by tab, add back in the filled containers if requested
    if (Config.CLIENT.showFilledFluidTanks.get()) {
      CopperCanItem.addFilledVariants(addItem);
      TankItem.addFilledVariants(addItem);
    }

    // fluid hiding, buckets are hidden via the creative tab logic
    // hide compat that is not present
    List<FluidStack> removeFluids = new ArrayList<>();
    for (SmelteryCompat compat : SmelteryCompat.values()) {
      // if none of the tags exist, remove the fluid
      if (!compat.isPresent()) {
        FluidObject<?> fluid = compat.getFluid();
        removeItems.add(new ItemStack(fluid));
        removeFluid(removeFluids, fluid.get());
      }
    }
    if (!ModList.get().isLoaded("ceramics")) {
      removeItems.add(new ItemStack(TinkerFluids.moltenPorcelain));
      removeFluid(removeFluids, TinkerFluids.moltenPorcelain.get());
    }

    // add potion fluids for each potion variant if requested
    if (Config.CLIENT.showPotionFluidInJEI.get()) {
      manager.addIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK,
                                      BuiltInRegistries.POTION.listElements().filter(holder -> {
                                        return !holder.is(Potions.WATER) && !holder.is(TinkerTags.Potions.HIDDEN_FLUID);
                                      }).map(holder -> PotionFluidType.potionFluid(holder.key(), FluidType.BUCKET_VOLUME)).toList());
    }
    // remove variantless potion fluid
    removeFluid(removeFluids, TinkerFluids.potion.get());

    // update ingredients as requested
    if (!removeItems.isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, removeItems);
    }
    if (!addItems.isEmpty()) {
      manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, addItems);
    }
    IIngredientVisibility visibility = jeiRuntime.getJeiHelpers().getIngredientVisibility();
    if (!hideItems.isEmpty()) {
      visibility.hideIngredients(VanillaTypes.ITEM_STACK, hideItems, Set.of(UidContext.Ingredient));
    }
    if (!showItems.isEmpty()) {
      visibility.unhideIngredients(VanillaTypes.ITEM_STACK, showItems, Set.of(UidContext.Ingredient));
    }
    manager.removeIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK, removeFluids);

    // hide easter egg recipes
    Level level = SafeClientAccess.getLevel();
    if (level != null) {
      RecipeMap recipes = getRecipes(level);
      @SuppressWarnings("unchecked")
      List<RecipeHolder<CraftingRecipe>> easterEggs = Arrays.stream(EASTER_EGG_RECIPES)
        .flatMap(id -> Stream.ofNullable(recipes.byKey(RecipeHelper.key(id))))
        .filter(holder -> holder.value() instanceof CraftingRecipe)
        .map(holder -> (RecipeHolder<CraftingRecipe>) holder)
        .toList();
      if (!easterEggs.isEmpty()) {
        jeiRuntime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, easterEggs);
      }
    }

    modIdHelper = jeiRuntime.getJeiHelpers().getModIdHelper();
  }
}
