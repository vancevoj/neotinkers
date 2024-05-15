package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.MaterialArmorModel;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.item.armor.texture.MaterialArmorTextureSupplier.MaterialSetCache;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Armor item for an item with multiple materials */
public class MaterialArmorItem extends ModifiableArmorItem {
  private final MaterialSetCache cache;
  public MaterialArmorItem(ModifiableArmorMaterial material, ArmorSlotType slotType, Properties properties) {
    super(material, slotType, properties);
    this.cache = MaterialSetCache.FACTORY.apply(material.getId());
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
      return MaterialArmorModel.INSTANCE.setup(entityLiving, stack, armorSlot, _default, getToolDefinition(), cache);
      }
    });
  }
}
