package slimeknights.tconstruct.library.tools.definition;

import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.tools.item.armor.DummyArmorMaterial;

import javax.annotation.Nullable;

/** Armor material that doubles as a container for tool definitions for each armor slot */
public class ModifiableArmorMaterial extends DummyArmorMaterial {
  /** Array of all four armor slot types */
  public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
  /** The four humanoid armor item types Tinkers registers armor for (excludes 1.21's BODY type for wolf/horse armor) */
  public static final ArmorType[] ARMOR_TYPES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};

  /** Array of slot index to tool definition for the slot */
  private final ToolDefinition[] armorDefinitions;

  private ModifiableArmorMaterial(Identifier id, SoundEvent equipSound, ToolDefinition... armorDefinitions) {
    super(id, equipSound);
    // 1.21 added ArmorType.BODY (wolf armor) as a 5th type; size by values().length so any slot ordinal fits
    if (armorDefinitions.length != ArmorType.values().length) {
      throw new IllegalArgumentException("Must have an armor definition slot for each armor type");
    }
    this.armorDefinitions = armorDefinitions;
  }

  /** Creates a modifiable armor material, creates tool definition for the selected slots */
  public static ModifiableArmorMaterial create(Identifier id, SoundEvent equipSound, ArmorType... slots) {
    ToolDefinition[] definitions = new ToolDefinition[ArmorType.values().length];
    for (ArmorType slot : slots) {
      definitions[slot.ordinal()] = ToolDefinition.create(id.withSuffix("_" + slot.getName()));
    }
    return new ModifiableArmorMaterial(id, equipSound, definitions);
  }

  /** Creates a modifiable armor material, creates tool definition for the four humanoid armor slots (not BODY) */
  public static ModifiableArmorMaterial create(Identifier id, SoundEvent equipSound) {
    return create(id, equipSound, ARMOR_TYPES);
  }

  /**
   * Gets the armor definition for the given armor slot, used in item construction
   * @param slotType  Slot type
   * @return  Armor definition
   */
  @Nullable
  public ToolDefinition getArmorDefinition(ArmorType slotType) {
    return armorDefinitions[slotType.ordinal()];
  }
}
