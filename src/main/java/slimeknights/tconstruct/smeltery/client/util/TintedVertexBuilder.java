package slimeknights.tconstruct.smeltery.client.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import lombok.RequiredArgsConstructor;

/**
 * Vertex builder wrapper that tints all quads passed in.
 * Every method returns this wrapper, so chained calls such as {@code addVertex(...).setColor(...)} keep the tint.
 */
@RequiredArgsConstructor
public class TintedVertexBuilder implements VertexConsumer {
  /** Base vertex builder */
  private final VertexConsumer inner;
  /** Tint color from 0-255 */
  private final int tintRed, tintGreen, tintBlue, tintAlpha;

  @Override
  public VertexConsumer addVertex(float x, float y, float z) {
    inner.addVertex(x, y, z);
    return this;
  }

  @Override
  public VertexConsumer setColor(int red, int green, int blue, int alpha) {
    inner.setColor((red * tintRed) / 0xFF, (green * tintGreen) / 0xFF, (blue * tintBlue) / 0xFF, (alpha * tintAlpha) / 0xFF);
    return this;
  }

  @Override
  public VertexConsumer setColor(int color) {
    return setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24);
  }

  @Override
  public VertexConsumer setUv(float u, float v) {
    inner.setUv(u, v);
    return this;
  }

  @Override
  public VertexConsumer setUv1(int u, int v) {
    inner.setUv1(u, v);
    return this;
  }

  @Override
  public VertexConsumer setUv2(int u, int v) {
    inner.setUv2(u, v);
    return this;
  }

  @Override
  public VertexConsumer setUv3(float u, float v) {
    inner.setUv3(u, v);
    return this;
  }

  @Override
  public VertexConsumer setNormal(float x, float y, float z) {
    inner.setNormal(x, y, z);
    return this;
  }

  @Override
  public VertexConsumer setLineWidth(float width) {
    inner.setLineWidth(width);
    return this;
  }

  @Override
  public VertexConsumer misc(VertexFormatElement element, int... rawData) {
    inner.misc(element, rawData);
    return this;
  }
}
