package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

import java.util.function.Consumer;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  private final Identifier name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties, Identifier name) {
    super(material, slot, properties, name);
    this.name = name;
  }

  public MultilayerArmorItem(DummyArmorMaterial material, ArmorType slot, Properties properties, ToolDefinition toolDefinition) {
    this(material, slot, properties, toolDefinition, material.getId());
  }

  public MultilayerArmorItem(DummyArmorMaterial material, ArmorType slot, Properties properties, ToolDefinition toolDefinition, Identifier name) {
    super(material, slot, properties, toolDefinition, name);
    this.name = name;
  }

  // 1.21.2 removed IItemExtension#getArmorTexture. The equipment asset (named after the armor model) needs no JSON and the
  // armor model dispatcher tells the vanilla layers to draw nothing, so the dummy texture workaround is no longer needed.

  /** Registered from the client extensions event, NeoForge 26.3 no longer calls this automatically */
  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new ArmorModelDispatcher() {
      @Override
      protected Identifier getName() {
        return name;
      }
    });
  }
}
