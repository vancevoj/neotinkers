package slimeknights.tconstruct.tools.data.client;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.MaterialModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialHasFallbackModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.ModifierModel;
import slimeknights.tconstruct.library.data.AbstractModifierModelMapProvider;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.data.ModifierIds;

/** Provider for modifier models on tools */
public class ModifierModelMapProvider extends AbstractModifierModelMapProvider {
  public ModifierModelMapProvider(PackOutput output) {
    super(output, TConstruct.MOD_ID);
  }

  @Override
  protected void addModels() {
    // plate armor
    ModifierId dyed = TinkerModifiers.dyed.getId();
    for (ArmorItem.Type type : ArmorItem.Type.values()) {
      String root = "armor/plate/" + type.getName() + "/maille";
      Item item = TinkerTools.plateArmor.get(type);
      tool(item).modifier(dyed, new MaterialHasFallbackModifierModel(1,
        new DyedModifierModel(toolMaterial(root + "_metal"), null),
        new DyedModifierModel(toolMaterial(root), null),
        "metal"
      )).trim();
      tool(item, "broken").modifier(dyed, new MaterialHasFallbackModifierModel(1,
        new DyedModifierModel(toolMaterial(root + "_broken_metal"), null),
        new DyedModifierModel(toolMaterial(root + "_broken"), null),
        "metal"
      ));
    }
    // travelers
    travelersDyed(ArmorItem.Type.HELMET, "goggles");
    travelersDyed(ArmorItem.Type.CHESTPLATE, "vest");
    travelersDyed(ArmorItem.Type.LEGGINGS, "pants");
    travelersDyed(ArmorItem.Type.BOOTS, "boots");
    travelersDyed(TinkerTools.travelersShield.get(), "shield");
    for (ArmorItem.Type type : ArmorItem.Type.values()) {
      // helmets don't have trim
      if (type != ArmorItem.Type.HELMET) {
        tool(TinkerTools.travelersGear.get(type)).trim();
      }
    }
    // slimesuit
    slimeEmbellishment(ArmorItem.Type.HELMET, "skull");
    slimeEmbellishment(ArmorItem.Type.CHESTPLATE, "wings");
    slimeEmbellishment(ArmorItem.Type.LEGGINGS, "shell");
    slimeEmbellishment(ArmorItem.Type.BOOTS, "boot");
    trim(TinkerTools.slimesuit);

    // ammo
    tool(TinkerTools.arrow).tipped("ammo/arrow_modifiers/tconstruct_tipped").smashing("ammo/arrow_modifiers/tconstruct_smashing_full");
    tool(TinkerTools.shuriken).tipped("ammo/shuriken_modifiers/tconstruct_tipped").smashing("ammo/shuriken_modifiers/tconstruct_smashing_full");
    tool(TinkerTools.throwingAxe).tipped("ammo/axe_modifiers/tconstruct_tipped").smashing("ammo/axe_modifiers/tconstruct_smashing_full");
    // fishing rods just have tipped
    tool(TinkerTools.fishingRod).tipped("fishing_rod/modifiers/tconstruct_tipped").fluid().basic(ModifierIds.tank);
    tool(TinkerTools.fishingRod, "broken").constant("tipped", ModifierModel.EMPTY);
    tool(TinkerTools.fishingRod, "cast").constant("tipped", ModifierModel.EMPTY);

    // tanks
    tool(TinkerTools.meltingPan).fluid();
    tool(TinkerTools.swasher).fluid();
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Modifier Model Map Provider";
  }

  /** Adds dyed textures for travelers gear */
  private void travelersDyed(Item item, String name) {
    String root = "armor/travelers/" + name + "/modifiers/";
    ModifierId dyed = TinkerModifiers.dyed.getId();
    tool(item).modifier(dyed, new DyedModifierModel(toolMaterial(root + "tconstruct_dyed"), null));
    tool(item, "broken").modifier(dyed, new DyedModifierModel(toolMaterial(root + "broken/tconstruct_dyed"), null));
  }

  /** Adds dyed textures for travelers gear */
  private void travelersDyed(ArmorItem.Type type, String name) {
    travelersDyed(TinkerTools.travelersGear.get(type), name);
  }

  /** Adds dyed textures for travelers gear */
  private void slimeEmbellishment(ArmorItem.Type type, String name) {
    String root = "armor/slime/" + name + "_modifiers/";
    ModifierId embellishment = TinkerModifiers.embellishment.getId();
    Item item = TinkerTools.slimesuit.get(type);
    tool(item).modifier(embellishment, new MaterialModifierModel(toolMaterial(root + "tconstruct_embellishment"), null));
    tool(item, "broken").modifier(embellishment, new MaterialModifierModel(toolMaterial(root + "broken/tconstruct_embellishment"), null));
  }
}
