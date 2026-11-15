package slimeknights.tconstruct.library.events.teleport;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

/** Event fired when an entity teleports via a tool modifier. Subclasses may have more context. */
@Getter
public class ModifierTeleportEvent extends EntityTeleportEvent {
  private final ModifierEntry modifier;
  public ModifierTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ModifierEntry modifier) {
    // teleports only fire on the server, within the current dimension
    super(entity, (ServerLevel) entity.level(), targetX, targetY, targetZ);
    this.modifier = modifier;
  }
}
