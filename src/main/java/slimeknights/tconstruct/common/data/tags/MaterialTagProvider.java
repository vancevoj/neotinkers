package slimeknights.tconstruct.common.data.tags;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.tinkering.AbstractMaterialTagProvider;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

public class MaterialTagProvider extends AbstractMaterialTagProvider {
  public MaterialTagProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
    super(packOutput, TConstruct.MOD_ID, existingFileHelper);
  }

  @Override
  protected void addTags() {
    tag(TinkerTags.Materials.EXCLUDE_FROM_LOOT);
    tag(TinkerTags.Materials.NETHER).add(
      // tier 1
      MaterialIds.wood, MaterialIds.flint, MaterialIds.rock, MaterialIds.bone,
      MaterialIds.leather, MaterialIds.vine, MaterialIds.string,
      // tier 2
      MaterialIds.gold,
      MaterialIds.scorchedStone, MaterialIds.slimewood, MaterialIds.necroticBone,
      // tier 3
      MaterialIds.nahuatl, MaterialIds.obsidian, MaterialIds.darkthread,
      MaterialIds.cobalt, MaterialIds.steel,
      // tier 4
      MaterialIds.manyullyn, MaterialIds.cinderslime,
      MaterialIds.queensSlime, MaterialIds.blazingBone, MaterialIds.blazewood,
      MaterialIds.ancientHide, MaterialIds.ancient
    );
    // materials bartered by piglins
    tag(TinkerTags.Materials.BARTERED).add(
      // tier 3
      MaterialIds.nahuatl, MaterialIds.obsidian, MaterialIds.darkthread,
      MaterialIds.cobalt, MaterialIds.steel,
      // tier 4
      MaterialIds.manyullyn, MaterialIds.hepatizon,
      MaterialIds.cinderslime, MaterialIds.queensSlime,
      MaterialIds.blazingBone, MaterialIds.blazewood,
      MaterialIds.ancientHide, MaterialIds.ancient
    ).addOptional(MaterialIds.necronium);

    // material categories
    // melee harvest
    tag(TinkerTags.Materials.GENERAL).add(
      // tier 1
      MaterialIds.wood, MaterialIds.string, MaterialIds.vine, MaterialIds.leather,
      // tier 2
      MaterialIds.iron, MaterialIds.slimewood,
      MaterialIds.osmium,
      // tier 3
      MaterialIds.slimesteel, MaterialIds.pigIron, MaterialIds.roseGold, MaterialIds.cobalt,
      MaterialIds.platedSlimewood, MaterialIds.electrum,
      // tier 4
      MaterialIds.cinderslime, MaterialIds.queensSlime
    );
    tag(TinkerTags.Materials.HARVEST).add(
      // tier 1
      MaterialIds.rock, MaterialIds.copper,
      // tier 2
      MaterialIds.searedStone, MaterialIds.whitestone, MaterialIds.skyslimeVine,
      MaterialIds.tungsten, MaterialIds.platinum,
      // tier 3
      MaterialIds.amethystBronze,
      MaterialIds.bronze, MaterialIds.constantan,
      // tier 4
      MaterialIds.hepatizon
    );
    tag(TinkerTags.Materials.MELEE).add(
      // tier 1
      MaterialIds.flint, MaterialIds.bone, MaterialIds.chorus,
      // tier 2
      MaterialIds.scorchedStone, MaterialIds.necroticBone, MaterialIds.venombone,
      MaterialIds.silver, MaterialIds.lead,
      // tier 3
      MaterialIds.nahuatl, MaterialIds.steel,
      MaterialIds.invar, MaterialIds.necronium,
      // tier 4
      MaterialIds.manyullyn, MaterialIds.blazingBone
    );

    // ranged
    tag(TinkerTags.Materials.BALANCED).add(
      // tier 1
      MaterialIds.wood, MaterialIds.chorus,
      MaterialIds.string, MaterialIds.vine,
      // tier 2
      MaterialIds.slimewood, MaterialIds.necroticBone, MaterialIds.skyslimeVine,
      MaterialIds.platinum,
      // tier 3
      MaterialIds.slimesteel, MaterialIds.roseGold, MaterialIds.darkthread, MaterialIds.cobalt,
      MaterialIds.invar,
      // tier 4
      MaterialIds.blazingBone
    );
    tag(TinkerTags.Materials.LIGHT).add(
      // tier 1
      MaterialIds.bamboo, MaterialIds.bone,
      // tier 2
      MaterialIds.venombone,
      MaterialIds.aluminum, MaterialIds.tungsten,
      // tier 3
      MaterialIds.nahuatl,
      MaterialIds.necronium, MaterialIds.constantan, MaterialIds.platedSlimewood,
      // tier 4
      MaterialIds.hepatizon, MaterialIds.queensSlime
    );
    tag(TinkerTags.Materials.HEAVY).add(
      // tier 1
      MaterialIds.copper,
      // tier 2
      MaterialIds.iron,
      MaterialIds.silver, MaterialIds.lead,
      // tier 3
      MaterialIds.amethystBronze, MaterialIds.steel,
      MaterialIds.bronze, MaterialIds.electrum,
      // tier 4
      MaterialIds.manyullyn, MaterialIds.cinderslime
    );
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Material Tag Provider";
  }
}
