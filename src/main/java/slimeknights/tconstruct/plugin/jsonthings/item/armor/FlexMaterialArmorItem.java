package slimeknights.tconstruct.plugin.jsonthings.item.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.MaterialArmorModel;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.armor.texture.MaterialArmorTextureSupplier.MaterialSetCache;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/** Armor item for an item with multiple materials */
public class FlexMaterialArmorItem extends FlexModifiableArmorItem {
  private final MaterialSetCache cache;
  public FlexMaterialArmorItem(ArmorMaterial material, EquipmentSlot slot, Properties properties, ToolDefinition toolDefinition) {
    super(material, slot, properties, toolDefinition);
    this.cache = MaterialSetCache.FACTORY.apply(new ResourceLocation(material.getName()));
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
