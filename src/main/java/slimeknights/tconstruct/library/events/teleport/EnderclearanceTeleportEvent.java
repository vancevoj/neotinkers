package slimeknights.tconstruct.library.events.teleport;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import slimeknights.tconstruct.library.utils.TeleportHelper.ITeleportEventFactory;

/** Event fired when an entity teleports via {@link slimeknights.tconstruct.tools.modules.armor.EnderclearanceModule} */
@Cancelable
public class EnderclearanceTeleportEvent extends EntityTeleportEvent {
  public static final ITeleportEventFactory TELEPORT_FACTORY = EnderclearanceTeleportEvent::new;

  public EnderclearanceTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
    super(entity, targetX, targetY, targetZ);
  }
}
