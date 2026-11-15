package slimeknights.tconstruct.gadgets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer for fancy item frames.
 * <p>
 * 26.3: extraction fills a {@link State} with the frame type and the standalone frame model, submission replaces the vanilla
 * item frame submission. Name tags are still handled by the base entity renderer logic.
 */
public class FancyItemFrameRenderer<T extends FancyItemFrameEntity> extends ItemFrameRenderer<T> {
  /** Standalone frame models, one per frame type */
  public static final Map<FrameType, StandaloneModelKey<BlockStateModelPart>> LOCATIONS_MODEL = new EnumMap<>(FrameType.class);
  /** Standalone frame models for maps, one per frame type */
  public static final Map<FrameType, StandaloneModelKey<BlockStateModelPart>> LOCATIONS_MODEL_MAP = new EnumMap<>(FrameType.class);
  /** Model IDs matching {@link #LOCATIONS_MODEL} and {@link #LOCATIONS_MODEL_MAP} */
  private static final Map<StandaloneModelKey<BlockStateModelPart>, Identifier> MODEL_IDS = new HashMap<>();
  static {
    for (FrameType type : FrameType.values()) {
      String name = type == FrameType.REVERSED_GOLD ? FrameType.GOLD.getSerializedName() : type.getSerializedName();
      LOCATIONS_MODEL.put(type, createKey(TConstruct.getResource("block/frame/" + name)));
      LOCATIONS_MODEL_MAP.put(type, createKey(TConstruct.getResource("block/frame/" + name + "_map")));
    }
  }

  /** Creates a standalone model key for the given model */
  private static StandaloneModelKey<BlockStateModelPart> createKey(Identifier model) {
    StandaloneModelKey<BlockStateModelPart> key = new StandaloneModelKey<>(model::toString);
    MODEL_IDS.put(key, model);
    return key;
  }

  /** Registers all frame models, call from {@link ModelEvent.RegisterStandalone} */
  public static void registerModels(ModelEvent.RegisterStandalone event) {
    for (Map.Entry<StandaloneModelKey<BlockStateModelPart>, Identifier> entry : MODEL_IDS.entrySet()) {
      event.register(entry.getKey(), SimpleUnbakedStandaloneModel.simpleModelWrapper(entry.getValue()));
    }
  }

  private final MapRenderer mapRenderer;
  public FancyItemFrameRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.mapRenderer = context.getMapRenderer();
  }

  @Override
  protected int getBlockLightLevel(T frame, BlockPos pPos) {
    int baseLight = super.getBlockLightLevel(frame, pPos);
    return frame.getFrameType() == FrameType.MANYULLYN ? Math.max(7, baseLight) : baseLight;
  }

  @Override
  public ItemFrameRenderState createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T frame, ItemFrameRenderState state, float partialTicks) {
    // vanilla fills the direction, item, rotation and map
    super.extractRenderState(frame, state, partialTicks);
    FrameType frameType = frame.getFrameType();
    if (state instanceof State fancy) {
      fancy.frameType = frameType;
    }

    // replace the vanilla frame model with the model for our frame type
    ItemStack stack = frame.getItem();
    boolean isMap = !stack.isEmpty() && stack.getItem() instanceof MapItem;
    // clear does not render the frame if filled
    boolean frameVisible = !frame.isInvisible() && (frameType != FrameType.CLEAR || stack.isEmpty());
    state.frameModel.clear();
    if (frameVisible) {
      BlockStateModelPart model = Minecraft.getInstance().getModelManager().getStandaloneModel((isMap ? LOCATIONS_MODEL_MAP : LOCATIONS_MODEL).get(frameType));
      if (model != null) {
        state.frameModel.setupModel(new Matrix4f(), false).add(model);
      }
    }
  }

  @Override
  public void submit(ItemFrameRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    FrameType frameType = state instanceof State fancy ? fancy.frameType : FrameType.NETHERITE;

    // base entity rendering logic, since calling super gives us the item frame renderer that we are replacing
    this.submitNameDisplay(state, matrices, collector, camera);

    // orient the renderer
    matrices.pushPose();
    Direction facing = state.direction;
    Vec3 offset = this.getRenderOffset(state);
    matrices.translate(facing.getStepX() * 0.46875D - offset.x(), facing.getStepY() * 0.46875D - offset.y(), facing.getStepZ() * 0.46875D - offset.z());
    float xRot;
    float yRot;
    if (facing.getAxis().isHorizontal()) {
      xRot = 0.0F;
      yRot = 180.0F - facing.toYRot();
    } else {
      xRot = -90 * facing.getAxisDirection().getStep();
      yRot = 180.0F;
    }
    matrices.rotate(Axis.XP.rotationDegrees(xRot));
    matrices.rotate(Axis.YP.rotationDegrees(yRot));

    // render the frame
    if (!state.frameModel.isEmpty()) {
      matrices.pushPose();
      matrices.translate(-0.5D, -0.5D, -0.5D);
      state.frameModel.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      matrices.popPose();
    }

    // render the item
    boolean isMap = state.mapId != null;
    if (isMap || !state.item.isEmpty()) {
      // if no frame, offset the item farther back
      matrices.translate(0.0D, 0.0D, 0.4375D);

      // determine rotation for the item inside
      int frameRotation = state.rotation;
      // for diamond, render the timer as a partial rotation
      if (frameType.hasMoreRotations()) {
        int rotation = isMap ? (frameRotation + 2) % 4 * 4 : frameRotation;
        matrices.rotate(Axis.ZP.rotationDegrees(rotation * 360f / 16f));
      } else {
        int rotation = isMap ? (frameRotation + 2) % 4 * 2 : frameRotation;
        matrices.rotate(Axis.ZP.rotationDegrees(rotation * 360f / 8f));
      }
      if (!NeoForge.EVENT_BUS.post(new RenderItemInFrameEvent(state, this, matrices, collector)).isCanceled()) {
        int light = frameType == FrameType.MANYULLYN ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
        if (isMap) {
          matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);
          matrices.translate(-64.0D, -64.0D, -1.0D);
          this.mapRenderer.render(state.mapRenderState, matrices, collector, true, light);
        } else {
          float scale = frameType == FrameType.CLEAR ? 0.75f : 0.5f;
          matrices.scale(scale, scale, scale);
          state.item.submit(matrices, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
      }
    }

    matrices.popPose();
  }

  /** Render state for fancy item frames */
  public static class State extends ItemFrameRenderState {
    /** Type of the frame */
    public FrameType frameType = FrameType.NETHERITE;
  }
}
