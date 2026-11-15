package slimeknights.tconstruct.common.multiblock;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.library.utils.TagUtil;

import javax.annotation.Nullable;

public class ServantTileEntity extends MantleBlockEntity implements IServantLogic {
  private static final String TAG_MASTER_POS = "masterOffset";
  private static final String TAG_MASTER_BLOCK = "masterBlock";

  @Getter
  @Nullable
  private BlockPos masterPos;
  @Nullable
  private Block masterBlock;

  public ServantTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  /** Checks if this servant has a master */
  public boolean hasMaster() {
    return masterPos != null;
  }

  /**
   * Called to change the master
   * @param master  New master
   * @param block   New master block
   */
  protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
    masterPos = master;
    masterBlock = block;
    this.setChangedFast();
  }

  /**
   * Checks that this servant has a valid master. Clears the master if invalid
   * @return  True if this servant has a valid master
   */
  protected boolean validateMaster() {
    if (masterPos == null) {
      return false;
    }

    // ensure the master block is correct
    assert level != null;
    if (level.getBlockState(masterPos).getBlock() == masterBlock) {
      return true;
    }
    // master invalid, so clear
    setMaster(null, null);
    return false;
  }

  @Override
  public boolean isValidMaster(IMasterLogic master) {
    // if we have a valid master, the passed master is only valid if its our current master
    if (validateMaster()) {
      return master.getMasterPos().equals(this.masterPos);
    }
    // otherwise, we are happy with any master
    return true;
  }

  @Override
  public void notifyMasterOfChange(BlockPos pos, BlockState state) {
    if (level != null && masterPos != null && validateMaster() && level.getBlockEntity(masterPos) instanceof IMasterLogic te) {
      te.notifyChange(pos, state);
    }
  }

  @Override
  public void setPotentialMaster(IMasterLogic master) {
    BlockPos newMaster = master.getMasterPos();
    // if this is our current master, simply update the master block
    if (newMaster.equals(this.masterPos)) {
      masterBlock = master.getMasterBlock().getBlock();
      this.setChangedFast();
    // otherwise, only set if we don't have a master
    } else if (!validateMaster()) {
      setMaster(newMaster, master.getMasterBlock().getBlock());
    }
  }

  @Override
  public void removeMaster(IMasterLogic master) {
    if (masterPos != null && masterPos.equals(master.getMasterPos())) {
      setMaster(null, null);
    }
  }


  /* NBT */

  /**
   * Sets the master from the read values, if both are valid
   * @param masterPos       Master position, null if missing
   * @param masterBlockName Master block name, null if missing
   */
  private void readMaster(@Nullable BlockPos masterPos, @Nullable String masterBlockName) {
    Block masterBlock = null;
    // if the master position is valid, get the master block
    if (masterPos != null && masterBlockName != null) {
      Identifier masterBlockId = Identifier.tryParse(masterBlockName);
      if (masterBlockId != null && BuiltInRegistries.BLOCK.containsKey(masterBlockId)) {
        masterBlock = BuiltInRegistries.BLOCK.getValue(masterBlockId);
      }
    }
    // if both valid, set
    if (masterBlock != null) {
      this.masterPos = masterPos;
      this.masterBlock = masterBlock;
    }
  }

  /**
   * Reads the master from NBT
   * @param tags  NBT to read
   */
  protected void readMaster(CompoundTag tags) {
    readMaster(TagUtil.readOptionalPos(tags, TAG_MASTER_POS, this.worldPosition), tags.getString(TAG_MASTER_BLOCK).orElse(null));
  }

  /**
   * Reads the master from a value input
   * @param input  Input to read
   */
  protected void readMaster(ValueInput input) {
    readMaster(TagUtil.readOptionalPos(input, TAG_MASTER_POS, this.worldPosition), input.getString(TAG_MASTER_BLOCK).orElse(null));
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    readMaster(input);
  }

  /**
   * Writes the master position and master block to the given compound
   * @param tags  Tags
   */
  @SuppressWarnings({"UnusedReturnValue"})
  protected CompoundTag writeMaster(CompoundTag tags) {
    if (masterPos != null && masterBlock != null) {
      TagUtil.writeOptionalPos(tags, TAG_MASTER_POS, masterPos, this.worldPosition);
      tags.putString(TAG_MASTER_BLOCK, Loadables.BLOCK.getKey(masterBlock).toString());
    }
    return tags;
  }

  /**
   * Writes the master position and master block to the given output
   * @param output  Output
   */
  protected void writeMaster(ValueOutput output) {
    if (masterPos != null && masterBlock != null) {
      TagUtil.writeOptionalPos(output, TAG_MASTER_POS, masterPos, this.worldPosition);
      output.putString(TAG_MASTER_BLOCK, Loadables.BLOCK.getKey(masterBlock).toString());
    }
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    writeMaster(output);
  }
}
