package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Shared logic between drains and ducts
 */
public abstract class SmelteryInputOutputBlockEntity<T> extends SmelteryComponentBlockEntity implements IRetexturedBlockEntity {
  /** Capability this TE watches */
  private final BlockCapability<T, Direction> capability;
  /** Empty capability for in case the valid capability becomes invalid without invalidating */
  protected final T emptyInstance;
  /** Cached handler proxied from the master, or null if not yet fetched */
  @Nullable
  private T cachedHandler = null;
  /** Whether the cached handler has been fetched */
  private boolean handlerFetched = false;

  /* Retexturing */
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;

  protected SmelteryInputOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockCapability<T, Direction> capability, T emptyInstance) {
    super(type, pos, state);
    this.capability = capability;
    this.emptyInstance = emptyInstance;
  }

  /** Clears all cached capabilities */
  private void clearHandler() {
    cachedHandler = null;
    handlerFetched = false;
  }

  @Override
  public void onMasterLoad(IMasterLogic master) {
    clearHandler();
  }

  @Override
  protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
    assert level != null;

    // if we have a new master, invalidate handlers
    boolean masterChanged = false;
    if (!Objects.equals(getMasterPos(), master)) {
      clearHandler();
      masterChanged = true;
    }
    super.setMaster(master, block);
    // notify neighbors of the change (state change skips the notify flag)
    if (masterChanged) {
      level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }
  }

  /**
   * Gets the capability to store in this IO block.
   * @param parent  Parent tile entity
   * @return  Capability from parent, or null if absent
   */
  @Nullable
  protected T getCapability(BlockEntity parent) {
    if (level != null) {
      T handler = level.getCapability(capability, parent.getBlockPos(), null);
      if (handler != null) {
        return handler;
      }
    }
    return null;
  }

  /**
   * Fetches the capability handler if missing
   */
  @Nullable
  private T getCachedCapability() {
    if (!handlerFetched) {
      if (validateMaster()) {
        BlockPos master = getMasterPos();
        if (master != null && this.level != null) {
          BlockEntity te = level.getBlockEntity(master);
          if (te != null) {
            cachedHandler = getCapability(te);
            handlerFetched = true;
            return cachedHandler;
          }
        }
      }
      cachedHandler = null;
      handlerFetched = true;
    }
    return cachedHandler;
  }

  /** Gets the handler for the given side, used by {@code RegisterCapabilitiesEvent} */
  @Nullable
  public T getHandler(@Nullable Direction side) {
    return getCachedCapability();
  }


  /* Retexturing */

  @Override
  @Nonnull
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(getTexture());
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /* NBT */

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    if (texture != Blocks.AIR) {
      output.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    input.getString(TAG_TEXTURE).ifPresent(name -> {
      texture = RetexturedHelper.getBlock(name);
      RetexturedHelper.onTextureUpdated(this);
    });
  }


  /** Fluid implementation of smeltery IO */
  public static abstract class SmelteryFluidIO extends SmelteryInputOutputBlockEntity<ResourceHandler<FluidResource>> {
    protected SmelteryFluidIO(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.Fluid.BLOCK, EmptyResourceHandler.instance());
    }

    /** Wraps the given capability */
    protected ResourceHandler<FluidResource> makeWrapper(ResourceHandler<FluidResource> capability) {
      return capability;
    }

    @Nullable
    @Override
    protected ResourceHandler<FluidResource> getCapability(BlockEntity parent) {
      // fluid capability is not exposed directly in the smeltery
      if (parent instanceof ISmelteryTankHandler tankHandler) {
        ResourceHandler<FluidResource> capability = tankHandler.getFluidCapability();
        if (capability != null) {
          return makeWrapper(capability);
        }
      }
      return null;
    }
  }

  /** Item implementation of smeltery IO */
  public static class ChuteBlockEntity extends SmelteryInputOutputBlockEntity<ResourceHandler<ItemResource>> {
    public ChuteBlockEntity(BlockPos pos, BlockState state) {
      this(TinkerSmeltery.chute.get(), pos, state);
    }

    protected ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.Item.BLOCK, EmptyItemHandler.INSTANCE);
    }
  }

}
