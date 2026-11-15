package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.world.level.block.state.BlockState;

public class SearedTintedGlassBlock extends SearedGlassBlock {
  public SearedTintedGlassBlock(Properties properties) {
    super(properties);
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state) {
    return false;
  }

  @Override
  public int getLightDampening(BlockState state) {
    // was the max light level of the world, which is always 15
    return 15;
  }
}
