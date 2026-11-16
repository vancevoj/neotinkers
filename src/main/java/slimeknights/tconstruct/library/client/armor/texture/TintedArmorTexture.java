package slimeknights.tconstruct.library.client.armor.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.ArmorTexture;

/** Armor texture which tints the texture */
@AllArgsConstructor
@Accessors(fluent = true)
@RequiredArgsConstructor
@Setter
public class TintedArmorTexture implements ArmorTexture {
  private static final int MAX_LIGHT = LightCoordsUtil.pack(15, 15);

  private final Identifier texture;
  @Getter
  private int color = -1;
  @Getter
  private int luminosity = 0;

  public TintedArmorTexture(Identifier texture, int color) {
    this(texture, color, 0);
  }

  /** Applies luminosity to the given lightmap color. Assumes that {@code luminosity} is between 1 and 15. */
  public static int applyLuminosity(int packedLight, int luminosity) {
    // if full bright, skip some math
    if (luminosity >= 15) {
      return TintedArmorTexture.MAX_LIGHT;
    }
    // inlined version of methods from LightCoordsUtil
    return Math.max(luminosity, (packedLight & 0xFFFF) >> 4) << 4
      | Math.max(luminosity, packedLight >> 20 & 0xFFFF) << 20;
  }

  @Override
  public <S> void submit(Model<? super S> model, S state, PoseStack matrices, OrderedSubmitNodeCollector collector, int packedLight, int packedOverlay, int color, boolean hasGlint, int outlineColor) {
    // glint is part of the render type now instead of a separate foil buffer
    RenderType renderType = hasGlint ? RenderTypes.armorCutoutNoCullGlint(texture) : RenderTypes.armorCutoutNoCull(texture);
    if (luminosity > 0) {
      packedLight = applyLuminosity(packedLight, luminosity);
    }
    // the tint is read here, so suppliers updating the color of a shared instance (dyed) stay correct after submitting
    collector.submitModel(model, state, matrices, renderType, packedLight, packedOverlay, ARGB.multiply(color, this.color), null, outlineColor);
  }
}
