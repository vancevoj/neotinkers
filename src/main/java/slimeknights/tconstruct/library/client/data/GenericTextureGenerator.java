package slimeknights.tconstruct.library.client.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Data generator to create png image files */
public abstract class GenericTextureGenerator extends GenericDataProvider {
  /** Client resource manager for reading existing textures, from {@code GatherDataEvent#getResourceManager(PackType.CLIENT_RESOURCES)}. Null if this generator never reads textures. */
  @Nullable
  protected final ResourceManager resourceManager;

  /** Constructor for a generator reading existing textures */
  public GenericTextureGenerator(PackOutput packOutput, @Nullable ResourceManager resourceManager, String folder) {
    super(packOutput, Target.RESOURCE_PACK, folder);
    this.resourceManager = resourceManager;
  }

  /** Constructor for a generator that does not read existing textures */
  public GenericTextureGenerator(PackOutput packOutput, String folder) {
    this(packOutput, null, folder);
  }

  /** Saves the given image to the given location */
  protected CompletableFuture<?> saveImage(CachedOutput cache, Identifier location, NativeImage image) {
    return CompletableFuture.runAsync(() -> {
      try {
        Path path = this.pathProvider.file(location, "png");
        // NativeImage#asByteArray was removed, Mantle has an equivalent STB based encoder
        byte[] bytes = slimeknights.mantle.data.client.GenericTextureGenerator.toPngBytes(image);
        cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
      } catch (IOException e) {
        TConstruct.LOG.error("Couldn't write image for {}", location, e);
        throw new CompletionException(e);
      }
    }, Util.backgroundExecutor());
  }

  /** Saves metadata for the given image */
  protected CompletableFuture<?> saveMetadata(CachedOutput cache, Identifier location, JsonObject metadata) {
    return DataProvider.saveStable(cache, metadata, this.pathProvider.file(location, "png.mcmeta"));
  }
}
