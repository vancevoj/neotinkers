package slimeknights.tconstruct.library.tools.definition.module.material;

import slimeknights.mantle.data.registry.IdAwareComponentRegistry;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStatsBuilder;
import slimeknights.tconstruct.tools.MeleeHarvestToolStatsBuilder;
import slimeknights.tconstruct.tools.RangedToolStatsBuilder;
import slimeknights.tconstruct.tools.stats.BowstringMaterialStats;
import slimeknights.tconstruct.tools.stats.ExtraMaterialStats;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;
import slimeknights.tconstruct.tools.stats.SkullStats;
import slimeknights.tconstruct.tools.stats.SkullToolStatsBuilder;

import java.util.Set;
import java.util.function.BiFunction;

/** Built in tool stat providers and the stat provider registry */
public class MaterialStatProviders {
  /** Registry for tool stat providers  */
  public static final IdAwareComponentRegistry<MaterialStatProvider> REGISTRY = new IdAwareComponentRegistry<>("Unknown Tool Stat Provider");

  /** Tools with 1 or more tool parts using melee stats */
  public static final MaterialStatProvider MELEE_HARVEST = register("melee_harvest", HeadMaterialStats.ID, Set.of(HandleMaterialStats.ID, ExtraMaterialStats.ID), MeleeHarvestToolStatsBuilder::from);
  /** Tools with 1 or more tool parts using ranged stats */
  public static final MaterialStatProvider RANGED = register("ranged", LimbMaterialStats.ID, Set.of(BowstringMaterialStats.ID, GripMaterialStats.ID), RangedToolStatsBuilder::from);
  /** Stat provider for slimeskulls */
  public static final MaterialStatProvider SKULL = register("skull", SkullStats.ID, Set.of(), SkullToolStatsBuilder::from);

  /** Helper to register in our domain */
  private static MaterialStatProvider register(String name, MaterialStatsId requiredType, Set<MaterialStatsId> otherTypes, BiFunction<ToolDefinition,MaterialNBT,ToolStatsBuilder> builder) {
    return REGISTRY.register(new MaterialStatProvider(TConstruct.getResource(name), Set.of(requiredType), otherTypes, builder));
  }
}
