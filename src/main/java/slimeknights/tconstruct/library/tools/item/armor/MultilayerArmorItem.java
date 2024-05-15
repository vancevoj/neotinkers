package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.MultilayerArmorModel;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  private final ArmorTextureSupplier[] textures;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorSlotType slot, Properties properties, ArmorTextureSupplier... textures) {
    super(material, slot, properties);
    this.textures = textures;
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorSlotType slot, Properties properties, Function<ResourceLocation,ArmorTextureSupplier[]> textures) {
    this(material, slot, properties, textures.apply(material.getId()));
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
