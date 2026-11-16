package slimeknights.tconstruct.smeltery.block.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockResult;
import slimeknights.tconstruct.smeltery.menu.HeatingStructureContainerMenu;
import slimeknights.tconstruct.smeltery.network.StructureErrorPositionPacket;

import javax.annotation.Nullable;

/**
 * Multiblock that displays the error from the tile entity on right click
 */
public abstract class HeatingControllerBlock extends ControllerBlock {
  protected HeatingControllerBlock(Properties builder) {
    super(builder);
  }

  @Override
  protected boolean openGui(Player player, Level world, BlockPos pos) {
    if (!world.isClientSide() && world.getBlockEntity(pos) instanceof HeatingStructureBlockEntity te) {
      BlockState state = world.getBlockState(pos);
      if (state.is(this) && canOpenGui(state) && player instanceof ServerPlayer serverPlayer) {
        // same as InventoryBlock#openGui, but the open data also carries the melting slot count so the client menu gets as many slots as the server menu
        HeatingStructureContainerMenu.open(serverPlayer, te);
      } else {
        super.openGui(player, world, pos);
      }
      // only need to update if holding the proper items
      MultiblockResult result = te.getStructureResult();
      if (!result.isSuccess() && te.showDebugBlockBorder(player)) {
        TinkerNetwork.getInstance().sendTo(new StructureErrorPositionPacket(pos, result.getPos()), player);
      }
    } else {
      super.openGui(player, world, pos);
    }
    return true;
  }

  @Override
  protected boolean displayStatus(Player player, Level world, BlockPos pos, BlockState state) {
    if (!world.isClientSide() && world.getBlockEntity(pos) instanceof HeatingStructureBlockEntity te) {
      MultiblockResult result = te.getStructureResult();
      if (!result.isSuccess()) {
        player.sendOverlayMessage(result.getMessage());
        TinkerNetwork.getInstance().sendTo(new StructureErrorPositionPacket(pos, result.getPos()), player);
      }
    }
    return true;
  }

  // tooltip: blocks no longer add item tooltips, the texture tooltip comes from RetexturedTooltipBlockItem

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(world, pos, state, placer, stack);
    if (world.getBlockEntity(pos) instanceof HeatingStructureBlockEntity te) {
      // update controller texture
      if (!stack.isComponentsPatchEmpty()) {
        te.updateTexture(RetexturedHelper.getTextureName(stack));
      }
      // check structure
      te.updateStructure();
    }
  }

  /**
   * Called by the block entity when it is removed due to the block changing. Replaces the old {@code onRemove} logic, which no longer has access to the block entity.
   * Invalidates the structure before the inventory is dropped.
   */
  @Override
  public void onBlockEntityRemoved(BlockState state, Level level, BlockPos pos, BlockEntity te) {
    if (te instanceof HeatingStructureBlockEntity structure) {
      structure.invalidateStructure();
    }
    super.onBlockEntityRemoved(state, level, pos, te);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return RetexturedBlock.getPickBlock(world, pos, state);
  }


  /* No rotation if in a structure  */

  @Deprecated
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    if (state.getValue(IN_STRUCTURE)) {
      return state;
    }
    return super.rotate(state, rotation);
  }

  @Deprecated
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    if (state.getValue(IN_STRUCTURE)) {
      return state;
    }
    return super.mirror(state, mirror);
  }
}
