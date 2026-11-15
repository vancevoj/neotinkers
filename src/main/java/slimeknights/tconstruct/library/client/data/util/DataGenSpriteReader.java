package slimeknights.tconstruct.library.client.data.util;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

/**
 * Logic to read sprites from existing images and return native images which can later be modified.
 * Reads through the datagen client resource manager ({@code GatherDataEvent#getResourceManager(PackType.CLIENT_RESOURCES)}), as {@code ExistingFileHelper} was removed.
 */
@Log4j2
@RequiredArgsConstructor
public class DataGenSpriteReader extends AbstractSpriteReader {
  private final ResourceManager manager;
  private final String folder;

  /** Gets a location with the given extension */
  private Identifier getLocation(Identifier base, String extension) {
    return base.withPath(folder + '/' + base.getPath() + extension);
  }

  @Override
  public boolean exists(Identifier path) {
    return manager.getResource(getLocation(path, ".png")).isPresent();
  }

  @Override
  public boolean metadataExists(Identifier path) {
    return manager.getResource(getLocation(path, ".png.mcmeta")).isPresent();
  }

  @Override
  public NativeImage read(Identifier path) throws IOException {
    try {
      Resource resource = manager.getResource(getLocation(path, ".png")).orElseThrow(FileNotFoundException::new);
      NativeImage image;
      try (InputStream stream = resource.open()) {
        image = NativeImage.read(stream);
      }
      openedImages.add(image);
      return image;
    } catch (IOException|NoSuchElementException e) {
      log.error("Failed to read image at {}", path);
      throw e;
    }
  }

  @Override
  public JsonObject readMetadata(Identifier path) throws IOException {
    try (BufferedReader reader = manager.getResource(getLocation(path, ".png.mcmeta")).orElseThrow(FileNotFoundException::new).openAsReader()) {
      return GsonHelper.parse(reader);
    }
  }
}
