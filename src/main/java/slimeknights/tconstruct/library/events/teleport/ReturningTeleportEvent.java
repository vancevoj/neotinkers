package slimeknights.tconstruct.library.events.teleport;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/** Event fired when {@link slimeknights.tconstruct.shared.TinkerEffects#returning} teleport triggers */
public class ReturningTeleportEvent extends EntityTeleportEvent {
  public ReturningTeleportEvent(LivingEntity entity, double targetX, double targetY, double targetZ) {
    // teleports only fire on the server, within the current dimension
    super(entity, (ServerLevel) entity.level(), targetX, targetY, targetZ);
  }
}
