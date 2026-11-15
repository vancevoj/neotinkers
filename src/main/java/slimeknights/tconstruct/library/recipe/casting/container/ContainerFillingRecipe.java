package slimeknights.tconstruct.library.recipe.casting.container;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;

import java.util.List;

/**
 * Casting recipe that takes an arbitrary fluid for a given amount and fills a container
 */
@RequiredArgsConstructor
public class ContainerFillingRecipe implements ICastingRecipe, IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<ContainerFillingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IntLoadable.FROM_ONE.requiredField("fluid_amount", r -> r.fluidAmount),
    Loadables.ITEM.requiredField("container", r -> r.container),
    ContainerFillingRecipe::new);

  @Getter
  private final TypeAwareRecipeSerializer<?> serializer;
  @Getter
  private final ResourceLocation id;
  @Getter
  private final String group;
  private final int fluidAmount;
  private final Item container;

  @Override
  public RecipeType<?> getType() {
    return serializer.getType();
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    Fluid fluid = inv.getFluid();
    IFluidHandlerItem handler = inv.getStack().getCapability(Capabilities.FluidHandler.ITEM);
    if (handler == null) {
      return 0;
    }
    return handler.fill(new FluidStack(fluid, this.fluidAmount), FluidAction.SIMULATE);
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
    return 5;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level worldIn) {
    ItemStack stack = inv.getStack();
    Fluid fluid = inv.getFluid();
    if (stack.getItem() != this.container.asItem()) {
      return false;
    }
    IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
    return handler != null && handler.fill(new FluidStack(fluid, this.fluidAmount), FluidAction.SIMULATE) > 0;
  }

  /** @deprecated use {@link ICastingRecipe#assemble(Container, HolderLookup.Provider)} */
  @Override
  @Deprecated
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.container);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    ItemStack stack = inv.getStack().copy();
    IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
    if (handler == null) {
      return stack;
    }
    handler.fill(new FluidStack(inv.getFluid(), this.fluidAmount), FluidAction.EXECUTE);
    return handler.getContainer();
  }

  /* Display */
  /** Cache of items to display for this container */
  private List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // filter fluid registry to just fluids we care about
      List<FluidStack> fluids = BuiltInRegistries.FLUID.stream()
        .filter(fluid -> {
          // skip flowing fluids (redundant to source), fluids with no bucket (probably internal), and fluids hidden from recipe viewers (they be hidden)
          if (fluid.isSource(fluid.defaultFluidState()) && !fluid.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS)) {
            try {
              Item bucket = fluid.getBucket();
              return bucket != Items.AIR && !bucket.builtInRegistryHolder().is(TinkerTags.Items.HIDDEN_IN_RECIPE_VIEWERS);
            } catch (Exception e) {
              // Registrate (popular dependency for making registration easier) is broken and throws in getBucket for fluids with no bucket
              // we could just skip the bucket check, but its just going to throw when we try to fill an empty bucket in map below
            }
          }
          return false;
        })
        .map(fluid -> new FluidStack(fluid, fluidAmount))
        .toList();
      // fill the container with each fluid stack
      List<ItemStack> results = fluids.stream().map(fluid -> {
        ItemStack stack = new ItemStack(container);
        return FluidUtil.getFluidHandler(stack).map(handler -> {
          handler.fill(fluid, FluidAction.EXECUTE);
          return handler.getContainer();
        }).orElse(stack);
      }).toList();
      displayRecipes = List.of(DisplayCastingRecipe.from(this)
        // cast is just an empty container, which is "consumed"
        .cast(new ItemStack(container)).consumed()
        .fluids(fluids).results(results).linkFluidsToOutput()
        .coolingTime(5)
        .build());
    }
    return displayRecipes;
  }
}
