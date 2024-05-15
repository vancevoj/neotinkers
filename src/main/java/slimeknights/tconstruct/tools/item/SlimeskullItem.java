package slimeknights.tconstruct.tools.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/** This item is mainly to return the proper model for a slimeskull */
public class SlimeskullItem extends ModifiableArmorItem {
  private final ArmorTextureSupplier helmet;
  public SlimeskullItem(ModifiableArmorMaterial material, Properties properties, ArmorTextureSupplier helmet) {
    super(material, ArmorSlotType.HELMET, properties);
    this.helmet = helmet;
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
        return SlimeskullArmorModel.INSTANCE.setup(entityLiving, stack, _default, helmet);
      }
    });
  }
}
