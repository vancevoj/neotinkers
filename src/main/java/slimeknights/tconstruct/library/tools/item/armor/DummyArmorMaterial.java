package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import slimeknights.mantle.registration.object.IdAwareObject;

/**
 * Armor material identity for Tinkers armor: holds the ID and the equip sound, since Tinkers bypasses all the vanilla
 * armor stat usages and computes them dynamically from the tool.
 * <p>
 * Since 1.21.5 vanilla armor is driven by the equippable component instead of {@code ArmorItem} and an armor material,
 * so this no longer produces a vanilla armor material. {@link ModifiableArmorItem} builds the equippable component from the
 * slot and {@link #getEquipSound()}.
 */
@Getter
public class DummyArmorMaterial implements IdAwareObject {
  private final Identifier id;
  private final SoundEvent equipSound;

  public DummyArmorMaterial(Identifier id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;
  }

  /** Gets the name of this material, matching the ID. Used as the registry key by Tinkers' display logic. */
  public String getName() {
    return id.toString();
  }
}
