package slimeknights.tconstruct.library.client.data.spritetransformer;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.JsonUtils;

import javax.annotation.Nullable;

/**
 * Transformer that shifts the sprite by the given offset
 * @param base     Transformer to apply first. If null, will offset the untransformed image.
 * @param xOffset  Amount to shift the sprite in the X direction, positive means right.
 * @param yOffset  Amount to shift the sprite in the Y direction, positive means down.
 */
public record OffsettingSpriteTransformer(@Nullable ISpriteTransformer base, int xOffset, int yOffset) implements ISpriteTransformer {
  public static final ResourceLocation NAME = TConstruct.getResource("offsetting");

  @Override
  public int getFallbackColor() {
    return base != null ? base.getFallbackColor() : -1;
  }

  @Override
  public int getFrames() {
    return base != null ? base.getFrames() : 1;
  }

  @Override
  public void transform(NativeImage image, boolean allowAnimated) {
    if (base != null) {
      base.transform(image, allowAnimated);
    }
    int width = image.getWidth();
    int frames = allowAnimated && base != null ? base.getFrames() : 1;
    int height = image.getHeight() / frames;
    for (int f = 0; f < frames; f++) {
      int frameOffset = f * height;
      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          // when shifting in positive directions, need to ensure we set the positive most pixels first to prevent data loss
          // easier to swap here than swap the loop bounds conditionally
          int localX = x;
          int localY = y;
          if (xOffset > 0) {
            localX = width - x - 1;
          }
          if (yOffset > 0) {
            localY = height - y - 1;
          }
          // it might be nice to do a circular shift instead of setting empty locations to 0,
          // though that would require cloning the whole pixel array before shifting it over
          int getX = localX - xOffset;
          int getY = localY - yOffset;
          if (0 <= getX && getX < width && 0 <= getY && getY < height) {
            image.setPixelRGBA(localX, localY + frameOffset, image.getPixelRGBA(getX, getY));
          } else {
            image.setPixelRGBA(localX, localY + frameOffset, 0);
          }
        }
      }
    }
  }

  @Override
  public NativeImage copyImage(NativeImage image, boolean allowAnimated) {
    return base != null ? base.copyImage(image, allowAnimated) : ISpriteTransformer.copyImage(image);
  }

  @Nullable
  @Override
  public JsonObject animationMeta(NativeImage image) {
    if (base != null) {
      return base.animationMeta(image);
    }
    return null;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = JsonUtils.withType(NAME);
    if (base != null) {
      json.add("base", base.serialize(context));
    }
    json.addProperty("x", xOffset);
    json.addProperty("y", yOffset);
    return json;
  }

  /** Deserializer instance */
  public static final JsonDeserializer<OffsettingSpriteTransformer> DESERIALIZER = (element, type, context) -> {
    JsonObject json = element.getAsJsonObject();
    ISpriteTransformer base = null;
    if (json.has("base")) {
      base = SERIALIZER.deserialize(json.get("base"), ISpriteTransformer.class, context);
    }
    int xOffset = GsonHelper.getAsInt(json, "x", 0);
    int yOffset = GsonHelper.getAsInt(json, "y", 0);
    if (xOffset == 0 && yOffset == 0) {
      throw new JsonSyntaxException("Must have offset to use offsetting transformer");
    }
    return new OffsettingSpriteTransformer(base, xOffset, yOffset);
  };
}
