package slimeknights.tconstruct.library.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;

/**
 * Class containing all of Tinkers Construct recipe types
 */
public class TinkerRecipeTypes {
  /** Deferred instance */
  private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TConstruct.MOD_ID);

  public static final DeferredHolder<RecipeType<?>, RecipeType<IPartBuilderRecipe>> PART_BUILDER = register("part_builder");
  public static final DeferredHolder<RecipeType<?>, RecipeType<MaterialRecipe>> MATERIAL = register("material");
  public static final DeferredHolder<RecipeType<?>, RecipeType<ITinkerStationRecipe>> TINKER_STATION = register("tinker_station");
  public static final DeferredHolder<RecipeType<?>, RecipeType<IModifierWorktableRecipe>> MODIFIER_WORKTABLE = register("modifier_worktable");

  // casting
  public static final DeferredHolder<RecipeType<?>, RecipeType<ICastingRecipe>> CASTING_BASIN = register("casting_basin");
  public static final DeferredHolder<RecipeType<?>, RecipeType<ICastingRecipe>> CASTING_TABLE = register("casting_table");
  public static final DeferredHolder<RecipeType<?>, RecipeType<MoldingRecipe>> MOLDING_TABLE = register("molding_table");
  public static final DeferredHolder<RecipeType<?>, RecipeType<MoldingRecipe>> MOLDING_BASIN = register("molding_basin");

  // smeltery
  public static final DeferredHolder<RecipeType<?>, RecipeType<IMeltingRecipe>> MELTING = register("melting");
  public static final DeferredHolder<RecipeType<?>, RecipeType<EntityMeltingRecipe>> ENTITY_MELTING = register("entity_melting");
  public static final DeferredHolder<RecipeType<?>, RecipeType<MeltingFuel>> FUEL = register("fuel");
  public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyRecipe>> ALLOYING = register("alloying");

  // modifiers
  public static final DeferredHolder<RecipeType<?>, RecipeType<SeveringRecipe>> SEVERING = register("severing");

  /** Internal recipe type for recipes that are not pulled by any specific crafting block */
  public static final DeferredHolder<RecipeType<?>, RecipeType<Recipe<?>>> DATA = register("data");

  /** Initializes the deferred register */
  public static void init(IEventBus bus) {
    TYPES.register(bus);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OnDatapackSyncEvent.class, TinkerRecipeTypes::sendRecipesToClient);
  }

  /**
   * Requests the recipe types the client needs. Since 1.21.2 the client has no recipe manager, and the server only sends the recipes of types requested here.
   * On the client, the recipes are read through {@link slimeknights.mantle.recipe.helper.RecipeHelper#recipes(net.minecraft.world.level.Level)}.
   * <ul>
   *   <li>Part builder, tinker station and modifier worktable: screen recipe lists and result previews, JEI</li>
   *   <li>Material: material item lookups ({@link slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache}), part builder, books, JEI</li>
   *   <li>Casting, molding, melting, entity melting, fuel, alloying and severing: JEI categories, books and the client side recipe caches</li>
   *   <li>Data: material casting fluids ({@link slimeknights.tconstruct.library.recipe.casting.material.MaterialFluidRecipe}) and modifier salvage</li>
   *   <li>Crafting: crafting station result, book crafting pages and the JEI repair kit display. Mantle requests it too, NeoForge merges requests</li>
   * </ul>
   * Registered on {@link NeoForge#EVENT_BUS} by {@link #init(IEventBus)}.
   */
  public static void sendRecipesToClient(OnDatapackSyncEvent event) {
    event.sendRecipes(
      PART_BUILDER.get(),
      MATERIAL.get(),
      TINKER_STATION.get(),
      MODIFIER_WORKTABLE.get(),
      CASTING_BASIN.get(),
      CASTING_TABLE.get(),
      MOLDING_TABLE.get(),
      MOLDING_BASIN.get(),
      MELTING.get(),
      ENTITY_MELTING.get(),
      FUEL.get(),
      ALLOYING.get(),
      SEVERING.get(),
      DATA.get(),
      RecipeType.CRAFTING
    );
  }

  /**
   * Registers a new recipe type, prefixing with the mod ID
   * @param name  Recipe type name
   * @param <T>   Recipe type
   * @return  Registered recipe type
   */
  static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> register(String name) {
    return TYPES.register(name, () -> new RecipeType<T>() {
      @Override
      public String toString() {
        return TConstruct.MOD_ID + ":" + name;
      }
    });
  }
}
