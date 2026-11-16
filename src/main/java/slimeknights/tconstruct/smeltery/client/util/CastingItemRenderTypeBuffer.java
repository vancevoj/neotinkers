package slimeknights.tconstruct.smeltery.client.util;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Submit node collector to render cooling items transparent and tint them based on cooling time.
 * <p>
 * 26.3: {@code MultiBufferSource} is gone, so instead of wrapping vertex buffers this wraps the {@link SubmitNodeCollector}
 * passed to {@link net.minecraft.client.renderer.item.ItemStackRenderState#submit}. Solid and cutout item quads switch to
 * translucent render types while the item fades in, like the old buffer did for entity render types.
 */
public class CastingItemRenderTypeBuffer extends TintedSubmitNodeCollector {
  /**
   * Creates a new instance of this class
   * @param inner        Base submit node collector
   * @param alpha        Opacity of the item from 0 to 255. 255 is the end of the animation.
   * @param temperature  Temperature of the item from 0 to 255. 0 is the end of the animation when the item is "cool"/untinted
   */
  public CastingItemRenderTypeBuffer(SubmitNodeCollector inner, int alpha, int temperature) {
    super(inner, getTint(alpha, temperature));
  }

  /** Computes the tint for the given alpha and temperature */
  private static int getTint(int alpha, int temperature) {
    // alpha is a direct fade from 0 to 255
    alpha = Mth.clamp(alpha, 0, 0xFF);
    // RGB based on temperature, fades from 0xB06020 tint to 0xFFFFFF
    temperature = Mth.clamp(temperature, 0, 0xFF);
    int red   = 0xFF - (temperature * (0xFF - 0xB0) / 0xFF);
    int green = 0xFF - (temperature * (0xFF - 0x60) / 0xFF);
    int blue  = 0xFF - (temperature * (0xFF - 0x20) / 0xFF);
    return (alpha << 24) | (red << 16) | (green << 8) | blue;
  }
}
