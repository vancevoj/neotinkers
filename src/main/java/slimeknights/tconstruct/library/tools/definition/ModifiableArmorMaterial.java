package slimeknights.tconstruct.library.tools.definition;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.mantle.util.IdExtender.LocationExtender;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import javax.annotation.Nullable;

/** Armor material that doubles as a container for tool definitions for each armor slot */
public class ModifiableArmorMaterial implements ArmorMaterial, IdAwareObject {
  /** Array of all four armor slot types */
  public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

  /** Namespaced name of the armor */
  @Getter
  private final ResourceLocation id;
  /** Array of slot index to tool definition for the slot */
  private final ToolDefinition[] armorDefinitions;
  /** Sound to play when equipping the armor */
  @Getter
  private final SoundEvent equipSound;

  public ModifiableArmorMaterial(ResourceLocation id, SoundEvent equipSound, ToolDefinition... armorDefinitions) {
    this.id = id;
    this.equipSound = equipSound;
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

  /** Gets the value of a stat for the given slot */
  private float getStat(FloatToolStat toolStat, @Nullable ArmorSlotType slotType) {
    ToolDefinition toolDefinition = slotType == null ? null : getArmorDefinition(slotType);
    float defaultValue = toolStat.getDefaultValue();
    if (toolDefinition == null) {
      return defaultValue;
    }
    ToolDefinitionData data = toolDefinition.getData();
    return data.getBaseStat(toolStat) * data.getMultiplier(toolStat);
  }

  @Override
  public String getName() {
    return id.toString();
  }

  @Override
  public int getDurabilityForSlot(EquipmentSlot slotIn) {
    return (int)getStat(ToolStats.DURABILITY, ArmorSlotType.fromEquipment(slotIn));
  }

  @Override
  public int getDefenseForSlot(EquipmentSlot slotIn) {
    return (int)getStat(ToolStats.ARMOR, ArmorSlotType.fromEquipment(slotIn));
  }

  @Override
  public float getToughness() {
    return getStat(ToolStats.ARMOR_TOUGHNESS, ArmorSlotType.CHESTPLATE);
  }

  @Override
  public float getKnockbackResistance() {
    return getStat(ToolStats.KNOCKBACK_RESISTANCE, ArmorSlotType.CHESTPLATE);
  }

  @Override
  public int getEnchantmentValue() {
    return 0;
  }

  @Override
  public Ingredient getRepairIngredient() {
    return Ingredient.EMPTY;
  }
}
