package slimeknights.tconstruct.gadgets.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Implementation of ichor cake, which is inverted and provides a rocket effect */
public class InvertedCakeBlock extends FoodCakeBlock {
  protected static final VoxelShape[] SHAPE_BY_BITE = {
    Block.box( 1, 8, 1, 15, 16, 15),
    Block.box( 3, 8, 1, 15, 16, 15),
    Block.box( 5, 8, 1, 15, 16, 15),
    Block.box( 7, 8, 1, 15, 16, 15),
    Block.box( 9, 8, 1, 15, 16, 15),
    Block.box(11, 8, 1, 15, 16, 15),
    Block.box(13, 8, 1, 15, 16, 15)
  };

  public InvertedCakeBlock(Properties properties, FoodProperties food, Consumable consumable, EffectCombination combination) {
    super(properties, food, consumable, combination);
  }

  @Override
  protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
    return SHAPE_BY_BITE[pState.getValue(BITES)];
  }

  @Override
  protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
    if (facing == Direction.UP && !state.canSurvive(level, pos)) {
      return Blocks.AIR.defaultBlockState();
    }
    return state;
  }

  @Override
  protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
    return pLevel.getBlockState(pPos.above()).isSolid();
  }
}
