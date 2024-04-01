package slimeknights.tconstruct.library.client.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.extern.log4j.Log4j2;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Data generator to create png image files */
@Log4j2
public abstract class GenericTextureGenerator implements DataProvider {
  private final DataGenerator generator;
  @Nullable
  private final ExistingFileHelper existingFileHelper;
  private final String folder;
  @Nullable
  private final ExistingFileHelper.ResourceType resourceType;

  /** Constructor which marks files as existing */
  public GenericTextureGenerator(DataGenerator generator, @Nullable ExistingFileHelper existingFileHelper, String folder) {
    this.generator = generator;
    this.folder = folder;
    this.existingFileHelper = existingFileHelper;
    if (existingFileHelper != null) {
      this.resourceType = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", folder);
    } else {
      this.resourceType = null;
    }
  }

  /** Constructor which does not mark files as existing */
  public GenericTextureGenerator(DataGenerator generator, String folder) {
    this(generator, null, folder);
  }

  /** Saves the given image to the given location */
  @SuppressWarnings("UnstableApiUsage")
  protected void saveImage(CachedOutput cache, ResourceLocation location, NativeImage image) {
    try {
      Path path = this.generator.getOutputFolder().resolve(Paths.get(PackType.CLIENT_RESOURCES.getDirectory(), location.getNamespace(), folder, location.getPath() + ".png"));
      if (existingFileHelper != null && resourceType != null) {
        existingFileHelper.trackGenerated(location, resourceType);
      }
      byte[] bytes = image.asByteArray();
      cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
    } catch (IOException e) {
      log.error("Couldn't write image for {}", location, e);
    }
  }

  /** Saves metadata for the given image */
  protected void saveMetadata(CachedOutput cache, ResourceLocation location, JsonObject metadata) {
    try {
      Path path = this.generator.getOutputFolder().resolve(Paths.get(PackType.CLIENT_RESOURCES.getDirectory(), location.getNamespace(), folder, location.getPath() + ".png.mcmeta"));
      DataProvider.saveStable(cache, metadata, path);
    } catch (IOException e) {
      log.error("Couldn't write image metadata for {}", location, e);
    }
  }
}
