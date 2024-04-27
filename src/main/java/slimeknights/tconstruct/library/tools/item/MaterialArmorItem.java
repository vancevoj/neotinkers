package slimeknights.tconstruct.library.tools.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.MaterialArmorModel;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/** Armor item for an item with multiple materials */
public class MaterialArmorItem extends ModifiableArmorItem {
  private final boolean renderBase;
  public MaterialArmorItem(ArmorMaterial material, EquipmentSlot slot, Properties properties, ToolDefinition toolDefinition, boolean renderBase) {
    super(material, slot, properties, toolDefinition);
    this.renderBase = renderBase;
  }

  public MaterialArmorItem(ModifiableArmorMaterial material, ArmorSlotType slotType, Properties properties, boolean renderBase) {
    super(material, slotType, properties);
    this.renderBase = renderBase;
  }

  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
      @Nonnull
      @Override
      public Model getGenericArmorModel(LivingEntity entityLiving, ItemStack stack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
        return MaterialArmorModel.INSTANCE.setup(_default, MaterialArmorItem.this, stack, armorSlot, renderBase);
      }
    });
  }
}
