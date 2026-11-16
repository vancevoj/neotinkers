package slimeknights.tconstruct.smeltery.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Submit node collector passing everything on to another collector. Subclasses override the submissions they want to change.
 * <p>
 * 26.3 replacement for wrapping a {@code MultiBufferSource}: renderers that support it, such as {@link ItemStackRenderState#submit},
 * can be given a subclass of this to intercept their submissions.
 */
public class DelegatingSubmitNodeCollector implements SubmitNodeCollector {
  /** Root collector, used to create ordered collectors */
  protected final SubmitNodeCollector root;
  /** Collector receiving the submissions */
  protected final OrderedSubmitNodeCollector inner;

  public DelegatingSubmitNodeCollector(SubmitNodeCollector inner) {
    this(inner, inner);
  }

  protected DelegatingSubmitNodeCollector(SubmitNodeCollector root, OrderedSubmitNodeCollector inner) {
    this.root = root;
    this.inner = inner;
  }

  /**
   * Creates a copy of this collector that submits to the given ordered collector.
   * Subclasses must override this to keep their behavior for ordered submissions.
   * @param ordered  Ordered collector from the root
   * @return  Collector submitting to the ordered collector
   */
  protected OrderedSubmitNodeCollector withInner(OrderedSubmitNodeCollector ordered) {
    return new DelegatingSubmitNodeCollector(root, ordered);
  }

  @Override
  public OrderedSubmitNodeCollector order(int order) {
    return withInner(root.order(order));
  }

  @Override
  public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    inner.submitShadow(poseStack, radius, pieces);
  }

  @Override
  public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {
    inner.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
  }

  @Override
  public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
    inner.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
  }

  @Override
  public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {
    inner.submitTextBackground(poseStack, x0, y0, x1, y1, color, displayMode, lightCoords);
  }

  @Override
  public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
    inner.submitFlame(poseStack, renderState, rotation);
  }

  @Override
  public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
    inner.submitLeash(poseStack, leashState);
  }

  @Override
  public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor) {
    inner.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor);
  }

  @Override
  public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
    inner.submitCrumblingOverlay(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, crumblingOverlay);
  }

  @Override
  public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
    inner.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
  }

  @Override
  public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
    inner.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
  }

  @Override
  public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
    inner.submitMultiLayerBlockModel(poseStack, modelParts, translucent, tintLayers, lightCoords, overlayCoords, outlineColor);
  }

  @Override
  public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
    inner.submitBreakingBlockModel(poseStack, parts, progress, isBlockTranslucent);
  }

  @Override
  public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
    inner.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
  }

  @Override
  public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
    inner.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
  }

  @Override
  public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
    inner.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
  }

  @Override
  public void submitQuadParticleGroup(QuadParticleRenderState particles) {
    inner.submitQuadParticleGroup(particles);
  }

  @Override
  public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
    inner.submitGizmoPrimitives(group, camera, onTop);
  }

  @Override
  public <T extends SubmitNode, S extends T> void submitSpecial(RenderPhaseKey<T> phaseKey, S submitNode) {
    inner.submitSpecial(phaseKey, submitNode);
  }
}
