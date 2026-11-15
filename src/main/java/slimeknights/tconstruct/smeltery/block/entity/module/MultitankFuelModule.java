package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.utils.TagUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Fuel module that supports multiple tanks, selecting just one for the fuel result */
public class MultitankFuelModule extends FuelModule implements IFluidHandler {
  /** Block position that will never be valid in world, used for sync */
  private static final BlockPos NULL_POS = new BlockPos(0, Short.MIN_VALUE, 0);

  /** Supplier for the list of valid tank positions */
  private final Supplier<List<BlockPos>> tankSupplier;
  /** Position of the last fluid handler */
  private BlockPos lastPos = NULL_POS;

  /** Map of all tank handlers at each relevant position. Used for fast switching between handlers, notably in the UI */
  private Map<BlockPos,IFluidHandler> tankHandlers;

  public MultitankFuelModule(MantleBlockEntity parent, Supplier<List<BlockPos>> tankSupplier) {
    super(parent);
    this.tankSupplier = tankSupplier;
  }

  /** Resets just the last fluid listener */
  private void clearLastListener() {
    super.resetHandler(null);
  }

  @Override
  protected void resetHandler(@Nullable IFluidHandler source) {
    if (source == null || source == fluidHandler) {
      this.lastPos = NULL_POS;
    }
    super.resetHandler(source);
  }

  /** Called on structure rebuild to clear the gui handler list */
  public void clearFluidListeners() {
    tankHandlers = null;
    invalidateHandlerCaches();
    // Drop the cached ACTIVE fuel handler so findFuel() re-resolves a swapped/added/removed tank (a swapped-out lava
    // tank's dead handler would otherwise stick and a hotter fuel like blazing blood would never be read -> "not hot
    // enough"). This is the port's stand-in for upstream's LazyOptional invalidation listener, which never fires.
    // Use clearLastListener(), NOT resetHandler(null): the latter also wipes lastPos, which is the fuel-tank position
    // the client GUI needs to render the fuel gauge. Structure re-checks fire routinely (inner-block checks, servant
    // loads, world load), so wiping lastPos here left reopened menus with a blank fuel gauge / "no fuel source" even
    // though fuel was still being consumed (issue #7 regression). findFuel() still re-resolves correctly: with
    // fluidHandler null it takes the lastPos branch and refetches through the rebuilt, self-healing handler map.
    clearLastListener();
  }

  /** Called on servant load to ensure the handler is present in the cache */
  public void ensureTankPresent(BlockEntity be) {
    BlockPos pos = be.getBlockPos();
    if (tankHandlers != null && !tankHandlers.containsKey(pos)) {
      IFluidHandler handler = getHandlerAt(pos);
      if (handler != null) {
        tankHandlers.put(pos, handler);
      }
    }
  }

  /**
   * Gets the map from position to fluid handler.
   * The handlers are resolved through {@link #getHandlerAt} so each entry is backed by a {@link net.neoforged.neoforge.capabilities.BlockCapabilityCache}
   * on the server (self-healing on invalidation) and re-queried on the client. We rebuild the map whenever it is null,
   * empty, or its key set no longer matches the structure's tank positions, so a transiently-missing capability (e.g.
   * right after a world reload, which previously left the fuel tank rendering empty / 0 temperature) recovers on a
   * later tick rather than sticking forever.
   */
  private Map<BlockPos,IFluidHandler> getTankHandlers() {
    List<BlockPos> positions = tankSupplier.get();
    // rebuild when the cache is null, empty, or no longer resolves every tank position. The latter is what makes the
    // display recover after a world reload: positions that returned null on a previous (too-early) query are retried.
    if (tankHandlers == null || tankHandlers.size() != positions.size()) {
      Map<BlockPos,IFluidHandler> handlers = new LinkedHashMap<>();
      for (BlockPos pos : positions) {
        IFluidHandler handler = getHandlerAt(pos);
        if (handler != null) {
          handlers.put(pos, handler);
        }
      }
      tankHandlers = handlers;
    }
    return tankHandlers;
  }


  /* Fuel finding */

  /**
   * Tries to consume fuel from the given position
   * @param pos  Position
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  private int tryFuelPosition(BlockPos pos, boolean consume) {
    IFluidHandler tankCap = getTankHandlers().get(pos);
    if (tankCap != null) {
      // if we find a valid cap, try to consume fuel from it
      int temperature = tryLiquidFuel(tankCap, consume);
      if (temperature > 0) {
        clearLastListener();
        fluidHandler = tankCap;
        lastPos = pos;
        return temperature;
      }
    }
    return 0;
  }

  /**
   * Attempts to consume fuel from one of the tanks
   * @return  temperature of the found fluid, 0 if none
   */
  @Override
  public int findFuel(boolean consume) {
    // only fetch a handler if we haven't done so
    if (fluidHandler != null) {
      // if we have a handler, try to use that if possible
      int temperature = tryLiquidFuel(fluidHandler, consume);
      if (temperature > 0) {
        return temperature;
      }
    } else if (lastPos != NULL_POS) {
      // if no handler, try to find one at the last position
      int posTemp = tryFuelPosition(lastPos, consume);
      if (posTemp > 0) {
        return posTemp;
      }
    }

    // find a new handler among our tanks
    for (BlockPos pos : tankSupplier.get()) {
      // already checked the last position above, no reason to try again
      if (!pos.equals(lastPos)) {
        int posTemp = tryFuelPosition(pos, consume);
        if (posTemp > 0) {
          return posTemp;
        }
      }
    }

    // no handler found, tell client of the lack of fuel
    if (consume) {
      temperature = 0;
      rate = 0;
    }
    return 0;
  }


  /* NBT */
  private static final String TAG_LAST_FUEL = "last_fuel";

  @Override
  public void readFromTag(CompoundTag nbt) {
    super.readFromTag(nbt);
    // reads the int array written by writeBlockPos, or the compound saved by 1.20.1 worlds
    BlockPos pos = TagUtil.readOptionalPos(nbt, TAG_LAST_FUEL, parent.getBlockPos());
    if (pos != null) {
      lastPos = pos;
    }
  }

  @Override
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt = super.writeToTag(nbt);
    if (lastPos != NULL_POS) {
      nbt.put(TAG_LAST_FUEL, NbtUtils.writeBlockPos(lastPos.subtract(parent.getBlockPos())));
    }
    return nbt;
  }


  /* UI syncing */
  private static final int LAST_X = 4;
  private static final int LAST_Y = 5;
  private static final int LAST_Z = 6;

  @Override
  public int getCount() {
    return 7;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case LAST_X -> lastPos.getX();
      case LAST_Y -> lastPos.getY();
      case LAST_Z -> lastPos.getZ();
      default -> super.get(index);
    };
  }

  @Override
  public void set(int index, int value) {
    if (LAST_X <= index && index <= LAST_Z) {
      switch (index) {
        case LAST_X -> lastPos = new BlockPos(value, lastPos.getY(), lastPos.getZ());
        case LAST_Y -> lastPos = new BlockPos(lastPos.getX(), value, lastPos.getZ());
        case LAST_Z -> lastPos = new BlockPos(lastPos.getX(), lastPos.getY(), value);
      }
      clearLastListener();
    } else {
      super.set(index, value);
    }
  }

  @Override
  public FuelInfo getFuelInfo() {
    // fetch the primary fuel handler, the tank we last pulled fuel from
    // Y of big negative is how the UI syncs null, meaning we have not yet consumed fuel
    // Re-resolve from the (self-healing) handler map each call rather than caching it once, so the display recovers
    // if the handler was missing/stale on an earlier query (e.g. right after a world reload).
    Map<BlockPos,IFluidHandler> handlers = getTankHandlers();
    fluidHandler = lastPos.getY() != NULL_POS.getY() ? handlers.get(lastPos) : null;

    // two cases leave us with no tank to show despite the structure holding fuel: we never consumed fuel so we have no handler,
    // and the tank we did consume from was drained dry while we are still burning the fuel we took out of it
    // in both cases promote the first tank containing fuel. The port searches for a valid fuel fluid rather than any fluid,
    // so a freshly (re)opened menu does not show an output tank holding a non-fuel.
    if (fluidHandler == null || fluidHandler.getFluidInTank(0).isEmpty()) {
      for (IFluidHandler handler : handlers.values()) {
        FluidStack fluid = handler.getFluidInTank(0);
        if (!fluid.isEmpty() && findRecipe(fluid.getFluid()) != null) {
          fluidHandler = handler;
          break;
        }
      }
      // no tank currently holds fuel and we never consumed any: fall back to the first structural tank (may be empty)
      // as the search runs every frame, the gauge recovers as soon as fuel returns
      if (fluidHandler == null) {
        List<BlockPos> positions = tankSupplier.get();
        if (positions.isEmpty()) {
          return FuelInfo.EMPTY;
        }
        fluidHandler = handlers.get(positions.get(0));
      }
    }

    // determine what fluid we have and hpw many other fluids we have
    FuelInfo info = super.getFuelInfo();
    // add extra fluid display
    if (!info.isEmpty()) {
      // add display info from each handler
      FluidStack currentFuel = info.getFluid();
      for (IFluidHandler handler : handlers.values()) {
        // skip the main tank, the info above is already its contents
        if (handler != fluidHandler) {
          // sum if empty (more capacity) or the same fluid (more amount and capacity)
          FluidStack fluid = handler.getFluidInTank(0);
          if (fluid.isEmpty()) {
            info.add(0, handler.getTankCapacity(0));
          } else if (FluidStack.isSameFluidSameComponents(currentFuel, fluid)) {
            info.add(fluid.getAmount(), handler.getTankCapacity(0));
          }
        }
      }
    }

    return info;
  }


  /* Fluid handler */

  /** Gets the most recently used fluid */
  public FluidStack getLastFluid() {
    if (fluidHandler != null) {
      return fluidHandler.getFluidInTank(0);
    }
    BlockPos pos;
    if (lastPos.getY() != NULL_POS.getY()) {
      pos = lastPos;
    } else {
      List<BlockPos> positions = tankSupplier.get();
      if (!positions.isEmpty()) {
        pos = positions.get(0);
      } else {
        return FluidStack.EMPTY;
      }
    }
    return getTankHandlers().getOrDefault(pos, EmptyFluidHandler.INSTANCE).getFluidInTank(0);
  }

  @Override
  public int getTanks() {
    return tankSupplier.get().size();
  }

  /** Gets the tank at the given index */
  private IFluidHandler getTank(int tank) {
    if (tank >= 0) {
      List<BlockPos> positions = tankSupplier.get();
      if (tank < positions.size()) {
        return getTankHandlers().getOrDefault(positions.get(tank), EmptyFluidHandler.INSTANCE);
      }
    }
    return EmptyFluidHandler.INSTANCE;
  }

  @Nonnull
  @Override
  public FluidStack getFluidInTank(int tank) {
    // getTank(tank) returns the single-slot handler for that outer index; its inner slot is always 0
    return getTank(tank).getFluidInTank(0);
  }

  @Override
  public int getTankCapacity(int tank) {
    return getTank(tank).getTankCapacity(0);
  }

  @Override
  public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
    return getTank(tank).isFluidValid(0, stack);
  }

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    int totalFilled = 0;
    resource = resource.copy();
    // try each handler, updating the amount we filled as we go
    // note the map internally is a linked hash map so order is consistent
    for (IFluidHandler handler : getTankHandlers().values()) {
      int filled = handler.fill(resource, action);
      if (filled > 0) {
        // if we finished filling, we are done, return that value
        // this is a quick exit that might save us a copy
        totalFilled += filled;
        if (filled >= resource.getAmount()) {
          break;
        }
        // if this was our first fill, copy the resource
        if (totalFilled == filled) {
          resource = resource.copyWithAmount(resource.getAmount() - filled);
        } else {
          resource.shrink(filled);
        }
        // resource will never be empty, as if it was the above break would be hit
      }
    }
    return totalFilled;
  }

  @Nonnull
  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    FluidStack drainedSoFar = FluidStack.EMPTY;
    // try each handler, updating the amount we filled as we go
    // note the map internally is a linked hash map so order is consistent
    for (IFluidHandler handler : getTankHandlers().values()) {
      FluidStack drained = handler.drain(resource, action);
      if (!drained.isEmpty()) {
        // if we managed to drain something, add it into our current drained stack, and decrease the amount we still want to drain
        if (drainedSoFar.isEmpty()) {
          drainedSoFar = drained;
          // if the first success, make a copy of the resource before shrinking it, need to shrink to prevent passing in too much to future hooks
          // though we can skip copying if the first one is all we need
          // note the >= part is just for redundancy, practically its always either = or less than
          if (drained.getAmount() >= resource.getAmount()) {
            break;
          }
          resource = resource.copyWithAmount(resource.getAmount() - drained.getAmount());
        } else {
          // resource is guaranteed a copy, and drainedSoFar is a newly created stack, both safe to mutate
          drainedSoFar.grow(drained.getAmount());
          resource.shrink(drained.getAmount());
          // if we drained everything desired, we are done
          if (resource.isEmpty()) {
            break;
          }
        }
      }
    }
    return drainedSoFar;
  }

  @Nonnull
  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    FluidStack drainedSoFar = FluidStack.EMPTY;
    FluidStack toDrain = FluidStack.EMPTY;
    // try each handler, updating the amount we filled as we go
    // note the map internally is a linked hash map so order is consistent
    for (IFluidHandler handler : getTankHandlers().values()) {
      // if we have not drained anything yet, can use typeless hook
      if (toDrain.isEmpty()) {
        FluidStack drained = handler.drain(maxDrain, action);
        if (!drained.isEmpty()) {
          drainedSoFar = drained;
          // if we finished draining, we are done, otherwise we need to create a filter for future drain attempts
          // note the >= part is just for redundancy, practically its always either = or less than
          if (drained.getAmount() >= maxDrain) {
            break;
          }
          toDrain = drained.copyWithAmount(maxDrain - drained.getAmount());
        }
      } else {
        // if we already drained some fluid, type sensitive and increase our results
        FluidStack drained = handler.drain(toDrain, action);
        if (!drained.isEmpty()) {
          drainedSoFar.grow(drained.getAmount());
          toDrain.shrink(drained.getAmount());
          // if we drained everything desired, we are done
          if (toDrain.isEmpty()) {
            break;
          }
        }
      }
    }
    return drainedSoFar;
  }
}
