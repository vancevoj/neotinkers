package slimeknights.tconstruct.common.data.advancement;

import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.TConstruct;

/** IDs used in {@link slimeknights.tconstruct.common.data.AdvancementsProvider} and {@link FunctionProvider} */
public class AdvancementIds {
  private AdvancementIds() {}

  // basic advancements
  /** Granted by crafting a crafting table, now also crafting stations */
  public static final Identifier STORY_ROOT = id("story/root");
  /** Granted by crafting a stone pickaxe, now also stone harvest tier pickaxes */
  public static final Identifier STONE_PICK = id("story/upgrade_tools");
  /** Granted by crafting an iron pickaxe, now also iron harvest tier pickaxes */
  public static final Identifier IRON_PICK = id("story/iron_tools");
  /** Granted by crafting a netherite hoe, now also nethetite + tilling */
  public static final Identifier NETHERITE_HOE = id("husbandry/obtain_netherite_hoe");
  /** Granted by crafting a netherite hoe, now also nethetite + tilling */
  public static final Identifier WALK_ON_POWDER_SNOW = id("adventure/walk_on_powder_snow_with_leather_boots");
  // armor advancements - has 4 piece criteria
  /** Granted by crafting full set of iron armor, now also iron plating armor */
  public static final Identifier OBTAIN_ARMOR = id("story/obtain_armor");
  /** Granted by crafting full set of diamond armor, now also diamond modifier */
  public static final Identifier SHINY_GEAR = id("story/shiny_gear");
  /** Granted by crafting full set of netherite armor, now also netherite modifier */
  public static final Identifier NETHERITE_ARMOR = id("nether/netherite_armor");

  // modifier volatile flags
  /** Apply to anything with plating to grant it iron armor achievements. */
  public static final Identifier IRON_ARMOR = TConstruct.getResource("iron_armor");
  /** Apply to anything with plating to grant it diamond armor achievements. */
  public static final Identifier DIAMOND_ARMOR = TConstruct.getResource("diamond_armor");
  /** Apply to anything with plating to grant it netherite armor achievements, and to anything with the {@code till} item ability to grant netherite hoe. */
  public static final Identifier NETHERITE = TConstruct.getResource("netherite");

  private static Identifier id(String key) {
    return Identifier.withDefaultNamespace(key);
  }

  /** Creates a function ID from the given advancement */
  public static Identifier function(Identifier advancement) {
    return TConstruct.getResource("grant_advancement/" + advancement.getPath().replace('/', '_'));
  }

  /** Creates a function ID from the given advancement */
  public static Identifier function(Identifier advancement, String criteria) {
    return function(advancement).withSuffix('/' + criteria);
  }
}
