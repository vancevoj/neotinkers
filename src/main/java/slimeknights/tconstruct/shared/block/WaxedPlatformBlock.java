package slimeknights.tconstruct.shared.block;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.modules.util.ToolActionTransformHelper;
import slimeknights.tconstruct.shared.TinkerCommons;

public class WaxedPlatformBlock extends PlatformBlock {
  private final WeatherState age;
  public WaxedPlatformBlock(WeatherState age, Properties prop) {
    super(prop);
    this.age = age;
  }

  @Override
  protected boolean verticalConnect(BlockState state) {
    return state.is(TinkerTags.Blocks.COPPER_PLATFORMS);
  }

  /** Gets the unwaxed version of this platform, used for the {@code neoforge:transformables} data map so vanilla axes can wax off platforms */
  public PlatformBlock getUnwaxed() {
    return TinkerCommons.copperPlatform.get(age);
  }

  /**
   * NeoForge removed the axe item abilities, vanilla axes use the {@code neoforge:transformables} data map instead.
   * Tinkers tools still ask for the legacy {@code axe_wax_off} ability.
   */
  @Nullable
  @Override
  public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility toolAction, boolean simulate) {
    if (ToolActionTransformHelper.AXE_WAX_OFF.equals(toolAction)) {
      return TinkerCommons.copperPlatform.get(age).withPropertiesOf(state);
    }
    return null;
  }
}
