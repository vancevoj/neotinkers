package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

/** Sprite source creating modifier textures for banners using shield banner textures */
public record ShieldBannerModifierSpriteSource(int cropX, int cropY, int cropWidth, int cropHeight, Identifier destinationPrefix, int offsetX, int offsetY, int outSize) implements SpriteSource {
  private static final Codec<Integer> NON_NEGATIVE = ExtraCodecs.intRange(0, Integer.MAX_VALUE);
  private static final Codec<Integer> SHIELD_SIZE = ExtraCodecs.intRange(0, 64);
  public static final MapCodec<ShieldBannerModifierSpriteSource> CODEC = RecordCodecBuilder.<ShieldBannerModifierSpriteSource>mapCodec(inst -> inst.group(
    SHIELD_SIZE.fieldOf("crop_x").forGetter(ShieldBannerModifierSpriteSource::cropX),
    SHIELD_SIZE.fieldOf("crop_y").forGetter(ShieldBannerModifierSpriteSource::cropY),
    SHIELD_SIZE.fieldOf("crop_width").forGetter(ShieldBannerModifierSpriteSource::cropWidth),
    SHIELD_SIZE.fieldOf("crop_height").forGetter(ShieldBannerModifierSpriteSource::cropHeight),
    Identifier.CODEC.fieldOf("destination_prefix").forGetter(ShieldBannerModifierSpriteSource::destinationPrefix),
    NON_NEGATIVE.fieldOf("offset_x").forGetter(ShieldBannerModifierSpriteSource::offsetX),
    NON_NEGATIVE.fieldOf("offset_y").forGetter(ShieldBannerModifierSpriteSource::offsetY),
    NON_NEGATIVE.fieldOf("output_size").forGetter(ShieldBannerModifierSpriteSource::outSize)
  ).apply(inst, ShieldBannerModifierSpriteSource::new)).validate(source -> {
    if (source.cropX + source.cropWidth >= 64 || source.cropY + source.cropHeight >= 64) {
      return DataResult.error(() -> "Invalid banner shield modifier sprite source: crop region must be within 64 by 64");
    } else if (source.offsetX + source.cropWidth >= source.outSize || source.offsetY + source.cropHeight >= source.outSize) {
      return DataResult.error(() -> "Invalid banner shield modifier sprite source: crop result must be placed within output size " + source.outSize);
    }
    return DataResult.success(source);
  });
  /** ID of this sprite source in atlas JSON */
  public static final Identifier ID = TConstruct.getResource("shield_banner_to_modifier");
  /** Folder containing the vanilla shield pattern textures, same folder the vanilla shield patterns atlas lists */
  private static final FileToIdConverter SHIELD_TEXTURES = new FileToIdConverter("textures/entity/shield", ".png");
  /** Textures in the shield folder that are not banner patterns */
  private static final Set<String> NOT_PATTERNS = Set.of("shield_base", "shield_base_nopattern");
  /** Set once registered to prevent duplicate registration */
  private static boolean registered = false;

  /**
   * Registers this sprite source codec.
   * Must be called from {@link RegisterSpriteSourcesEvent} (mod bus, client only).
   */
  @Internal
  public static void register(RegisterSpriteSourcesEvent event) {
    if (!registered) {
      registered = true;
      event.register(ID, CODEC);
    }
  }

  @Override
  public void run(ResourceManager manager, Output output) {
    // banner patterns are datapack controlled, so find the shield pattern textures with a folder search like the vanilla shield patterns atlas
    for (Entry<Identifier, Resource> entry : SHIELD_TEXTURES.listMatchingResources(manager).entrySet()) {
      Identifier input = entry.getKey();
      // key matches the banner pattern asset ID, e.g. minecraft:stripe_bottom
      Identifier pattern = SHIELD_TEXTURES.fileToId(input);
      if (!NOT_PATTERNS.contains(pattern.getPath())) {
        LazyLoadedImage image = new LazyLoadedImage(input, entry.getValue(), 1);
        Identifier destination = destinationPrefix.withSuffix(MaterialRenderInfo.getSuffix(pattern));
        output.add(destination, new BannerModifierSpriteSupplier(image, input, destination));
      }
    }
  }

  @Override
  public MapCodec<ShieldBannerModifierSpriteSource> codec() {
    return CODEC;
  }

  /** Generates a cropped sprite lazily */
  @RequiredArgsConstructor
  private class BannerModifierSpriteSupplier implements DiscardableLoader {
    private final LazyLoadedImage original;
    private final Identifier input, output;

    @Nullable
    @Override
    public SpriteContents get(SpriteResourceLoader loader) {
      try {
        // its possible the original is bigger than we expect due to HD pack, if so scale it accordingly
        // we only support scaling if it is a multiple of width
        NativeImage original = this.original.get();
        int scale = original.getWidth() / 64;
        if (scale == 0) {
          TConstruct.LOG.warn("Unable to crop {} to produce {} as texture size is less than 64", input, output);
        } else {
          NativeImage generated = new NativeImage(outSize * scale, outSize * scale, true);
          original.copyRect(generated, cropX * scale, cropY * scale, offsetX * scale, offsetY * scale, cropWidth * scale, cropHeight * scale, false, false);
          return new SpriteContents(this.output, new FrameSize(generated.getWidth(), generated.getHeight()), generated);
        }
      } catch (IllegalArgumentException | IOException ex) {
        TConstruct.LOG.warn("Unable to crop {} to produce {}", this.input, this.output, ex);
      } finally {
        this.original.release();
      }
      return null;
    }

    @Override
    public void discard() {
      this.original.release();
    }
  }
}
