package slimeknights.tconstruct.plugin.jei.melting;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootPredicates;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.recipe.helper.IngredientHelper;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MeltingFuelHandler {
  /**
   * List of pairs of temperature and list of fluids with that or greater temperature
   * Sorted from highest to lowest temperature
   */
  private static List<Pair<Integer,List<FluidStack>>> fuelLookup = Collections.emptyList();
  /** List of all solid fuels from the JEI recipe manager. */
  private static List<ItemStack> allSolidFuels = List.of();
  /** Map of fuel durations for all stacks */
  private static Object2IntMap<Object> fuelDurations = Object2IntMaps.emptyMap();
  /** Ingredient showing examples of fuels */
  private static final Ingredient FUEL_EXAMPLES = IngredientHelper.tag(TinkerTags.Items.FUEL_EXAMPLES);
  /** List of solid fuels for solid melting examples */
  public static final Lazy<List<ItemStack>> SOLID_FUELS = Lazy.of(() -> IngredientHelper.getStacks(FUEL_EXAMPLES));
  /** Item stack helper for grabbing cache keys */
  private static IIngredientHelper<ItemStack> itemHelper = null;

  /**
   * Updates the melting cache, called on JEI load.
   * TODO 1.21: fix method name.
   * @param fuels  List of fuel recipes
   */
  public static void setMeltngFuels(List<MeltingFuel> fuels) {
    // sort the fuels highest first
    fuels.sort(Comparator.comparingInt(MeltingFuel::getTemperature));
    // get a list of temperature to fuel
    fuelLookup = fuels.stream()
      .mapToInt(MeltingFuel::getTemperature)
      .distinct()
      .mapToObj((temperature) -> Pair.of(temperature, fuels.stream()
        .filter(fuel -> fuel.getTemperature() >= temperature && !fuel.getInputs().isEmpty())
        .flatMap(fuel -> fuel.getInputs().stream())
        .toList()))
      .toList();
  }

  /**
   * Gets a fluid stack list for the given temperature
   * @param temperature  Recipe temperature
   * @return  List of fuels for the given temperature
   */
  public static List<FluidStack> getUsableFuels(int temperature) {
    // first fuel list with a temperature bigger is the one to use
    for (Pair<Integer, List<FluidStack>> pair : fuelLookup) {
      if (temperature <= pair.getFirst()) {
        return pair.getSecond();
      }
    }
    return Collections.emptyList();
  }


  /* Solid fuels */

  /**
   * Gets the registries used to resolve cooking fuel burn times.
   * Burn times are context providers in a reloadable registry, which is not synced to clients.
   * @return  Registries containing the cooking fuel providers
   */
  private static HolderGetter.Provider getFuelRegistries() {
    // singleplayer and LAN hosts have the real data pack values
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
      return server.reloadableRegistries().lookup();
    }
    // remote clients only know the synced registries, so rebuild the vanilla defaults like JEI does for its own fuel category
    return new RegistrySetBuilder()
      .add(Registries.PREDICATE, LootPredicates::bootstrap)
      .add(Registries.CONTEXT_INT_PROVIDER, ContextIntProviders::bootstrap)
      .build(RecipeHelper.registryAccess());
  }

  /**
   * Gets the burn time of a solid fuel on the client.
   * Replacement for the removed burn time hook, resolves the cooking fuel component like {@link slimeknights.tconstruct.smeltery.block.entity.module.FuelModule#getBurnTime}
   * using the heater block state. The client has no server level, which the cooking fuel providers do not need.
   * @param stack       Fuel stack
   * @param registries  Registries from {@link #getFuelRegistries()}
   * @return  Burn time in ticks, 0 if not a fuel
   */
  private static int getBurnTime(ItemStack stack, HolderGetter.Provider registries) {
    if (!stack.has(DataComponents.COOKING_FUEL)) {
      return 0;
    }
    ContextMap params = ContextMap.builder()
      .set(LootContextParams.BLOCK_STATE, TinkerSmeltery.searedHeater.get().defaultBlockState())
      .set(NeoForgeLootContextParams.QUERIED_STACK, stack)
      .build();
    // needs the LootContext constructor access transformer, the builder requires a server level
    LootContext context = new LootContext(new LootParams(null, params, Map.of(), 0), RandomSource.create(), registries);
    return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
  }

  /** Sets the solid fuels from the given fuel stacks */
  public static void registerSolidFuels(IIngredientManager ingredientManager) {
    Collection<ItemStack> allStacks = ingredientManager.getAllItemStacks();
    List<ItemStack> fuels = new ArrayList<>(allStacks.size());
    Object2IntMap<Object> newFuels = new Object2IntOpenHashMap<>(allStacks.size());
    itemHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
    // 26.3: per recipe type burn times are gone, fuels use the furnace cooking fuel component
    HolderGetter.Provider registries = getFuelRegistries();
    for (ItemStack stack : allStacks) {
      try {
        int burnTime = getBurnTime(stack, registries);
        if (burnTime > 0) {
          fuels.add(stack);
          newFuels.put(itemHelper.getUid(stack, UidContext.Ingredient), burnTime);
        }
      } catch (RuntimeException | LinkageError e) {
        TConstruct.LOG.error("Failed to check if item is fuel {}.", stack, e);
      }
    }
    allSolidFuels = List.copyOf(fuels);
    fuelDurations = Object2IntMaps.unmodifiable(newFuels);
  }

  /** Gets a list of all solid fuels for display in the fuel category */
  public static List<ItemStack> getAllSolidFuels() {
    if (allSolidFuels.isEmpty()) {
      return SOLID_FUELS.get();
    }
    return allSolidFuels;
  }

  /** Gets the duration of the given item stack */
  public static int getFuelDuration(ItemStack stack) {
    if (itemHelper != null) {
      return fuelDurations.getOrDefault(itemHelper.getUid(stack, UidContext.Ingredient), 0);
    }
    return 0;
  }
}
