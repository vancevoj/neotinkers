package slimeknights.tconstruct.gadgets.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class PunjiBlock extends Block {

  public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

  private static final BooleanProperty NORTH = BlockStateProperties.NORTH;
  private static final BooleanProperty EAST = BlockStateProperties.EAST;
  private static final BooleanProperty NORTHEAST = BooleanProperty.create("northeast");
  private static final BooleanProperty NORTHWEST = BooleanProperty.create("northwest");
  private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  public PunjiBlock(Properties properties) {
    super(properties);
    this.registerDefaultState(this.stateDefinition.any()
                                            .setValue(FACING, Direction.DOWN)
                                            .setValue(NORTH, false)
                                            .setValue(EAST, false)
                                            .setValue(NORTHEAST, false)
                                            .setValue(NORTHWEST, false)
                                            .setValue(WATERLOGGED, false));
  }

  @Nullable
  @Override
  public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
    return PathType.DAMAGING;
  }

  @Override
  protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
    if (state.getValue(WATERLOGGED)) {
      ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    }

    // break if now invalid
    Direction direction = state.getValue(FACING);
    if (facing == direction && !state.canSurvive(world, pos)) {
      return Blocks.AIR.defaultBlockState();
    }

    // apply north and east if relevant
    Direction north = getLocalNorth(direction);
    Direction east = getLocalEast(direction);
    if (facing == north) {
      state = state.setValue(NORTH, isConnected(world, direction, facingPos));
    } else if (facing == east) {
      state = state.setValue(EAST, isConnected(world, direction, facingPos));
    }

    // always update northeast and northwest, never gets direct updates
    BlockPos northPos = pos.relative(north);
    return state.setValue(NORTHEAST, isConnected(world, direction, northPos.relative(east)))
                .setValue(NORTHWEST, isConnected(world, direction, northPos.relative(east.getOpposite())));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, NORTH, EAST, NORTHEAST, NORTHWEST, WATERLOGGED);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction direction = context.getClickedFace().getOpposite();
    LevelReader world = context.getLevel();
    BlockPos pos = context.getClickedPos();

    BlockState state = this.defaultBlockState().setValue(FACING, direction);
    // if the space is invalid, try again on other sides
    if (!state.canSurvive(world, pos)) {
      boolean isValid = false;
      for (Direction side : Direction.values()) {
        if (side != direction) {
          state = state.setValue(FACING, side);
          if (state.canSurvive(world, pos)) {
            isValid = true;
            direction = side;
            break;
          }
        }
      }
      if (!isValid) {
        return null;
      }
    }

    // apply connections
    Direction north = getLocalNorth(direction);
    Direction east = getLocalEast(direction);
    BlockPos northPos = pos.relative(north);
    return state.setValue(NORTH,     isConnected(world, direction, northPos))
                .setValue(EAST,      isConnected(world, direction, pos.relative(east)))
                .setValue(NORTHEAST, isConnected(world, direction, northPos.relative(east)))
                .setValue(NORTHWEST, isConnected(world, direction, northPos.relative(east.getOpposite())))
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
  }

  /**
   * Checks if this should connect to the block at the position
   * @param world   World instance
   * @param facing  Punji stick facing
   * @param target  Position to check
   * @return  True if connected
   */
  private boolean isConnected(LevelReader world, Direction facing, BlockPos target) {
    BlockState state = world.getBlockState(target);
    return state.getBlock() == this && state.getValue(FACING) == facing;
  }

  /**
   * Gets the facing relative north
   * @param facing  Punji stick facing
   * @return  North for the given facing
   */
  private static Direction getLocalNorth(Direction facing) {
    return switch (facing) {
      case DOWN -> Direction.NORTH;
      case UP -> Direction.SOUTH;
      default -> Direction.UP;
    };
  }

  /**
   * Gets the facing relative east
   * @param facing  Punji stick facing
   * @return  East for the given facing
   */
  private static Direction getLocalEast(Direction facing) {
    if (facing.getAxis() == Axis.Y) {
      return Direction.EAST;
    }
    return facing.getClockWise();
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
    Direction direction = state.getValue(FACING);
    BlockPos target = pos.relative(direction);
    return world.getBlockState(target).isFaceSturdy(world, target, direction.getOpposite());
  }

  @Override
  protected void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    if (entityIn instanceof LivingEntity && worldIn instanceof ServerLevel serverLevel) {
      Direction side = state.getValue(FACING);
      Axis axis = side.getAxis();
      // only take damage if in the same half as the punji sticks
      if (side.getAxisDirection() == AxisDirection.POSITIVE) {
        if (entityIn.getBoundingBox().max(axis) <= pos.get(axis) + 0.5f) {
          return;
        }
      } else {
        if (entityIn.getBoundingBox().min(axis) >= pos.get(axis) + 0.5f) {
          return;
        }
      }

      float damage = 1f;
      if (entityIn.fallDistance > 0) {
        damage += entityIn.fallDistance + 1;
      }
      entityIn.hurtServer(serverLevel, entityIn.damageSources().cactus(), damage);
    }
  }

  /* Bounds */
  private static final Map<Direction, VoxelShape> BOUNDS;
  static {
    BOUNDS = new EnumMap<>(Direction.class);
    BOUNDS.put(Direction.DOWN, Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.375, 0.8125));
    BOUNDS.put(Direction.UP, Shapes.box(0.1875, 0.625, 0.1875, 0.8125, 1, 0.8125));
    BOUNDS.put(Direction.NORTH, Shapes.box(0.1875, 0.1875, 0, 0.8125, 0.8125, 0.375));
    BOUNDS.put(Direction.SOUTH, Shapes.box(0.1875, 0.1875, 0.625, 0.8125, 0.8125, 1));
    BOUNDS.put(Direction.EAST, Shapes.box(0.625, 0.1875, 0.1875, 1, 0.8125, 0.8125));
    BOUNDS.put(Direction.WEST, Shapes.box(0, 0.1875, 0.1875, 0.375, 0.8125, 0.8125));
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
    return BOUNDS.get(state.getValue(FACING));
  }

  @Override
  protected FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }
}
