package slimeknights.tconstruct.plugin.jsonthings.item.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.MultilayerArmorModel;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Armor model with two texture layers, the base and an overlay */
public class FlexMultilayerArmorModel extends FlexModifiableArmorItem {
  private final ArmorTextureSupplier[] textures;

  public FlexMultilayerArmorModel(ArmorMaterial material, EquipmentSlot slot, Properties properties, ToolDefinition toolDefinition, ArmorTextureSupplier... textures) {
    super(material, slot, properties, toolDefinition);
    this.textures = textures;
  }

  @Nullable
  @Override
  public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
    return ArmorUtil.getDummyArmorTexture(slot);
  }

  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
      @Nonnull
      @Override
      public Model getGenericArmorModel(LivingEntity entityLiving, ItemStack stack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
        return MultilayerArmorModel.INSTANCE.setup(entityLiving, stack, armorSlot, _default, textures);
      }
    });
  }
}
