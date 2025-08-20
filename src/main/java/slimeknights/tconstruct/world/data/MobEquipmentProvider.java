package slimeknights.tconstruct.world.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.tinkering.AbstractMobEquipmentProvider;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tools.TinkerTools;

/** Provider for custom mob equipment */
public class MobEquipmentProvider extends AbstractMobEquipmentProvider {
  public MobEquipmentProvider(PackOutput output) {
    super(output, TConstruct.MOD_ID);
  }

  @Override
  protected void addEquipment() {
    IJsonPredicate<MaterialVariantId> includeInLoot = MaterialPredicate.tag(TinkerTags.Materials.EXCLUDE_FROM_LOOT).inverted();
    RandomMaterial random = RandomMaterial.random().allowHidden().material(includeInLoot).build();

    // piglins spawn with battle signs
    equip(TinkerTags.EntityTypes.PIGLINS)
      .slot(EquipmentSlot.MAINHAND)
      // only replace golden weapons, never a crossbow
      .match(ItemPredicate.set(Items.GOLDEN_SWORD, Items.GOLDEN_AXE))
      .tool(TinkerTools.battlesign)
      .material(random, random);
    // drowned spawn with swashers
    equip(EntityType.DROWNED)
      .slot(EquipmentSlot.MAINHAND)
      // only replace empty hand
      .match(ItemPredicate.set(Items.AIR))
      .tool(TinkerTools.swasher)
      .fluid(Fluids.LAVA)
      .material(random, random, random);
    // husks spawn with melting pans
    equip(EntityType.HUSK)
      .slot(EquipmentSlot.MAINHAND)
      .tool(TinkerTools.meltingPan)
      .material(random, random);
    // zombie villagers spawn with war picks
    equip(EntityType.ZOMBIE_VILLAGER)
      .slot(EquipmentSlot.MAINHAND)
      .tool(TinkerTools.warPick)
      .material(random, random, random);
  }

  @Override
  public String getName() {
    return "Tinkers' Construct mob equipment";
  }
}
