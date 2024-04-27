package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.client.model.ArmorModelHelper;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.item.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;

import javax.annotation.Nullable;

/** Model for armor with multiple materials */
public class MaterialArmorModel extends Model {
  public static final MaterialArmorModel INSTANCE = new MaterialArmorModel();

  /** Texture name for this model */
  private ResourceLocation name = new ResourceLocation("missingno");
  /** If true, renders the base layer of the model */
  private boolean renderBase;
  /* Properties set before render */
  /** Base model instance for rendering */
  @Nullable
  private HumanoidModel<?> base;
  /** Number of materials to render */
  private int expectedMaterials = 1;
  /** List of materials on the item */
  private MaterialIdNBT materials = MaterialIdNBT.EMPTY;
  /** If true, uses the legs texture */
  private boolean isLegs = false;
  /** If true, applies the enchantment glint to extra layers */
  private boolean hasGlint = false;
  private MaterialArmorModel() {
    super(RenderType::entityCutoutNoCull);
  }

  /** Setup the model for the current properties */
  public Model setup(HumanoidModel<?> base, ModifiableArmorItem item, ItemStack stack, EquipmentSlot slot, boolean renderBase) {
    this.name = new ResourceLocation(item.getMaterial().getName());
    this.base = base;
    this.renderBase = renderBase;
    this.expectedMaterials = ToolMaterialHook.stats(item.getToolDefinition()).size();
    this.materials = MaterialIdNBT.from(stack);
    this.isLegs = slot == EquipmentSlot.LEGS;
    this.hasGlint = stack.hasFoil();
    return this;
  }

  @Override
  public void renderToBuffer(PoseStack matrices, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
    if (this.base != null) {
      // render the base layer if requested
      // TODO: consider allowing this to be dyeable
      if (renderBase) {
        base.renderToBuffer(matrices, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
      }
      // render all material layers
      if (ArmorModelHelper.buffer != null){
        for (int i = 0; i < expectedMaterials; i++) {
          ResourceLocation texture = getArmorTexture(materials.getMaterial(i), isLegs ? "leggings" : "armor", i+1);
          VertexConsumer overlayBuffer = ItemRenderer.getArmorFoilBuffer(ArmorModelHelper.buffer, ArmorModelHelper.getRenderType(texture), false, hasGlint);
          base.renderToBuffer(matrices, overlayBuffer, packedLightIn, packedOverlayIn, red, green, blue, alpha);
        }
      }
    }
  }

  /** Gets the armor texture for a material */
  private ResourceLocation getArmorTexture(MaterialVariantId material, String variant, int index) {
    String basePath = "textures/models/armor/" + name.getPath() + '/' + variant + '_' + index;
    if (material.equals(IMaterial.UNKNOWN_ID)) {
      return new ResourceLocation(name.getNamespace(), basePath + ".png");
    }
    ResourceLocation location = material.getLocation('_');
    return new ResourceLocation(name.getNamespace(), basePath + '_' + location.getNamespace() + '_' + location.getPath() + ".png");
  }
}
