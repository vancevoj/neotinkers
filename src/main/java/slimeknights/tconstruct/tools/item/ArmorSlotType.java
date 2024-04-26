package slimeknights.tconstruct.tools.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.Nullable;
import java.util.Locale;

/** Enum to aid in armor registraton */
@RequiredArgsConstructor
@Getter
public enum ArmorSlotType implements StringRepresentable {
  BOOTS(EquipmentSlot.FEET),
  LEGGINGS(EquipmentSlot.LEGS),
  CHESTPLATE(EquipmentSlot.CHEST),
  HELMET(EquipmentSlot.HEAD);

  /** Armor slots in order from helmet to boots, {@link #values()} will go from boots to helmet. */
  public static final ArmorSlotType[] TOP_DOWN = { HELMET, CHESTPLATE, LEGGINGS, BOOTS };
  /** copy of the vanilla array for use in builders */
  public static final int[] MAX_DAMAGE_ARRAY = {13, 15, 16, 11};

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
    T build(ArmorSlotType slot);
  }
}
