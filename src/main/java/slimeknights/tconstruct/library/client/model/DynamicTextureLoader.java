package slimeknights.tconstruct.library.client.model;

import lombok.extern.log4j.Log4j2;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Logic to handle dynamic texture scans. Really just logging missing textures at this point.
 * TODO: worth merging into tool model or modifier model manager?
 */
@Log4j2
public class DynamicTextureLoader {
  /** Set of all textures that are missing from the resource pack, to avoid logging twice */
  private static final Set<ResourceLocation> SKIPPED_TEXTURES = new HashSet<>();

  /** Clears all cached texture names */
  public static void clearCache() {
    SKIPPED_TEXTURES.clear();
  }

  /** Registers this manager */
  public static void init() {
    // clear cache on texture stitch, no longer need it then as its too late to lookup textures
    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TextureStitchEvent.Post.class, e -> clearCache());
  }

  /** Logs that a dynamic texture is missing, config option to disable */
  public static void logMissingTexture(ResourceLocation location) {
    if (!SKIPPED_TEXTURES.contains(location)) {
      SKIPPED_TEXTURES.add(location);
      log.debug("Skipping loading texture '{}' as it does not exist in the resource pack", location);
    }
  }

  /**
   * Gets a consumer to add textures to the given collection
   *
   * @param spriteGetter        Function mapping material names to sprites
   * @param logMissingTextures  If true, log textures that were not found
   * @return  Texture consumer
   */
  public static Predicate<Material> getTextureValidator(Function<Material,TextureAtlasSprite> spriteGetter, boolean logMissingTextures) {
    return mat -> {
      // either must be non-blocks, or must exist. We have fallbacks if it does not exist
      if (!MissingTextureAtlasSprite.getLocation().equals(spriteGetter.apply(mat).contents().name())) {
        return true;
      }
      if (logMissingTextures) {
        logMissingTexture(mat.texture());
      }
      return false;
    };
  }
}
