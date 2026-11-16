package slimeknights.tconstruct.world.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.MangroveRootsBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SlimeRootsBlock extends MangroveRootsBlock {
  public SlimeRootsBlock(Properties props) {
    super(props);
  }

  @Override
  public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
    return neighbor.is(this) && side.getAxis() == Direction.Axis.Y;
  }
}
