package slimeknights.tconstruct.tools;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolDefinitions {
  // rock
  public static final ToolDefinition PICKAXE = ToolDefinition.builder(TinkerTools.pickaxe).build();
  public static final ToolDefinition SLEDGE_HAMMER = ToolDefinition.builder(TinkerTools.sledgeHammer).build();
  public static final ToolDefinition VEIN_HAMMER = ToolDefinition.builder(TinkerTools.veinHammer).build();

  // dirt
  public static final ToolDefinition MATTOCK = ToolDefinition.builder(TinkerTools.mattock).build();
  public static final ToolDefinition PICKADZE = ToolDefinition.builder(TinkerTools.pickadze).build();
  public static final ToolDefinition EXCAVATOR = ToolDefinition.builder(TinkerTools.excavator).build();

  // wood
  public static final ToolDefinition HAND_AXE = ToolDefinition.builder(TinkerTools.handAxe).build();
  public static final ToolDefinition BROAD_AXE = ToolDefinition.builder(TinkerTools.broadAxe).build();

  // scythes
  public static final ToolDefinition KAMA = ToolDefinition.builder(TinkerTools.kama).build();
  public static final ToolDefinition SCYTHE = ToolDefinition.builder(TinkerTools.scythe).build();
  // swords
  public static final ToolDefinition DAGGER = ToolDefinition.builder(TinkerTools.dagger).build();
  public static final ToolDefinition SWORD = ToolDefinition.builder(TinkerTools.sword).build();
  public static final ToolDefinition CLEAVER = ToolDefinition.builder(TinkerTools.cleaver).build();

  // bows
  public static final ToolDefinition CROSSBOW = ToolDefinition.builder(TinkerTools.crossbow).build();
  public static final ToolDefinition LONGBOW = ToolDefinition.builder(TinkerTools.longbow).build();

  // special
  public static final ToolDefinition FLINT_AND_BRICK = ToolDefinition.builder(TinkerTools.flintAndBrick).build();
  public static final ToolDefinition SKY_STAFF = ToolDefinition.builder(TinkerTools.skyStaff).build();
  public static final ToolDefinition EARTH_STAFF = ToolDefinition.builder(TinkerTools.earthStaff).build();
  public static final ToolDefinition ICHOR_STAFF = ToolDefinition.builder(TinkerTools.ichorStaff).build();
  public static final ToolDefinition ENDER_STAFF = ToolDefinition.builder(TinkerTools.enderStaff).build();
}
