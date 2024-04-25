package slimeknights.tconstruct.library.tools.definition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.tools.item.DummyArmorMaterial;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import javax.annotation.Nullable;

/** Armor material that doubles as a container for tool definitions for each armor slot */
public class ModifiableArmorMaterial extends DummyArmorMaterial {
  /** Array of all four armor slot types */
  public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

  /** Array of slot index to tool definition for the slot */
  private final ToolDefinition[] armorDefinitions;

  public ModifiableArmorMaterial(ResourceLocation id, SoundEvent equipSound, ToolDefinition... armorDefinitions) {
    super(id, equipSound);
    if (armorDefinitions.length != 4) {
      throw new IllegalArgumentException("Must have an armor definition for each slot");
    }
    this.armorDefinitions = armorDefinitions;
  }

  /** Creates a modifiable armor material, creates tool definition for the selected slots */
  public static ModifiableArmorMaterial create(ResourceLocation id, SoundEvent equipSound, ArmorSlotType... slots) {
    ToolDefinition[] definitions = new ToolDefinition[4];
    for (ArmorSlotType slot : slots) {
      definitions[slot.getIndex()] = ToolDefinition.create(LocationExtender.INSTANCE.suffix(id, "_" + slot.getSerializedName()));
    }
    return new ModifiableArmorMaterial(id, equipSound, definitions);
  }

  /** Creates a modifiable armor material, creates tool definition for all four armor slots */
  public static ModifiableArmorMaterial create(ResourceLocation id, SoundEvent equipSound) {
    return create(id, equipSound, ArmorSlotType.values());
  }

  /**
   * Gets the armor definition for the given armor slot, used in item construction
   * @param slotType  Slot type
   * @return  Armor definition
   */
  @Nullable
  public ToolDefinition getArmorDefinition(ArmorSlotType slotType) {
    return armorDefinitions[slotType.getIndex()];
  }
}
