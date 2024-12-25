package slimeknights.tconstruct.tools.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Enum to aid in armor registraton.
 * TODO: can this be replaced with {@link net.minecraft.world.item.ArmorItem.Type}?
 */
@RequiredArgsConstructor
@Getter
public enum ArmorSlotType implements StringRepresentable {
  @Deprecated(forRemoval = true)
  BOOTS(EquipmentSlot.FEET),
  @Deprecated(forRemoval = true)
  LEGGINGS(EquipmentSlot.LEGS),
  @Deprecated(forRemoval = true)
  CHESTPLATE(EquipmentSlot.CHEST),
  @Deprecated(forRemoval = true)
  HELMET(EquipmentSlot.HEAD);

  /** Armor slots in order from helmet to boots, {@link #values()} will go from boots to helmet. */
  @Deprecated(forRemoval = true)
  public static final ArmorSlotType[] TOP_DOWN = { HELMET, CHESTPLATE, LEGGINGS, BOOTS };
  /** copy of the vanilla array for use in builders */
  public static final int[] MAX_DAMAGE_ARRAY = {11, 16, 15, 13};
  /** factor for shield durability */
  public static final int SHIELD_DAMAGE = 22;

  private final EquipmentSlot equipmentSlot;
  private final String serializedName = toString().toLowerCase(Locale.ROOT);
  private final int index = ordinal();

  /** Gets an equipment slot for the given armor slot */
  @Nullable
  public static ArmorSlotType fromEquipment(EquipmentSlot slotType) {
    return switch (slotType) {
      case FEET -> BOOTS;
      case LEGS -> LEGGINGS;
      case CHEST -> CHESTPLATE;
      case HEAD -> HELMET;
      default -> null;
    };
  }

  /**
   * Interface for armor module builders, which are builders designed to create slightly varied modules based on the armor slot
   */
  public interface ArmorBuilder<T> {
    /** Builds the object for the given slot */
    T build(ArmorItem.Type slot);
  }

  /**
   * Builder for an object that also includes shields
   */
  public interface ArmorShieldBuilder<T> extends ArmorBuilder<T> {
    /** Builds the object for the shield */
    T buildShield();
  }
}
