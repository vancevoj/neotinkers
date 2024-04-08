package slimeknights.tconstruct.plugin.jei.entity;

import net.minecraft.world.entity.EntityType;

import java.util.List;

/** Simple wrapper around entity type as JEI does not support multiple mods registering the same class */
public record EntityInput(EntityType<?> type) {
  /** Wraps the given list into a list of entity inputs */
  public static List<EntityInput> wrap(List<EntityType<?>> types) {
    return types.stream().map(EntityInput::new).toList();
  }
  /** Wraps the given list into a list of entity inputs */
  @SuppressWarnings("rawtypes")
  public static List<EntityInput> wrapRaw(List<EntityType> types) {
    return types.stream().map(EntityInput::new).toList();
  }
}
