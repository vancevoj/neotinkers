package slimeknights.tconstruct.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import slimeknights.tconstruct.common.TinkerTags;

import javax.annotation.Nullable;
import java.util.Objects;

import static net.minecraft.world.level.block.MangrovePropaguleBlock.AGE;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HANGING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

/** Recreation of {@link net.minecraft.world.level.block.MangrovePropaguleBlock} to swap out the tree grower. */
public class SlimePropaguleBlock extends SlimeSaplingBlock {
  /** Shape of a planted propagule, matches the sapling shape */
  private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 12.0);
  /** Minimum Y for each age of a hanging propagule */
  private static final int[] SHAPE_MIN_Y = {13, 10, 7, 3, 0};
  /** Shapes of a hanging propagule per age */
  private static final VoxelShape[] SHAPE_PER_AGE = Block.boxes(4, age -> Block.column(2.0, SHAPE_MIN_Y[age], 16.0));

  public SlimePropaguleBlock(TreeGrower treeIn, FoliageType foliageType, Properties properties) {
    super(treeIn, foliageType, properties);
    this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0).setValue(AGE, 0).setValue(WATERLOGGED, false).setValue(HANGING, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> pBuilder) {
    pBuilder.add(STAGE).add(AGE).add(WATERLOGGED).add(HANGING);
  }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext pContext) {
    FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
    boolean flag = fluidstate.getType() == Fluids.WATER;
    return Objects.requireNonNull(super.getStateForPlacement(pContext))
                  .setValue(WATERLOGGED, flag)
                  .setValue(AGE, 4);
  }


  @Override
  protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
    Vec3 vec3 = pState.getOffset(pPos);
    VoxelShape voxelshape;
    if (pState.getValue(HANGING)) {
      voxelshape = SHAPE_PER_AGE[pState.getValue(AGE)];
    } else {
      voxelshape = SHAPE;
    }

    return voxelshape.move(vec3.x, vec3.y, vec3.z);
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    return state.getValue(HANGING)
           ? level.getBlockState(pos.above()).is(TinkerTags.Blocks.SLIMY_LEAVES)
           : super.canSurvive(state, level, pos);
  }

  @Override
  protected BlockState updateShape(BlockState pState, LevelReader pLevel, ScheduledTickAccess ticks, BlockPos pCurrentPos, Direction pFacing, BlockPos pFacingPos, BlockState pFacingState, RandomSource random) {
    if (pState.getValue(WATERLOGGED)) {
      ticks.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
    }
    return pFacing == Direction.UP && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pLevel, ticks, pCurrentPos, pFacing, pFacingPos, pFacingState, random);
  }

  @Override
  protected FluidState getFluidState(BlockState pState) {
    return pState.getValue(WATERLOGGED)
           ? Fluids.WATER.getSource(false)
           : super.getFluidState(pState);
  }

  @Override
  protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
    if (!pState.getValue(HANGING)) {
      if (pRandom.nextInt(7) == 0) {
        this.advanceTree(pLevel, pPos, pState, pRandom);
      }

    } else {
      if (pState.getValue(AGE) != 4) {
        pLevel.setBlock(pPos, pState.cycle(AGE), 2);
      }

    }
  }

  @Override
  public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState state, BonemealSource source) {
    return !state.getValue(HANGING) || state.getValue(AGE) != 4;
  }

  @Override
  public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState, BonemealSource source) {
    return pState.getValue(HANGING)
           ? pState.getValue(AGE) != 4
           : super.isBonemealSuccess(pLevel, pRandom, pPos, pState, source);
  }

  @Override
  public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState, BonemealSource source) {
    if (pState.getValue(HANGING) && pState.getValue(AGE) != 4) {
      pLevel.setBlock(pPos, pState.cycle(AGE), 2);
    } else {
      super.performBonemeal(pLevel, pRandom, pPos, pState, source);
    }
  }
}
