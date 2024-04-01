package slimeknights.tconstruct.library.client.data.util;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Base sprite reader implementation for both datagen and the command */
@RequiredArgsConstructor
public abstract class AbstractSpriteReader {
  protected final List<NativeImage> openedImages = new ArrayList<>();

  /** Checks if an image exists in the given location */
  public abstract boolean exists(ResourceLocation path);

  /** Checks if metadata exists at the given location */
  public abstract boolean metadataExists(ResourceLocation path);

  /**
   * Reads an image at the given path, relative to the folder
   * @param path  Path containing the file
   * @return  Loaded image
   * @throws IOException  If the image failed to load
   */
  public abstract NativeImage read(ResourceLocation path) throws IOException;

  /** Reads the file if it exists */
  @Nullable
  public NativeImage readIfExists(ResourceLocation path) {
    if (exists(path)) {
      try {
        return read(path);
      } catch (IOException e) {
        // no-op should never happen
      }
    }
    return null;
  }

  /** Reads metadata from the given path */
  public abstract JsonObject readMetadata(ResourceLocation path) throws IOException;

  /** Creates a new image with the given dimensions */
  public NativeImage create(int width, int height) {
    NativeImage image = new NativeImage(width, height, true);
    openedImages.add(image);
    return image;
  }

  /** Tracks the given image so when this reader is closed, that image is closed */
  public void track(NativeImage transformed) {
    openedImages.add(transformed);
  }

  /** Closes all opened images */
  public void closeAll() {
    for (NativeImage image : openedImages) {
      image.close();
    }
    openedImages.clear();
  }
}
