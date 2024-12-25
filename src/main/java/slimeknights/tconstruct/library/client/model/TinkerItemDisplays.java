package slimeknights.tconstruct.library.client.model;

import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.tconstruct.TConstruct;

import java.util.Locale;

import static slimeknights.tconstruct.library.client.model.tools.ToolModel.registerSmallTool;

/** Custom transform types used for tinkers item rendering */
@SuppressWarnings("unused") // used in JSON
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  public static void init()    {}

  /** Used by the melter and smeltery for display of items its melting */
  public static ItemDisplayContext MELTER = registerSmallTool(create("melter", ItemDisplayContext.NONE));
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static ItemDisplayContext TABLE = create("table", ItemDisplayContext.NONE);
  /** Used by the casting table for item rendering */
  public static ItemDisplayContext CASTING_TABLE = registerSmallTool(create("casting_table", ItemDisplayContext.FIXED));
  /** Used by the casting basin for item rendering */
  public static ItemDisplayContext CASTING_BASIN = registerSmallTool(create("casting_basin", ItemDisplayContext.NONE));

  /** Creates a transform type */
  private static ItemDisplayContext create(String name, ItemDisplayContext fallback) {
    String key = "TCONSTRUCT_" + name.toUpperCase(Locale.ROOT);
    if (fallback == ItemDisplayContext.NONE) {
      return ItemDisplayContext.create(key, TConstruct.getResource(name), null);
    }
    return ItemDisplayContext.create(key, TConstruct.getResource(name), fallback);
  }
}
