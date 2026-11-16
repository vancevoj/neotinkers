package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.tconstruct.smeltery.block.entity.GaugeBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

/** Renderer for the obisidian gauge block */
public class GaugeBlockEntityRenderer implements BlockEntityRenderer<GaugeBlockEntity, GaugeBlockEntityRenderer.State> {
  public GaugeBlockEntityRenderer(Context context) {}

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(GaugeBlockEntity tile, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
    state.sprites = null;
    state.fluids = List.of();
    List<FluidCuboid> fluids = FluidCuboid.REGISTRY.get(tile.getBlockState(), List.of());
    if (!fluids.isEmpty()) {
      ResourceHandler<FluidResource> tank = tile.getTank();
      if (tank.size() > 0) {
        FluidResource resource = tank.getResource(0);
        int amount = tank.getAmountAsInt(0);
        if (!resource.isEmpty() && amount > 0) {
          FluidStack fluid = resource.toStack(amount);
          state.fluids = List.copyOf(fluids);
          state.sprites = FluidSprites.of(fluid);
          state.fluidLight = FluidRenderer.getFluidLight(state.lightCoords, fluid);
        }
      }
    }
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    FluidSprites sprites = state.sprites;
    if (sprites != null) {
      List<FluidCuboid> fluids = state.fluids;
      int light = state.fluidLight;
      collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> FluidRenderer.renderCuboids(pose, buffer, fluids, sprites, light));
    }
  }

  /** Render state for gauges */
  public static class State extends BlockEntityRenderState {
    /** Fluid display locations */
    List<FluidCuboid> fluids = List.of();
    /** Sprites of the displayed fluid, null if nothing to render */
    @Nullable
    FluidSprites sprites;
    /** Light including the fluid light */
    int fluidLight;
  }
}
