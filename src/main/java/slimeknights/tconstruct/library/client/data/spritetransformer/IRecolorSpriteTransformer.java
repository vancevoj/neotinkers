package slimeknights.tconstruct.library.client.data.spritetransformer;

import com.mojang.blaze3d.platform.NativeImage;
import slimeknights.tconstruct.library.client.data.util.ABGR32;

/** Sprite transformer that just swaps the color on each pixel */
public interface IRecolorSpriteTransformer extends ISpriteTransformer {
  /** Gets the new color for the given pixel coordinates */
  int getNewColor(int color, int x, int y, int frame);

  @Override
  default void transform(NativeImage image, boolean allowAnimated) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        ABGR32.setPixel(image, x, y, getNewColor(ABGR32.getPixel(image, x, y), x, y, 0));
      }
    }
  }

  @Override
  default int getFallbackColor() {
    return getNewColor(0xFFD8D8D8, 0, 0, 0);
  }
}
