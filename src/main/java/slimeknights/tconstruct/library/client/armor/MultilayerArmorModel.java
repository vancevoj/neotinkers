package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier.ArmorTexture;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier.TextureType;

/** Armor model that just applies the list of textures */
public class MultilayerArmorModel extends AbstractArmorModel {
  public static final MultilayerArmorModel INSTANCE = new MultilayerArmorModel();

  private ItemStack armorStack = ItemStack.EMPTY;
  private ArmorTextureSupplier[] textures = new ArmorTextureSupplier[0];

  protected MultilayerArmorModel() {}

  /** Prepares this model */
  public Model setup(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> base, ArmorTextureSupplier[] textures) {
    setup(living, stack, slot, base);
    this.armorStack = stack;
    this.textures = textures;
    return this;
  }

  @Override
  public void renderToBuffer(PoseStack matrices, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
    if (this.base != null && buffer != null) {
      for (ArmorTextureSupplier textureSupplier : textures) {
        ArmorTexture texture = textureSupplier.getArmorTexture(armorStack, textureType);
        if (texture != ArmorTexture.EMPTY) {
          renderTexture(base, matrices, packedLightIn, packedOverlayIn, texture, red, green, blue, alpha);
        }
        if (hasWings) {
          texture = textureSupplier.getArmorTexture(armorStack, TextureType.WINGS);
          if (texture != ArmorTexture.EMPTY) {
            renderWings(matrices, packedLightIn, packedOverlayIn, texture, red, green, blue, alpha);
          }
        }
      }
    }
  }
}
