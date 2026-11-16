package slimeknights.tconstruct.shared.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.CombatHelper;
import slimeknights.tconstruct.common.TinkerDamageTypes;

/** Block implementing knightmetal's spiky behavior. Based on <a href="https://github.com/TeamTwilight/twilightforest/blob/1.21.x/src/main/java/twilightforest/block/KnightmetalBlock.java">Twilight Forest</a> */
public class KnightMetalBlock extends Block implements SimpleWaterloggedBlock {
  private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  // TODO: more accurate shape? would need to ensure hitbox is still this
  public static final VoxelShape SHAPE = Block.box(1.0D, 1.0D, 1.0D, 15.0D, 15.0D, 15.0D);
  public static final float BLOCK_DAMAGE = 4;
  public KnightMetalBlock(Properties props) {
    super(props);
    this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false));
  }

  @Override
  public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
    return SHAPE;
  }

  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
    return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER && fluid.getAmount() == 8);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction pDirection, BlockPos neighborPos, BlockState pNeighborState, RandomSource random) {
    if (state.getValue(WATERLOGGED)) {
      ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }
    return super.updateShape(state, level, ticks, pos, pDirection, neighborPos, pNeighborState, random);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(WATERLOGGED);
  }

  @Nullable
  @Override
  public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
    // DAMAGE_OTHER was renamed in 1.21.2
    return PathType.DAMAGING;
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    // damage is server side only since 1.21.2
    if (level instanceof ServerLevel server) {
      entity.hurtServer(server, CombatHelper.damageSource(level, TinkerDamageTypes.KNIGHTMETAL), BLOCK_DAMAGE);
    }
  }
}
