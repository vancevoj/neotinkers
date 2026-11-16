package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModel;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.ArmorTexture;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.TextureType;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;

/** Armor model that just applies the list of textures */
public class MultilayerArmorModel extends AbstractArmorModel {
  public static final MultilayerArmorModel INSTANCE = new MultilayerArmorModel();
  /** First submit order used for armor layers, matching vanilla equipment layers. Order 0 is the entity itself */
  public static final int FIRST_ORDER = 1;

  protected MultilayerArmorModel() {}

  /**
   * Submits all texture layers of the armor model.
   * @param matrices      Pose stack, positioned like the entity model
   * @param collector     Collector to submit to
   * @param packedLight   Light coordinates
   * @param state         Entity render state, poses the armor model when it is drawn
   * @param stack         Armor stack
   * @param slot          Slot the armor is worn in
   * @param armorModel    Vanilla armor model for the slot (from the renderer's armor model set), drawn once per texture layer
   * @param model         Armor model to render
   * @return  Next free submit order after the submitted layers, for subclasses rendering more on top
   */
  public <S extends HumanoidRenderState> int submit(PoseStack matrices, SubmitNodeCollector collector, int packedLight, S state, ItemStack stack, EquipmentSlot slot, HumanoidModel<S> armorModel, ArmorModel model) {
    return submit(matrices, collector, packedLight, state, stack, slot, armorModel, model, -1, FIRST_ORDER);
  }

  /**
   * Submits all texture layers of the armor model.
   * @param color       ARGB color multiplier for all layers, -1 for none
   * @param firstOrder  Submit order of the first layer. Each layer gets its own order as layers share the same geometry
   * @return  Next free submit order after the submitted layers
   * @see #submit(PoseStack, SubmitNodeCollector, int, HumanoidRenderState, ItemStack, EquipmentSlot, HumanoidModel, ArmorModel)
   */
  protected <S extends HumanoidRenderState> int submit(PoseStack matrices, SubmitNodeCollector collector, int packedLight, S state, ItemStack stack, EquipmentSlot slot, HumanoidModel<S> armorModel, ArmorModel model, int color, int firstOrder) {
    if (model.layers().isEmpty()) {
      return firstOrder;
    }
    RegistryAccess registryAccess = getRegistryAccess();
    TextureType textureType = TextureType.fromSlot(slot);
    boolean hasGlint = stack.hasFoil();
    boolean hasWings = slot == EquipmentSlot.CHEST && ModifierUtil.checkVolatileFlag(stack, ModifiableArmorItem.ELYTRA);
    int packedOverlay = OverlayTexture.NO_OVERLAY;
    boolean armorGlint = hasGlint;
    boolean wingGlint = hasGlint;
    int armorOrder = firstOrder;
    int wingOrder = firstOrder;
    for (ArmorTextureSupplier textureSupplier : model.layers()) {
      ArmorTexture texture = textureSupplier.getArmorTexture(stack, textureType, registryAccess);
      if (texture != ArmorTexture.EMPTY) {
        texture.submit(armorModel, state, matrices, collector.order(armorOrder++), packedLight, packedOverlay, color, armorGlint, state.outlineColor);
        armorGlint = false;
      }
      if (hasWings) {
        texture = textureSupplier.getArmorTexture(stack, TextureType.WINGS, registryAccess);
        if (texture != ArmorTexture.EMPTY) {
          submitWings(matrices, collector.order(wingOrder++), state, packedLight, packedOverlay, texture, color, wingGlint);
          wingGlint = false;
        }
      }
    }
    return Math.max(armorOrder, wingOrder);
  }
}
