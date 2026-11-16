package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathType;
import slimeknights.mantle.block.InventoryBlock;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.DuctBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import javax.annotation.Nullable;

/** Filtering drain block, have to reimplement either inventory block logic or seared block logic unfortunately */
public class SearedDuctBlock extends InventoryBlock {
  public static final BooleanProperty IN_STRUCTURE = SearedBlock.IN_STRUCTURE;
  public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
  public SearedDuctBlock(Properties properties) {
    super(properties);
    this.registerDefaultState(this.defaultBlockState().setValue(IN_STRUCTURE, false));
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return new DuctBlockEntity(pPos, pState);
  }

  /* Seared block interaction */

  // removal: the master is told about the change in SmelteryComponentBlockEntity#preRemoveSideEffects, which also drops the inventory through InventoryBlock
  // tooltip: blocks no longer add item tooltips, the texture tooltip comes from RetexturedTooltipBlockItem

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    SmelteryComponentBlockEntity.updateNeighbors(world, pos, state);
    RetexturedBlock.updateTextureBlock(world, pos, stack);
  }
  
  @Override
  public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return RetexturedBlock.getPickBlock(world, pos, state);
  }


  /* Orientation */

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
    builder.add(IN_STRUCTURE, FACING);
  }

  @Nullable
  @Override
  public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
    return state.getValue(IN_STRUCTURE) ? PathType.FIRE : PathType.OPEN;
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
  }
}
