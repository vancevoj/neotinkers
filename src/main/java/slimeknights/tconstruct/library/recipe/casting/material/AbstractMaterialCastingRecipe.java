package slimeknights.tconstruct.library.recipe.casting.material;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.recipe.casting.AbstractCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Casting recipe that takes an arbitrary fluid of a given amount and set the material on the output based on that fluid
 */
public abstract class AbstractMaterialCastingRecipe extends AbstractCastingRecipe {
  protected static final LoadableField<Integer,AbstractMaterialCastingRecipe> ITEM_COST_FIELD = IntLoadable.FROM_ONE.requiredField("item_cost", r -> r.itemCost);

  @Getter
  private final RecipeSerializer<?> serializer;
  protected final int itemCost;
  protected Optional<MaterialFluidRecipe> cachedFluidRecipe = Optional.empty();

  public AbstractMaterialCastingRecipe(TypeAwareRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient cast, int itemCost, boolean consumed, boolean switchSlots) {
    super(serializer.getType(), id, group, cast, consumed, switchSlots);
    this.serializer = serializer;
    this.itemCost = itemCost;
  }

  /** Gets the material fluid recipe for the given recipe */
  protected Optional<MaterialFluidRecipe> getMaterialFluid(ICastingContainer inv) {
    return MaterialCastingLookup.getCastingFluid(inv);
  }

  /** Gets the cached fluid recipe if it still matches, refetches if not */
  protected Optional<MaterialFluidRecipe> getCachedMaterialFluid(ICastingContainer inv) {
    Optional<MaterialFluidRecipe> fluidRecipe = cachedFluidRecipe;
    if (fluidRecipe.filter(recipe -> recipe.matches(inv)).isEmpty()) {
      fluidRecipe = getMaterialFluid(inv);
      if (fluidRecipe.isPresent()) {
        cachedFluidRecipe = fluidRecipe;
      }
    }
    return fluidRecipe;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return getCachedMaterialFluid(inv)
      .map(recipe -> ICastingRecipe.calcCoolingTime(recipe.getTemperature(), recipe.getFluidAmount(inv.getFluid()) * itemCost))
      .orElse(1);
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    return getCachedMaterialFluid(inv)
             .map(recipe -> recipe.getFluidAmount(inv.getFluid()))
             .orElse(1) * this.itemCost;
  }

  /** Resizes the list of the fluids with respect to the item cost */
  protected List<FluidStack> resizeFluids(List<FluidStack> fluids) {
    if (itemCost != 1) {
      return fluids.stream()
                   .map(fluid -> new FluidStack(fluid, fluid.getAmount() * itemCost))
                   .collect(Collectors.toList());
    }
    return fluids;
  }
}
