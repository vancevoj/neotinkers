package slimeknights.tconstruct.library.json.variable.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Optional;

/** Gets the biome temperature at the targeted block */
public record BlockTemperatureVariable(float fallback) implements MiningSpeedVariable {
  public static final RecordLoadable<BlockTemperatureVariable> LOADER = RecordLoadable.create(
    FloatLoadable.ANY.requiredField("fallback", BlockTemperatureVariable::fallback),
    BlockTemperatureVariable::new);

  /** Gets the block position relative to the arguments */
  private static BlockPos getPos(@Nullable BreakSpeed event, Player player) {
    // use block position if possible player position otherwise
    if (event != null) {
      Optional<BlockPos> eventPos = event.getPosition();
      if (eventPos.isPresent()) {
        return eventPos.get();
      }
    }
    return player.blockPosition();
  }

  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeed event, @Nullable Player player, @Nullable Direction sideHit) {
    if (player != null) {
      BlockPos pos = getPos(event, player);
      return player.level().getBiome(pos).value().getTemperature(pos);
    }
    return fallback;
  }

  @Override
  public RecordLoadable<? extends IHaveLoader> getLoader() {
    return LOADER;
  }
}
