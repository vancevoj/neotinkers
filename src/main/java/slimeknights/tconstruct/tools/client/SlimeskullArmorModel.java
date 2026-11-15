package slimeknights.tconstruct.tools.client;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModel;
import slimeknights.tconstruct.library.client.armor.MultilayerArmorModel;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.utils.SimpleCache;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.world.client.BlockModelSkullRenderer;
import slimeknights.tconstruct.world.client.PiglinSkullModel;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/** Model to render a slimeskull helmet with both the helmet and skull */
public class SlimeskullArmorModel extends MultilayerArmorModel {
  /** Singleton model instance, all data is passed in via setters */
  public static final SlimeskullArmorModel INSTANCE = new SlimeskullArmorModel();
  /** Cache of colors for materials */
  static final SimpleCache<MaterialVariantId,Integer> MATERIAL_COLOR_CACHE = new SimpleCache<>(mat ->
    MaterialRenderInfoLoader.INSTANCE.getRenderInfo(mat)
            .map(MaterialRenderInfo::vertexColor)
            .orElse(-1));
  /** Listener to clear caches */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> {
    HEAD_MODELS = null;
    MATERIAL_COLOR_CACHE.clear();
  };

  /** Head to render under the helmet */
  @Nullable
  private Identifier headTexture;
  /** Tint color for the head */
  private int headColor = -1;
  /** Texture for the head */
  @Nullable
  private SkullModelBase headModel;
  /** Current animation time for the skull */
  private float walkAnimation = 0;

  private SlimeskullArmorModel() {}

  /** Prepares the model */
  public Model setup(LivingEntity living, ItemStack stack, HumanoidModel<?> base, ArmorModel model) {
    super.setup(living, stack, EquipmentSlot.HEAD, base, model);
    MaterialId materialId = MaterialIdNBT.from(stack).getMaterial(0).getId();
    if (!materialId.equals(MaterialId.UNKNOWN)) {
      SkullModelBase skull = getHeadModel(materialId);
      Identifier texture = HEAD_TEXTURES.get(materialId);
      if (skull != null && texture != null) {
        headModel = skull;
        headTexture = texture;
        // if dyed, color is the dye
        ModifierId dyed = TinkerModifiers.dyed.getId();
        if (ModifierUtil.getModifierLevel(stack, dyed) > 0) {
          headColor = 0xFF000000 | ModifierUtil.getPersistentInt(stack, dyed, -1);
        } else {
          // if not dyed, color is the material, fallback to no tint if missing
          MaterialVariantId material = MaterialIdNBT.getMaterial(stack, 1);
          if (MaterialId.UNKNOWN.equals(material)) {
            headColor = -1;
          } else {
            headColor = MATERIAL_COLOR_CACHE.apply(material);
          }
        }

        // setup walk animation
        WalkAnimationState walkState = living.getVehicle() instanceof LivingEntity vehicle ? vehicle.walkAnimation : living.walkAnimation;
        this.walkAnimation = walkState.position();
        return this;
      }
    }
    headTexture = null;
    headModel = null;
    headColor = -1;
    walkAnimation = 0;
    return this;
  }

  @Override
  public void renderToBuffer(PoseStack matrixStackIn, VertexConsumer vertexBuilder, int packedLightIn, int packedOverlayIn, int color) {
    if (base != null && buffer != null) {
      // 1.21: vanilla passes a packed ARGB color rather than float channels; unpack for our float-based head tinting
      float alpha = (color >> 24 & 0xFF) / 255.0F;
      float red = (color >> 16 & 0xFF) / 255.0F;
      float green = (color >> 8 & 0xFF) / 255.0F;
      float blue = (color & 0xFF) / 255.0F;
      if (headModel != null && headTexture != null) {
        VertexConsumer heaadBuffer = ItemRenderer.getArmorFoilBuffer(buffer, RenderTypes.entityCutoutZOffset(headTexture), hasGlint);
        matrixStackIn.pushPose();
        if (base.crouching) {
          matrixStackIn.translate(0, base.head.y / 16.0F, 0);
        }
        if (base.young) {
          matrixStackIn.scale(0.85F, 0.85F, 0.85F);
          matrixStackIn.translate(0.0D, 0.9D, 0.0D);
        } else {
          matrixStackIn.scale(1.115f, 1.115f, 1.115f);
        }
        // handle head rotation directly as it fixes issues with dragon heads
        // could fix it directly, but saves us some math to handle here
        matrixStackIn.rotate((new Quaternionf()).rotationZYX(0, base.head.yRot, base.head.xRot));
        headModel.setupAnim(walkAnimation, 0, 0);
        renderColored(headModel, matrixStackIn, heaadBuffer, packedLightIn, packedOverlayIn, headColor, red, green, blue, alpha);
        matrixStackIn.popPose();
      }
      if (model != ArmorModel.EMPTY) {
        matrixStackIn.pushPose();
        // apply rotation before translation
        // offset and resize helmet to be around head
        matrixStackIn.translate(0.0D, base.young ? -0.09D : -0.025D, 0.0D);
        matrixStackIn.scale(1.1f, 1.1f, 1.1f);
        super.renderToBuffer(matrixStackIn, vertexBuilder, packedLightIn, packedOverlayIn, color);
        matrixStackIn.popPose();
      }
    }
  }


  /* Head models */

  /** Map of all skull factories */
  private static final Map<MaterialId,Function<EntityModelSet,? extends SkullModelBase>> HEAD_MODEL_FACTORIES = new HashMap<>();
  /** Map of texture for the skull textures */
  private static final Map<MaterialId,Identifier> HEAD_TEXTURES = new HashMap<>();

  /** Registers a head model and texture, using the default skull model */
  public static void registerHeadModel(MaterialId materialId, ModelLayerLocation headModel, Identifier texture) {
    registerHeadModel(materialId, modelSet -> new SkullModel(modelSet.bakeLayer(headModel)), texture);
  }

  /** Registers a head model and texture, using the piglin skull model */
  public static void registerPiglinHeadModel(MaterialId materialId, ModelLayerLocation headModel, Identifier texture) {
    registerHeadModel(materialId, modelSet -> new PiglinSkullModel(modelSet.bakeLayer(headModel)), texture);
  }

  /** Registers a skull model using an item as the model */
  public static void registerBlockModel(MaterialId materialId, ItemStack stack) {
    registerHeadModel(materialId, modelSet -> new BlockModelSkullRenderer(Minecraft.getInstance().getItemRenderer(), stack), InventoryMenu.BLOCK_ATLAS);
  }

  /** Registers a head model and texture, using a custom skull model */
  public static void registerHeadModel(MaterialId materialId, Function<EntityModelSet,? extends SkullModelBase> headFunction, Identifier texture) {
    if (HEAD_MODEL_FACTORIES.containsKey(materialId)) {
      throw new IllegalArgumentException("Duplicate head model " + materialId);
    }
    HEAD_MODEL_FACTORIES.put(materialId, headFunction);
    HEAD_TEXTURES.put(materialId, texture);
  }

  /** Map of baked head models, if null it is not currently computed */
  private static Map<MaterialId, SkullModelBase> HEAD_MODELS;

  /** Gets the head model for the given material */
  @Nullable
  private static SkullModelBase getHeadModel(MaterialId materialId) {
    if (HEAD_MODELS == null) {
      // vanilla rebakes these a lot, so figure we should at least do it every resource reload
      EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
      ImmutableMap.Builder<MaterialId,SkullModelBase> models = ImmutableMap.builder();
      for (Entry<MaterialId,Function<EntityModelSet,? extends SkullModelBase>> entry : HEAD_MODEL_FACTORIES.entrySet()) {
        models.put(entry.getKey(), entry.getValue().apply(modelSet));
      }
      HEAD_MODELS = models.build();
    }
    return HEAD_MODELS.get(materialId);
  }
}
