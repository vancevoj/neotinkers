package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import slimeknights.tconstruct.TConstruct;

import java.util.Locale;

import static slimeknights.tconstruct.library.client.model.tools.ToolModel.registerSmallTool;

/** Custom transform types used for tinkers item rendering */
@SuppressWarnings("unused") // used in JSON
public class TinkerTransformTypes {
  private TinkerTransformTypes() {}

  public static void init() {}

  /** Used by the melter and smeltery for display of items its melting */
  public static TransformType MELTER = registerSmallTool(create("melter", TransformType.NONE));
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static TransformType TABLE = create("table", TransformType.NONE);
  /** Used by the casting table for item rendering */
  public static TransformType CASTING_TABLE = registerSmallTool(create("casting_table", TransformType.FIXED));
  /** Used by the casting basin for item rendering */
  public static TransformType CASTING_BASIN = registerSmallTool(create("casting_basin", TransformType.NONE));

  /** Creates a transform type */
  private static TransformType create(String name, TransformType fallback) {
    String key = "TCONSTRUCT_" + name.toUpperCase(Locale.ROOT);
    if (fallback == TransformType.NONE) {
      return TransformType.create(key, TConstruct.getResource(name));
    }
    return TransformType.create(key, TConstruct.getResource(name), fallback);
  }
}
