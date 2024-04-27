package slimeknights.tconstruct.library.tools.stat;

import slimeknights.mantle.data.registry.IdAwareComponentRegistry;
import slimeknights.tconstruct.tools.stats.ArmorMaterialStatProvider;
import slimeknights.tconstruct.tools.stats.MeleeHarvestMaterialStatProvider;
import slimeknights.tconstruct.tools.stats.RangedMaterialStatProvider;
import slimeknights.tconstruct.tools.stats.SkullMaterialStatProvider;

import static slimeknights.tconstruct.TConstruct.getResource;

/** Built in tool stat providers and the stat provider registry */
public class MaterialStatProviders {
  /** Registry for tool stat providers  */
  public static final IdAwareComponentRegistry<MaterialStatProvider> REGISTRY = new IdAwareComponentRegistry<>("Unknown Tool Stat Provider");

  /** Tools with 1 or more tool parts using melee stats */
  public static final MeleeHarvestMaterialStatProvider MELEE_HARVEST = REGISTRY.register(new MeleeHarvestMaterialStatProvider(getResource("melee_harvest")));
  /** Tools with 1 or more tool parts using ranged stats */
  public static final RangedMaterialStatProvider RANGED = REGISTRY.register(new RangedMaterialStatProvider(getResource("ranged")));
  /** Tools with 1 or more types of plating and some chainmail */
  public static final ArmorMaterialStatProvider ARMOR = REGISTRY.register(new ArmorMaterialStatProvider(getResource("armor")));
  /** Stat provider for slimeskulls */
  public static final SkullMaterialStatProvider SKULL = REGISTRY.register(new SkullMaterialStatProvider(getResource("skull")));
}
