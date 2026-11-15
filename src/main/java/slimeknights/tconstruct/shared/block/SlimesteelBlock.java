package slimeknights.tconstruct.shared.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class SlimesteelBlock extends Block {
  public SlimesteelBlock(Properties properties) {
    // 26.2 replaced updateEntityAfterFallOn with bounce restitution. Living entities bounce at 75%,
    // other entities get 80% of that from the NeoForge bounce logic, which is the old 60%
    super(properties.bounceRestitution(0.75f));
  }

  @Override
  public boolean isSlimeBlock(BlockState state) {
    return true;
  }

  @Override
  public boolean canStickTo(BlockState state, BlockState other) {
    return other.isSlimeBlock();
  }

  @Override
  public void fallOn(Level worldIn, BlockState state, BlockPos pos, Entity entityIn, double fallDistance) {
    if (entityIn.isSuppressingBounce()) {
      super.fallOn(worldIn, state, pos, entityIn, fallDistance);
    } else {
      entityIn.causeFallDamage(fallDistance, 0.0F, worldIn.damageSources().fall());
    }
  }

  @Override
  protected boolean isPathfindable(BlockState state, PathComputationType type) {
    return false;
  }
}
