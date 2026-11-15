package slimeknights.tconstruct.smeltery.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Submit node collector that tints and fades everything submitted through it.
 * <p>
 * 26.3 replacement for wrapping a {@code MultiBufferSource} with {@link TintedVertexBuilder}. Item and block model quads are
 * resubmitted as custom geometry with the tint multiplied into every vertex, switching to translucent render types when the
 * alpha is below 255 so the fade shows. Models only get the tint multiplied into their tint color.
 */
public class TintedSubmitNodeCollector extends DelegatingSubmitNodeCollector {
  /** Directions to fetch quads from block model parts, null is the unculled faces */
  private static final Direction[] QUAD_DIRECTIONS = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

  /** Tint in ARGB format */
  protected final int tint;

  /**
   * Creates a new instance
   * @param inner  Collector receiving the submissions
   * @param red    Red tint from 0 to 255
   * @param green  Green tint from 0 to 255
   * @param blue   Blue tint from 0 to 255
   * @param alpha  Opacity from 0 to 255
   */
  public TintedSubmitNodeCollector(SubmitNodeCollector inner, int red, int green, int blue, int alpha) {
    this(inner, ARGB.color(alpha, red, green, blue));
  }

  /**
   * Creates a new instance
   * @param inner  Collector receiving the submissions
   * @param tint   Tint in ARGB format
   */
  public TintedSubmitNodeCollector(SubmitNodeCollector inner, int tint) {
    super(inner);
    this.tint = tint;
  }

  protected TintedSubmitNodeCollector(SubmitNodeCollector root, OrderedSubmitNodeCollector inner, int tint) {
    super(root, inner);
    this.tint = tint;
  }

  @Override
  protected OrderedSubmitNodeCollector withInner(OrderedSubmitNodeCollector ordered) {
    return new TintedSubmitNodeCollector(root, ordered, tint);
  }

  /** If true, the tint is partially transparent */
  protected boolean isTransparent() {
    return ARGB.alpha(tint) < 0xFF;
  }

  /** Gets the tint color for the given quad from the tint layers */
  private static int getLayerColor(int[] tintLayers, BakedQuad quad) {
    BakedQuad.MaterialInfo material = quad.materialInfo();
    if (material.isTinted()) {
      int index = material.tintIndex();
      if (index >= 0 && index < tintLayers.length) {
        return tintLayers[index];
      }
    }
    return -1;
  }

  /** Adds the quad to the list for the given render type */
  private static void addQuad(Map<RenderType, List<BakedQuad>> quads, RenderType renderType, BakedQuad quad) {
    quads.computeIfAbsent(renderType, type -> new ArrayList<>()).add(quad);
  }

  /** Submits all quads in the map as custom geometry */
  private void submitQuads(PoseStack poseStack, Map<RenderType, List<BakedQuad>> quadMap, int[] tintLayers, int lightCoords, int overlayCoords) {
    int[] tints = tintLayers.clone();
    int tint = this.tint;
    for (Entry<RenderType, List<BakedQuad>> entry : quadMap.entrySet()) {
      List<BakedQuad> quads = List.copyOf(entry.getValue());
      inner.submitCustomGeometry(poseStack, entry.getKey(), (pose, buffer) -> {
        QuadInstance instance = new QuadInstance();
        instance.setLightCoords(lightCoords);
        instance.setOverlayCoords(overlayCoords);
        for (BakedQuad quad : quads) {
          instance.setColor(ARGB.multiply(getLayerColor(tints, quad), tint));
          buffer.putBakedQuad(pose, quad, instance);
        }
      });
    }
  }

  @Override
  public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
    if (quads.isEmpty()) {
      return;
    }
    boolean transparent = isTransparent();
    boolean foil = foilType != ItemStackRenderState.FoilType.NONE;
    // group quads by render type, as each custom geometry submission has a single render type
    Map<RenderType, List<BakedQuad>> quadMap = new LinkedHashMap<>();
    for (BakedQuad quad : quads.all()) {
      BakedQuad.MaterialInfo material = quad.materialInfo();
      RenderType renderType;
      if (transparent) {
        // solid and cutout types ignore alpha, so swap to the translucent type for the same atlas
        renderType = foil ? RenderTypes.itemTranslucentGlint(material.sprite().atlasLocation()) : RenderTypes.itemTranslucent(material.sprite().atlasLocation());
      } else {
        renderType = foil ? material.itemGlintRenderType() : material.itemRenderType();
      }
      addQuad(quadMap, renderType, quad);
    }
    submitQuads(poseStack, quadMap, tintLayers, lightCoords, overlayCoords);
  }

  @Override
  public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
    boolean transparent = isTransparent();
    Map<RenderType, List<BakedQuad>> quadMap = new LinkedHashMap<>();
    for (BlockStateModelPart part : parts) {
      for (Direction direction : QUAD_DIRECTIONS) {
        for (BakedQuad quad : part.getQuads(direction)) {
          addQuad(quadMap, transparent ? RenderTypes.itemTranslucent(quad.materialInfo().sprite().atlasLocation()) : renderType, quad);
        }
      }
    }
    submitQuads(poseStack, quadMap, tintLayers, lightCoords, overlayCoords);
  }

  @Override
  public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
    boolean transparent = isTransparent();
    Map<RenderType, List<BakedQuad>> quadMap = new LinkedHashMap<>();
    for (BlockStateModelPart part : modelParts) {
      for (Direction direction : QUAD_DIRECTIONS) {
        for (BakedQuad quad : part.getQuads(direction)) {
          BakedQuad.MaterialInfo material = quad.materialInfo();
          addQuad(quadMap, transparent ? RenderTypes.itemTranslucent(material.sprite().atlasLocation()) : material.itemRenderType(), quad);
        }
      }
    }
    submitQuads(poseStack, quadMap, tintLayers, lightCoords, overlayCoords);
  }

  @Override
  public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor) {
    // models cannot swap render types, so they only get the tint; the alpha only shows on translucent model render types
    super.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, ARGB.multiply(tintedColor, tint), uvMapping, outlineColor);
  }
}
