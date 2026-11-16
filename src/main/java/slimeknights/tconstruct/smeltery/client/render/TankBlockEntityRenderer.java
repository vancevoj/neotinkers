package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.RenderUtils.FluidTankInfo;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class TankBlockEntityRenderer<T extends BlockEntity & ITankBlockEntity> implements BlockEntityRenderer<T, TankBlockEntityRenderer.State> {
  public TankBlockEntityRenderer(Context context) {}

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(T tile, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
    state.fluids = List.of();
    state.tank = null;
    if (Config.CLIENT.tankFluidModel.get()) {
      return;
    }
    // render the fluid
    List<FluidCuboid> fluids = FluidCuboid.REGISTRY.get(tile.getBlockState(), List.of());
    if (!fluids.isEmpty()) {
      state.fluids = List.copyOf(fluids);
      // updates the animated render offset of the tank
      state.tank = FluidTankInfo.of(tile.getTank(), partialTicks, state.lightCoords, true);
    }
  }

  @Override
  public void submit(State state, PoseStack matrixStack, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.tank != null) {
      state.tank.submit(matrixStack, collector, state.fluids);
    }
  }

  /** Render state for tanks */
  public static class State extends BlockEntityRenderState {
    /** Fluid display locations */
    List<FluidCuboid> fluids = List.of();
    /** Tank contents, null if nothing to render */
    @Nullable
    FluidTankInfo tank;
  }
}
