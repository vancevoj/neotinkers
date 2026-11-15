package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Alloy tank that takes inputs from neighboring blocks
 */
@RequiredArgsConstructor
public class MixerAlloyTank implements IMutableAlloyTank {
  // parameters
  /** Handler parent */
  private final MantleBlockEntity parent;
  /** Tank for outputs */
  private final ResourceHandler<FluidResource> outputTank;

  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter
  @Setter
  private int temperature = 0;

  // side tank cache
  /** Cache of present fluid handlers for each of the sides; absent key with present {@link #checked} entry means no handler */
  private final Map<Direction,ResourceHandler<FluidResource>> inputs = new EnumMap<>(Direction.class);
  /** Set of sides already queried since the last refresh, so we can distinguish "no handler" from "not yet fetched" */
  private final Set<Direction> checked = EnumSet.noneOf(Direction.class);
  /** Map of tank index to tank on the side */
  @Nullable
  private ResourceHandler<FluidResource>[] indexedList = null;

  // state
  /** If true, tanks are marked for refresh later */
  private boolean needsRefresh = true;
  /** Number of currently held tanks */
  private int currentTanks = 0;

  @Override
  public int getTanks() {
    checkTanks();
    return currentTanks;
  }

  /** Gets the map of index to direction */
  @SuppressWarnings("unchecked")
  private ResourceHandler<FluidResource>[] indexTanks() {
    // convert map into indexed list of fluid handlers, will be cleared next time a side updates
    if (indexedList == null) {
      indexedList = (ResourceHandler<FluidResource>[]) new ResourceHandler[currentTanks];
      if (currentTanks > 0) {
        int nextTank = 0;
        for (Direction direction : Direction.values()) {
          if (direction != Direction.DOWN) {
            ResourceHandler<FluidResource> handler = inputs.get(direction);
            if (handler != null) {
              indexedList[nextTank] = handler;
              nextTank++;
            }
          }
        }
      }
    }
    return indexedList;
  }

  /** Gets the fluid handler for the given tank index, a handler with no tanks if the index is invalid */
  public ResourceHandler<FluidResource> getFluidHandler(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return EmptyResourceHandler.instance();
    }
    return indexTanks()[tank];
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    // get the first fluid from the proper tank, we do not support multiple fluids on a side
    ResourceHandler<FluidResource> handler = indexTanks()[tank];
    return handler.size() > 0 ? FluidUtil.getStack(handler, 0) : FluidStack.EMPTY;
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    if (fluidStack.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = FluidResource.of(fluidStack);
    try (Transaction transaction = Transaction.openRoot()) {
      int drained = indexTanks()[tank].extract(resource, fluidStack.getAmount(), transaction);
      transaction.commit();
      return drained > 0 ? resource.toStack(drained) : FluidStack.EMPTY;
    }
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    checkTanks();
    if (fluid.isEmpty()) {
      return true;
    }
    try (Transaction simulate = Transaction.openRoot()) {
      return outputTank.insert(FluidResource.of(fluid), fluid.getAmount(), simulate) == fluid.getAmount();
    }
  }

  @Override
  public int fill(FluidStack fluidStack) {
    if (fluidStack.isEmpty()) {
      return 0;
    }
    try (Transaction transaction = Transaction.openRoot()) {
      int filled = outputTank.insert(FluidResource.of(fluidStack), fluidStack.getAmount(), transaction);
      transaction.commit();
      return filled;
    }
  }

  /**
   * Refreshes the cached tanks if needed
   * After calling this method, all five tank sides will have been fetched
   */
  private void checkTanks() {
    // need world to do anything
    Level world = parent.getLevel();
    if (world == null) {
      return;
    }
    if (needsRefresh) {
      for (Direction direction : Direction.values()) {
        // update each direction we are missing
        if (direction != Direction.DOWN && !checked.contains(direction)) {
          BlockPos target = parent.getBlockPos().relative(direction);
          // limit by blocks as that gives the modpack more control, say they want to allow only scorched tanks
          if (world.getBlockState(target).is(TinkerTags.Blocks.ALLOYER_TANKS)) {
            // if we found a tank, increment the number of tanks
            ResourceHandler<FluidResource> capability = world.getCapability(Capabilities.Fluid.BLOCK, target, direction.getOpposite());
            checked.add(direction);
            if (capability != null) {
              inputs.put(direction, capability);
              currentTanks++;
            }
          }
        }
      }
      needsRefresh = false;
    }
  }

  /**
   * Called on block update or when a capability invalidates to mark that a direction needs updates
   * @param direction  Side updating
   * @param checkInput If true, validates that the side contains an input before reducing tank count. False when invalidated through the capability
   * */
  public void refresh(Direction direction, boolean checkInput) {
    if (direction == Direction.DOWN) {
      return;
    }
    if (!checkInput || inputs.get(direction) != null) {
      currentTanks--;
    }
    inputs.remove(direction);
    checked.remove(direction);
    needsRefresh = true;
    indexedList = null;
  }
}
