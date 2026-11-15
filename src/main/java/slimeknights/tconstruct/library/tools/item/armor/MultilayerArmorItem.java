package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  private final Identifier name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorType slot, Properties properties, Identifier name) {
    super(material, slot, properties);
    this.name = name;
  }

  public MultilayerArmorItem(DummyArmorMaterial material, ArmorType slot, Properties properties, ToolDefinition toolDefinition) {
    this(material, slot, properties, toolDefinition, material.getId());
  }

  public MultilayerArmorItem(DummyArmorMaterial material, ArmorType slot, Properties properties, ToolDefinition toolDefinition, Identifier name) {
    super(material, slot, properties, toolDefinition);
    this.name = name;
  }

  @Nullable
  @Override
  public Identifier getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
    // bypasses vanilla's missing-texture errors; our own model dispatcher handles the real rendering
    return Identifier.parse(ArmorUtil.getDummyArmorTexture(slot));
  }

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
