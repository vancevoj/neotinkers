package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier.ArmorTexture;
import slimeknights.tconstruct.library.tools.item.armor.texture.ArmorTextureSupplier.TextureType;
import slimeknights.tconstruct.library.tools.item.armor.texture.MaterialArmorTextureSupplier.MaterialSetCache;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Model for armor with multiple materials */
public class MaterialArmorModel extends AbstractArmorModel {
  public static final MaterialArmorModel INSTANCE = new MaterialArmorModel();

  /** Number of materials to render */
  private int expectedMaterials = 1;
  /** Texture cache for material armor */
  private MaterialSetCache cache;
  /** List of materials on the item */
  private ListTag materials = new ListTag();

  private MaterialArmorModel() {}

  /** Setup the model for the current properties */
  public Model setup(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> base, ToolDefinition definition, MaterialSetCache cache) {
    this.setup(living, stack, slot, base);
    this.expectedMaterials = ToolMaterialHook.stats(definition).size();
    cache.checkSize(expectedMaterials);
    this.cache = cache;
    this.materials = stack.getOrCreateTag().getList(ToolStack.TAG_MATERIALS, Tag.TAG_STRING);
    return this;
  }

  @Override
  public void renderToBuffer(PoseStack matrices, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
    if (this.base != null && buffer != null && cache != null) {
      for (int i = 0; i < expectedMaterials; i++) {
        String material = materials.getString(i);
        ArmorTexture texture = this.cache.getTexture(material, i, textureType);
        if (texture != ArmorTexture.EMPTY) {
          renderTexture(base, matrices, packedLightIn, packedOverlayIn, texture, red, green, blue, alpha);
        }
        if (hasWings) {
          texture = this.cache.getTexture(material, i, TextureType.WINGS);
          if (texture != ArmorTexture.EMPTY) {
            renderWings(matrices, packedLightIn, packedOverlayIn, texture, red, green, blue, alpha);
          }
        }
      }
    }
  }
}
