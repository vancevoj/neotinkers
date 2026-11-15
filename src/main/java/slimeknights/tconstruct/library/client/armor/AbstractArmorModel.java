package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ARGB;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.ArmorTexture;

import javax.annotation.Nullable;

/**
 * Common shared logic for material armor models.
 * <p>
 * 26.3: this is no longer a {@link net.minecraft.client.model.Model}. Models can no longer draw several textures themselves
 * ({@code Model#renderToBuffer} is final and models are drawn later from a render state), so armor is submitted by
 * {@link TinkerArmorLayer} through {@link ArmorModelManager.ArmorModelDispatcher#submitArmor}, which submits the vanilla armor
 * model once per texture layer.
 */
public abstract class AbstractArmorModel {
  /** Wings model to render */
  @Nullable
  private static ElytraModel wingsModel;
  /** Wings model to render on babies */
  @Nullable
  private static ElytraModel babyWingsModel;

  /** Combines the given ARGB colors, -1 meaning no tint for either. Replaces the old float channel multiplication in {@code renderColored} */
  public static int multiplyColor(int color, int tint) {
    return ARGB.multiply(color, tint);
  }

  /** Gets the registry access for texture suppliers, as the armor hooks no longer receive the entity */
  public static RegistryAccess getRegistryAccess() {
    ClientLevel level = Minecraft.getInstance().level;
    return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
  }

  /**
   * Submits the wings layer
   * @param matrices       Pose stack
   * @param collector      Ordered collector for this layer
   * @param state          Entity state, poses the wings
   * @param packedLight    Light coordinates
   * @param packedOverlay  Overlay coordinates
   * @param texture        Texture to submit
   * @param color          ARGB color multiplier
   * @param hasGlint       If true, draws the enchantment glint
   */
  protected static void submitWings(PoseStack matrices, OrderedSubmitNodeCollector collector, HumanoidRenderState state, int packedLight, int packedOverlay, ArmorTexture texture, int color, boolean hasGlint) {
    matrices.pushPose();
    matrices.translate(0.0D, 0.0D, 0.125D);
    texture.submit(getWings(state.isBaby), state, matrices, collector, packedLight, packedOverlay, color, hasGlint, state.outlineColor);
    matrices.popPose();
  }


  /* Helpers */

  /**
   * Formerly registered listeners to capture the buffer from the render living event.
   * @deprecated Buffers no longer exist in 26.3, all drawing goes through the collector passed to {@link TinkerArmorLayer}. Does nothing.
   */
  @Deprecated(forRemoval = true)
  public static void init() {}

  /** Gets or creates the elytra model */
  private static ElytraModel getWings(boolean baby) {
    if (baby) {
      if (babyWingsModel == null) {
        babyWingsModel = new ElytraModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA_BABY));
      }
      return babyWingsModel;
    }
    if (wingsModel == null) {
      wingsModel = new ElytraModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA));
    }
    return wingsModel;
  }
}
