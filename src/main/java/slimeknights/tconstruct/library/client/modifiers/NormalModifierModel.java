package slimeknights.tconstruct.library.client.modifiers;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Default modifier model loader, loads a single texture from the standard path
 */
@RequiredArgsConstructor
public class NormalModifierModel implements IBakedModifierModel {
  /** Constant unbaked model instance, as they are all the same */
  public static final IUnbakedModifierModel UNBAKED_INSTANCE = new Unbaked(-1, 0);

  /** Textures to show */
  @Nullable
  private final Material small;
  @Nullable
  private final Material large;
  /** Color to apply to the texture */
  private final int color;
  /** Luminosity to apply to the texture */
  private final int luminosity;

  public NormalModifierModel(@Nullable Material smallTexture, @Nullable Material largeTexture) {
    this(smallTexture, largeTexture, -1, 0);
  }

  @Override
  public void addQuads(IToolStackView tool, ModifierEntry entry, Function<Material,TextureAtlasSprite> spriteGetter, Transformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
    Material spriteName = isLarge ? large : small;
    if (spriteName != null) {
      quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(color, -1, spriteGetter.apply(spriteName), transforms, luminosity, pixels));
    }
  }

  private record Unbaked(int color, int luminosity) implements IUnbakedModifierModel {
    @Nullable
    @Override
    public IBakedModifierModel forTool(Function<String,Material> smallGetter, Function<String,Material> largeGetter) {
      Material smallTexture = smallGetter.apply("");
      Material largeTexture = largeGetter.apply("");
      if (smallTexture != null || largeTexture != null) {
        return new NormalModifierModel(smallTexture, largeTexture, color, luminosity);
      }
      return null;
    }

    @Override
    public IUnbakedModifierModel configure(JsonObject data) {
      // parse the two keys, if we ended up with something new create an instance
      int color = ColorLoadable.ALPHA.getOrDefault(data, "color", -1);
      int luminosity = GsonHelper.getAsInt(data, "luminosity");
      if (color != this.color || luminosity != this.luminosity) {
        return new Unbaked(color, luminosity);
      }
      return this;
    }
  }
}
