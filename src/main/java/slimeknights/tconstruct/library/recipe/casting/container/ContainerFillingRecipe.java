package slimeknights.tconstruct.library.recipe.casting.container;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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

  /** Since 26.1 the type aware serializer is not a serializer, see {@link #getSerializer()} */
  private final TypeAwareRecipeSerializer<?> serializer;
  @Getter
  private final Identifier id;
  @Getter
  private final String group;
  private final int fluidAmount;
  private final Item container;

  @Override
  public RecipeType<? extends Recipe<ICastingContainer>> getType() {
    return ICastingRecipe.castingType(serializer);
  }

  @Override
  public RecipeSerializer<? extends Recipe<ICastingContainer>> getSerializer() {
    return ICastingRecipe.castingSerializer(serializer);
  }

  /** Gets the fluid to fill into the container. 1.21: copies the full stack so data components (e.g. potion contents) are kept, the 1.20 equivalent of the fluid tag */
  private FluidStack getFillFluid(ICastingContainer inv) {
    FluidStack fluid = inv.getFluidStack();
    if (fluid.isEmpty()) {
      return new FluidStack(inv.getFluid(), this.fluidAmount);
    }
    return fluid.copyWithAmount(this.fluidAmount);
  }

  /** Result of filling a copy of a container, see {@link #fill(ItemStack, FluidStack)} */
  private record FillResult(int filled, ItemStack container) {}

  /**
   * Fills a copy of the given container with the given fluid, replacing the old fluid handler simulate and execute actions.
   * The copy lives in a private single slot handler, so the container is free to change item when filled (e.g. bucket to filled bucket).
   * The transaction is never committed, as the result is read before it closes; it is nested in the current transaction if one is open, as opening a root would throw.
   * @param stack  Container stack, not modified
   * @param fluid  Fluid to fill
   * @return  Amount filled and the resulting container. If the stack is not a fluid container, fills nothing and returns a copy of the stack.
   */
  private static FillResult fill(ItemStack stack, FluidStack fluid) {
    if (stack.isEmpty()) {
      return new FillResult(0, ItemStack.EMPTY);
    }
    ItemStacksResourceHandler slot = new ItemStacksResourceHandler(1);
    slot.set(0, ItemResource.of(stack), stack.getCount());
    ResourceHandler<FluidResource> handler = ItemAccess.forHandlerIndexStrict(slot, 0).getCapability(Capabilities.Fluid.ITEM);
    if (handler == null || fluid.isEmpty()) {
      return new FillResult(0, stack.copy());
    }
    Transaction.Lifecycle lifecycle = Transaction.getLifecycle();
    if (lifecycle != Transaction.Lifecycle.NONE && lifecycle != Transaction.Lifecycle.OPEN) {
      // a transaction is closing on this thread, so we cannot open one to fill the container
      return new FillResult(0, stack.copy());
    }
    @SuppressWarnings("deprecation")
    TransactionContext parent = lifecycle == Transaction.Lifecycle.OPEN ? Transaction.getCurrentOpenedTransaction() : null;
    try (Transaction transaction = Transaction.open(parent)) {
      int filled = handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
      return new FillResult(filled, ItemUtil.getStack(slot, 0));
    }
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    return fill(inv.getStack(), getFillFluid(inv)).filled();
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
    if (stack.getItem() != this.container.asItem()) {
      return false;
    }
    return fill(stack, getFillFluid(inv)).filled() > 0;
  }

  /** @deprecated use {@link ICastingRecipe#assemble(net.minecraft.world.item.crafting.RecipeInput, HolderLookup.Provider)} */
  @Override
  @Deprecated
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(this.container);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access) {
    return fill(inv.getStack(), getFillFluid(inv)).container();
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
      ItemStack empty = new ItemStack(container);
      List<ItemStack> results = fluids.stream().map(fluid -> fill(empty, fluid).container()).toList();
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
