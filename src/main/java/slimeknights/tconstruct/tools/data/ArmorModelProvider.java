package slimeknights.tconstruct.tools.data;

import net.minecraft.data.DataGenerator;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.DyedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FirstArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.FixedArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.MaterialArmorTextureSupplier;
import slimeknights.tconstruct.library.client.data.AbstractArmorModelProvider;
import slimeknights.tconstruct.tools.ArmorDefinitions;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

public class ArmorModelProvider extends AbstractArmorModelProvider {
  public ArmorModelProvider(DataGenerator generator) {
    super(generator);
  }

  @Override
  protected void addModels() {
    addModel(ArmorDefinitions.TRAVELERS, name -> new ArmorTextureSupplier[] {
      new FirstArmorTextureSupplier(
        FixedArmorTextureSupplier.builder(name, "/golden_").modifier(TinkerModifiers.golden.getId()).build(),
        FixedArmorTextureSupplier.builder(name, "/base_").build()),
      new DyedArmorTextureSupplier(name, "/overlay_", TinkerModifiers.dyed.getId(), null)
    });
    addModel(ArmorDefinitions.PLATE, name -> new ArmorTextureSupplier[] {
      new MaterialArmorTextureSupplier.Material(name, "/plating_", 0),
      new MaterialArmorTextureSupplier.Material(name, "/maille_", 1)
    });
    addModel(ArmorDefinitions.SLIMESUIT, name -> new ArmorTextureSupplier[] {
      new FirstArmorTextureSupplier(
        FixedArmorTextureSupplier.builder(name, "/").materialSuffix(MaterialIds.gold).modifier(TinkerModifiers.golden.getId()).build(),
        new MaterialArmorTextureSupplier.PersistentData(name, "/", TinkerModifiers.embellishment.getId()),
        FixedArmorTextureSupplier.builder(name, "/").materialSuffix(MaterialIds.enderslime).build())
    });
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Armor Models";
  }
}
