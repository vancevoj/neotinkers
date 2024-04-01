package slimeknights.tconstruct.common.data.model;

import com.google.common.math.IntMath;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.data.GenericTextureGenerator;
import slimeknights.tconstruct.library.client.data.util.DataGenSpriteReader;
import slimeknights.tconstruct.shared.block.SlimeType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Provides textures used in general models
 */
public class ModelSpriteProvider extends GenericTextureGenerator {
  private final DataGenSpriteReader spriteReader;
  public ModelSpriteProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
    super(generator, existingFileHelper, "textures");
    spriteReader = new DataGenSpriteReader(existingFileHelper, "textures");
  }

  @Override
  public void run(CachedOutput cache) throws IOException {
    ResourceLocation rootsSide = getResource("block/wood/enderbark/roots");
    ResourceLocation rootsTop = getResource("block/wood/enderbark/roots_top");

    // generate slimy root textures
    for (SlimeType slime : SlimeType.values()) {
      String name = slime.getSerializedName();
      ResourceLocation congealed = getResource("block/slime/storage/congealed_" + name);
      stackSprites(cache, getResource("block/wood/enderbark/roots/" + name), rootsSide, congealed);
      stackSprites(cache, getResource("block/wood/enderbark/roots/" + name + "_top"), rootsTop, congealed);
    }

    spriteReader.closeAll();
  }

  /** Gets the LCM of two ints */
  private static int lcm(int a, int b){
    return a * (b / IntMath.gcd(a, b));
  }

  /**
   * Generates a sprite by stacking each of the inputs
   * @param cache    Output cache
   * @param output   Output path
   * @param inputs   List of inputs, will iterate from 0 to the end and grab the first non-transparent pixel
   */
  protected void stackSprites(CachedOutput cache, ResourceLocation output, ResourceLocation... inputs) {
    List<NativeImage> sprites = Arrays.stream(inputs).map(path -> {
      try {
        return spriteReader.read(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).toList();
    // figure out output dimensions, we expect same width
    // height can vary, as long as all either have the same height or the mismatching heights
    // TODO: we could just use LCM to generate an image that merges all
    int width = 1;
    int height = 1;
    ResourceLocation metaLocation = null;
    for (int i = 0; i < sprites.size(); i++) {
      NativeImage sprite = sprites.get(i);
      width = lcm(width, sprite.getWidth());
      height = lcm(height, sprite.getHeight());
      ResourceLocation location = inputs[i];
      // TODO: metadata may be wrong if we have multiple sprites with different frame counts
      if (spriteReader.metadataExists(location)) {
        if (metaLocation == null) {
          metaLocation = location;
        } else {
          throw new IllegalStateException("Multiple sprites have metadata, this will not work, found " + metaLocation + " and " + location);
        }
      }
    }
    // build the output sprite
    NativeImage generated = spriteReader.create(width, height);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int color = 0;
        // locate the first sprite with a non-zero alpha value and copy the color
        for (NativeImage sprite : sprites) {
          // tile the sprite if its smaller than the output, lets you merge multiple animations
          int spriteColor = sprite.getPixelRGBA(x % sprite.getHeight(), y % sprite.getHeight());
          if (NativeImage.getA(spriteColor) != 0) {
            // TODO: this does not merge alpha, though will we ever need that?
            color = spriteColor;
            break;
          }
        }
        generated.setPixelRGBA(x, y, color);
      }
    }
    saveImage(cache, output, generated);
    if (metaLocation != null) {
      try {
        saveMetadata(cache, output, spriteReader.readMetadata(output));
      } catch (IOException e) {
        TConstruct.LOG.error("Failed to save sprite metadata", e);
      }
    }
  }

  @Override
  public String getName() {
    return "Tinkers' Construct model sprite provider";
  }
}
