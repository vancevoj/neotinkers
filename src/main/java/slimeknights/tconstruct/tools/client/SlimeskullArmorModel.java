package slimeknights.tconstruct.tools.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
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
import slimeknights.tconstruct.smeltery.client.util.TintedSubmitNodeCollector;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.world.client.BlockModelSkullRenderer;
import slimeknights.tconstruct.world.client.PiglinSkullModel;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model to render a slimeskull helmet with both the helmet and skull.
 * <p>
 * 26.3: the armor model hook no longer receives the entity and models are submitted instead of drawn, so this submits the skull and the
 * helmet layers from {@link #submit(PoseStack, SubmitNodeCollector, int, HumanoidRenderState, ItemStack, HumanoidModel, ArmorModel)},
 * called by the slimeskull {@code ArmorModelDispatcher#submitArmor}.
 */
public class SlimeskullArmorModel extends MultilayerArmorModel {
  /** Singleton model instance, all data is passed in through {@link #submit(PoseStack, SubmitNodeCollector, int, HumanoidRenderState, ItemStack, HumanoidModel, ArmorModel)} */
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

  private SlimeskullArmorModel() {}

  /** Gets the color to tint the head for the given helmet */
  private static int getHeadColor(ItemStack stack) {
    // if dyed, color is the dye
    ModifierId dyed = TinkerModifiers.dyed.getId();
    if (ModifierUtil.getModifierLevel(stack, dyed) > 0) {
      return 0xFF000000 | ModifierUtil.getPersistentInt(stack, dyed, -1);
    }
    // if not dyed, color is the material, fallback to no tint if missing
    MaterialVariantId material = MaterialIdNBT.getMaterial(stack, 1);
    if (MaterialId.UNKNOWN.equals(material)) {
      return -1;
    }
    return MATERIAL_COLOR_CACHE.apply(material);
  }

  /**
   * Submits the slimeskull: the skull under the helmet, then the helmet layers.
   * @param matrices     Pose stack, positioned like the entity model
   * @param collector    Collector to submit to
   * @param packedLight  Light coordinates
   * @param state        Entity render state
   * @param stack        Slimeskull stack
   * @param armorModel   Vanilla armor model for the head slot
   * @param model        Armor model for the helmet layers
   * @return  Next free submit order after the submitted layers
   */
  public <S extends HumanoidRenderState> int submit(PoseStack matrices, SubmitNodeCollector collector, int packedLight, S state, ItemStack stack, HumanoidModel<S> armorModel, ArmorModel model) {
    int order = FIRST_ORDER;
    MaterialId materialId = MaterialIdNBT.from(stack).getMaterial(0).getId();
    if (!materialId.equals(MaterialId.UNKNOWN)) {
      SkullModelBase skull = getHeadModel(materialId);
      Identifier texture = HEAD_TEXTURES.get(materialId);
      BlockModelSkullRenderer blockHead = BLOCK_HEADS.get(materialId);
      if ((skull != null && texture != null) || blockHead != null) {
        int headColor = getHeadColor(stack);
        // pose the armor model for this entity, the same pose the old base model had, so the skull follows the head
        armorModel.setupAnim(state);
        matrices.pushPose();
        if (state.isCrouching) {
          matrices.translate(0, armorModel.head.y / 16.0F, 0);
        }
        if (state.isBaby) {
          matrices.scale(0.85F, 0.85F, 0.85F);
          matrices.translate(0.0D, 0.9D, 0.0D);
        } else {
          matrices.scale(1.115f, 1.115f, 1.115f);
        }
        // handle head rotation directly as it fixes issues with dragon heads
        // could fix it directly, but saves us some math to handle here
        matrices.rotate((new Quaternionf()).rotationZYX(0, armorModel.head.yRot, armorModel.head.xRot));
        OrderedSubmitNodeCollector headCollector = collector.order(order++);
        if (skull != null && texture != null) {
          SkullModelBase.State skullState = new SkullModelBase.State();
          skullState.animationPos = state.wornHeadAnimationPos;
          headCollector.submitModel(skull, skullState, matrices, RenderTypes.entityCutoutZOffset(texture), packedLight, OverlayTexture.NO_OVERLAY, headColor, null, state.outlineColor);
          // the glint is a separate pass in 26.3, same as vanilla armor trims
          if (stack.hasFoil()) {
            collector.order(order++).submitModel(skull, skullState, matrices, RenderTypes.trimmedArmorGlint(), packedLight, OverlayTexture.NO_OVERLAY, -1, null, 0);
          }
        } else {
          // block item heads cannot be models, they submit the item where the skull model would be
          SubmitNodeCollector itemCollector = headColor != -1 ? new TintedSubmitNodeCollector(collector, headColor) : collector;
          blockHead.submit(matrices, itemCollector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor, 0, 0);
        }
        matrices.popPose();
      }
    }
    if (!model.layers().isEmpty()) {
      matrices.pushPose();
      // apply rotation before translation
      // offset and resize helmet to be around head
      matrices.translate(0.0D, state.isBaby ? -0.09D : -0.025D, 0.0D);
      matrices.scale(1.1f, 1.1f, 1.1f);
      order = submit(matrices, collector, packedLight, state, stack, EquipmentSlot.HEAD, armorModel, model, -1, order);
      matrices.popPose();
    }
    return order;
  }


  /* Head models */

  /** Map of all skull factories */
  private static final Map<MaterialId,Function<EntityModelSet,? extends SkullModelBase>> HEAD_MODEL_FACTORIES = new HashMap<>();
  /** Map of texture for the skull textures */
  private static final Map<MaterialId,Identifier> HEAD_TEXTURES = new HashMap<>();
  /** Map of block item heads, rendered through their item model */
  private static final Map<MaterialId,BlockModelSkullRenderer> BLOCK_HEADS = new HashMap<>();

  /** Registers a head model and texture, using the default skull model */
  public static void registerHeadModel(MaterialId materialId, ModelLayerLocation headModel, Identifier texture) {
    registerHeadModel(materialId, modelSet -> new SkullModel(modelSet.bakeLayer(headModel)), texture);
  }

  /** Registers a head model and texture, using the piglin skull model */
  public static void registerPiglinHeadModel(MaterialId materialId, ModelLayerLocation headModel, Identifier texture) {
    registerHeadModel(materialId, modelSet -> new PiglinSkullModel(modelSet.bakeLayer(headModel)), texture);
  }

  /** Checks that the material has no head yet */
  private static void checkDuplicate(MaterialId materialId) {
    if (HEAD_MODEL_FACTORIES.containsKey(materialId) || BLOCK_HEADS.containsKey(materialId)) {
      throw new IllegalArgumentException("Duplicate head model " + materialId);
    }
  }

  /**
   * Registers a skull model using an item as the model.
   * The stack is fetched lazily, as item stacks cannot be created before item components are bound.
   */
  public static void registerBlockModel(MaterialId materialId, Supplier<ItemStack> stack) {
    checkDuplicate(materialId);
    BLOCK_HEADS.put(materialId, new BlockModelSkullRenderer(stack));
  }

  /** Registers a skull model using an item as the model */
  public static void registerBlockModel(MaterialId materialId, ItemStack stack) {
    checkDuplicate(materialId);
    BLOCK_HEADS.put(materialId, new BlockModelSkullRenderer(stack));
  }

  /** Registers a head model and texture, using a custom skull model */
  public static void registerHeadModel(MaterialId materialId, Function<EntityModelSet,? extends SkullModelBase> headFunction, Identifier texture) {
    checkDuplicate(materialId);
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
