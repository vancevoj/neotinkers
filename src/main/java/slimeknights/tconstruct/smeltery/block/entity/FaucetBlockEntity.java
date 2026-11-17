package slimeknights.tconstruct.smeltery.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;

import javax.annotation.Nullable;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.network.FaucetActivationPacket;

import static slimeknights.tconstruct.smeltery.block.FaucetBlock.FACING;

public class FaucetBlockEntity extends MantleBlockEntity {
  /** amount of MB to extract from the input at a time */
  public static final int PACKET_SIZE = FluidValues.INGOT;
  /** Transfer rate of the faucet */
  public static final int MB_PER_TICK = FluidValues.NUGGET;

  public static final BlockEntityTicker<FaucetBlockEntity> SERVER_TICKER = (level, pos, world, self) -> self.tick();

  private static final String TAG_DRAINED = "drained";
  private static final String TAG_RENDER_FLUID = "render_fluid";
  private static final String TAG_STOP = "stop";
  private static final String TAG_STATE = "state";
  private static final String TAG_LAST_REDSTONE = "lastRedstone";

  /** If true, faucet is currently pouring */
  private FaucetState faucetState = FaucetState.OFF;
  /** If true, redstone told this faucet to stop, so stop when ready */
  private boolean stopPouring = false;
  /** Current fluid in the faucet */
  private FluidStack drained = FluidStack.EMPTY;
  /** Fluid for rendering, used to reduce the number of packets. There is a brief moment where {@link this#drained} is empty but we should be rendering something */
  @Getter
  private FluidStack renderFluid = FluidStack.EMPTY;
  /** Used for pulse detection */
  private boolean lastRedstoneState = false;

  /** Cached fluid handler of the input to the faucet */
  @Nullable
  private BlockCapabilityCache<ResourceHandler<FluidResource>,Direction> inputHandler;
  /** Cached fluid handler of the output from the faucet */
  @Nullable
  private BlockCapabilityCache<ResourceHandler<FluidResource>,Direction> outputHandler;

  public FaucetBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.faucet.get(), pos, state);
  }

  @SuppressWarnings("WeakerAccess")
  protected FaucetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }


  /* Fluid handler */

  /**
   * Creates a fluid handler cache for the given side
   * @param side  Side to check
   * @return  Capability cache
   */
  private BlockCapabilityCache<ResourceHandler<FluidResource>,Direction> createCache(Direction side) {
    assert level instanceof ServerLevel;
    return BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, (ServerLevel) level, worldPosition.relative(side), side.getOpposite());
  }

  /**
   * Gets the input fluid handler
   * @return  Input fluid handler, or null if absent
   */
  @Nullable
  private ResourceHandler<FluidResource> getInputHandler() {
    if (inputHandler == null) {
      inputHandler = createCache(getBlockState().getValue(FACING).getOpposite());
    }
    return inputHandler.getCapability();
  }

  /**
   * Gets the output fluid handler
   * @return  Output fluid handler, or null if absent
   */
  @Nullable
  private ResourceHandler<FluidResource> getOutputHandler() {
    if (outputHandler == null) {
      outputHandler = createCache(Direction.DOWN);
    }
    return outputHandler.getCapability();
  }

  /**
   * Called when a neighbor changes to invalidate the cached fluid handler
   * @param neighbor  Neighbor position that changed
   */
  public void neighborChanged(BlockPos neighbor) {
    // if the neighbor was below us, remove output
    if (worldPosition.equals(neighbor.above())) {
      outputHandler = null;
      // neighbor behind us
    } else if (worldPosition.equals(neighbor.relative(getBlockState().getValue(FACING)))) {
      inputHandler = null;
    }
  }


  /* Data */

  /**
   * Gets whether the faucet is pouring
   * @return True if pouring
   */
  public boolean isPouring() {
    return faucetState != FaucetState.OFF;
  }

  /* Activation */

  /**
   * Toggles pouring state and initiates transfer if appropriate. Called on right click and from redstone
   */
  public void activate() {
    // don't run on client
    if (level == null || level.isClientSide()) {
      return;
    }
    // already pouring? we want to start
    switch (faucetState) {
      // off activates the faucet
      case OFF -> {
        stopPouring = false;
        doTransfer(true);
      }
      // powered deactivates the faucet, sync to client
      case POWERED -> {
        faucetState = FaucetState.OFF;
        syncToClient(FluidStack.EMPTY, false);
      }
      // pouring means we stop pouring as soon as possible
      case POURING -> stopPouring = true;
    }
  }

  /**
   * Flips hasSignal and schedules a tick if appropriate.
   * @param hasSignal  New signal state
   */
  public void handleRedstone(boolean hasSignal) {
    if (hasSignal != lastRedstoneState) {
      lastRedstoneState = hasSignal;
      if (hasSignal) {
        if (level != null){
          level.scheduleTick(worldPosition, this.getBlockState().getBlock(), 2);
        }
      } else if (faucetState == FaucetState.POWERED) {
        faucetState = FaucetState.OFF;
        syncToClient(FluidStack.EMPTY, false);
      }
    }
  }


  /* Pouring */

  /** Handles server ticks */
  private void tick() {
    // nothing to do if not pouring
    if (faucetState == FaucetState.OFF) {
      return;
      // if powered and we can transfer, schedule transfer for next tick
    } else if (faucetState == FaucetState.POWERED && doTransfer(false)) {
      faucetState = FaucetState.POURING;
      return;
    }

    // continue current stack
    if (!drained.isEmpty()) {
      pour();
      // stop if told to stop once done
    } else if (stopPouring) {
      reset();
      // otherwise keep going
    } else {
      doTransfer(true);
    }
  }

  /**
   * Initiate fluid transfer
   */
  private boolean doTransfer(boolean execute) {
    // still got content left
    ResourceHandler<FluidResource> input = getInputHandler();
    ResourceHandler<FluidResource> output = getOutputHandler();
    if (input != null && output != null) {
      // simulate the whole check, nothing here is committed
      FluidResource resource = null;
      int filled = 0;
      boolean canPour = false;
      try (Transaction simulate = Transaction.openRoot()) {
        // can we drain? drains the bottom fluid for the smeltery
        ResourceStack<FluidResource> drained = ResourceHandlerUtil.extractFirst(input, fluid -> true, PACKET_SIZE, simulate);
        if (drained != null) {
          resource = drained.resource();
          // can we fill
          try (Transaction fill = Transaction.open(simulate)) {
            filled = output.insert(resource, drained.amount(), fill);
          }
          if (filled > 0) {
            // ensure we can actually fill in our min increment, deals with handlers like copper cans
            // can skip this step if we already received a small enough number
            if (filled <= MB_PER_TICK) {
              canPour = true;
            } else {
              try (Transaction fill = Transaction.open(simulate)) {
                canPour = output.insert(resource, MB_PER_TICK, fill) > 0;
              }
            }
          }
        }
      }
      if (canPour) {
        // execute if requested
        if (execute) {
          // drain the liquid and transfer it, buffer the amount for delay
          this.drained = FluidStack.EMPTY;
          try (Transaction transaction = Transaction.openRoot()) {
            int amount = input.extract(resource, filled, transaction);
            if (amount > 0) {
              transaction.commit();
              this.drained = resource.toStack(amount);
            }
          }

          // sync to clients if we have changes
          if (faucetState == FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
            syncToClient(this.drained, true);
          }
          faucetState = FaucetState.POURING;
          // pour after initial packet, in case we end up resetting later
          pour();
        }
        return true;
      }

      // if powered, keep faucet running
      if (lastRedstoneState) {
        // sync if either we were not pouring before (particle effects), or if the client thinks we have fluid
        if (execute && (faucetState == FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, FluidStack.EMPTY))) {
          syncToClient(FluidStack.EMPTY, true);
        }
        faucetState = FaucetState.POWERED;
        return false;
      }
    }
    // reset if not powered, or if nothing to do
    if (execute) {
      reset();
    }
    return false;
  }

  /**
   * Takes the liquid inside and executes one pouring step.
   */
  private void pour() {
    if (drained.isEmpty()) {
      return;
    }

    // ensure we have an output
    ResourceHandler<FluidResource> output = getOutputHandler();
    if (output != null) {
      FluidResource resource = FluidResource.of(drained);
      int fillAmount = Math.min(drained.getAmount(), MB_PER_TICK);

      // can we fill?
      int filled;
      try (Transaction simulate = Transaction.openRoot()) {
        filled = output.insert(resource, fillAmount, simulate);
      }
      if (filled > 0) {
        // update client if they do not think we have fluid
        if (!FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
          syncToClient(drained, true);
        }

        // transfer it
        this.drained.shrink(filled);
        try (Transaction transaction = Transaction.openRoot()) {
          output.insert(resource, filled, transaction);
          transaction.commit();
        }
      } else if (returnDrained()) {
        // the target stopped accepting while we still held fluid, for instance another faucet filled it (#32).
        // the fluid went back to the source, so stop cleanly like doTransfer does when nothing can move
        stopPouring = false;
        if (lastRedstoneState) {
          drained = FluidStack.EMPTY;
          faucetState = FaucetState.POWERED;
          syncToClient(FluidStack.EMPTY, true);
        } else {
          reset();
        }
      }
      // otherwise the source is full, keep the rest and try again next tick
    }
    else {
      // output got lost, give what we hold back to the source rather than voiding it
      returnDrained();
      reset();
    }
  }

  /**
   * Gives fluid the faucet still holds back to the tank it came from.
   * @return  True if the faucet holds nothing anymore
   */
  private boolean returnDrained() {
    if (!drained.isEmpty()) {
      ResourceHandler<FluidResource> input = getInputHandler();
      if (input != null) {
        try (Transaction transaction = Transaction.openRoot()) {
          int returned = input.insert(FluidResource.of(drained), drained.getAmount(), transaction);
          transaction.commit();
          drained.shrink(returned);
        }
      }
    }
    return drained.isEmpty();
  }

  /**
   * Resets TE to default state.
   */
  private void reset() {
    stopPouring = false;
    drained = FluidStack.EMPTY;
    if (faucetState != FaucetState.OFF || !FluidStack.isSameFluidSameComponents(renderFluid, drained)) {
      faucetState = FaucetState.OFF;
      syncToClient(FluidStack.EMPTY, false);
    }
  }


  /* NBT and networking */

  /**
   * Sends an update to the client with the most recent
   * @param fluid       New fluid
   * @param isPouring   New isPouring status
   */
  private void syncToClient(FluidStack fluid, boolean isPouring) {
    renderFluid = fluid.copy();
    if (level instanceof ServerLevel) {
      TinkerNetwork.getInstance().sendToClientsAround(new FaucetActivationPacket(worldPosition, fluid, isPouring), (ServerLevel) level, getBlockPos());
    }
  }

  /**
   * Sets draining fluid to specified stack.
   * @param fluid new FluidStack
   */
  public void onActivationPacket(FluidStack fluid, boolean isPouring) {
    // pouring and powered are interchangable on the client
    this.faucetState = isPouring ? FaucetState.POURING : FaucetState.OFF;
    this.renderFluid = fluid;
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    output.putByte(TAG_STATE, (byte)faucetState.ordinal());
    if (!renderFluid.isEmpty()) {
      output.store(TAG_RENDER_FLUID, FluidStack.CODEC, renderFluid);
    }
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    output.putBoolean(TAG_STOP, stopPouring);
    output.putBoolean(TAG_LAST_REDSTONE, lastRedstoneState);
    if (!drained.isEmpty()) {
      output.store(TAG_DRAINED, FluidStack.CODEC, drained);
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);

    faucetState = FaucetState.fromIndex(input.getByteOr(TAG_STATE, (byte)0));
    stopPouring = input.getBooleanOr(TAG_STOP, false);
    lastRedstoneState = input.getBooleanOr(TAG_LAST_REDSTONE, false);
    // fluids
    drained = input.read(TAG_DRAINED, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
    renderFluid = input.read(TAG_RENDER_FLUID, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
  }

  private enum FaucetState {
    OFF,
    POURING,
    POWERED;

    /** Gets the state for the given index */
    public static FaucetState fromIndex(int index) {
      switch (index) {
        case 1: return POURING;
        case 2: return POWERED;
      }
      return OFF;
    }
  }
}
