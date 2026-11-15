package slimeknights.tconstruct.library.client.data.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;

/**
 * Helpers for colors in AABBGGRR format, the format used by all Tinkers sprite transformers and color mappings.
 * Replaces the removed vanilla {@code FastColor.ABGR32}. Since 1.21.2, {@link NativeImage#getPixel(int, int)} and
 * {@link NativeImage#setPixel(int, int, int)} use AARRGGBB, so all image access in the sprite generators goes through
 * {@link #getPixel(NativeImage, int, int)} and {@link #setPixel(NativeImage, int, int, int)} to avoid swapping red and blue.
 */
public final class ABGR32 {
  private ABGR32() {}

  /** Gets the alpha channel of the color */
  public static int alpha(int color) {
    return color >>> 24;
  }

  /** Gets the red channel of the color */
  public static int red(int color) {
    return color & 0xFF;
  }

  /** Gets the green channel of the color */
  public static int green(int color) {
    return (color >> 8) & 0xFF;
  }

  /** Gets the blue channel of the color */
  public static int blue(int color) {
    return (color >> 16) & 0xFF;
  }

  /** Packs the given channels into an AABBGGRR color */
  public static int color(int alpha, int blue, int green, int red) {
    return alpha << 24 | blue << 16 | green << 8 | red;
  }

  /** Reads the pixel at the given location in AABBGGRR format */
  public static int getPixel(NativeImage image, int x, int y) {
    return ARGB.toABGR(image.getPixel(x, y));
  }

  /** Sets the pixel at the given location from an AABBGGRR color */
  public static void setPixel(NativeImage image, int x, int y, int color) {
    image.setPixelABGR(x, y, color);
  }
}
