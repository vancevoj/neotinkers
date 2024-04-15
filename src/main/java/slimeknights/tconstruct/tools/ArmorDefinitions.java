package slimeknights.tconstruct.tools;

import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

public class ArmorDefinitions {
   /** Balanced armor set */
  public static final ModifiableArmorMaterial TRAVELERS = ModifiableArmorMaterial
    .builder(TConstruct.getResource("travelers"))
    .setSoundEvent(Sounds.EQUIP_TRAVELERS.getSound())
    .build();
  public static final ToolDefinition TRAVELERS_SHIELD = ToolDefinition.builder(TinkerTools.travelersShield).build();

  /** High defense armor set */
  public static final ModifiableArmorMaterial PLATE = ModifiableArmorMaterial
    .builder(TConstruct.getResource("plate"))
    .setSoundEvent(Sounds.EQUIP_PLATE.getSound())
    .build();
  public static final ToolDefinition PLATE_SHIELD = ToolDefinition.builder(TinkerTools.plateShield).build();

  /** High modifiers armor set */
  public static final ModifiableArmorMaterial SLIMESUIT = ModifiableArmorMaterial
    .builder(TConstruct.getResource("slime"))
    .set(ArmorSlotType.HELMET, builder -> builder.setDefaultMaxTier(6))
    .setSoundEvent(Sounds.EQUIP_SLIME.getSound())
    .build();

}
