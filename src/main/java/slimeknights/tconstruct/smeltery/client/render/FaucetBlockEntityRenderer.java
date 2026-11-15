package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FaucetFluid;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity, FaucetBlockEntityRenderer.State> {
  public FaucetBlockEntityRenderer(Context context) {}

  @Override
  public AABB getRenderBoundingBox(FaucetBlockEntity tileEntity) {
    BlockPos pos = tileEntity.getBlockPos();
    return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(FaucetBlockEntity tileEntity, State state, float partialTicks, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(tileEntity, state, partialTicks, cameraPosition, breakProgress);
    state.sprites = null;
    FluidStack renderFluid = tileEntity.getRenderFluid();
    if (!tileEntity.isPouring() || renderFluid.isEmpty()) {
      return;
    }

    // safety
    Level world = tileEntity.getLevel();
    if (world == null) {
      return;
    }

    // fetch faucet model to determine where to render fluids
    BlockState blockState = tileEntity.getBlockState();
    List<FluidCuboid> fluids = FluidCuboid.REGISTRY.get(blockState, List.of());
    if (!fluids.isEmpty()) {
      // if side, rotate fluid model
      Direction direction = blockState.getValue(FaucetBlock.FACING);
      state.direction = direction;
      state.fluids = List.copyOf(fluids);
      // fluid props
      state.sprites = FluidSprites.of(renderFluid);
      state.fluidLight = FluidRenderer.getFluidLight(state.lightCoords, renderFluid);
      // fluids in the block(s) below
      state.below = getFaucetFluids(world, tileEntity.getBlockPos(), direction);
    }
  }

  @Override
  public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    FluidSprites sprites = state.sprites;
    if (sprites != null) {
      // if side, rotate fluid model
      boolean isRotated = RenderingHelper.applyRotation(matrices, state.direction);

      // render all cubes in the model, then into the block(s) below
      List<FluidCuboid> fluids = state.fluids;
      List<FaucetSegment> below = state.below;
      int light = state.fluidLight;
      collector.submitCustomGeometry(matrices, MantleRenderTypes.FLUID, (pose, buffer) -> {
        FluidRenderer.renderCuboids(pose, buffer, fluids, sprites, light);
        renderFaucetFluids(pose, buffer, below, sprites, light);
      });

      // if rotated, pop back rotation
      if(isRotated) {
        matrices.popPose();
      }
    }
  }


  /* Faucet fluids */

  /**
   * Fluids to render in a block below a faucet or channel
   * @param offset  Number of blocks below
   * @param cubes   Cubes to render
   */
  public record FaucetSegment(int offset, List<FluidCuboid> cubes) {}

  /**
   * Finds the fluids to render below a faucet, same logic as {@link RenderingHelper#renderFaucetFluids(LevelAccessor, BlockPos, Direction, PoseStack.Pose, VertexConsumer, net.minecraft.client.renderer.texture.TextureAtlasSprite, net.minecraft.client.renderer.texture.TextureAtlasSprite, int, int)}.
   * Called during extraction so the level is not accessed while rendering.
   * @param world      World instance
   * @param pos        Base position
   * @param direction  Direction of the flow
   * @return  List of segments to render
   */
  public static List<FaucetSegment> getFaucetFluids(LevelAccessor world, BlockPos pos, Direction direction) {
    List<FaucetSegment> segments = new ArrayList<>();
    int i = 0;
    FaucetFluid faucetFluid;
    do {
      // get the faucet data for the block
      i++;
      faucetFluid = FaucetFluid.REGISTRY.get(world.getBlockState(pos.below(i)));
      // if nothing to render, skip, but allow the next iteration of the loop on the off chance they continue
      List<FluidCuboid> cubes = faucetFluid.getFluids(direction);
      if (!cubes.isEmpty()) {
        segments.add(new FaucetSegment(i, List.copyOf(cubes)));
      }
    } while (faucetFluid.isContinued());
    return List.copyOf(segments);
  }

  /**
   * Renders fluids below a faucet
   * @param pose      Pose from the custom geometry renderer
   * @param buffer    Vertex consumer for {@link MantleRenderTypes#FLUID}
   * @param segments  Segments from {@link #getFaucetFluids(LevelAccessor, BlockPos, Direction)}
   * @param sprites   Fluid sprites and color
   * @param light     Fluid light
   */
  public static void renderFaucetFluids(PoseStack.Pose pose, VertexConsumer buffer, List<FaucetSegment> segments, FluidSprites sprites, int light) {
    for (FaucetSegment segment : segments) {
      // render all down cubes with the given offset
      PoseStack.Pose offset = pose.copy();
      offset.translate(0, -segment.offset(), 0);
      FluidRenderer.renderCuboids(offset, buffer, segment.cubes(), sprites, light);
    }
  }

  /** Render state for faucets */
  public static class State extends BlockEntityRenderState {
    /** Faucet direction */
    Direction direction = Direction.NORTH;
    /** Fluid display locations in the faucet */
    List<FluidCuboid> fluids = List.of();
    /** Fluids in the blocks below */
    List<FaucetSegment> below = List.of();
    /** Fluid sprites and color, null if nothing to render */
    @Nullable
    FluidSprites sprites;
    /** Light including the fluid light */
    int fluidLight;
  }
}
