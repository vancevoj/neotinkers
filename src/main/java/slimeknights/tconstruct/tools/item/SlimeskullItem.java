package slimeknights.tconstruct.tools.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;

import java.util.function.Consumer;

/** This item is mainly to return the proper model for a slimeskull */
public class SlimeskullItem extends ModifiableArmorItem {
  /** Model ID for our slimeskull. You may want your own for a custom slimeskull */
  public static final Identifier MODEL_LOCATION = TConstruct.getResource("slimeskull");

  private final Identifier name;

  public SlimeskullItem(ModifiableArmorMaterial material, Identifier name, Properties properties) {
    // the equipment asset is named after the armor model, the asset JSON may be missing
    super(material, ArmorType.HELMET, properties, name);
    this.name = name;
  }

  public SlimeskullItem(ModifiableArmorMaterial material, Properties properties) {
    this(material, material.getId(), properties);
  }

  // 1.21.2 removed IItemExtension#getArmorTexture. The armor model dispatcher tells the vanilla equipment layers to draw nothing,
  // so the dummy armor texture is no longer needed.

  /** Registered from the client extensions event, NeoForge 26.3 no longer calls this automatically */
  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new ArmorModelDispatcher() {
      @Override
      protected Identifier getName() {
        return name;
      }

      /** The skull under the helmet is submitted by the slimeskull render layer, the helmet is resized to fit around it */
      @Override
      public <S extends HumanoidRenderState> void submitArmor(PoseStack matrices, SubmitNodeCollector collector, int packedLight, S state, ItemStack stack, EquipmentSlot slot, HumanoidModel<S> armorModel) {
        matrices.pushPose();
        SlimeskullArmorModel.applyHelmetTransform(matrices, state.isBaby);
        super.submitArmor(matrices, collector, packedLight, state, stack, slot, armorModel);
        matrices.popPose();
      }
    });
  }
}
