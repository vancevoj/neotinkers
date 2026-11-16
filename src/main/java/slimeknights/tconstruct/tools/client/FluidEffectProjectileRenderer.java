package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.tools.entity.FluidEffectProjectile;

import javax.annotation.Nullable;
import java.util.List;

public class FluidEffectProjectileRenderer extends EntityRenderer<FluidEffectProjectile, FluidEffectProjectileRenderer.State> {
  private final List<FluidCuboid> fluids;
  public FluidEffectProjectileRenderer(Context context) {
    super(context);
    this.fluids = List.of(
      FluidCuboid.builder().from(-4,  0,  0).to(-2,  2,  2).build(),
      FluidCuboid.builder().from( 0, -4,  0).to( 2, -2,  2).build(),
      FluidCuboid.builder().from( 0,  0, -4).to( 2,  2, -2).build(),
      FluidCuboid.builder().from( 2,  0,  0).to( 4,  2,  2).build(),
      FluidCuboid.builder().from( 0,  0,  0).to( 2,  4,  2).build(),
      FluidCuboid.builder().from( 0,  0,  2).to( 2,  2,  4).build());
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(FluidEffectProjectile entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.yRot = entity.getYRot(partialTicks);
    state.xRot = entity.getXRot(partialTicks);
    FluidStack fluid = entity.getFluid();
    if (fluid.isEmpty()) {
      state.sprites = null;
    } else {
      state.sprites = FluidSprites.of(fluid);
      state.fluidLight = FluidRenderer.getFluidLight(state.lightCoords, fluid);
    }
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    FluidSprites sprites = state.sprites;
    if (sprites != null) {
      poseStack.pushPose();
      poseStack.translate(0.0D, 0.15F, 0.0D);
      poseStack.rotate(Axis.YP.rotationDegrees(state.yRot - 90.0F));
      poseStack.rotate(Axis.ZP.rotationDegrees(state.xRot));
      List<FluidCuboid> fluids = this.fluids;
      int light = state.fluidLight;
      collector.submitCustomGeometry(poseStack, MantleRenderTypes.FLUID, (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, fluids, sprites, light));
      poseStack.popPose();
    }
    super.submit(state, poseStack, collector, camera);
  }

  /** Render state for fluid projectiles */
  public static class State extends EntityRenderState {
    /** Interpolated rotation */
    public float xRot, yRot;
    /** Fluid sprites and color, null if no fluid */
    @Nullable
    public FluidSprites sprites;
    /** Light including the fluid light */
    public int fluidLight;
  }
}
