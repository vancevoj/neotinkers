package slimeknights.tconstruct.library.events.teleport;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/** @deprecated No longer used. See {@link SlingModifierTeleportEvent} */
@Deprecated(forRemoval = true)
public class SlimeslingTeleportEvent extends EntityTeleportEvent {
  @Getter
  private final ItemStack sling;
  public SlimeslingTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ItemStack sling) {
    // teleports only fire on the server, within the current dimension
    super(entity, (ServerLevel) entity.level(), targetX, targetY, targetZ);
    this.sling = sling;
  }
}
