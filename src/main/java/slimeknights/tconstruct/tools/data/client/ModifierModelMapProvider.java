package slimeknights.tconstruct.tools.data.client;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.model.MaterialHasFallbackModifierModel;
import slimeknights.tconstruct.library.data.AbstractModifierModelMapProvider;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;

/** Provider for modifier models on tools */
public class ModifierModelMapProvider extends AbstractModifierModelMapProvider {
  public ModifierModelMapProvider(PackOutput output) {
    super(output, TConstruct.MOD_ID);
  }

  @Override
  protected void addModels() {
    ModifierId dyed = TinkerModifiers.dyed.getId();
    for (ArmorItem.Type type : ArmorItem.Type.values()) {
      String root = "armor/plate/" + type.getName() + "/maille";
      Item item = TinkerTools.plateArmor.get(type);
      model(item, dyed, new MaterialHasFallbackModifierModel(1,
        new DyedModifierModel(toolMaterial(root + "_metal"), null),
        new DyedModifierModel(toolMaterial(root), null),
        "metal"
      ));
      model(item, "broken", dyed, new MaterialHasFallbackModifierModel(1,
        new DyedModifierModel(material(root + "_broken_metal"), null),
        new DyedModifierModel(material(root + "_broken"), null),
        "metal"
      ));
    }
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Modifier Model Map Provider";
  }
}
