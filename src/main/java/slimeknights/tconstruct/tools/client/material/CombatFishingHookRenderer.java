package slimeknights.tconstruct.tools.client.material;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.client.armor.texture.TintedArmorTexture;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.utils.SimpleCache;
import slimeknights.tconstruct.tools.entity.CombatFishingHook;

import javax.annotation.Nullable;
import java.util.Optional;

/** Renderer for {@link CombatFishingHookRenderer}. Mostly a recreation of {@link net.minecraft.client.renderer.entity.FishingHookRenderer} */
public class CombatFishingHookRenderer extends EntityRenderer<CombatFishingHook, CombatFishingHookRenderer.State> {
  /** Texture under the local folder */
  private static final Identifier LOCAL = TConstruct.getResource("fishing_hook/material");
  /** Easier to append material variants without root */
  private static final Identifier BASE = ArmorTextureSupplier.getTexturePath(LOCAL);

  /** Checks if the given texture is valid */
  @Nullable
  private static Identifier tryTexture(String material) {
    Identifier texture = LOCAL.withSuffix(material);
    if (ArmorTextureSupplier.TEXTURE_VALIDATOR.test(texture)) {
      return ArmorTextureSupplier.getTexturePath(texture);
    }
    return null;
  }

  /** Cache of texture and color for each material. */
  private static final SimpleCache<MaterialVariantId,MaterialTexture> TEXTURE_CACHE = new SimpleCache<>(material -> {
    if (!MaterialId.UNKNOWN.equals(material)) {
      Optional<MaterialRenderInfo> infoOptional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
      int color = -1;
      int luminosity = 0;
      if (infoOptional.isPresent()) {
        MaterialRenderInfo info = infoOptional.get();
        // first try untinted
        Identifier untinted = info.texture();
        luminosity = info.luminosity();
        if (untinted != null) {
          Identifier texture = tryTexture('_' + untinted.getNamespace() + '_' + untinted.getPath());
          if (texture != null) {
            return new MaterialTexture(texture, -1, luminosity);
          }
        }
        // fallback to tinted
        color = info.vertexColor();
        for (String fallback : info.fallbacks()) {
          Identifier texture = tryTexture('_' + fallback);
          if (texture != null) {
            return new MaterialTexture(texture, color, luminosity);
          }
        }
      }
      // tint base texture
      return new MaterialTexture(ArmorTextureSupplier.getTexturePath(LOCAL), color, luminosity);
    }
    return MaterialTexture.EMPTY;
  });

  public CombatFishingHookRenderer(Context context) {
    super(context);
  }

  /** Clears any cache in the renderer. */
  public static void clearCache() {
    TEXTURE_CACHE.clear();
  }

  @Override
  public boolean shouldRender(CombatFishingHook entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
    return super.shouldRender(entity, culler, camX, camY, camZ, partialTicks) && entity.getPlayerOwner() != null;
  }

  @Override
  protected boolean affectedByCulling(CombatFishingHook entity) {
    return false;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  /** Gets the position of the hand holding the rod, same as vanilla {@link FishingHookRenderer} */
  private Vec3 getPlayerHandPos(Player owner, float swing, float partialTicks) {
    // handle hand side
    int sideOffset = FishingHookRenderer.getHoldingArm(owner) == HumanoidArm.RIGHT ? 1 : -1;
    if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && owner == Minecraft.getInstance().player) {
      float fov = this.entityRenderDispatcher.options.fov().get().intValue();
      double viewBobbingScale = 960.0 / fov;
      Vec3 viewVec = this.entityRenderDispatcher.camera
        .getNearPlane(fov)
        .getPointOnPlane(sideOffset * 0.525F, -0.1F)
        .scale(viewBobbingScale)
        .yRot(swing * 0.5F)
        .xRot(-swing * 0.7F);
      return owner.getEyePosition(partialTicks).add(viewVec);
    } else {
      float ownerYRot = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * (float) (Math.PI / 180.0);
      double sin = Mth.sin(ownerYRot);
      double cos = Mth.cos(ownerYRot);
      float playerScale = owner.getScale();
      double rightOffset = sideOffset * 0.35 * playerScale;
      double forwardOffset = 0.8 * playerScale;
      float yOffset = owner.isCrouching() ? -0.1875F : 0.0F;
      return owner.getEyePosition(partialTicks).add(-cos * rightOffset - sin * forwardOffset, yOffset - 0.45 * playerScale, -sin * rightOffset + cos * forwardOffset);
    }
  }

  @Override
  public void extractRenderState(CombatFishingHook hook, State state, float partialTicks) {
    super.extractRenderState(hook, state, partialTicks);
    Player player = hook.getPlayerOwner();
    if (player == null) {
      state.lineOriginOffset = Vec3.ZERO;
      state.texture = MaterialTexture.EMPTY;
    } else {
      // select material
      state.texture = TEXTURE_CACHE.apply(hook.getMaterial());
      // prepare string rendering
      float swing = Mth.sin(Mth.sqrt(player.getSwingAnimation(partialTicks)) * (float) Math.PI);
      Vec3 playerPos = this.getPlayerHandPos(player, swing, partialTicks);
      Vec3 hookPos = hook.getPosition(partialTicks).add(0.0, 0.25, 0.0);
      state.lineOriginOffset = playerPos.subtract(hookPos);
    }
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    // setup rendering
    poseStack.pushPose();
    poseStack.pushPose();
    poseStack.scale(0.5F, 0.5F, 0.5F);
    poseStack.rotate(camera.orientation);

    // render bobber
    MaterialTexture texture = state.texture;
    int bobberLight = texture.applyLuminosity(state.lightCoords);
    collector.submitCustomGeometry(poseStack, texture.renderType(), (pose, buffer) -> {
      texture.vertex(buffer, pose, bobberLight, 0f, 0, 0, 1);
      texture.vertex(buffer, pose, bobberLight, 1f, 0, 1, 1);
      texture.vertex(buffer, pose, bobberLight, 1f, 1, 1, 0);
      texture.vertex(buffer, pose, bobberLight, 0f, 1, 0, 0);
    });
    poseStack.popPose();

    // render the string
    float xa = (float)state.lineOriginOffset.x;
    float ya = (float)state.lineOriginOffset.y;
    float za = (float)state.lineOriginOffset.z;
    float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
    collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
      for (int i = 0; i < 16; i++) {
        float a0 = i / 16f;
        float a1 = (i + 1) / 16f;
        stringVertex(xa, ya, za, buffer, pose, a0, a1, width);
        stringVertex(xa, ya, za, buffer, pose, a1, a0, width);
      }
    });

    poseStack.popPose();
    super.submit(state, poseStack, collector, camera);
  }

  /** Adds a vertex for the fishing line, copy of the private vanilla {@link FishingHookRenderer} method */
  private static void stringVertex(float xa, float ya, float za, VertexConsumer stringBuffer, PoseStack.Pose stringPose, float aa, float nexta, float width) {
    float x = xa * aa;
    float y = ya * (aa * aa + aa) * 0.5F + 0.25F;
    float z = za * aa;
    float nx = xa * nexta - x;
    float ny = ya * (nexta * nexta + nexta) * 0.5F + 0.25F - y;
    float nz = za * nexta - z;
    float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
    nx /= length;
    ny /= length;
    nz /= length;
    stringBuffer.addVertex(stringPose, x, y, z).setColor(0xFF000000).setNormal(stringPose, nx, ny, nz).setLineWidth(width);
  }

  /** Render state for combat fishing hooks */
  public static class State extends EntityRenderState {
    /** Offset from the hook to the player hand */
    public Vec3 lineOriginOffset = Vec3.ZERO;
    /** Material texture for the bobber */
    public MaterialTexture texture = MaterialTexture.EMPTY;
  }

  public record MaterialTexture(RenderType renderType, int luminosity, int alpha, int red, int green, int blue) {
    public static final MaterialTexture EMPTY = new MaterialTexture(BASE, -1, 0);

    public MaterialTexture(Identifier texture, int color, int luminosity) {
      this(RenderTypes.entityCutoutCull(texture), luminosity,
        color >> 24 & 255,
        color >> 16 & 255,
        color >> 8 & 255,
        color & 255
      );
    }

    /** Applies luminosity to the given lightmap color */
    public int applyLuminosity(int packedLight) {
      if (luminosity > 0) {
        return TintedArmorTexture.applyLuminosity(packedLight, luminosity);
      }
      return packedLight;
    }

    /** Draws a vertex using this texture. */
    public void vertex(VertexConsumer consumer, PoseStack.Pose pose, int lightmap, float pX, int pY, int pU, int pV) {
      consumer.addVertex(pose, pX - 0.5f, pY - 0.5f, 0f)
        .setColor(red, green, blue, alpha)
        .setUv(pU, pV)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(lightmap)
        .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
  }
}
