package slimeknights.tconstruct.library.tools.definition.module.material;

import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;

import java.util.List;

/** Hook for getting material info on tools */
public interface ToolMaterialHook {
  /**
   * Gets the list of parts on the tool
   * @param definition  Tool definition instance
   * @return  List of part requirements
   */
  List<MaterialStatsId> getStatTypes(ToolDefinition definition);

  /** Gets the stat types from the given definition */
  static List<MaterialStatsId> stats(ToolDefinition definition) {
    return definition.getHook(ToolHooks.TOOL_MATERIALS).getStatTypes(definition);
  }
}
