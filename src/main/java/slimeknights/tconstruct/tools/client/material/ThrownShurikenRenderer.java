package slimeknights.tconstruct.tools.client.material;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;
import slimeknights.tconstruct.tools.entity.ToolProjectile;

/** Renderer for {@link ThrownShuriken} */
public class ThrownShurikenRenderer<T extends Projectile & ToolProjectile> extends EntityRenderer<T, ThrownShurikenRenderer.State> {
  private final ItemModelResolver itemModelResolver;
  public ThrownShurikenRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemModelResolver = context.getItemModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    // hide the shuriken for the first two ticks when close to the camera, so it does not cover the screen when thrown
    Camera camera = this.entityRenderDispatcher.camera;
    Entity cameraEntity = camera != null ? camera.entity() : null;
    state.visible = entity.tickCount >= 2 || cameraEntity == null || !(cameraEntity.distanceToSqr(entity) < 12.25D);
    state.yRot = entity.getYRot(partialTicks);
    // TODO: custom display properties?
    this.itemModelResolver.updateForNonLiving(state.item, entity.getDisplayTool(), ItemDisplayContext.GROUND, entity);
  }

  @Override
  public void submit(State state, PoseStack matrixStackIn, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.visible) {
      matrixStackIn.pushPose();
      matrixStackIn.rotate(Axis.YP.rotationDegrees(state.yRot + 90));
      matrixStackIn.rotate(Axis.ZP.rotationDegrees(state.ageInTicks * 30 % 360));
      matrixStackIn.translate(-0.03125, -0.09375, 0);
      state.item.submit(matrixStackIn, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      matrixStackIn.popPose();
      super.submit(state, matrixStackIn, collector, camera);
    }
  }

  /** Render state for thrown shuriken */
  public static class State extends EntityRenderState {
    /** If false, the shuriken is too close to the camera to render */
    public boolean visible;
    /** Interpolated Y rotation */
    public float yRot;
    /** Resolved shuriken item */
    public final ItemStackRenderState item = new ItemStackRenderState();
  }
}
