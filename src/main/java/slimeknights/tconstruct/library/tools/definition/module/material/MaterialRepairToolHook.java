package slimeknights.tconstruct.library.tools.definition.module.material;

import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/** Hook for repairing a tool via tool materials */
public interface MaterialRepairToolHook {
  /**
   * Checks if the given material can be used to repair this tool
   * @param tool      Tool to check
   * @param material  Material to check
   * @return  True if it can be used to repair this tool
   */
  boolean isRepairMaterial(IToolStackView tool, MaterialId material);

  /**
   * Gets the amount of durability restored by this materail for repair
   * @param tool      Tool instance
   * @param material  Material used for repair
   * @return  Repair amount
   */
  float getRepairAmount(IToolStackView tool, MaterialId material);

  /**
   * Gets the factor to multiply an attempted repair with this material, used for part swapping repair notably.
   * @param tool     Tool instance
   * @param material Material used to repair
   * @return  Repair factor, 1 is the default
   */
  float getRepairFactor(IToolStackView tool, MaterialId material);


  /** Gets the repair stat for the given tool */
  static boolean canRepairWith(IToolStackView tool, MaterialId material) {
    return tool.getHook(ToolHooks.MATERIAL_REPAIR).isRepairMaterial(tool, material);
  }

  /** Gets the repair stat for the given tool */
  static float repairAmount(IToolStackView tool, MaterialId material) {
    return tool.getHook(ToolHooks.MATERIAL_REPAIR).getRepairAmount(tool, material);
  }

  /** Gets the repair stat for the given tool */
  static float repairFactor(IToolStackView tool, MaterialId material) {
    return tool.getHook(ToolHooks.MATERIAL_REPAIR).getRepairFactor(tool, material);
  }

  /** Merger that takes the largest option from all nested modules */
  record MaxMerger(Collection<MaterialRepairToolHook> hooks) implements MaterialRepairToolHook {
    @Override
    public boolean isRepairMaterial(IToolStackView tool, MaterialId material) {
      for (MaterialRepairToolHook hook : hooks) {
        if (hook.isRepairMaterial(tool, material)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public float getRepairAmount(IToolStackView tool, MaterialId material) {
      float amount = 0;
      for (MaterialRepairToolHook hook : hooks) {
        amount = Math.max(amount, hook.getRepairAmount(tool, material));
      }
      return amount;
    }

    @Override
    public float getRepairFactor(IToolStackView tool, MaterialId material) {
      float factor = 0;
      for (MaterialRepairToolHook hook : hooks) {
        factor = Math.max(factor, hook.getRepairFactor(tool, material));
      }
      return factor;
    }
  }
}
