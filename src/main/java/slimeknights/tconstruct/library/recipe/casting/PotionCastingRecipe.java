package slimeknights.tconstruct.library.recipe.casting;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe for casting a fluid onto an item, copying the fluid potion data to the item.
 * TODO 1.21: move to {@link slimeknights.tconstruct.library.recipe.casting.potion}
 */
public class PotionCastingRecipe implements ICastingRecipe, IMultiRecipe<IDisplayableCastingRecipe> {
  protected static final LoadableField<FluidIngredient, PotionCastingRecipe> FLUID_FIELD = FluidIngredient.LOADABLE.requiredField("fluid", r -> r.fluid);
  protected static final LoadableField<Integer, PotionCastingRecipe> COOLING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooling_time", 5, r -> r.coolingTime);
  public static final RecordLoadable<PotionCastingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("bottle", r -> r.bottle),
    FLUID_FIELD,
    Loadables.ITEM.requiredField("result", r -> r.result),
    COOLING_TIME_FIELD,
    PotionCastingRecipe::new);

  /** Tag key written by the molten potion fluid, see {@code PotionFluidType}. TODO(neoport): migrate potion fluid storage to the {@code PotionContents} data component. */
  protected static final String TAG_POTION = "Potion";

  @Getter
  protected final TypeAwareRecipeSerializer<?> serializer;
  /** Recipe ID, retained for JEI display purposes (vanilla 1.21 recipes no longer expose their own ID). */
  @Getter
  protected final ResourceLocation id;
  @Getter
  protected final String group;
  /** Input on the casting table, always consumed */
  protected final Ingredient bottle;
  /** Potion ingredient, typically just the potion tag */
  protected final FluidIngredient fluid;
  /** Potion item result, will be given the proper potion contents */
  protected final Item result;
  /** Cooling time for this recipe, used for tipped arrows */
  protected final int coolingTime;

  public PotionCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient bottle, FluidIngredient fluid, Item result, int coolingTime) {
    this.serializer = serializer;
    this.id = id;
    this.group = group;
    this.bottle = bottle;
    this.fluid = fluid;
    this.result = result;
    this.coolingTime = coolingTime;
    CastingRecipeLookup.registerCastable(result);
  }

  @Override
  public RecipeType<?> getType() {
    return serializer.getType();
  }

  @Override
  public boolean matches(ICastingContainer inv, Level level) {
    return bottle.test(inv.getStack()) && fluid.test(inv.getFluid());
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    return fluid.getAmount(inv.getFluid());
  }

  @Override
  public boolean isConsumed() {
    return true;
  }

  @Override
  public boolean switchSlots() {
    return false;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return coolingTime;
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack result = new ItemStack(this.result);
    // copy the full contents, not just the potion, so custom effects and colors carry over as the fluid NBT did on 1.20
    PotionContents contents = inv.getFluidStack().get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      result.set(DataComponents.POTION_CONTENTS, contents);
    }
    return result;
  }


  /* JEI */
  // TODO 1.21: consider making this a display recipe instead of a multirecipe
  protected List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      List<Holder.Reference<Potion>> potions = getPotions();
      List<ItemStack> results = new ArrayList<>(potions.size());
      // first, make all the potion items
      for (Holder<Potion> potion : potions) {
        ItemStack result = new ItemStack(this.result);
        result.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        results.add(result);
      }
      // next, it's time to do the fluids
      // we want an order of Mod 1 Potion 1, Mod 1 Potion 2, ..., Mod 2 Potion 1, ...
      // this is potentially displaying multiple fluids from the fluid tag
      List<FluidStack> potionFluids = this.fluid.getFluids();
      int totalSize = results.size() * potionFluids.size();
      List<ItemStack> displayResults = new ArrayList<>(totalSize);
      List<FluidStack> displayFluids = new ArrayList<>(totalSize);
      for (FluidStack fluid : potionFluids) {
        displayResults.addAll(results);
        // 1.21: potion data lives on the fluid's POTION_CONTENTS component rather than fluid NBT
        for (Holder<Potion> potion : potions) {
          FluidStack copy = fluid.copy();
          copy.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
          displayFluids.add(copy);
        }
      }
      this.displayRecipes = List.of(DisplayCastingRecipe.from(this)
        .cast(bottle).consumed()
        .fluids(List.copyOf(displayFluids))
        .results(List.copyOf(displayResults)).linkFluidsToOutput()
        .coolingTime(coolingTime)
        .build());
    }
    return displayRecipes;
  }


  /* Recipe interface methods */

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.of(Ingredient.EMPTY, bottle);
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.result);
  }


  /* JEI helpers */

  /** Gets a list of all potions to display. 1.21 has no empty potion, so water is skipped as the default potion instead. */
  public static List<Holder.Reference<Potion>> getPotions() {
    return BuiltInRegistries.POTION.holders()
      .filter(holder -> !holder.is(Potions.WATER))
      .toList();
  }

  /** Gets a list of all potion IDs ready for usage in building tool persistent data */
  public static List<String> getPotionIds() {
    return getPotions().stream()
      .map(holder -> holder.key().location().toString())
      .toList();
  }
}
