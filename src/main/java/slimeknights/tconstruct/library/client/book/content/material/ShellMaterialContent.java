package slimeknights.tconstruct.library.client.book.content.material;

import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.tools.stats.RepairStats;

/** Content page for slimesuit shells, showing stats and traits. */
public class ShellMaterialContent extends SingleMaterialStatContent {
  public static final Identifier ID = TConstruct.getResource("shell_material");

  public ShellMaterialContent(MaterialVariantId materialVariant, boolean detailed) {
    super(materialVariant, detailed);
  }

  @Override
  public Identifier getId() {
    return ID;
  }

  @Override
  protected MaterialStatsId getStatType() {
    return RepairStats.SHELL.getId();
  }

  @Override
  protected boolean hasPart() {
    return true;
  }

  @Override
  protected String translationSuffix() {
    return "shell";
  }
}
