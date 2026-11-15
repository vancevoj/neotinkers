package slimeknights.tconstruct.smeltery.block.entity.module;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Module handling fuel consumption for the melter and smeltery
 */
@RequiredArgsConstructor
public abstract class FuelModule implements ContainerData {
  /** Parent TE */
  protected final MantleBlockEntity parent;

  /** Last fuel recipe used */
  @Nullable
  private MeltingFuel lastRecipe;
  /**
   * Last fluid handler where fluid was extracted.
   * In NeoForge capabilities are nullable rather than {@code LazyOptional}; this caches the most recent block
   * capability and is reset via {@link #resetHandler(ResourceHandler)} on structure/neighbor changes.
   */
  @Nullable
  protected ResourceHandler<FluidResource> fluidHandler;

  /** Current amount of fluid in the TE */
  @Getter
  protected int fuel = 0;
  /** Amount of fuel produced by the last source */
  @Getter
  protected int fuelQuality = 0;
  /** Temperature of the current fuel */
  @Getter
  protected int temperature = 0;
  /** Amount to progress recipes by per time step */
  @Getter
  protected int rate = 0;


  /*
   * Helpers
   */

  /**
   * Server-side caches of the fluid handler at each fuel-tank position. {@link BlockCapabilityCache} auto-tracks
   * capability invalidation (neighbor change, BE unload/reload, etc.) and refetches, restoring the self-healing
   * behavior upstream had with {@code LazyOptional} invalidation listeners. Only valid on a {@link ServerLevel};
   * the client uses a fresh {@link Level#getCapability} query each call (see {@link #getHandlerAt}).
   */
  private final Map<BlockPos,BlockCapabilityCache<ResourceHandler<FluidResource>,?>> serverHandlerCaches = new HashMap<>();

  /** Called when the capability invalidates to reset any cached handlers */
  protected void resetHandler(@Nullable ResourceHandler<FluidResource> source) {
    if (source == null || source == fluidHandler) {
      fluidHandler = null;
    }
  }

  /** Gets a nonnull world instance from the parent */
  protected Level getLevel() {
    return Objects.requireNonNull(parent.getLevel(), "Parent tile entity has null world");
  }

  /**
   * Resolves the fluid handler at the given position in a way that survives world reloads and capability invalidation.
   * On the server a {@link BlockCapabilityCache} is used so the handler self-heals (refetched automatically when the
   * neighbor tank's capability invalidates). On the client (where the fuel DISPLAY runs) we cannot build a capability
   * cache (it requires a {@link ServerLevel}), so we re-query every call and never permanently cache a null or stale
   * handler - this is what makes the GUI recover after a world reload.
   * @param pos  Position to resolve, should not be mutated externally
   * @return  Fluid handler, or null if none present
   */
  @Nullable
  protected ResourceHandler<FluidResource> getHandlerAt(BlockPos pos) {
    Level level = getLevel();
    if (level instanceof ServerLevel serverLevel) {
      // immutable copy as the cache holds the reference long-term
      BlockPos key = pos.immutable();
      return serverHandlerCaches.computeIfAbsent(key, p -> BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, serverLevel, p, null)).getCapability();
    }
    // client: always re-query so a transiently-missing capability (e.g. right after a reload) recovers on a later tick
    return level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
  }

  /** Clears any server-side capability caches; called when the set of relevant positions may have changed */
  protected void invalidateHandlerCaches() {
    serverHandlerCaches.clear();
  }


  /**
   * Finds a recipe for the given fluid
   * @param fluid  Fluid
   * @return  Recipe
   */
  @Nullable
  protected MeltingFuel findRecipe(Fluid fluid) {
    if (lastRecipe != null && lastRecipe.matches(fluid)) {
      return lastRecipe;
    }
    MeltingFuel recipe = MeltingFuelLookup.findFuel(fluid);
    if (recipe != null) {
      lastRecipe = recipe;
    }
    return recipe;
  }


  /* Solid fuel */

  /**
   * Checks if the given stack is a solid fuel. Safe to call on both sides, but cannot tell how long the fuel lasts.
   * @param stack  Stack to check
   * @return  True if the stack has a fuel burn time
   */
  public static boolean isSolidFuel(ItemStack stack) {
    return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
  }

  /**
   * Gets the furnace burn time of the given solid fuel. Replacement for the removed burn time hook, resolves the cooking fuel component
   * using a container loot context like the vanilla furnace.
   * @param stack        Fuel stack
   * @param level        Server level
   * @param blockEntity  Block entity holding the fuel, used for the loot context
   * @return  Burn time in ticks, 0 if not a fuel
   */
  public static int getBurnTime(ItemStack stack, ServerLevel level, BlockEntity blockEntity) {
    if (!isSolidFuel(stack)) {
      return 0;
    }
    LootContext context = new LootContext.Builder(
      new LootParams.Builder(level)
        .withParameter(LootContextParams.BLOCK_STATE, blockEntity.getBlockState())
        .withParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockEntity.getBlockPos()))
        .withParameter(LootContextParams.CONTAINER, slot -> null)
        .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
        .create(LootContextParamSets.CONTAINER_PROCESS)
    ).create(Optional.empty());
    return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
  }


  /* Fuel attributes */

  /**
   * Checks if we have fuel
   * @return  True if we have fuel
   */
  public boolean hasFuel() {
    return fuel > 0;
  }

  /**
   * Consumes fuel from the module
   * @param amount  Amount of fuel to consume
   */
  public void decreaseFuel(int amount) {
    fuel = Math.max(0, fuel - amount);
    parent.setChangedFast();
  }


  /* Fuel updating */

  /**
   * Gets the fluid in the first tank of the given handler
   * @param handler  Handler to check
   * @return  Fluid in the first tank, or empty if the handler has no tanks
   */
  protected static FluidStack getFirstFluid(ResourceHandler<FluidResource> handler) {
    return handler.size() > 0 ? FluidUtil.getStack(handler, 0) : FluidStack.EMPTY;
  }

  /**
   * Gets the capacity of the first tank of the given handler
   * @param handler  Handler to check
   * @return  Capacity of the first tank, or 0 if the handler has no tanks
   */
  protected static int getFirstCapacity(ResourceHandler<FluidResource> handler) {
    return handler.size() > 0 ? handler.getCapacityAsInt(0, FluidResource.EMPTY) : 0;
  }

  /**
   * Trys to consume fuel from the given fluid handler
   * @param handler  Handler to consume fuel from
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  protected int tryLiquidFuel(ResourceHandler<FluidResource> handler, boolean consume) {
    FluidStack fluid = getFirstFluid(handler);
    if (fluid.isEmpty()) {
      return 0;
    }
    MeltingFuel recipe = findRecipe(fluid.getFluid());
    if (recipe != null) {
      int amount = recipe.getAmount(fluid.getFluid());
      if (fluid.getAmount() >= amount) {
        if (consume) {
          int drained = 0;
          if (amount > 0) {
            try (Transaction transaction = Transaction.openRoot()) {
              drained = handler.extract(FluidResource.of(fluid), amount, transaction);
              transaction.commit();
            }
          }
          if (drained != amount) {
            TConstruct.LOG.error("Invalid amount of fuel drained from tank");
          }
          fuel += recipe.getDuration();
          fuelQuality = recipe.getDuration();
          temperature = recipe.getTemperature();
          rate = recipe.getRate();
          parent.setChangedFast();
          return temperature;
        } else {
          return recipe.getTemperature();
        }
      }
    }
    return 0;
  }

  /**
   * Attempts to consume fuel from one of the tanks
   * @return  temperature of the found fluid, 0 if none
   */
  public abstract int findFuel(boolean consume);

  /* NBT */
  private static final String TAG_FUEL = "fuel";
  private static final String TAG_TEMPERATURE = "temperature";
  private static final String TAG_RATE = "rate";

  /**
   * Reads the fuel from NBT
   * @param nbt  Tag to read from
   */
  public void readFromTag(CompoundTag nbt) {
    if (nbt.get(TAG_FUEL) instanceof NumericTag fuelTag) {
      fuel = fuelTag.intValue();
    }
    if (nbt.get(TAG_TEMPERATURE) instanceof NumericTag temperatureTag) {
      temperature = temperatureTag.intValue();
      rate = nbt.getIntOr(TAG_RATE, 0);
    }
  }

  /**
   * Reads the fuel from the given input, same keys as {@link #readFromTag(CompoundTag)}
   * @param input  Input to read from
   */
  public void readFromTag(ValueInput input) {
    input.getInt(TAG_FUEL).ifPresent(value -> fuel = value);
    input.getInt(TAG_TEMPERATURE).ifPresent(value -> {
      temperature = value;
      rate = input.getIntOr(TAG_RATE, 0);
    });
  }

  /**
   * Writes the fuel to NBT
   * @param nbt  Tag to write to
   * @return  Tag written to
   */
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt.putInt(TAG_FUEL, fuel);
    nbt.putInt(TAG_TEMPERATURE, temperature);
    nbt.putInt(TAG_RATE, rate);
    return nbt;
  }

  /**
   * Writes the fuel to the given output, same keys as {@link #writeToTag(CompoundTag)}
   * @param output  Output to write to
   */
  public void writeToTag(ValueOutput output) {
    output.putInt(TAG_FUEL, fuel);
    output.putInt(TAG_TEMPERATURE, temperature);
    output.putInt(TAG_RATE, rate);
  }


  /* UI syncing */
  private static final int FUEL = 0;
  private static final int FUEL_QUALITY = 1;
  private static final int TEMPERATURE = 2;
  private static final int RATE = 3;

  @Override
  public int getCount() {
    return 4;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case FUEL         -> fuel;
      case FUEL_QUALITY -> fuelQuality;
      case TEMPERATURE  -> temperature;
      case RATE         -> rate;
      default -> 0;
    };
  }

  @Override
  public void set(int index, int value) {
    switch (index) {
      case FUEL         -> fuel = value;
      case FUEL_QUALITY -> fuelQuality = value;
      case TEMPERATURE  -> temperature = value;
      case RATE         -> rate = value;
    }
  }

  /**
   * Called client side to get the fuel info for the current tank
   * Note this relies on the client side fuel handlers containing fuel, which is common for our blocks as show fluid in world.
   * If a tank does not do that this won't work.
   * @return  Fuel info
   */
  public FuelInfo getFuelInfo() {
    ResourceHandler<FluidResource> handler = fluidHandler;
    if (handler == null) {
      return FuelInfo.EMPTY;
    }
    FluidStack fluid = getFirstFluid(handler);
    int temperature = 0;
    if (!fluid.isEmpty()) {
      MeltingFuel fuel = findRecipe(fluid.getFluid());
      if (fuel != null) {
        temperature = fuel.getTemperature();
      }
    }
    // if no liquid, fallback to either item or empty
    return FuelInfo.of(fluid, getFirstCapacity(handler), temperature);
  }

  /** Data class to hold information about the current fuel */
  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class FuelInfo {
    /** Empty fuel instance */
    public static final FuelInfo EMPTY = new FuelInfo(FluidStack.EMPTY, 0, 0, 0);
    /** Item fuel instance, doesn't really matter what data we set as it will all be ignored */
    public static final FuelInfo ITEM = new FuelInfo(FluidStack.EMPTY, 0, 0, 0);

    private final FluidStack fluid;
    private int totalAmount;
    private int capacity;
    private final int temperature;

    /**
     * Gets fuel info from the given stack and capacity
     * @param fluid     Fluid
     * @param capacity  Capacity
     * @return  Fuel info
     */
    public static FuelInfo of(FluidStack fluid, int capacity, int temperature) {
      if (fluid.isEmpty()) {
        return EMPTY;
      }
      return new FuelInfo(fluid, fluid.getAmount(), Math.max(capacity, fluid.getAmount()), temperature);
    }

    /**
     * Adds an additional amount and capacity to this info
     * @param amount    Amount to add
     * @param capacity  Capacity to add
     */
    protected void add(int amount, int capacity) {
      this.totalAmount += amount;
      this.capacity += capacity;
    }

    /**
     * Checks if this fuel info is an item
     * @return  True if an item
     */
    public boolean isItem() {
      return this == ITEM;
    }

    /** Checks if this fuel info has no fluid */
    public boolean isEmpty() {
      return fluid.isEmpty() || totalAmount == 0 || capacity == 0;
    }
  }
}
